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

public class BackendDriver {
    private final int responseDeadline = 20000;
    private final int rerunRequestDelay = 200;
    private final int numberOfRetriesPerQuery = 5;

    private final List<EngineConnection> startedEngineConnections = new ArrayList<>();
    private final Map<Engine, BlockingQueue<EngineConnection>> availableEngineConnections = new HashMap<>();
    private final BlockingQueue<GrpcRequest> requestQueue = new ArrayBlockingQueue<>(200);

    public BackendDriver() {
        // ToDo NIELS: Consider multiple consumer threads using 'for(int i = 0; i < x; i++) {}'
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

    public void setConnectionAsAvailable(EngineConnection engineConnection) {
        var relatedQueue = this.availableEngineConnections.get(engineConnection.getEngine());
        if (!relatedQueue.contains(engineConnection)) relatedQueue.add(engineConnection);
    }

    /**
     * Close all open engine connection and kill all locally running processes
     *
     * @throws IOException if any of the sockets do not respond
     */
    public void closeAllEngineConnections() throws IOException {
        for (EngineConnection ec : startedEngineConnections) ec.close();
    }

    /**
     * Filters the list of open {@link EngineConnection}s to the specified {@link Engine} and returns the
     * first match or attempts to start a new connection if none is found.
     *
     * @param engine engine to get a connection to (e.g. Reveaal, j-Ecdar, custom_engine)
     * @return a EngineConnection object linked to the engine, either from the open engine connection list
     * or a newly started connection.
     * @throws BackendException.NoAvailableEngineConnectionException if unable to retrieve a connection to the engine
     *                                                                and unable to start a new one
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

            // Block until a connection becomes available
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
        Process p = null;
        String hostAddress = (engine.isLocal() ? "127.0.0.1" : engine.getEngineLocation());
        long portNumber = 0;

        if (engine.isLocal()) {
            try {
                portNumber = SocketUtils.findAvailableTcpPort(engine.getPortStart(), engine.getPortEnd());
            } catch (IllegalStateException e) {
                // No port was available in range, we assume that connections are running on all ports
                return;
            }

            do {
                ProcessBuilder pb = new ProcessBuilder(engine.getEngineLocation(), "-p", hostAddress + ":" + portNumber);

                try {
                    p = pb.start();
                } catch (IOException ioException) {
                    Ecdar.showToast("Unable to start local engine instance");
                    ioException.printStackTrace();
                    return;
                }
                // If the process is not alive, it failed while starting up, try again
            } while (!p.isAlive());
        } else {
            // Filter open connections to this backend and map their used ports to an int stream
            var activeEnginePorts = startedEngineConnections.stream()
                    .mapToInt((bi) -> Integer.parseInt(bi.getStub().getChannel().authority().split(":", 2)[1]));

            int currentPort = engine.getPortStart();
            do {
                // Find port not already connected to
                int tempPortNumber = currentPort;
                if (activeEnginePorts.noneMatch((i) -> i == tempPortNumber)) {
                    portNumber = tempPortNumber;
                } else {
                    currentPort++;
                }
            } while (portNumber == 0 && currentPort <= engine.getPortEnd());

            if (currentPort > engine.getPortEnd()) {
                Ecdar.showToast("Unable to connect to remote engine: " + engine.getName() + " within port range " + engine.getPortStart() + " - " + engine.getPortEnd());
                return;
            }
        }

        ManagedChannel channel = ManagedChannelBuilder.forTarget(hostAddress + ":" + portNumber)
                .usePlaintext()
                .keepAliveTime(1000, TimeUnit.MILLISECONDS)
                .build();

        EcdarBackendGrpc.EcdarBackendStub stub = EcdarBackendGrpc.newStub(channel);
        EngineConnection newConnection = new EngineConnection(engine, p, stub, channel);
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
            }

            @Override
            public void onCompleted() {
                setConnectionAsAvailable(newConnection);
            }
        };

        newConnection.getStub().withDeadlineAfter(responseDeadline, TimeUnit.MILLISECONDS)
                .updateComponents(componentsBuilder.build(), observer);
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
