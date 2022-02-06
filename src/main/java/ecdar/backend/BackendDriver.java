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
import org.springframework.util.SocketUtils;

import java.io.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class BackendDriver {
    private final List<BackendConnection> openBackendConnections = new CopyOnWriteArrayList<>();
    private final int deadlineForResponses = 20000;
    private final int rerunQueryDelay = 200;

    public BackendDriver() {
    }

    /**
     * Add the query to execution queue with consumers for success and failure, executed when response received from backends
     *
     * @param query         the query to be executed
     * @param backendInstance       name of the backend to execute the query with
     * @param success       consumer for a successful response
     * @param failure       consumer for a failure response
     * @param queryListener query listener for referencing the query for GUI purposes
     */
    public void addQueryToExecutionQueue(String query, BackendInstance backendInstance, Consumer<Boolean> success, Consumer<BackendException> failure, QueryListener queryListener) {
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                new ExecutableQuery(query, backendInstance, success, failure, queryListener).execute();
            }
        }, rerunQueryDelay);
    }

    /**
     * Asynchronous method for fetching inputs and outputs for the given refinement query and adding these to the
     * ignored inputs and outputs pane for the given query
     *
     * @param query the ignored input output query containing the query and related GUI elements
     */
    public void getInputOutputs(IgnoredInputOutputQuery query, BackendInstance backendInstance) {
        ExecutableQuery executableQuery = new ExecutableQuery(query.getQuery().getQuery(), backendInstance,
                a -> {

                },
                e -> {

                },
                new QueryListener(query.getQuery()));

        // Get available connection or start new (only Reveaal supports ignored inputs/outputs)
        final Optional<BackendConnection> connection;
        connection = openBackendConnections.stream().filter((element) -> !element.isRunningQuery() && element.getBackendInstance().equals(backendInstance)).findFirst();
        final BackendConnection backendConnection = connection.orElseGet(() ->
                (startNewBackendConnection(backendInstance)));
        // If the query connection is null, there are no available sockets
        // and the maximum number of sockets has already been reached
        if (backendConnection == null) {
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    getInputOutputs(query, backendInstance);
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
            public void onNext(Empty value) {}

            @Override
            public void onError(Throwable t) {
                error = true;
            }

            @Override
            public void onCompleted() {
                if (!error) {
                    StreamObserver<EcdarProtoBuf.QueryProtos.QueryResponse> responseObserver = new StreamObserver<>() {
                        @Override
                        public void onNext(QueryProtos.QueryResponse value) {
                            if (value.hasQuery() && value.getQuery().hasIgnoredInputOutputs()) {
                                System.out.println(value.getQuery().getIgnoredInputOutputs());
                            } else {
                                System.out.println("Not the desired output: " + value);
                            }
                        }

                        @Override
                        public void onError(Throwable t) {
                        }

                        @Override
                        public void onCompleted() {
                            backendConnection.setExecutableQuery(null);
                        }
                    };
                    backendConnection.getStub().withDeadlineAfter(deadlineForResponses, TimeUnit.MILLISECONDS).sendQuery(QueryProtos.Query.newBuilder().setId(0).setQuery(query.getQuery().getQuery()).setIgnoredInputOutputs(QueryProtos.IgnoredInputOutputs.newBuilder().getDefaultInstanceForType()).build(), responseObserver);
                }
            }
        };

        backendConnection.getStub().withDeadlineAfter(deadlineForResponses, TimeUnit.MILLISECONDS).updateComponents(componentsBuilder.build(), observer);
    }

    /**
     * Close all open backend connection
     *
     * @throws IOException if any of the sockets do not respond
     */
    public void closeAllBackendConnections() throws IOException {
        for (BackendConnection s : openBackendConnections) s.close();
    }

    private void executeQuery(ExecutableQuery executableQuery) {
        if (executableQuery.queryListener.getQuery().getQueryState() == QueryState.UNKNOWN) return;

        // Get available connection or start new
        final BackendConnection backendConnection = openBackendConnections.stream()
                .filter((connection) -> connection.getBackendInstance() != null && connection.getBackendInstance().equals(executableQuery.backend))
                .findFirst()
                .orElseGet(() -> startNewBackendConnection(executableQuery.backend));

        // If the connection is null, there are no available connections,
        // and it was not possible to start a new one
        if (backendConnection == null) {
            if (executableQuery.tries < 5) {
                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        executableQuery.execute();
                    }
                }, rerunQueryDelay);
            } else {
                executableQuery.failure.accept(new BackendException("Failed to execute query after five tries"));
                executableQuery.queryListener.getQuery().setQueryState(QueryState.ERROR);
            }
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
            }

            @Override
            public void onError(Throwable t) {
                if (executableQuery.queryListener.getQuery().getQueryState() == QueryState.UNKNOWN)
                    backendConnection.setExecutableQuery(null);
                else {
                    handleBackendError(t, backendConnection);
                    error = true;
                    backendConnection.setExecutableQuery(null);
                }
            }

            @Override
            public void onCompleted() {
                if (!error) {
                    StreamObserver<EcdarProtoBuf.QueryProtos.QueryResponse> responseObserver = new StreamObserver<>() {
                        @Override
                        public void onNext(QueryProtos.QueryResponse value) {
                            if (executableQuery.queryListener.getQuery().getQueryState() != QueryState.UNKNOWN) {
                                handleResponse(backendConnection.getExecutableQuery(), value);
                            }
                            backendConnection.setExecutableQuery(null);
                        }

                        @Override
                        public void onError(Throwable t) {
                            if (executableQuery.queryListener.getQuery().getQueryState() != QueryState.UNKNOWN) {
                                handleBackendError(t, backendConnection);
                            }
                            backendConnection.setExecutableQuery(null);
                        }

                        @Override
                        public void onCompleted() {
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
                backendConnection.getExecutableQuery().queryListener.getQuery().setQueryState(QueryState.ERROR);
                backendConnection.getExecutableQuery().failure.accept(new BackendException.QueryErrorException("The query was cancelled"));
                break;
            case "DEADLINE_EXCEEDED":
                backendConnection.getExecutableQuery().queryListener.getQuery().setQueryState(QueryState.ERROR);
                backendConnection.getExecutableQuery().failure.accept(new BackendException.QueryErrorException("The backend did not answer the request in time"));

                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        query.execute();
                    }
                }, rerunQueryDelay);

                break;
            case "UNIMPLEMENTED":
                backendConnection.getExecutableQuery().queryListener.getQuery().setQueryState(QueryState.SYNTAX_ERROR);
                backendConnection.getExecutableQuery().failure.accept(new BackendException.QueryErrorException("The query type is not supported by the backend"));
                break;
            case "INTERNAL":
                backendConnection.getExecutableQuery().queryListener.getQuery().setQueryState(QueryState.ERROR);
                backendConnection.getExecutableQuery().failure.accept(new BackendException.QueryErrorException("Reveaal was unable to execute this query:\n" + t.getMessage().split(": ", 2)[1]));
                break;
            case "UNKNOWN":
                backendConnection.getExecutableQuery().queryListener.getQuery().setQueryState(QueryState.ERROR);
                backendConnection.getExecutableQuery().failure.accept(new BackendException.QueryErrorException("The backend encountered an unknown error"));
                break;
            default:
                backendConnection.getExecutableQuery().queryListener.getQuery().setQueryState(QueryState.ERROR);
                backendConnection.getExecutableQuery().failure.accept(new BackendException.QueryErrorException("The query failed and gave the following error: " + errorType));

//                try {
//                    backendConnection.close();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }

                break;
        }

        backendConnection.setExecutableQuery(null);
    }

    private void handleResponse(ExecutableQuery executableQuery, QueryProtos.QueryResponse value) {
        if (value.hasRefinement() && value.getRefinement().getSuccess()) {
            executableQuery.queryListener.getQuery().setQueryState(QueryState.SUCCESSFUL);
            executableQuery.success.accept(true);
        } else if (value.hasConsistency() && value.getConsistency().getSuccess()) {
            System.out.println("Consistency");
            executableQuery.queryListener.getQuery().setQueryState(QueryState.SUCCESSFUL);
            executableQuery.success.accept(true);
        } else if (value.hasDeterminism() && value.getDeterminism().getSuccess()) {
            System.out.println("Determinism");
            executableQuery.queryListener.getQuery().setQueryState(QueryState.SUCCESSFUL);
            executableQuery.success.accept(true);
        } else if (value.hasComponent()) {
            System.out.println("Component");
            executableQuery.queryListener.getQuery().setQueryState(QueryState.SUCCESSFUL);
            executableQuery.success.accept(true);
        } else {
            System.out.println(value.getError());
            executableQuery.queryListener.getQuery().setQueryState(QueryState.ERROR);
            executableQuery.success.accept(false);
        }
    }
 
    private BackendConnection startNewBackendConnection(BackendInstance backend) {
        Process p = null;
        String hostAddress = (backend.isLocal() ? "127.0.0.1" : backend.getBackendLocation());
        int portNumber = 0;

        try {
             portNumber = SocketUtils.findAvailableTcpPort(backend.getPortStart(), backend.getPortEnd());
        } catch (IllegalStateException e) {
            // No free port could be found
        }

        // ToDo NIELS: Check for the number of open connections to ensure that we do not exceed the number of desired backend
        // Possibly just try all ports in range and exiting after reaching the highest numbered port

        if (backend.isLocal()) {
            do {
                ProcessBuilder pb = new ProcessBuilder(backend.getBackendLocation(), "-p", "" + portNumber).redirectErrorStream(true);
//          ToDo NIELS: Find out if we should still use path search (BELOW IS FROM FIXED BACKEND IMPLEMENTATION)
//            File engine = null;
//            if (isReveaal) {
//                List<File> searchPath = List.of (
//                        new File("lib/Reveaal.exe"), new File("lib/Reveaal")
//                );
//                for (var f: searchPath){
//                    if (f.exists()) {
//                        engine = f;
//                        break;
//                    }
//                }
//                if (engine == null) {
//                    throw new RuntimeException("Could not locate Reveaal engine");
//                }
//                pb = new ProcessBuilder(engine.getAbsolutePath(), "-p", this.hostAddress + ":" + portNumber);
//            } else {
//                pb = new ProcessBuilder("java", "-jar", "lib/j-Ecdar.jar", "-p" + portNumber );
//            }
                pb.inheritIO().redirectErrorStream(true);

                try {
                    p = pb.start();
                } catch (IOException ioException) {
                    Ecdar.showToast("Unable to start backend instance. Check the error tab for more details.");
                    // ToDo NIELS: Add error to errors tab with text:
                    //  "The backend instance could not be started. Make sure that the following is correct:
                    //  - Path/address
                    //  - At least one port in the port range is free for the given address (localhost if backend is set to local)
                    //  - The backend is an executable
                    //  - The backend supports the '-p {host}:{port}' flag on startup
                    ioException.printStackTrace();
                    return null;
                }
                // If the process is not alive, it failed while starting up, try again
            } while (!p.isAlive());
        }

        ManagedChannel channel = ManagedChannelBuilder.forTarget(hostAddress + ":" + portNumber)
                .usePlaintext()
                .keepAliveTime(1000, TimeUnit.MILLISECONDS)
                .build();

        EcdarBackendGrpc.EcdarBackendStub stub = EcdarBackendGrpc.newStub(channel);

        BackendConnection newConnection = new BackendConnection(p, stub);
        this.openBackendConnections.add(newConnection);
        return newConnection;
    }

    private class ExecutableQuery {
        private final String query;
        private final BackendInstance backend;
        private final Consumer<Boolean> success;
        private final Consumer<BackendException> failure;
        private final QueryListener queryListener;
        public int tries = 0;

        ExecutableQuery(String query, BackendInstance backendInstance, Consumer<Boolean> success, Consumer<BackendException> failure, QueryListener queryListener) {
            this.query = query;
            this.backend = backendInstance;
            this.success = success;
            this.failure = failure;
            this.queryListener = queryListener;
        }

        /**
         * Execute the query using the backend driver
         */
        public void execute() {
            executeQuery(this);
            tries++;
        }
    }

    private class BackendConnection {
        private final Process process;
        private final EcdarBackendGrpc.EcdarBackendStub stub;
        private ExecutableQuery executableQuery = null;

        BackendConnection(Process process, EcdarBackendGrpc.EcdarBackendStub stub) {
            this.process = process;
            this.stub = stub;
        }

        /**
         * Get the gRPC stub of the connection to use for query execution
         *
         * @return the gRPC stub of this connection
         */
        public EcdarBackendGrpc.EcdarBackendStub getStub() {
            return stub;
        }

        /**
         * Get the executable query currently being executed or waiting for execution with this connection
         *
         * @return the query
         */
        public ExecutableQuery getExecutableQuery() {
            return executableQuery;
        }

        /**
         * Get the backend instance that should be used to execute
         * the query currently associated with this backend connection
         *
         * @return the instance of the associated executable query object,
         * or null, if no executable query is currently associated
         */
        public BackendInstance getBackendInstance() {
            if (executableQuery == null) {
                return null;
            }
            return executableQuery.backend;
        }

        /**
         * Set the executable query to execute with the connection
         *
         * @param executableQuery query to execute
         */
        public void setExecutableQuery(ExecutableQuery executableQuery) {
            this.executableQuery = executableQuery;
        }

        /**
         * Get whether this backend connection is currently occupied with execution of a query
         *
         * @return true if the connection is in use
         */
        public boolean isRunningQuery() {
            return executableQuery != null;
        }

        /**
         * Close the gRPC connection and end the process
         *
         * @throws IOException originally thrown by the destroy method on java.lang.Process
         */
        public void close() throws IOException {
            // Remove the connection from the connection list
            if (openBackendConnections.remove(this)) {
                System.out.println("Successfully closed connection to backend");
            } else {
                System.out.println("Tried to remove a connection not present in either connection list");
            }

            // If the backend-instance is null, or it is a remote process, we do not need to destroy it
            if (this.getBackendInstance() != null && !this.getBackendInstance().isLocal()) {
                process.destroy();
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
