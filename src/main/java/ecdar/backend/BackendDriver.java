package ecdar.backend;

import EcdarProtoBuf.ComponentProtos;
import EcdarProtoBuf.EcdarBackendGrpc;
import EcdarProtoBuf.QueryProtos;
import com.google.protobuf.Empty;
import ecdar.Ecdar;
import ecdar.abstractions.BackendInstance;
import ecdar.abstractions.Component;
import ecdar.abstractions.QueryState;
import io.grpc.*;
import io.grpc.stub.StreamObserver;
import javafx.util.Pair;
import org.springframework.util.SocketUtils;

import java.io.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class BackendDriver {
    private final List<BackendConnection> openBackendConnections = new CopyOnWriteArrayList<>();
    private final int deadlineForResponses = 20000;
    private final int rerunQueryDelay = 200;

    public BackendDriver() {
    }

    public void addQueryToExecutionQueue(String query, BackendInstance backend, Consumer<Boolean> success, Consumer<BackendException> failure, QueryListener queryListener) {
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                new ExecutableQuery(query, backend, success, failure, queryListener).execute();
            }
        }, rerunQueryDelay);
    }

    public Pair<ArrayList<String>, ArrayList<String>> getInputOutputs(String query) {
        if (!query.startsWith("refinement")) {
            return null;
        }

        // ToDo NIELS: Reimplement with backend connection

//        // Pair is used as a tuple, not a key-value pair
//        Pair<ArrayList<String>, ArrayList<String>> inputOutputs = new Pair<>(new ArrayList<>(), new ArrayList<>());
//
//        ProcessBuilder pb = new ProcessBuilder("src/Reveaal", "-c", Ecdar.projectDirectory.get(), query.replaceAll("\\s", ""));
//        pb.redirectErrorStream(true);
//        try {
//            //Start the j-Ecdar process
//            Process ReveaalEngineInstance = pb.start();
//
//            //Communicate with the j-Ecdar process
//            try (var ReveaalReader = new BufferedReader(new InputStreamReader(ReveaalEngineInstance.getInputStream()))) {
//                //Read the result of the query from the j-Ecdar process
//                String line;
//                while ((line = ReveaalReader.readLine()) != null) {
//                    // Process the query result
//                    if (line.endsWith("extra inputs")){
//                        Matcher m = Pattern.compile("[\"]([^\"]+)[\"]").matcher(line);
//                        while(m.find()){
//                            inputOutputs.getKey().add(m.group(1));
//                        }
//                    } else if (line.startsWith("extra outputs")) {
//                        Matcher m = Pattern.compile("[\"]([^\"]+)[\"]").matcher(line);
//                        while(m.find()){
//                            inputOutputs.getValue().add(m.group(1));
//                        }
//                    }
//                }
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
        return null; //inputOutputs;
    }

    /**
     * Close all open backend connection
     *
     * @throws IOException if any of the sockets do not respond
     */
    public void closeAllBackendConnection() throws IOException {
        for (BackendConnection s : openBackendConnections) s.close();
    }

    private void executeQuery(ExecutableQuery executableQuery) {
        if (executableQuery.queryListener.getQuery().getQueryState() == QueryState.UNKNOWN) return;

        // Get available connection or start new
        final BackendConnection backendConnection = openBackendConnections.stream()
                .filter((connection) -> connection.getBackendInstance().equals(executableQuery.backend))
                .findFirst()
                .orElseGet(() -> startNewBackendConnection(executableQuery.backend));

        // If the connection is null, there are no available sockets
        // and the specified port range is occupied. Schedule a rerun of the query
        if (backendConnection == null) {
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    executableQuery.execute();
                }
            }, rerunQueryDelay);
            return;
        }

        backendConnection.setExecutableQuery(executableQuery);

        QueryProtos.ComponentsUpdateRequest.Builder componentsBuilder = QueryProtos.ComponentsUpdateRequest.newBuilder();
        for (Component c : Ecdar.getProject().getComponents()) {
            componentsBuilder.addComponents(ComponentProtos.Component.newBuilder().setJson(c.serialize().toString()).build());
        }

        StreamObserver<Empty> observer = new StreamObserver<>() {
            private boolean error = false;

            @Override
            public void onNext(Empty value) {
                System.out.println("Response to Connection!");
            }

            @Override
            public void onError(Throwable t) {
                if (executableQuery.queryListener.getQuery().getQueryState() == QueryState.UNKNOWN)
                    backendConnection.setExecutableQuery(null);
                else {
                    handleBackendError(t, backendConnection);
                    error = true;
                }
            }

            @Override
            public void onCompleted() {
                if (!error) {
                    StreamObserver<EcdarProtoBuf.QueryProtos.QueryResponse> responseObserver = new StreamObserver<>() {
                        @Override
                        public void onNext(QueryProtos.QueryResponse value) {
                            if (executableQuery.queryListener.getQuery().getQueryState() == QueryState.UNKNOWN) {
                                backendConnection.setExecutableQuery(null);
                                return;
                            }
                            handleResponse(backendConnection.getExecutableQuery(), value);
                        }

                        @Override
                        public void onError(Throwable t) {
                            handleBackendError(t, backendConnection);
                        }

                        @Override
                        public void onCompleted() {
                            backendConnection.setExecutableQuery(null);
                        }
                    };
                    backendConnection.getStub().withDeadlineAfter(deadlineForResponses, TimeUnit.MILLISECONDS).sendQuery(QueryProtos.Query.newBuilder().setId(0).setQuery(backendConnection.getExecutableQuery().query).build(), responseObserver);
                }
            }
        };

        backendConnection.getStub().withDeadlineAfter(deadlineForResponses, TimeUnit.MILLISECONDS).updateComponents(componentsBuilder.build(), observer);
    }

    private void handleBackendError(Throwable t, BackendConnection backendConnection) {
        // Each error starts with a capitalized description of the error equal to the gRPC error type encountered
        String errorType = t.getMessage().split(":\\s+", 2)[0];
        final ExecutableQuery query = backendConnection.getExecutableQuery();

        switch (errorType) {
            case "CANCELLED":
                backendConnection.getExecutableQuery().queryListener.getQuery().setQueryState(QueryState.SYNTAX_ERROR);
                backendConnection.getExecutableQuery().failure.accept(new BackendException.QueryErrorException("The query was cancelled by the backend."));
                break;
            case "DEADLINE_EXCEEDED":
                backendConnection.getExecutableQuery().queryListener.getQuery().setQueryState(QueryState.SYNTAX_ERROR);
                backendConnection.getExecutableQuery().failure.accept(new BackendException.QueryErrorException("The backend did not answer the request in time."));

                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        query.execute();
                    }
                }, rerunQueryDelay);

                break;
            case "UNIMPLEMENTED":
                backendConnection.getExecutableQuery().queryListener.getQuery().setQueryState(QueryState.ERROR);
                backendConnection.getExecutableQuery().failure.accept(new BackendException.QueryErrorException("The given query type is not supported by the backend."));
                break;
            default:
                backendConnection.getExecutableQuery().queryListener.getQuery().setQueryState(QueryState.ERROR);
                backendConnection.getExecutableQuery().failure.accept(new BackendException.QueryErrorException("The query failed due to error type: " + errorType));

                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        query.execute();
                    }
                }, rerunQueryDelay);

                try {
                    backendConnection.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                break;
        }

        backendConnection.setExecutableQuery(null);
    }

    private void handleResponse(ExecutableQuery executableQuery, QueryProtos.QueryResponse value) {
        if (value.hasRefinement() && value.getRefinement().getSuccess()) {
            executableQuery.queryListener.getQuery().setQueryState(QueryState.SUCCESSFUL);
            executableQuery.success.accept(true);
        } else {
            System.out.println(value.getError());
            executableQuery.queryListener.getQuery().setQueryState(QueryState.ERROR);
            executableQuery.success.accept(false);
        }
    }

    private BackendConnection startNewBackendConnection(BackendInstance backend) {
        try {
            Process p;
            BackendConnection newConnection = null;
            String hostAddress = (backend.isLocal() ? "127.0.0.1" : backend.getBackendLocation());

            try {
                int portNumber = SocketUtils.findAvailableTcpPort(backend.getPortStart(), backend.getPortEnd());

                do {
                    ProcessBuilder pb;
                    if (backend.getBackendLocation().endsWith(".jar")) {
                        pb = new ProcessBuilder("java", "-jar", backend.getBackendLocation());
                    } else {
                        pb = new ProcessBuilder(backend.getBackendLocation(), "-p", hostAddress + ":" + portNumber).redirectErrorStream(true);
                    }

                    p = pb.start();
                    // If the process is not alive, it failed while starting up, try again
                } while (!p.isAlive());

                ManagedChannel channel = ManagedChannelBuilder.forTarget(hostAddress + ":" + portNumber)
                        .usePlaintext()
                        .keepAliveWithoutCalls(true)
                        .keepAliveTime(1000, TimeUnit.MILLISECONDS)
                        .keepAliveTimeout(2000, TimeUnit.MILLISECONDS)
                        .build();

                EcdarBackendGrpc.EcdarBackendStub stub = EcdarBackendGrpc.newStub(channel);

                newConnection = new BackendConnection(p, stub);
                this.openBackendConnections.add(newConnection);
            } catch (IllegalStateException e) {
                Ecdar.showToast("Unable to find a free port in port range: " + backend.getPortStart() + " - " + backend.getPortEnd() + " for " + backend.getName() + " sockets");
            }

            return newConnection;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    private class ExecutableQuery {
        private final String query;
        private final BackendInstance backend;
        private final Consumer<Boolean> success;
        private final Consumer<BackendException> failure;
        private final QueryListener queryListener;

        ExecutableQuery(String query, BackendInstance backend, Consumer<Boolean> success, Consumer<BackendException> failure, QueryListener queryListener) {
            this.query = query;
            this.backend = backend;
            this.success = success;
            this.failure = failure;
            this.queryListener = queryListener;
        }

        public void execute() {
            executeQuery(this);
        }
    }

    private class BackendConnection {
        private final Process process;
        private final EcdarBackendGrpc.EcdarBackendStub stub;
        private ExecutableQuery executableQuery = null;

        BackendConnection(Process process, EcdarBackendGrpc.EcdarBackendStub stub) throws IOException {
            this.process = process;
            this.stub = stub;
        }

        public EcdarBackendGrpc.EcdarBackendStub getStub() {
            return stub;
        }

        public ExecutableQuery getExecutableQuery() {
            return executableQuery;
        }

        public BackendInstance getBackendInstance() {
            return executableQuery.backend;
        }

        public void setExecutableQuery(ExecutableQuery executableQuery) {
            this.executableQuery = executableQuery;
        }

        public boolean isRunningQuery() {
            return executableQuery != null;
        }

        public void close() throws IOException {
            // ToDo NIELS: Close channels as well
            process.destroy();
        }
    }

    enum TraceType {
        NONE, SOME, SHORTEST, FASTEST;

        @Override
        public String toString() {
            return "trace " + this.ordinal();
        }
    }
}
