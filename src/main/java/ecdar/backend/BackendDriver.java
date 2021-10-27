package ecdar.backend;

import EcdarProtoBuf.ComponentProtos;
import EcdarProtoBuf.EcdarBackendGrpc;
import EcdarProtoBuf.QueryProtos;
import com.google.protobuf.Empty;
import ecdar.Ecdar;
import ecdar.abstractions.Component;
import ecdar.abstractions.QueryState;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import javafx.util.Pair;
import org.springframework.util.SocketUtils;

import java.io.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class BackendDriver {
    private final Pair<AtomicInteger, AtomicInteger> numberOfReveaalConnections = new Pair<>(new AtomicInteger(0), new AtomicInteger(5));
    private final List<BackendConnection> reveaalConnections = new CopyOnWriteArrayList<>();

    private final Pair<AtomicInteger, AtomicInteger> numberOfJEcdarConnections = new Pair<>(new AtomicInteger(0), new AtomicInteger(5));
    private final List<BackendConnection> jEcdarConnections = new CopyOnWriteArrayList<>();

    private final Queue<ExecutableQuery> waitingQueries = new LinkedList<>();

    private final ArrayList<Process> backendProcesses = new ArrayList<>();

    private final String hostAddress;

    public BackendDriver(String hostAddress) {
        this.hostAddress = hostAddress;

        new Thread(() -> {
            while(true) {
                // Execute the next waiting query
                ExecutableQuery query = waitingQueries.poll();
                if (query != null) query.execute();

                // Currently necessary in order not to crash the program
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    return;
                }
            }
        }).start();
    }

    public void addQueryToExecutionQueue(String query, BackendHelper.BackendNames backend, Consumer<Boolean> success, Consumer<BackendException> failure, QueryListener queryListener) {
        waitingQueries.add(new ExecutableQuery(query, backend, success, failure, queryListener));
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

    public void closeAllSockets() throws IOException {
        for (BackendConnection s : reveaalConnections) s.close();
        for (BackendConnection s : jEcdarConnections) s.close();
        for (Process p : backendProcesses) p.destroy();
    }

    public void setMaxNumberOfSockets(int i) {
        numberOfReveaalConnections.getValue().set(i);
        numberOfJEcdarConnections.getValue().set(i);

        // ToDo NIELS: Potentially close connections until within new range [0, i]
    }

    public int getMaxNumberOfSockets() {
        return numberOfReveaalConnections.getValue().get();
    }

    private void executeQuery(ExecutableQuery executableQuery) {
        if(executableQuery.queryListener.getQuery().getQueryState() == QueryState.UNKNOWN) return;

        // Get available socket or start new
        final Optional<BackendConnection> socket;
        if (executableQuery.backend.equals(BackendHelper.BackendNames.jEcdar)) {
            socket = jEcdarConnections.stream().filter((element) -> !element.isRunningQuery()).findFirst();
        } else {
            socket = reveaalConnections.stream().filter((element) -> !element.isRunningQuery()).findFirst();
        }

        BackendConnection backendConnection = socket.orElseGet(() -> (executableQuery.backend == BackendHelper.BackendNames.Reveaal
                ? startNewBackendConnection(BackendHelper.BackendNames.Reveaal, numberOfReveaalConnections, reveaalConnections)
                : startNewBackendConnection(BackendHelper.BackendNames.jEcdar, numberOfJEcdarConnections, jEcdarConnections)));

        // If the query socket is null, there are no available sockets
        // and the maximum number of sockets has already been reached
        if (backendConnection == null) {
            waitingQueries.add(executableQuery);
            return;
        }

        backendConnection.setExecutableQuery(executableQuery);

        var componentsBuilder = QueryProtos.ComponentsUpdateRequest.newBuilder();
        for (Component c : Ecdar.getProject().getComponents()) {
            componentsBuilder.addComponents(ComponentProtos.Component.newBuilder().setJson(c.serialize().toString()).build());
        }

        StreamObserver<Empty> observer = new StreamObserver<>() {
            boolean error = false;

            @Override
            public void onNext(Empty value) {}

            @Override
            public void onError(Throwable t) {
                backendConnection.getExecutableQuery().queryListener.getQuery().setQueryState(QueryState.ERROR);
                backendConnection.getExecutableQuery().failure.accept(new BackendException.QueryErrorException(t.getMessage()));
                waitingQueries.add(backendConnection.getExecutableQuery());
                backendConnection.setExecutableQuery(null);
                error = true;
            }

            @Override
            public void onCompleted() {
                if (!error) {
                    StreamObserver<EcdarProtoBuf.QueryProtos.QueryResponse> responseObserver = new StreamObserver<>() {
                        @Override
                        public void onNext(QueryProtos.QueryResponse value) {
                            if (value.getRefinement().getSuccess()) {
                                backendConnection.getExecutableQuery().queryListener.getQuery().setQueryState(QueryState.SUCCESSFUL);
                                backendConnection.getExecutableQuery().success.accept(true);
                            } else {
                                backendConnection.getExecutableQuery().queryListener.getQuery().setQueryState(QueryState.ERROR);
                                backendConnection.getExecutableQuery().success.accept(false);
                            }
                        }

                        @Override
                        public void onError(Throwable t) {
                            backendConnection.getExecutableQuery().queryListener.getQuery().setQueryState(QueryState.SYNTAX_ERROR);
                            backendConnection.getExecutableQuery().failure.accept(new BackendException.QueryErrorException(t.getMessage()));
                            waitingQueries.add(backendConnection.getExecutableQuery());
                            backendConnection.setExecutableQuery(null);
                        }

                        @Override
                        public void onCompleted() {
                            backendConnection.setExecutableQuery(null);
                            System.out.println("We are done testing!!!");
                        }
                    };

                    backendConnection.getStub().sendQuery(QueryProtos.Query.newBuilder().setId(0).setQuery(backendConnection.getExecutableQuery().query).build(), responseObserver);
                }
            }
        };

        backendConnection.getStub().updateComponents(componentsBuilder.build(), observer);
    }

    private BackendConnection startNewBackendConnection(BackendHelper.BackendNames backend, Pair<AtomicInteger, AtomicInteger> numberOfSockets, List<BackendConnection> backendConnections) {
        if (numberOfSockets.getKey().get() < numberOfSockets.getValue().get()) {
            try {
                int portNumber = SocketUtils.findAvailableTcpPort();
                while (true) {
                    ProcessBuilder pb;
                    if (backend.equals(BackendHelper.BackendNames.jEcdar)) {
                        pb = new ProcessBuilder("java", "-jar", "src/libs/j-Ecdar.jar");
                    } else {
                        pb = new ProcessBuilder("src/Reveaal", "-p", this.hostAddress + ":" + portNumber).inheritIO();
                    }

                    Process p = pb.start();
                    if (p.isAlive()) {
                        // If the process is not alive, it failed while starting up, try again
                        backendProcesses.add(p);
                        break;
                    }
                }

                ManagedChannel channel = ManagedChannelBuilder.forTarget(this.hostAddress + ":" + portNumber).usePlaintext().build();
                EcdarBackendGrpc.EcdarBackendStub stub = EcdarBackendGrpc.newStub(channel);

                BackendConnection newConnection = new BackendConnection(stub);

                backendConnections.add(newConnection);
                numberOfSockets.getKey().getAndIncrement();

                return newConnection;

            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
//            System.out.println("Max number of sockets already reached");
        }

        return null;
    }

    private class ExecutableQuery {
        private final String query;
        private final BackendHelper.BackendNames backend;
        private final Consumer<Boolean> success;
        private final Consumer<BackendException> failure;
        private final QueryListener queryListener;

        ExecutableQuery(String query, BackendHelper.BackendNames backend, Consumer<Boolean> success, Consumer<BackendException> failure, QueryListener queryListener) {
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
        private final EcdarBackendGrpc.EcdarBackendStub stub;
        private ExecutableQuery executableQuery = null;

        BackendConnection(EcdarBackendGrpc.EcdarBackendStub stub) throws IOException {
            this.stub = stub;
        }

        public EcdarBackendGrpc.EcdarBackendStub getStub() {
            return stub;
        }

        public ExecutableQuery getExecutableQuery() {
            return executableQuery;
        }

        public void setExecutableQuery(ExecutableQuery executableQuery) {
            this.executableQuery = executableQuery;
        }

        public boolean isRunningQuery() {
            return executableQuery != null;
        }

        public void close() throws IOException {
            // Remove the socket from the socket list
            if (jEcdarConnections.remove(this)) {
                numberOfJEcdarConnections.getKey().getAndDecrement();
            } else if(reveaalConnections.remove(this)) {
                numberOfReveaalConnections.getKey().getAndDecrement();
            } else {
                // ToDo NIELS: Somehow the socket was not present in either list
                System.out.println("The socket was not found in the socket lists");
            }
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
