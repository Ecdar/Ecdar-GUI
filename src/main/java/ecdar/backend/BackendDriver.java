package ecdar.backend;

import EcdarProtoBuf.ComponentProtos;
import EcdarProtoBuf.EcdarBackendGrpc;
import EcdarProtoBuf.QueryProtos;
import com.google.protobuf.Empty;
import ecdar.Ecdar;
import ecdar.abstractions.BackendInstance;
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
    private final BlockingQueue<GrpcRequest> requestQueue = new ArrayBlockingQueue<>(200);
    private final Map<BackendInstance, BlockingQueue<BackendConnection>> openBackendConnections = new HashMap<>();
    private final int responseDeadline = 20000;
    private final int rerunRequestDelay = 200;
    private final int numberOfRetriesPerQuery = 5;

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
     * Add a GrpcRequest to the request queue to be executed when a backend is available
     *
     * @param request The GrpcRequest to be executed later
     */
    public void addRequestToExecutionQueue(GrpcRequest request) {
        requestQueue.add(request);
    }

    public void addBackendConnection(BackendConnection backendConnection) {
        var relatedQueue = this.openBackendConnections.get(backendConnection.getBackendInstance());
        if (!relatedQueue.contains(backendConnection)) relatedQueue.add(backendConnection);
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
                addBackendConnection(newConnection);
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
                        request.execute(getBackendConnection(request.getBackend()));
                    } catch (BackendException.NoAvailableBackendConnectionException e) {
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
