package ecdar.backend;

import ecdar.abstractions.Engine;

import java.util.function.Consumer;

public class GrpcRequest {
    private final Consumer<EngineConnection> request;
    private final Engine engine;
    public int tries = 0;

    public GrpcRequest(Consumer<EngineConnection> request, Engine engine) {
        this.request = request;
        this.engine = engine;
    }

    public void execute(EngineConnection engineConnection) {
        this.request.accept(engineConnection);
    }

    public Engine getEngine() {
        return engine;
    }
}