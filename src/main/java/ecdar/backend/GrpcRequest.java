package ecdar.backend;

import java.util.function.Consumer;

public class GrpcRequest {
    private final Consumer<BackendConnection> request;
    private final BackendInstance backend;
    public int tries = 0;

    public GrpcRequest(Consumer<BackendConnection> request, BackendInstance backend) {
        this.request = request;
        this.backend = backend;
    }

    public void execute(BackendConnection backendConnection) {
        this.request.accept(backendConnection);
    }

    public BackendInstance getBackend() {
        return backend;
    }
}