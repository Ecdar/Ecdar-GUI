package ecdar.backend;

import java.util.function.Consumer;

public class GrpcRequest {
    private final Consumer<EngineConnection> request;
    public int tries = 0;

    public GrpcRequest(Consumer<EngineConnection> request) {
        this.request = request;
    }

    public void execute(EngineConnection engineConnection) {
        this.request.accept(engineConnection);
    }
}