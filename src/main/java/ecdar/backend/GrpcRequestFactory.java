package ecdar.backend;

import EcdarProtoBuf.ComponentProtos;
import EcdarProtoBuf.QueryProtos;
import ecdar.Ecdar;
import ecdar.abstractions.Component;
import ecdar.abstractions.Query;
import ecdar.abstractions.Decision;
import io.grpc.stub.StreamObserver;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class GrpcRequestFactory {
    private final Runnable onNext;
    private final Consumer<EngineConnection> onFinished;
    private static final int responseDeadline = 20000;

    public GrpcRequestFactory(Runnable onNext, Consumer<EngineConnection> onFinished) {
        this.onNext = onNext;
        this.onFinished = onFinished;
    }

    public GrpcRequest create(Query query, Consumer<QueryProtos.QueryResponse> queryResponseConsumer, Consumer<Throwable> errorConsumer) {
        return new GrpcRequest(engineConnection -> {
            StreamObserver<QueryProtos.QueryResponse> responseObserver = getResponseStreamObserver(engineConnection, queryResponseConsumer, errorConsumer);

            var componentsInfoBuilder = BackendHelper.getComponentsInfoBuilder(query.getQuery());

            var queryBuilder = QueryProtos.QueryRequest.newBuilder()
                    .setUserId(1)
                    .setQueryId(UUID.randomUUID().hashCode())
                    .setSettings(QueryProtos.QueryRequest.Settings.newBuilder().setDisableClockReduction(true))
                    .setQuery(query.getType().getQueryName() + ": " + query.getQuery())
                    .setComponentsInfo(componentsInfoBuilder);

            engineConnection.getStub().withDeadlineAfter(responseDeadline, TimeUnit.MILLISECONDS)
                    .sendQuery(queryBuilder.build(), responseObserver);
        });
    }

    public GrpcRequest create(Decision decision, Consumer<QueryProtos.SimulationStepResponse> simulationStepResponseConsumer, Consumer<Throwable> errorConsumer) {
        if (decision.isInitial()) {
            return createInitialSimulationStepRequest(decision, simulationStepResponseConsumer, errorConsumer);
        }

        return createSimulationStepRequest(decision, simulationStepResponseConsumer, errorConsumer);
    }

    private GrpcRequest createInitialSimulationStepRequest(Decision step, Consumer<QueryProtos.SimulationStepResponse> simulationStepResponseConsumer, Consumer<Throwable> errorConsumer) {
        return new GrpcRequest(engineConnection -> {
            StreamObserver<QueryProtos.SimulationStepResponse> responseObserver = getResponseStreamObserver(engineConnection, simulationStepResponseConsumer, errorConsumer);
            ComponentProtos.ComponentsInfo.Builder comInfo = getComponentInfoBuilder();

            var simStartRequest = QueryProtos.SimulationStartRequest.newBuilder();
            var simInfo = QueryProtos.SimulationInfo.newBuilder()
                    .setComponentComposition(step.composition)
                    .setComponentsInfo(comInfo);
            simStartRequest.setSimulationInfo(simInfo);
            engineConnection.getStub().withDeadlineAfter(responseDeadline, TimeUnit.MILLISECONDS)
                    .startSimulation(simStartRequest.build(), responseObserver);
        });
    }

    private GrpcRequest createSimulationStepRequest(Decision decision, Consumer<QueryProtos.SimulationStepResponse> simulationStepResponseConsumer, Consumer<Throwable> errorConsumer) {
        return new GrpcRequest(engineConnection -> {
            StreamObserver<QueryProtos.SimulationStepResponse> responseObserver = getResponseStreamObserver(engineConnection, simulationStepResponseConsumer, errorConsumer);

            ComponentProtos.ComponentsInfo.Builder comInfo = getComponentInfoBuilder();

            var simStepRequest = QueryProtos.SimulationStepRequest.newBuilder();
            var simInfo = QueryProtos.SimulationInfo.newBuilder()
                    .setComponentComposition(decision.composition)
                    .setComponentsInfo(comInfo);
            simStepRequest.setSimulationInfo(simInfo);

            // ToDo NIELS: Handle below (might not be necessary)
//            var specComp = ObjectProtos.ComponentInstance.newBuilder().setComponentName(decision.componentName).setComponentIndex(decision.componentId);

            simStepRequest.setChosenDecision(decision.protoDecision);
            engineConnection.getStub().withDeadlineAfter(responseDeadline, TimeUnit.MILLISECONDS)
                    .takeSimulationStep(simStepRequest.build(), responseObserver);
        });
    }

    private <T> StreamObserver<T> getResponseStreamObserver(EngineConnection connection, Consumer<T> queryResponseConsumer, Consumer<Throwable> errorConsumer) {
        return new StreamObserver<>() {
            @Override
            public void onNext(T value) {
                onNext.run();
                queryResponseConsumer.accept(value);
            }

            @Override
            public void onError(Throwable t) {
                onFinished.accept(connection);
                errorConsumer.accept(t);
            }

            @Override
            public void onCompleted() {
                onFinished.accept(connection);
            }
        };
    }

    private ComponentProtos.ComponentsInfo.Builder getComponentInfoBuilder() {
        var comInfo = ComponentProtos.ComponentsInfo.newBuilder();
        for (Component c : Ecdar.getProject().getComponents()) {
            comInfo.addComponents(ComponentProtos.Component.newBuilder().setJson(c.serialize().toString()).build());
        }
        comInfo.setComponentsHash(comInfo.getComponentsList().hashCode());
        return comInfo;
    }
}
