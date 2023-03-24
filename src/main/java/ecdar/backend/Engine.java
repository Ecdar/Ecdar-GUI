package ecdar.backend;

import EcdarProtoBuf.ComponentProtos;
import EcdarProtoBuf.QueryProtos;
import com.google.gson.JsonObject;
import com.google.protobuf.Empty;
import ecdar.Ecdar;
import ecdar.abstractions.Component;
import ecdar.abstractions.Query;
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
    private final int responseDeadline = 20000;
    private final int rerunRequestDelay = 200;
    private final int numberOfRetriesPerQuery = 5;

    private String name;
    private boolean isLocal;
    private boolean isDefault;
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
     * @param query the query to enqueue for execution
     * @param successConsumer consumer for returned QueryResponse
     * @param errorConsumer consumer for any throwable that might result from the execution
     */
    public void enqueueQuery(Query query, Consumer<QueryProtos.QueryResponse> successConsumer, Consumer<Throwable> errorConsumer) {
        GrpcRequest request = new GrpcRequest(engineConnection -> {
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

            var queryBuilder = QueryProtos.Query.newBuilder()
                    .setId(0)
                    .setQuery(query.getType().getQueryName() + ": " + query.getQuery());

            engineConnection.getStub().withDeadlineAfter(responseDeadline, TimeUnit.MILLISECONDS)
                    .sendQuery(queryBuilder.build(), responseObserver);
        });

        requestQueue.add(request);
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
                    initializeConnection(newConnection);
                }
            }

            // Blocks until a connection becomes available
            connection = availableConnections.take();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        return connection;
    }

    /**
     * Executes the gRPC requests required to initialize the connection for query execution
     * NOTE: This will be unnecessary with the SW5 changes for the ProtoBuf
     */
    private void initializeConnection(EngineConnection connection) {
        QueryProtos.ComponentsUpdateRequest.Builder componentsBuilder = QueryProtos.ComponentsUpdateRequest.newBuilder();
        for (Component c : Ecdar.getProject().getComponents()) {
            componentsBuilder.addComponents(ComponentProtos.Component.newBuilder().setJson(c.serialize().toString()).build());
        }

        StreamObserver<Empty> observer = new StreamObserver<>() {
            @Override
            public void onNext(Empty value) {
            }

            @Override
            public void onError(Throwable t) {
                try {
                    connection.close();
                } catch (BackendException.gRpcChannelShutdownException |
                         BackendException.EngineProcessDestructionException e) {
                    Ecdar.showToast("An error occurred while trying to start new connection to: \"" + getName() + "\" and an exception was thrown while trying to remove gRPC channel and potential process");
                }
                startedConnections.remove(connection);
            }

            @Override
            public void onCompleted() {
                if (startedConnections.contains(connection)) setConnectionAsAvailable(connection);
            }
        };

        connection.getStub().withDeadlineAfter(responseDeadline, TimeUnit.MILLISECONDS)
                .updateComponents(componentsBuilder.build(), observer);
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
