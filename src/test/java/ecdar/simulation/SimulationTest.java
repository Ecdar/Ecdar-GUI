package ecdar.simulation;

import EcdarProtoBuf.EcdarBackendGrpc;
import EcdarProtoBuf.ObjectProtos;
import EcdarProtoBuf.QueryProtos;
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
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;

public class SimulationTest {
    public GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();
    private final String serverName = InProcessServerBuilder.generateName();

    // TODO fix this test
    @Test
    public void testGetInitialStateHighlightsTheInitialLocation() {
        final List<Component> components = generateComponentsWithInitialLocations();

        BindableService testService = new EcdarBackendGrpc.EcdarBackendImplBase() {
            @Override
            public void startSimulation(QueryProtos.SimulationStartRequest request,
                                        StreamObserver<QueryProtos.SimulationStepResponse> responseObserver) {
                try {
                    var leafLocations = components.stream()
                            .map(c -> ObjectProtos.LeafLocation.newBuilder()
                                    .setComponentInstance(ObjectProtos.ComponentInstance.newBuilder()
                                            .setComponentName(c.getName()))
                                    .setId(c.getInitialLocation().getId())
                                    .build())
                            .collect(Collectors.toList());

                    var locationTreeBuilder = ObjectProtos.LocationTree.newBuilder();
                    leafLocations.stream().map(locationTreeBuilder::mergeLeafLocation);

                    var locationTree = locationTreeBuilder.build();
                    
                    ObjectProtos.State state = ObjectProtos.State.newBuilder().setLocationTree(locationTree).build();
                    ObjectProtos.Decision decision = ObjectProtos.Decision.newBuilder().setSource(state).build();
                    QueryProtos.SimulationStepResponse response = QueryProtos.SimulationStepResponse.newBuilder()
                            .addNewDecisionPoints(decision)
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

            var expectedResponse = new ObjectProtos.LocationTree[components.size()];

            for (int i = 0; i < components.size(); i++) {
                Component comp = components.get(i);
                expectedResponse[i] = ObjectProtos.LocationTree.newBuilder()
                        .setLeafLocation(ObjectProtos.LeafLocation.newBuilder().setId(comp.getInitialLocation().getId())
                                .setComponentInstance(ObjectProtos.ComponentInstance
                                        .newBuilder()
                                        .setComponentName(comp.getName()).build()
                                )
                        )
                        .build();
            }

            var result = stub.startSimulation(request).getFullState().getLocationTree();

            Assertions.assertTrue(Arrays.asList(expectedResponse).contains(result));
        } catch (IOException e) {
            Assertions.fail("Exception encountered: " + e.getMessage());
        }
    }

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
