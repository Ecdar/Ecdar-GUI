package ecdar.backend;

import EcdarProtoBuf.ComponentProtos;
import EcdarProtoBuf.EcdarBackendGrpc;
import EcdarProtoBuf.QueryProtos;
import com.google.protobuf.Empty;
import ecdar.Ecdar;
import ecdar.abstractions.Engine;
import ecdar.abstractions.Component;
import io.grpc.*;
import io.grpc.stub.StreamObserver;
import org.springframework.util.SocketUtils;

import java.io.*;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class BackendDriver {
    private final int responseDeadline = 20000;
    private final int rerunRequestDelay = 200;
    private final int numberOfRetriesPerQuery = 5;
    private final int maxRetriesForStartingEngineProcess = 3;

    private final ArrayList<EngineConnection> startedEngineConnections = new ArrayList<>();
    private final Map<Engine, BlockingQueue<EngineConnection>> availableEngineConnections = new HashMap<>();
    private final BlockingQueue<GrpcRequest> requestQueue = new ArrayBlockingQueue<>(200);

    public BackendDriver() {
        GrpcRequestConsumer consumer = new GrpcRequestConsumer();
        Thread consumerThread = new Thread(consumer);
        consumerThread.start();
    }

    public int getResponseDeadline() {
        return responseDeadline;
    }

    /**
     * Add a GrpcRequest to the request queue to be executed when an engine is available
     *
     * @param request The GrpcRequest to be executed later
     */
    public void addRequestToExecutionQueue(GrpcRequest request) {
        requestQueue.add(request);
    }

    /**
     * Signal that the EngineConnection can be used not in use and available for queries
     *
     * @param connection to make available
     */
    public void setConnectionAsAvailable(EngineConnection connection) {
        var relatedQueue = this.availableEngineConnections.get(connection.getEngine());
        if (!relatedQueue.contains(connection)) relatedQueue.add(connection);
    }

    /**
     * Close all engine connections and stop all queries
     */
    public void reset() throws BackendException {
        BackendHelper.stopQueries();
        requestQueue.clear();
        closeAllEngineConnections();
    }

    /**
     * Filters the list of open {@link EngineConnection}s to the specified {@link Engine} and returns the
     * first match or attempts to start a new connection if none is found.
     *
     * @param engine engine to get a connection to (e.g. Reveaal, j-Ecdar, custom_engine)
     * @return a EngineConnection object linked to the engine, either from the open engine connection list
     * or a newly started connection.
     * @throws BackendException.NoAvailableEngineConnectionException if unable to retrieve a connection to the engine
     *                                                               and unable to start a new one
     */
    private EngineConnection getEngineConnection(Engine engine) throws BackendException.NoAvailableEngineConnectionException {
        EngineConnection connection;
        try {
            if (!availableEngineConnections.containsKey(engine))
                availableEngineConnections.put(engine, new ArrayBlockingQueue<>(engine.getNumberOfInstances() + 1));

            // If no open connection is free, attempt to start a new one
            if (availableEngineConnections.get(engine).size() < 1) {
                tryStartNewEngineConnection(engine);
            }

            // Blocks until a connection becomes available
            connection = availableEngineConnections.get(engine).take();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        return connection;
    }

    /**
     * Attempts to start a new connection to the specified engine. On success, the engine is added to the associated
     * queue, otherwise, nothing happens.
     *
     * @param engine the target engine for the connection
     */
    private void tryStartNewEngineConnection(Engine engine) {
        EngineConnection newConnection;

        if (engine.isLocal()) {
            newConnection = startAndGetConnectionToEngineProcess(engine);
        } else {
            newConnection = startAndGetConnectionToRemoteEngine(engine);
        }

        // If the connection is null, no new connection was started
        if (newConnection == null) return;

        startedEngineConnections.add(newConnection);

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
                    newConnection.close();
                } catch (BackendException.gRpcChannelShutdownException |
                         BackendException.EngineProcessDestructionException e) {
                    Ecdar.showToast("An error occurred while trying to start new connection to: \"" + engine.getName() + "\" and an exception was thrown while trying to remove gRPC channel and potential process");
                }
                startedEngineConnections.remove(newConnection);
            }

            @Override
            public void onCompleted() {
                if (startedEngineConnections.contains(newConnection)) setConnectionAsAvailable(newConnection);
            }
        };

        newConnection.getStub().withDeadlineAfter(responseDeadline, TimeUnit.MILLISECONDS)
                .updateComponents(componentsBuilder.build(), observer);
    }

    /**
     * Starts a process, creates an EngineConnection to it, and returns that connection
     *
     * @param engine to run in the process
     * @return an EngineConnection to a local engine running in a Process or null if all ports are already in use
     */
    private EngineConnection startAndGetConnectionToEngineProcess(final Engine engine) {
        long port;
        try {
            port = SocketUtils.findAvailableTcpPort(engine.getPortStart(), engine.getPortEnd());
        } catch (IllegalStateException e) {
            // All ports specified for engine are already used for running engines
            return null;
        }

        // Start local process of engine
        Process p;
        int attempts = 0;

        do {
            attempts++;
            ProcessBuilder pb = new ProcessBuilder(engine.getEngineLocation(), "-p", "127.0.0.1:" + port);

            try {
                p = pb.start();
            } catch (IOException ioException) {
                Ecdar.showToast("Unable to start local engine instance");
                ioException.printStackTrace();
                return null;
            }
        } while (!p.isAlive() && attempts < maxRetriesForStartingEngineProcess);

        ManagedChannel channel = startGrpcChannel("127.0.0.1", port);
        EcdarBackendGrpc.EcdarBackendStub stub = EcdarBackendGrpc.newStub(channel);
        return new EngineConnection(engine, channel, stub, p);
    }

    /**
     * Creates and returns an EngineConnection to the remote engine
     *
     * @param engine to connect to
     * @return an EngineConnection to a remote engine or null if all ports are already connected to
     */
    private EngineConnection startAndGetConnectionToRemoteEngine(Engine engine) {
        // Get a stream of ports already used for connections
        Supplier<Stream<Integer>> activeEnginePortsStream = () -> startedEngineConnections.stream()
                .mapToInt(EngineConnection::getPort).boxed();

        long port = engine.getPortStart();
        for (int currentPort = engine.getPortStart(); currentPort <= engine.getPortEnd(); currentPort++) {
            final int tempPort = currentPort;
            if (activeEnginePortsStream.get().anyMatch((i) -> i == tempPort)) {
                port = currentPort;
                break;
            }
        }

        if (port > engine.getPortEnd()) {
            // All ports specified for engine are already used for connections
            return null;
        }

        ManagedChannel channel = startGrpcChannel(engine.getEngineLocation(), port);
        EcdarBackendGrpc.EcdarBackendStub stub = EcdarBackendGrpc.newStub(channel);
        return new EngineConnection(engine, channel, stub);
    }

    /**
     * Connects a gRPC channel to the address at the specified port, expecting that an engine is running there
     *
     * @param address of the target engine
     * @param port    of the target engine at the address
     * @return the created gRPC channel
     */
    private ManagedChannel startGrpcChannel(final String address, final long port) {
        return ManagedChannelBuilder.forTarget(address + ":" + port)
                .usePlaintext()
                .keepAliveTime(1000, TimeUnit.MILLISECONDS)
                .build();
    }

    /**
     * Close all open engine connection and kill all locally running processes
     *
     * @throws BackendException if one or more connections throw an exception on {@link EngineConnection#close()}
     * (use getSuppressed() to see all thrown exceptions)
     */
    private void closeAllEngineConnections() throws BackendException {
        // Create a list for storing all terminated connection
        ArrayList<EngineConnection> terminatedConnections = new ArrayList<>();
        BackendException exceptions = new BackendException("Exceptions were thrown while attempting to close engine connections");

        // Attempt to close all connections
        for (EngineConnection ec : startedEngineConnections) {
            try {
                ec.close();

                // If connection is not available,
                availableEngineConnections.get(ec.getEngine()).remove(ec);
                terminatedConnections.add(ec);
            } catch (BackendException.gRpcChannelShutdownException |
                     BackendException.EngineProcessDestructionException e) {
                exceptions.addSuppressed(e);
            }
        }

        // Remove all successfully terminated connections
        startedEngineConnections.removeAll(terminatedConnections);
        if (!startedEngineConnections.isEmpty()) throw exceptions;
    }

    private class GrpcRequestConsumer implements Runnable {
        @Override
        public void run() {
            while (true) {
                try {
                    GrpcRequest request = requestQueue.take();

                    try {
                        request.tries++;
                        request.execute(getEngineConnection(request.getEngine()));
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
}
