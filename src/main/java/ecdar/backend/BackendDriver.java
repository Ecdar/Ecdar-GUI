package ecdar.backend;

import EcdarProtoBuf.ComponentProtos;
import EcdarProtoBuf.EcdarBackendGrpc;
import EcdarProtoBuf.QueryProtos;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.protobuf.Empty;
import ecdar.Ecdar;
import ecdar.abstractions.BackendInstance;
import ecdar.abstractions.Component;
import ecdar.abstractions.QueryState;
import ecdar.controllers.EcdarController;
import ecdar.utility.UndoRedoStack;
import io.grpc.*;
import io.grpc.stub.StreamObserver;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import org.springframework.util.SocketUtils;

import java.io.*;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class BackendDriver {
    private final BlockingQueue<ExecutableQuery> queryQueue = new ArrayBlockingQueue<>(200);
    private final Map<BackendInstance, BlockingQueue<BackendConnection>> openBackendConnections = new HashMap<>();
    private final int deadlineForResponses = 20000;
    private final int rerunQueryDelay = 200;
    private final int numberOfRetriesPerQuery = 5;

    public BackendDriver() {
        // ToDo NIELS: Consider multiple consumer threads using 'for(int i = 0; i < x; i++) {}'
        QueryConsumer consumer = new QueryConsumer(queryQueue);
        Thread consumerThread = new Thread(consumer);
        consumerThread.start();
    }

    /**
     * Enqueue query for execution with consumers for success and failure, which are executed when a response or
     * an error is received from the backend
     *
     * @param query           the query to be executed
     * @param backendInstance name of the backend to execute the query with
     * @param success         consumer for a successful response
     * @param failure         consumer for a failure response
     * @param queryListener   query listener for referencing the query for GUI purposes
     */
    public void addQueryToExecutionQueue(String query,
                                         BackendInstance backendInstance,
                                         Consumer<Boolean> success,
                                         Consumer<BackendException> failure,
                                         QueryListener queryListener) {
        queryQueue.add(new ExecutableQuery(query, backendInstance, success, failure, queryListener));
    }

    /**
     * ToDo NIELS: Reimplement this with query queue when backends support this feature
     * Asynchronous method for fetching inputs and outputs for the given refinement query and adding these to the
     * ignored inputs and outputs pane for the given query.
     *
     * @param query           the ignored input output query containing the query and related GUI elements
     * @param backendInstance the backend that should be used to execute the query
     */
    public void getInputOutputs(IgnoredInputOutputQuery query, BackendInstance backendInstance) {
        final BackendConnection backendConnection;
        try {
            backendConnection = getBackendConnection(backendInstance);
        } catch (BackendException.NoAvailableBackendConnectionException e) {
            if (query.tries < numberOfRetriesPerQuery) {
                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        getInputOutputs(query, backendInstance);
                    }
                }, rerunQueryDelay);
            }

            return;
        }

        QueryProtos.ComponentsUpdateRequest.Builder componentsBuilder = QueryProtos.ComponentsUpdateRequest.newBuilder();
        for (Component c : Ecdar.getProject().getComponents()) {
            componentsBuilder.addComponents(ComponentProtos.Component.newBuilder().setJson(c.serialize().toString()).build());
        }

        executeGrpcRequest(query.getQuery().getQuery(),
                backendConnection,
                componentsBuilder,
                QueryProtos.IgnoredInputOutputs.newBuilder().getDefaultInstanceForType(),
                (response) -> {
                    if (response.hasQuery() && response.getQuery().hasIgnoredInputOutputs()) {
                        var ignoredInputOutputs = response.getQuery().getIgnoredInputOutputs();
                        query.addNewElementsToMap(new ArrayList<>(ignoredInputOutputs.getIgnoredInputsList()), new ArrayList<>(ignoredInputOutputs.getIgnoredOutputsList()));
                    } else {
                        // Response is unexpected, maybe just ignore
                    }
                }, (t) -> {
                }
        );

    }

    /**
     * Close all open backend connection and kill all locally running processes
     *
     * @throws IOException if any of the sockets do not respond
     */
    public void closeAllBackendConnections() throws IOException {
        for (BlockingQueue<BackendConnection> bq : openBackendConnections.values()) {
            for (BackendConnection bc : bq) bc.close();
        }
    }

    private void executeQuery(ExecutableQuery executableQuery, BackendConnection backendConnection) {
        if (executableQuery.queryListener.getQuery().getQueryState() == QueryState.UNKNOWN) return;

        QueryProtos.ComponentsUpdateRequest.Builder componentsBuilder = QueryProtos.ComponentsUpdateRequest.newBuilder();
        for (Component c : Ecdar.getProject().getComponents()) {
            componentsBuilder.addComponents(ComponentProtos.Component.newBuilder().setJson(c.serialize().toString()).build());
        }

        executeGrpcRequest(executableQuery, backendConnection, componentsBuilder);
    }

    /**
     * Executes the specified query as a gRPC request using the specified backend connection.
     * componentsBuilder is used to update the components of the engine.
     *
     * @param executableQuery   executable query to be executed by the backend
     * @param backendConnection connection to the backend
     * @param componentsBuilder components builder containing the components relevant to the query execution
     */
    private void executeGrpcRequest(ExecutableQuery executableQuery,
                                    BackendConnection backendConnection,
                                    QueryProtos.ComponentsUpdateRequest.Builder componentsBuilder) {
        executeGrpcRequest(executableQuery.query,
                backendConnection,
                componentsBuilder,
                null,
                (response) -> handleQueryResponse(response, executableQuery),
                (error) -> handleQueryBackendError(error, executableQuery)
        );
    }

    /**
     * Executes the specified query as a gRPC request using the specified backend connection.
     * componentsBuilder is used to update the components of the engine and on completion of this transaction,
     * the query is sent and its response is consumed by responseConsumer. Any error encountered is handled by
     * the errorConsumer.
     *
     * @param query                       query to be executed by the backend
     * @param backendConnection           connection to the backend
     * @param componentsBuilder           components builder containing the components relevant to the query execution
     * @param protoBufIgnoredInputOutputs ProtoBuf object containing the inputs and outputs that should be ignored
     *                                    (can be null)
     * @param responseConsumer            consumer for handling the received response
     * @param errorConsumer               consumer for handling a potential error
     */
    private void executeGrpcRequest(String query,
                                    BackendConnection backendConnection,
                                    QueryProtos.ComponentsUpdateRequest.Builder componentsBuilder,
                                    QueryProtos.IgnoredInputOutputs protoBufIgnoredInputOutputs,
                                    Consumer<QueryProtos.QueryResponse> responseConsumer,
                                    Consumer<Throwable> errorConsumer) {
        StreamObserver<Empty> observer = new StreamObserver<>() {
            @Override
            public void onNext(Empty value) {
            }

            @Override
            public void onError(Throwable t) {
                errorConsumer.accept(t);
                addBackendConnection(backendConnection);
            }

            @Override
            public void onCompleted() {
                StreamObserver<QueryProtos.QueryResponse> responseObserver = new StreamObserver<>() {
                    @Override
                    public void onNext(QueryProtos.QueryResponse value) {
                        responseConsumer.accept(value);
                    }

                    @Override
                    public void onError(Throwable t) {
                        errorConsumer.accept(t);
                        addBackendConnection(backendConnection);
                    }

                    @Override
                    public void onCompleted() {
                        addBackendConnection(backendConnection);
                    }
                };

                var queryBuilder = QueryProtos.Query.newBuilder()
                        .setId(0)
                        .setQuery(query);

                if (protoBufIgnoredInputOutputs != null)
                    queryBuilder.setIgnoredInputOutputs(protoBufIgnoredInputOutputs);

                backendConnection.getStub().withDeadlineAfter(deadlineForResponses, TimeUnit.MILLISECONDS)
                        .sendQuery(queryBuilder.build(), responseObserver);
            }
        };

        backendConnection.getStub().withDeadlineAfter(deadlineForResponses, TimeUnit.MILLISECONDS)
                .updateComponents(componentsBuilder.build(), observer);
    }

    private void addBackendConnection(BackendConnection backendConnection) {
        this.openBackendConnections.get(backendConnection.getBackendInstance()).add(backendConnection);
    }

    /**
     * Filters the list of open {@link BackendConnection}s to the specified {@link BackendInstance} and returns the
     * first match or attempts to start a new connection if none is found.
     *
     * @param backend backend instance to get a connection to (e.g. Reveaal, j-Ecdar, custom_engine)
     * @return a BackendConnection object linked to the backend, either from the open backend connection list
     * or a newly started connection.
     * @throws BackendException.NoAvailableBackendConnectionException if unable to retrieve a connection to the backend
     *                                                                and unable to start a new one
     */
    private BackendConnection getBackendConnection(BackendInstance backend) throws BackendException.NoAvailableBackendConnectionException {
        BackendConnection connection;
        try {
            if (!openBackendConnections.containsKey(backend))
                openBackendConnections.put(backend, new ArrayBlockingQueue<>(backend.getNumberOfInstances() + 1));

            // If no open connection is free, attempt to start a new one
            if (openBackendConnections.get(backend).size() < 1) {
                tryStartNewBackendConnection(backend);
            }

            // Block until a connection becomes available
            connection = openBackendConnections.get(backend).take();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        return connection;
    }

    /**
     * Attempts to start a new connection to the specified backend. On success, the backend is added to the associated
     * queue, otherwise, nothing happens.
     *
     * @param backend the target backend for the connection
     */
    private void tryStartNewBackendConnection(BackendInstance backend) {
        Process p = null;
        String hostAddress = (backend.isLocal() ? "127.0.0.1" : backend.getBackendLocation());
        long portNumber = 0;

        if (backend.isLocal()) {
            try {
                portNumber = SocketUtils.findAvailableTcpPort(backend.getPortStart(), backend.getPortEnd());
            } catch (IllegalStateException e) {
                // No port was available in range, we assume that connections are running on all ports
                return;
            }

            do {
                ProcessBuilder pb = new ProcessBuilder(backend.getBackendLocation(), "-p", hostAddress + ":" + portNumber);

                try {
                    p = pb.start();
                } catch (IOException ioException) {
                    Ecdar.showToast("Unable to start local backend instance");
                    ioException.printStackTrace();
                    return;
                }
                // If the process is not alive, it failed while starting up, try again
            } while (!p.isAlive());
        } else {
            // Filter open connections to this backend and map their used ports to an int stream
            var activeEnginePorts = openBackendConnections.get(backend).stream()
                    .mapToInt((bi) -> Integer.parseInt(bi.getStub().getChannel().authority().split(":", 2)[1]));

            int currentPort = backend.getPortStart();
            do {
                // Find port not already connected to
                int tempPortNumber = currentPort;
                if (activeEnginePorts.noneMatch((i) -> i == tempPortNumber)) {
                    portNumber = tempPortNumber;
                } else {
                    currentPort++;
                }
            } while (portNumber == 0 && currentPort <= backend.getPortEnd());

            if (currentPort > backend.getPortEnd()) {
                Ecdar.showToast("Unable to connect to remote engine: " + backend.getName() + " within port range " + backend.getPortStart() + " - " + backend.getPortEnd());
                return;
            }
        }

        ManagedChannel channel = ManagedChannelBuilder.forTarget(hostAddress + ":" + portNumber)
                .usePlaintext()
                .keepAliveTime(1000, TimeUnit.MILLISECONDS)
                .build();

        EcdarBackendGrpc.EcdarBackendStub stub = EcdarBackendGrpc.newStub(channel);
        BackendConnection newConnection = new BackendConnection(backend, p, stub, channel);
        addBackendConnection(newConnection);
    }

    private void handleQueryResponse(QueryProtos.QueryResponse value, ExecutableQuery executableQuery) {
        // If the query has been cancelled, ignore the result
        if (executableQuery.queryListener.getQuery().getQueryState() == QueryState.UNKNOWN) return;

        if (value.hasRefinement() && value.getRefinement().getSuccess()) {
            executableQuery.queryListener.getQuery().setQueryState(QueryState.SUCCESSFUL);
            executableQuery.success.accept(true);
        } else if (value.hasConsistency() && value.getConsistency().getSuccess()) {
            executableQuery.queryListener.getQuery().setQueryState(QueryState.SUCCESSFUL);
            executableQuery.success.accept(true);
        } else if (value.hasDeterminism() && value.getDeterminism().getSuccess()) {
            executableQuery.queryListener.getQuery().setQueryState(QueryState.SUCCESSFUL);
            executableQuery.success.accept(true);
        } else if (value.hasComponent()) {
            executableQuery.queryListener.getQuery().setQueryState(QueryState.SUCCESSFUL);
            executableQuery.success.accept(true);
            JsonObject returnedComponent = (JsonObject) JsonParser.parseString(value.getComponent().getComponent().getJson());
            addGeneratedComponent(new Component(returnedComponent));
        } else {
            executableQuery.queryListener.getQuery().setQueryState(QueryState.ERROR);
            executableQuery.success.accept(false);
        }
    }

    private void handleQueryBackendError(Throwable t, ExecutableQuery executableQuery) {
        // If the query has been cancelled, ignore the error
        if (executableQuery.queryListener.getQuery().getQueryState() == QueryState.UNKNOWN) return;

        // Each error starts with a capitalized description of the error equal to the gRPC error type encountered
        String errorType = t.getMessage().split(":\\s+", 2)[0];


        switch (errorType) {
            case "CANCELLED":
                executableQuery.queryListener.getQuery().setQueryState(QueryState.ERROR);
                executableQuery.failure.accept(new BackendException.QueryErrorException("The query was cancelled"));
                break;

            case "DEADLINE_EXCEEDED":
                executableQuery.queryListener.getQuery().setQueryState(QueryState.ERROR);
                executableQuery.failure.accept(new BackendException.QueryErrorException("The backend did not answer the request in time"));
                queryQueue.add(executableQuery);
                break;

            case "UNIMPLEMENTED":
                executableQuery.queryListener.getQuery().setQueryState(QueryState.SYNTAX_ERROR);
                executableQuery.failure.accept(new BackendException.QueryErrorException("The query type is not supported by the backend"));
                break;

            case "INTERNAL":
                executableQuery.queryListener.getQuery().setQueryState(QueryState.ERROR);
                executableQuery.failure.accept(new BackendException.QueryErrorException("The backend was unable to execute this query:\n" + t.getMessage().split(": ", 2)[1]));
                break;

            case "UNKNOWN":
                executableQuery.queryListener.getQuery().setQueryState(QueryState.ERROR);
                executableQuery.failure.accept(new BackendException.QueryErrorException("The backend encountered an unknown error"));
                break;

            case "UNAVAILABLE":
                executableQuery.queryListener.getQuery().setQueryState(QueryState.SYNTAX_ERROR);
                executableQuery.failure.accept(new BackendException.QueryErrorException("The backend could not be reached"));
                break;

            default:
                try {
                    executableQuery.queryListener.getQuery().setQueryState(QueryState.ERROR);
                    executableQuery.failure.accept(new BackendException.QueryErrorException("The query failed and gave the following error: " + errorType));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
        }
    }

    private void addGeneratedComponent(Component newComponent) {
        Platform.runLater(() -> {
            newComponent.setTemporary(true);

            ObservableList<Component> listOfGeneratedComponents = Ecdar.getProject().getTempComponents();
            Component matchedComponent = null;

            for (Component currentGeneratedComponent : listOfGeneratedComponents) {
                int comparisonOfNames = currentGeneratedComponent.getName().compareTo(newComponent.getName());

                if (comparisonOfNames == 0) {
                    matchedComponent = currentGeneratedComponent;
                    break;
                } else if (comparisonOfNames < 0) {
                    break;
                }
            }

            if (matchedComponent == null) {
                UndoRedoStack.pushAndPerform(() -> { // Perform
                    Ecdar.getProject().getTempComponents().add(newComponent);
                }, () -> { // Undo
                    Ecdar.getProject().getTempComponents().remove(newComponent);
                }, "Created new component: " + newComponent.getName(), "add-circle");
            } else {
                // Remove current component with name and add the newly generated one
                Component finalMatchedComponent = matchedComponent;
                UndoRedoStack.pushAndPerform(() -> { // Perform
                    Ecdar.getProject().getTempComponents().remove(finalMatchedComponent);
                    Ecdar.getProject().getTempComponents().add(newComponent);
                }, () -> { // Undo
                    Ecdar.getProject().getTempComponents().remove(newComponent);
                    Ecdar.getProject().getTempComponents().add(finalMatchedComponent);
                }, "Created new component: " + newComponent.getName(), "add-circle");
            }

            EcdarController.getActiveCanvasPresentation().getController().setActiveModel(newComponent);
        });
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
        void execute(BackendConnection backendConnection) {
            tries++;
            executeQuery(this, backendConnection);
        }
    }

    private class BackendConnection {
        private final Process process;
        private final EcdarBackendGrpc.EcdarBackendStub stub;
        private final ManagedChannel channel;
        private final BackendInstance backendInstance;

        BackendConnection(BackendInstance backendInstance, Process process, EcdarBackendGrpc.EcdarBackendStub stub, ManagedChannel channel) {
            this.process = process;
            this.stub = stub;
            this.backendInstance = backendInstance;
            this.channel = channel;
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
         * Get the backend instance that should be used to execute
         * the query currently associated with this backend connection
         *
         * @return the instance of the associated executable query object,
         * or null, if no executable query is currently associated
         */
        public BackendInstance getBackendInstance() {
            return backendInstance;
        }

        /**
         * Close the gRPC connection and end the process
         *
         * @throws IOException originally thrown by the destroy method on java.lang.Process
         */
        public void close() throws IOException {
            if (!channel.isShutdown()) {
                try {
                    channel.shutdown();
                    if (!channel.awaitTermination(45, TimeUnit.SECONDS)) {
                        channel.shutdownNow(); // Forcefully close the connection
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            // If the backend-instance is remote, there will not be a process
            if (process != null) {
                process.destroy();
            }

            openBackendConnections.get(backendInstance).remove(this);
        }
    }

    private class QueryConsumer implements Runnable {
        BlockingQueue<ExecutableQuery> queue;

        private QueryConsumer(BlockingQueue<ExecutableQuery> queue) {
            this.queue = queue;
        }

        @Override
        public void run() {
            while (true) {
                try {
                    ExecutableQuery executableQuery = this.queue.take();

                    final BackendConnection backendConnection;
                    try {
                        backendConnection = getBackendConnection(executableQuery.backend);
                        executableQuery.execute(backendConnection);
                    } catch (BackendException.NoAvailableBackendConnectionException e) {
                        e.printStackTrace();
                        if (executableQuery.tries < numberOfRetriesPerQuery) {
                            new Timer().schedule(new TimerTask() {
                                @Override
                                public void run() {
                                    queryQueue.add(executableQuery);
                                }
                            }, rerunQueryDelay);
                        } else {
                            executableQuery.failure.accept(new BackendException("Failed to execute query after five tries"));
                            executableQuery.queryListener.getQuery().setQueryState(QueryState.ERROR);
                        }
                        return;
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
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
