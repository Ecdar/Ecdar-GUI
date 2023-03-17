package ecdar.backend;

import EcdarProtoBuf.EcdarBackendGrpc;
import ecdar.Ecdar;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.util.SocketUtils;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class EngineConnectionStarter {
    private final Engine engine;
    private final int maxRetriesForStartingEngineProcess = 3;

    EngineConnectionStarter(Engine engine) {
        this.engine = engine;
    }

    /**
     * Attempts to start a new connection to the specified engine.
     *
     * @return the started EngineConnection if successful,
     * otherwise, null.
     */
    protected EngineConnection tryStartNewConnection() {
        EngineConnection newConnection;

        if (engine.isLocal()) {
            newConnection = startLocalConnection();
        } else {
            newConnection = startRemoteConnection();
        }

        // If the connection is null, no new connection was started
        return newConnection;
    }

    /**
     * Starts a process, creates an EngineConnection to it, and returns that connection
     *
     * @return an EngineConnection to a local engine running in a Process or null if all ports are already in use
     */
    private EngineConnection startLocalConnection() {
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
            ProcessBuilder pb = new ProcessBuilder(engine.getEngineLocation(), "-p", engine.getIpAddress() + ":" + port);

            try {
                p = pb.start();
            } catch (IOException ioException) {
                Ecdar.showToast("Unable to start local engine instance");
                ioException.printStackTrace();
                return null;
            }
        } while (!p.isAlive() && attempts < maxRetriesForStartingEngineProcess);

        ManagedChannel channel = startGrpcChannel(engine.getIpAddress(), port);
        EcdarBackendGrpc.EcdarBackendStub stub = EcdarBackendGrpc.newStub(channel);
        return new EngineConnection(engine, channel, stub, p);
    }

    /**
     * Creates and returns an EngineConnection to the remote engine
     *
     * @return an EngineConnection to a remote engine or null if all ports are already connected to
     */
    private EngineConnection startRemoteConnection() {
        // Get a stream of ports already used for connections
        Supplier<Stream<Integer>> activeEnginePortsStream = () -> engine.getStartedConnections().stream()
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

        ManagedChannel channel = startGrpcChannel(engine.getIpAddress(), port);
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
}
