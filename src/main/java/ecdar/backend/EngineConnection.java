package ecdar.backend;

import EcdarProtoBuf.EcdarBackendGrpc;
import ecdar.abstractions.Engine;
import io.grpc.ManagedChannel;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class EngineConnection {
    private final Engine engine;
    private final EcdarBackendGrpc.EcdarBackendStub stub;
    private final ManagedChannel channel;
    private final Process process;
    private final int port;

    EngineConnection(Engine engine, ManagedChannel channel, EcdarBackendGrpc.EcdarBackendStub stub, Process process) {
        this.engine = engine;
        this.stub = stub;
        this.channel = channel;
        this.process = process;
        this.port = Integer.parseInt(getStub().getChannel().authority().split(":", 2)[1]);
    }

    EngineConnection(Engine engine, ManagedChannel channel, EcdarBackendGrpc.EcdarBackendStub stub) {
        this(engine, channel, stub, null);
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
        return port;
    }

    /**
     * Close the gRPC connection and end the process
     *
     * @throws BackendException.gRpcChannelShutdownException      if an InterruptedException is encountered while trying to shut down the gRPC channel.
     * @throws BackendException.EngineProcessDestructionException if the connected engine process throws an ExecutionException, an InterruptedException, or a TimeoutException.
     */
    public void close() throws BackendException.gRpcChannelShutdownException, BackendException.EngineProcessDestructionException {
        if (!channel.isShutdown()) {
            try {
                channel.shutdown();
                if (!channel.awaitTermination(45, TimeUnit.SECONDS)) {
                    channel.shutdownNow(); // Forcefully close the connection
                }
            } catch (InterruptedException e) {
                // Engine location is either the file path or the IP, here we want the channel address
                throw new BackendException.gRpcChannelShutdownException("The gRPC channel to \"" + this.engine.getName() + "\" instance running at: " + (this.engine.isLocal() ? "127.0.0.1" : this.engine.getEngineLocation()) + ":" + this.port + "was interrupted during termination", e.getCause());
            }
        }

        // If the engine is remote, there will not be a process
        if (process != null) {
            try {
                java.util.concurrent.CompletableFuture<Process> terminated = process.onExit();
                process.destroy();
                terminated.get(45, TimeUnit.SECONDS);
            } catch (ExecutionException | InterruptedException | TimeoutException e) {
                // Add the engine location to the exception, as it contains the path to the executable
                throw new BackendException.EngineProcessDestructionException("A process running: " + this.engine.getEngineLocation() + " on port " + this.port + " threw an exception during shutdown", e.getCause());
            }
        }
    }
}
