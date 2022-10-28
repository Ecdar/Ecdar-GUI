package ecdar.backend;

import EcdarProtoBuf.ComponentProtos;
import EcdarProtoBuf.EcdarBackendGrpc;
import EcdarProtoBuf.ObjectProtos;
import EcdarProtoBuf.QueryProtos;
import EcdarProtoBuf.QueryProtos.SimulationStepResponse;
import EcdarProtoBuf.QueryProtos.QueryResponse.QueryOk;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.protobuf.Empty;
import ecdar.Ecdar;
import ecdar.abstractions.BackendInstance;
import ecdar.abstractions.Component;
import ecdar.abstractions.QueryState;
import ecdar.backend.BackendException.NoAvailableBackendConnectionException;
import ecdar.simulation.SimulationState;
import io.grpc.*;
import io.grpc.stub.StreamObserver;
import javafx.util.Pair;
import ecdar.controllers.EcdarController;
import ecdar.utility.UndoRedoStack;
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

        ComponentProtos.ComponentsInfo.Builder componentsInfoBuilder = ComponentProtos.ComponentsInfo.newBuilder();
        for (Component c : Ecdar.getProject().getComponents()) {
            componentsInfoBuilder.addComponents(ComponentProtos.Component.newBuilder().setJson(c.serialize().toString()).build());
        }

        executeGrpcRequest(query.getQuery().getQuery(),
                backendConnection,
                componentsInfoBuilder,
                QueryProtos.IgnoredInputOutputs.newBuilder().getDefaultInstanceForType(),
                (response) -> {
                    // ToDo ANDREAS: Spørg niels om hvad ignored inputs og outputs er
                    System.out.println(query.ignoredInputs);
                    System.out.println(query.ignoredOutputs);
                    if (response.hasQueryOk() && query.ignoredInputs != null && query.ignoredOutputs != null) {
//                        var ignoredInputOutputs = response.getQuery().getIgnoredInputOutputs();
                        query.addNewElementsToMap(new ArrayList<>(query.ignoredInputs.keySet()), new ArrayList<>(query.ignoredOutputs.keySet()));
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

        ComponentProtos.ComponentsInfo.Builder componentsInfoBuilder = ComponentProtos.ComponentsInfo.newBuilder();
        for (Component c : Ecdar.getProject().getComponents()) {
            if (executableQuery.query.contains(c.getName())) {
                componentsInfoBuilder.addComponents(ComponentProtos.Component.newBuilder().setJson(c.serialize().toString()).build());
            }
        }
        componentsInfoBuilder.setComponentsHash(componentsInfoBuilder.getComponentsList().hashCode());

        executeGrpcRequest(executableQuery, backendConnection, componentsInfoBuilder);
    }

    /**
     * Executes the specified query as a gRPC request using the specified backend connection.
     * componentsInfoBuilder is used to update the components of the engine.
     *
     * @param executableQuery   executable query to be executed by the backend
     * @param backendConnection connection to the backend
     * @param componentsInfoBuilder components builder containing the components relevant to the query execution
     */
    private void executeGrpcRequest(ExecutableQuery executableQuery,
                                    BackendConnection backendConnection,
                                    ComponentProtos.ComponentsInfo.Builder componentsInfoBuilder) {
        executeGrpcRequest(executableQuery.query,
                backendConnection,
                componentsInfoBuilder,
                null,
                (response) -> handleQueryResponse(response, executableQuery),
                (error) -> handleQueryBackendError(error, executableQuery)
        );
    }

    public void executeStartSimRequest(String componentComposition, Consumer<SimulationStepResponse> responseConsumer, Consumer<Throwable> errorConsumer) {
        BackendConnection backendConnection;
        try {
            backendConnection = getBackendConnection(BackendHelper.getDefaultBackendInstance());
        } catch (NoAvailableBackendConnectionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return;
        }

        StreamObserver<QueryProtos.SimulationStepResponse> responseObserver = new StreamObserver<>() {
            @Override
            public void onNext(QueryProtos.SimulationStepResponse value) {
                System.out.println(value);
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

        var comInfo = ComponentProtos.ComponentsInfo.newBuilder();
        for (Component c : Ecdar.getProject().getComponents()) {
            if (componentComposition.contains(c.getName())) {
                comInfo.addComponents(ComponentProtos.Component.newBuilder().setJson(c.serialize().toString()).build());
            }
        }
        comInfo.setComponentsHash(comInfo.getComponentsList().hashCode());
        var simStartRequest = QueryProtos.SimulationStartRequest.newBuilder();
        var simInfo = QueryProtos.SimulationInfo.newBuilder()
                .setComponentComposition(componentComposition)
                .setComponentsInfo(comInfo);
        simStartRequest.setSimulationInfo(simInfo);
        backendConnection.getStub().withDeadlineAfter(deadlineForResponses, TimeUnit.MILLISECONDS)
                .startSimulation(simStartRequest.build(), responseObserver);
    }

    /**
     * Executes the specified query as a gRPC request using the specified backend connection.
     * componentsInfoBuilder is used to update the components of the engine and on completion of this transaction,
     * the query is sent and its response is consumed by responseConsumer. Any error encountered is handled by
     * the errorConsumer.
     *
     * @param query                       query to be executed by the backend
     * @param backendConnection           connection to the backend
     * @param componentsInfoBuilder       components builder containing the components relevant to the query execution
     * @param protoBufIgnoredInputOutputs ProtoBuf object containing the inputs and outputs that should be ignored
     *                                    (can be null)
     * @param responseConsumer            consumer for handling the received response
     * @param errorConsumer               consumer for handling a potential error
     */
    private void executeGrpcRequest(String query,
                                    BackendConnection backendConnection,
                                    ComponentProtos.ComponentsInfo.Builder componentsInfoBuilder,
                                    QueryProtos.IgnoredInputOutputs protoBufIgnoredInputOutputs,
                                    Consumer<QueryProtos.QueryResponse> responseConsumer,
                                    Consumer<Throwable> errorConsumer) {
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

        QueryProtos.QueryRequest.Builder queryRequestBuilder = QueryProtos.QueryRequest.newBuilder()
            .setUserId(1)
            .setQueryId(1)
            .setQuery(query)
            .setComponentsInfo(componentsInfoBuilder);

        if (protoBufIgnoredInputOutputs != null)
            queryRequestBuilder.setIgnoredInputOutputs(protoBufIgnoredInputOutputs);

        backendConnection.getStub().withDeadlineAfter(deadlineForResponses, TimeUnit.MILLISECONDS)
                .sendQuery(queryRequestBuilder.build(), responseObserver);
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

        switch (value.getResponseCase()) {
            case QUERY_OK:
                QueryOk queryOk = value.getQueryOk();
                switch (queryOk.getResultCase()) {
                    case REFINEMENT:
                        if (queryOk.getRefinement().getSuccess()) {
                            executableQuery.queryListener.getQuery().setQueryState(QueryState.SUCCESSFUL);
                            executableQuery.success.accept(true);
                        } else {
                            executableQuery.queryListener.getQuery().setQueryState(QueryState.ERROR);
                            executableQuery.failure.accept(new BackendException.QueryErrorException(queryOk.getRefinement().getReason()));
                            // executableQuery.success.accept(false);
                        }
                        break;

                    case CONSISTENCY:
                        if (queryOk.getConsistency().getSuccess()) {
                            executableQuery.queryListener.getQuery().setQueryState(QueryState.SUCCESSFUL);
                            executableQuery.success.accept(true);
                        } else {
                            executableQuery.queryListener.getQuery().setQueryState(QueryState.ERROR);
                            executableQuery.failure.accept(new BackendException.QueryErrorException(queryOk.getConsistency().getReason()));
                            executableQuery.success.accept(false);
                        }
                        break;

                    case DETERMINISM:
                        if (queryOk.getDeterminism().getSuccess()) {
                            executableQuery.queryListener.getQuery().setQueryState(QueryState.SUCCESSFUL);
                            executableQuery.success.accept(true);
                        } else {
                            executableQuery.queryListener.getQuery().setQueryState(QueryState.ERROR);
                            executableQuery.failure.accept(new BackendException.QueryErrorException(queryOk.getDeterminism().getReason()));
                            executableQuery.success.accept(false);
                        }
                        break;

                    case IMPLEMENTATION:
                        if (queryOk.getImplementation().getSuccess()) {
                            executableQuery.queryListener.getQuery().setQueryState(QueryState.SUCCESSFUL);
                            executableQuery.success.accept(true);
                        } else {
                            executableQuery.queryListener.getQuery().setQueryState(QueryState.ERROR);
                            executableQuery.failure.accept(new BackendException.QueryErrorException(queryOk.getImplementation().getReason()));
                            executableQuery.success.accept(false);
                        }
                        break;

                    case REACHABILITY:
                        if (queryOk.getReachability().getSuccess()) {
                            executableQuery.queryListener.getQuery().setQueryState(QueryState.SUCCESSFUL);
                            executableQuery.success.accept(true);
                        } else {
                            executableQuery.queryListener.getQuery().setQueryState(QueryState.ERROR);
                            executableQuery.failure.accept(new BackendException.QueryErrorException(queryOk.getReachability().getReason()));
                            executableQuery.success.accept(false);
                        }
                        break;

                    case COMPONENT:
                        executableQuery.queryListener.getQuery().setQueryState(QueryState.SUCCESSFUL);
                        executableQuery.success.accept(true);
                        JsonObject returnedComponent = (JsonObject) JsonParser.parseString(queryOk.getComponent().getComponent().getJson());
                        addGeneratedComponent(new Component(returnedComponent));
                        break;

                    case ERROR:
                        executableQuery.queryListener.getQuery().setQueryState(QueryState.ERROR);
                        executableQuery.failure.accept(new BackendException.QueryErrorException(queryOk.getError()));
                        executableQuery.success.accept(false);
                        break;

                    case RESULT_NOT_SET:
                        executableQuery.queryListener.getQuery().setQueryState(QueryState.ERROR);
                        executableQuery.success.accept(false);
                        break;
                }
                break;

            case USER_TOKEN_ERROR:
                executableQuery.queryListener.getQuery().setQueryState(QueryState.ERROR);
                executableQuery.failure.accept(new BackendException.QueryErrorException(value.getUserTokenError().getErrorMessage()));
                executableQuery.success.accept(false);
                break;

            case RESPONSE_NOT_SET:
                executableQuery.queryListener.getQuery().setQueryState(QueryState.ERROR);
                executableQuery.success.accept(false);
                break;
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

    public SimulationState getInitialSimulationState() {
            SimulationState state = new SimulationState(ObjectProtos.State.newBuilder().getDefaultInstanceForType());
            state.getLocations().add(new Pair<>(Ecdar.getProject().getComponents().get(0).getName(), Ecdar.getProject().getComponents().get(0).getLocations().get(0).getId()));
            return state;
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

    // private class ExecutableStartSimRequest  {
    //     private final String componentComposition;
    //     private final BackendInstance backendInstance;
    //     private final Consumer<Boolean> success;
    //     private final Consumer<BackendException> failure;
    //     private final StartSimListener startSimListener;
    //     public int tries = 0;

    //     public ExecutableStartSimRequest(String componentComposition, BackendInstance backendInstance,
    //             Consumer<Boolean> success, Consumer<BackendException> failure, StartSimListener startSimListener,
    //             int tries) {
    //         this.componentComposition = componentComposition;
    //         this.backendInstance = backendInstance;
    //         this.success = success;
    //         this.failure = failure;
    //         this.startSimListener = startSimListener;
    //         this.tries = tries;
    //     }
    // }

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
