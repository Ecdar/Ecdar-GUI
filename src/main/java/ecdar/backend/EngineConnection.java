package ecdar.backend;

import EcdarProtoBuf.EcdarBackendGrpc;
import ecdar.abstractions.Engine;
import io.grpc.ManagedChannel;

import java.util.concurrent.TimeUnit;

public class EngineConnection {
    private final Process process;
    private final EcdarBackendGrpc.EcdarBackendStub stub;
    private final ManagedChannel channel;
    private final Engine engine;

    EngineConnection(Engine engine, Process process, EcdarBackendGrpc.EcdarBackendStub stub, ManagedChannel channel) {
        this.process = process;
        this.engine = engine;
        this.stub = stub;
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
     * Get the engine that should be used to execute
     * the query currently associated with this engine connection
     *
     * @return the instance of the associated executable query object,
     * or null, if no executable query is currently associated
     */
    public Engine getEngine() {
        return engine;
    }

    public int getPort() {
        return Integer.parseInt(getStub().getChannel().authority().split(":", 2)[1]);
    }

    /**
     * Close the gRPC connection and end the process
     */
    public void close() {
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

        // If the engine is remote, there will not be a process
        if (process != null) {
            process.destroy();
        }
    }
}
