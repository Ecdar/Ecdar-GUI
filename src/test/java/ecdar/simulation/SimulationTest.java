package ecdar.simulation;

import EcdarProtoBuf.EcdarBackendGrpc;
import EcdarProtoBuf.ObjectProtos;
import EcdarProtoBuf.QueryProtos;
import EcdarProtoBuf.ObjectProtos.DecisionPoint;
import EcdarProtoBuf.ObjectProtos.LocationTuple;
import EcdarProtoBuf.ObjectProtos.SpecificComponent;
import ecdar.abstractions.Component;
import ecdar.abstractions.Location;
import io.grpc.BindableService;
import io.grpc.ManagedChannel;
import io.grpc.Status;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import io.grpc.testing.GrpcCleanupRule;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;

public class SimulationTest {
    public GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();
    private final String serverName = InProcessServerBuilder.generateName();
/*
    @Test
    public void testGetInitialStateHighlightsTheInitialLocation() {
        final List<Component> components = generateComponentsWithInitialLocations();

        BindableService testService = new EcdarBackendGrpc.EcdarBackendImplBase() {
            @Override
            public void startSimulation(QueryProtos.SimulationStartRequest request,
                                        StreamObserver<QueryProtos.SimulationStepResponse> responseObserver) {
                try {
                    ObjectProtos.LocationTuple locations = LocationTuple.newBuilder().addAllLocations(components.stream()
                            .map(c -> ObjectProtos.Location.newBuilder()
                            .setSpecificComponent(SpecificComponent.newBuilder()
                                    .setComponentName(c.getName()))
                            .setId(c.getInitialLocation().getId())
                            .build())
                            .collect(Collectors.toList()))
                        .build();
                    
                    ObjectProtos.State state = ObjectProtos.State.newBuilder().setLocationTuple(locations).build();
                    DecisionPoint decisionPoint = DecisionPoint.newBuilder().setSource(state).build();
                    QueryProtos.SimulationStepResponse response = QueryProtos.SimulationStepResponse.newBuilder()
                            .addNewDecisionPoints(decisionPoint)
                            .build();
                    responseObserver.onNext(response);
                    responseObserver.onCompleted();
                } catch (Throwable e) {
                    responseObserver.onError(Status.INVALID_ARGUMENT.withDescription(e.getMessage()).asException());
                }
            }

            @Override
            public void takeSimulationStep(EcdarProtoBuf.QueryProtos.SimulationStepRequest request,
                                           io.grpc.stub.StreamObserver<EcdarProtoBuf.QueryProtos.SimulationStepResponse> responseObserver) {
            }
        };

        final ManagedChannel channel;
        final EcdarBackendGrpc.EcdarBackendBlockingStub stub;
        try {
            grpcCleanup.register(InProcessServerBuilder
                    .forName(serverName).directExecutor().addService(testService).build().start());
            channel = grpcCleanup.register(InProcessChannelBuilder
                    .forName(serverName).directExecutor().build());
            stub = EcdarBackendGrpc.newBlockingStub(channel);

            QueryProtos.SimulationStartRequest request = QueryProtos.SimulationStartRequest.newBuilder().build();

            var expectedResponse = new ObjectProtos.Location[components.size()];

            for (int i = 0; i < components.size(); i++) {
                Component comp = components.get(i);
                expectedResponse[i] = ObjectProtos.Location.newBuilder()
                        .setSpecificComponent(SpecificComponent.newBuilder().setComponentName(comp.getName()))
                        .setId(comp.getInitialLocation().getId()).build();
            }

            var result = stub.startSimulation(request).getNewDecisionPoints(0).getSource().getLocationTuple().getLocationsList().toArray();

            Assertions.assertArrayEquals(expectedResponse, result);
        } catch (IOException e) {
            Assertions.fail("Exception encountered: " + e.getMessage());
        }
    }
*/
    private List<Component> generateComponentsWithInitialLocations() {
        List<Component> comps = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            var comp = new Component();
            comp.setName(comp + "_" + i);
            var loc = new Location(comp + "_initial");
            loc.setType(Location.Type.INITIAL);
            comp.addLocation(loc);
            comps.add(comp);
        }

        return comps;
    }
}
