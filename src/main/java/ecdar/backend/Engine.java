package ecdar.backend;

import EcdarProtoBuf.ComponentProtos;
import EcdarProtoBuf.ObjectProtos;
import EcdarProtoBuf.QueryProtos;
import com.google.gson.JsonObject;
import ecdar.Ecdar;
import ecdar.abstractions.Component;
import ecdar.abstractions.Query;
import ecdar.simulation.SimulationState;
import ecdar.utility.serialize.Serializable;
import io.grpc.stub.StreamObserver;
import javafx.beans.property.SimpleBooleanProperty;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

public class Engine implements Serializable {
    private static final String NAME = "name";
    private static final String IS_LOCAL = "isLocal";
    private static final String IS_DEFAULT = "isDefault";
    private static final String LOCATION = "location";
    private static final String PORT_RANGE_START = "portRangeStart";
    private static final String PORT_RANGE_END = "portRangeEnd";
    private static final String LOCKED = "locked";
    private static final String IS_THREAD_SAFE = "isThreadSafe";
    private final int responseDeadline = 20000;
    private final int rerunRequestDelay = 200;
    private final int numberOfRetriesPerQuery = 5;

    private String name;
    private boolean isLocal;
    private boolean isDefault;
    private boolean isThreadSafe;
    private int portStart;
    private int portEnd;
    private SimpleBooleanProperty locked = new SimpleBooleanProperty(false);
    /**
     * This is either a path to the engines executable or an IP address at which the engine is running
     */
    private String engineLocation;

    private final ArrayList<EngineConnection> startedConnections = new ArrayList<>();
    private final BlockingQueue<GrpcRequest> requestQueue = new ArrayBlockingQueue<>(200); // Magic number
    // ToDo NIELS: Refactor to resize queue on port range change
    private final BlockingQueue<EngineConnection> availableConnections = new ArrayBlockingQueue<>(200); // Magic number
    private final EngineConnectionStarter connectionStarter = new EngineConnectionStarter(this);

    public Engine() {
        GrpcRequestConsumer consumer = new GrpcRequestConsumer();
        Thread consumerThread = new Thread(consumer);
        consumerThread.start();
    }

    public Engine(final JsonObject jsonObject) {
        this();
        deserialize(jsonObject);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isLocal() {
        return isLocal;
    }

    public void setLocal(boolean local) {
        isLocal = local;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public void setDefault(boolean aDefault) {
        isDefault = aDefault;
    }

    public boolean isThreadSafe() {
        return isThreadSafe;
    }

    public void setIsThreadSafe(boolean threadSafe) {
        isThreadSafe = threadSafe;
    }

    public String getEngineLocation() {
        return engineLocation;
    }

    public void setEngineLocation(String engineLocation) {
        this.engineLocation = engineLocation;
    }

    public String getIpAddress() {
        if (isLocal()) {
            return "127.0.0.1";
        } else {
            return getEngineLocation();
        }
    }

    public int getPortStart() {
        return portStart;
    }

    public void setPortStart(int portStart) {
        this.portStart = portStart;
    }

    public int getPortEnd() {
        return portEnd;
    }

    public void setPortEnd(int portEnd) {
        this.portEnd = portEnd;
    }

    public int getNumberOfInstances() {
        return this.portEnd - this.portStart + 1;
    }

    public void lockInstance() {
        locked.set(true);
    }

    public SimpleBooleanProperty getLockedProperty() {
        return locked;
    }

    public ArrayList<EngineConnection> getStartedConnections() {
        return startedConnections;
    }

    /**
     * Enqueue query for execution with consumers for success and error
     *
     * @param query to enqueue for execution
     * @param successConsumer for returned QueryResponse
     * @param errorConsumer for any throwable that might result from the execution
     */
    public void enqueueQuery(Query query, Consumer<QueryProtos.QueryResponse> successConsumer, Consumer<Throwable> errorConsumer) {
        GrpcRequest request = new GrpcRequest(engineConnection -> {
            var componentsInfoBuilder = BackendHelper.getComponentsInfoBuilder(query.getQuery());

            StreamObserver<QueryProtos.QueryResponse> responseObserver = new StreamObserver<>() {
                @Override
                public void onNext(QueryProtos.QueryResponse value) {
                    successConsumer.accept(value);
                }

                @Override
                public void onError(Throwable t) {
                    errorConsumer.accept(t);
                    setConnectionAsAvailable(engineConnection);
                }

                @Override
                public void onCompleted() {
                    // Release engine connection
                    setConnectionAsAvailable(engineConnection);
                }
            };

            var queryBuilder = QueryProtos.QueryRequest.newBuilder()
                    .setUserId(1)
                    .setQueryId(UUID.randomUUID().hashCode())
                    .setSettings(QueryProtos.QueryRequest.Settings.newBuilder().setDisableClockReduction(true))
                    .setQuery(query.getType().getQueryName() + ": " + query.getQuery())
                    .setComponentsInfo(componentsInfoBuilder);

            engineConnection.getStub().withDeadlineAfter(responseDeadline, TimeUnit.MILLISECONDS)
                    .sendQuery(queryBuilder.build(), responseObserver);
        });

        requestQueue.add(request);
    }

    /**
     * Enqueue request of initial simulation step with consumers for success and error
     *
     * @param composition of the current simulated query
     * @param stepConsumer for the resulting step
     * @param errorConsumer for potential errors
     */
    public void enqueueInitialSimulationStepRequest(String composition, Consumer<QueryProtos.SimulationStepResponse> stepConsumer, Consumer<Throwable> errorConsumer) {
        GrpcRequest request = new GrpcRequest(engineConnection -> {
            StreamObserver<QueryProtos.SimulationStepResponse> responseObserver = getSimulationResponseObserver(stepConsumer, errorConsumer);

            var comInfo = ComponentProtos.ComponentsInfo.newBuilder();
            for (Component c : Ecdar.getProject().getComponents()) {
                comInfo.addComponents(ComponentProtos.Component.newBuilder().setJson(c.serialize().toString()).build());
            }

            comInfo.setComponentsHash(comInfo.getComponentsList().hashCode());
            var simStartRequest = QueryProtos.SimulationStartRequest.newBuilder();
            var simInfo = QueryProtos.SimulationInfo.newBuilder()
                    .setComponentComposition(composition)
                    .setComponentsInfo(comInfo);
            simStartRequest.setSimulationInfo(simInfo);
            engineConnection.getStub().withDeadlineAfter(responseDeadline, TimeUnit.MILLISECONDS)
                    .startSimulation(simStartRequest.build(), responseObserver);
        });

        requestQueue.add(request);
    }

    /**
     * Enqueue request of initial simulation step with consumers for success and error
     *
     * @param composition of the current simulated query
     * @param stepConsumer for the resulting step
     * @param errorConsumer for potential errors
     */
    public void enqueueSimulationStepRequest(String composition, SimulationState state, String edgeId, String componentName, int componentId, Consumer<QueryProtos.SimulationStepResponse> stepConsumer, Consumer<Throwable> errorConsumer) {
        GrpcRequest request = new GrpcRequest(engineConnection -> {
            StreamObserver<QueryProtos.SimulationStepResponse> responseObserver = getSimulationResponseObserver(stepConsumer, errorConsumer);

            var comInfo = ComponentProtos.ComponentsInfo.newBuilder();
            for (Component c : Ecdar.getProject().getComponents()) {
                comInfo.addComponents(ComponentProtos.Component.newBuilder().setJson(c.serialize().toString()).build());
            }

            comInfo.setComponentsHash(comInfo.getComponentsList().hashCode());
            var simStepRequest = QueryProtos.SimulationStepRequest.newBuilder();
            var simInfo = QueryProtos.SimulationInfo.newBuilder()
                    .setComponentComposition(composition)
                    .setComponentsInfo(comInfo);
            simStepRequest.setSimulationInfo(simInfo);
            var specComp = ObjectProtos.SpecificComponent.newBuilder().setComponentName(componentName).setComponentIndex(componentId);
            var edge = EcdarProtoBuf.ObjectProtos.Edge.newBuilder().setId(edgeId).setSpecificComponent(specComp);
            var decision = ObjectProtos.Decision.newBuilder().setEdge(edge).setSource(state.getState());
            simStepRequest.setChosenDecision(decision);

            engineConnection.getStub().withDeadlineAfter(responseDeadline, TimeUnit.MILLISECONDS)
                    .takeSimulationStep(simStepRequest.build(), responseObserver);
        });

        requestQueue.add(request);
    }

    private static StreamObserver<QueryProtos.SimulationStepResponse> getSimulationResponseObserver(Consumer<QueryProtos.SimulationStepResponse> stepConsumer, Consumer<Throwable> errorConsumer) {
        return new StreamObserver<>() {
            @Override
            public void onNext(QueryProtos.SimulationStepResponse value) {
                stepConsumer.accept(value);
            }

            @Override
            public void onError(Throwable t) {
                errorConsumer.accept(t);
            }

            @Override
            public void onCompleted() {}
        };
    }

    /**
     * Signal that the EngineConnection can be used not in use and available for queries
     *
     * @param connection to make available
     */
    public void setConnectionAsAvailable(EngineConnection connection) {
        if (!availableConnections.contains(connection)) availableConnections.add(connection);
    }

    /**
     * Clears all queued queries, stops all active engines, and closes all open engine connections
     */
    public void clear() throws BackendException {
        BackendHelper.stopQueries();
        requestQueue.clear();
        closeConnections();
    }

    /**
     * Filters the list of open {@link EngineConnection}s to the specified {@link Engine} and returns the
     * first match or attempts to start a new connection if none is found.
     *
     * @return a EngineConnection object linked to the engine, either from the open engine connection list
     * or a newly started connection.
     * @throws BackendException.NoAvailableEngineConnectionException if unable to retrieve a connection to the engine
     *                                                               and unable to start a new one
     */
    private EngineConnection getConnection() throws BackendException.NoAvailableEngineConnectionException {
        EngineConnection connection;
        try {
            // If no open connection is free, attempt to start a new one
            if (availableConnections.size() < 1) {
                EngineConnection newConnection = this.connectionStarter.tryStartNewConnection();

                if (newConnection != null) {
                    startedConnections.add(newConnection);
                    setConnectionAsAvailable(newConnection);
                }
            }

            if (isThreadSafe){
                connection = availableConnections.peek();
            }
            else{
                // Block until a connection becomes available
                connection = availableConnections.take();
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        return connection;
    }

     /**
     * Close all open engine connections and kill all locally running processes
     *
     * @throws BackendException if one or more connections throw an exception on {@link EngineConnection#close()}
     *                          (use getSuppressed() to see all thrown exceptions)
     */
    public void closeConnections() throws BackendException {
        // Create a list for storing all terminated connection
        List<CompletableFuture<EngineConnection>> closeFutures = new ArrayList<>();
        BackendException exceptions = new BackendException("Exceptions were thrown while attempting to close engine connections on " + getName());

        // Attempt to close all connections
        for (EngineConnection ec : startedConnections) {
            CompletableFuture<EngineConnection> closeFuture = CompletableFuture.supplyAsync(() -> {
                try {
                    ec.close();
                } catch (BackendException.gRpcChannelShutdownException |
                         BackendException.EngineProcessDestructionException e) {
                    throw new RuntimeException(e);
                }

                return ec;
            });

            closeFutures.add(closeFuture);
        }

        for (CompletableFuture<EngineConnection> closeFuture : closeFutures) {
            try {
                EngineConnection ec = closeFuture.get();

                availableConnections.remove(ec);
                startedConnections.remove(ec);
            } catch (InterruptedException | ExecutionException e) {
                exceptions.addSuppressed(e.getCause());
            }
        }

        if (!startedConnections.isEmpty()) throw exceptions;
    }

    private class GrpcRequestConsumer implements Runnable {
        @Override
        public void run() {
            while (true) {
                try {
                    GrpcRequest request = requestQueue.take();

                    try {
                        request.tries++;
                        request.execute(getConnection());
                    } catch (BackendException.NoAvailableEngineConnectionException e) {
                        e.printStackTrace();
                        if (request.tries < numberOfRetriesPerQuery) {
                            new Timer().schedule(new TimerTask() {
                                @Override
                                public void run() {
                                    requestQueue.add(request);
                                }
                            }, rerunRequestDelay);
                        } else {
                            Ecdar.showToast("Unable to find a connection to the requested engine");
                        }
                        return;
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public JsonObject serialize() {
        final JsonObject result = new JsonObject();
        result.addProperty(NAME, getName());
        result.addProperty(IS_LOCAL, isLocal());
        result.addProperty(IS_DEFAULT, isDefault());
        result.addProperty(IS_THREAD_SAFE, isThreadSafe());
        result.addProperty(LOCATION, getEngineLocation());
        result.addProperty(PORT_RANGE_START, getPortStart());
        result.addProperty(PORT_RANGE_END, getPortEnd());
        result.addProperty(LOCKED, getLockedProperty().get());

        return result;
    }

    @Override
    public void deserialize(final JsonObject json) {
        setName(json.getAsJsonPrimitive(NAME).getAsString());
        setLocal(json.getAsJsonPrimitive(IS_LOCAL).getAsBoolean());
        setDefault(json.getAsJsonPrimitive(IS_DEFAULT).getAsBoolean());
        setIsThreadSafe(json.has(IS_THREAD_SAFE) && json.getAsJsonPrimitive(IS_THREAD_SAFE).getAsBoolean());
        setEngineLocation(json.getAsJsonPrimitive(LOCATION).getAsString());
        setPortStart(json.getAsJsonPrimitive(PORT_RANGE_START).getAsInt());
        setPortEnd(json.getAsJsonPrimitive(PORT_RANGE_END).getAsInt());
        if (json.getAsJsonPrimitive(LOCKED).getAsBoolean()) lockInstance();
    }

    @Override
    public String toString() {
        return name;
    }
}
