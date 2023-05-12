package ecdar.abstractions;

import EcdarProtoBuf.ObjectProtos;
import EcdarProtoBuf.QueryProtos;
import ecdar.backend.GrpcRequest;
import ecdar.backend.GrpcRequestFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class Decision implements RequestSource<QueryProtos.SimulationStepResponse> {
    public final String composition;
    public final State source;
    public final State target;
    public final List<String> edgeIds;
    public final String action;
    public ObjectProtos.Decision protoDecision;

    public Decision(String composition, State source, State target, List<String> edgeIds, String action) {
        this.composition = composition;
        this.source = source;
        this.target = target;
        this.edgeIds = edgeIds;
        this.action = action;
        this.protoDecision = null;
    }

    /**
     * This constructed is used to represent the decision submitted for the initial state
     * ToDo NIELS: refactor to use a factory pattern for constructing new decisions
     */
    public Decision(String composition) {
        this(composition, null, null, new ArrayList<>(), null);
    }

    public Decision(String composition, ObjectProtos.Decision protoDecision) {
        this(composition,
                new State(protoDecision.getSource()),
                new State(protoDecision.getDestination()),
                protoDecision.getEdgesList().stream().map(ObjectProtos.Edge::getId).collect(Collectors.toList()),
                protoDecision.getAction());

        this.protoDecision = protoDecision; // Save the proto decision for requesting simulation step
    }

    public boolean isInitial() {
        return source == null;
    }

    @Override
    public GrpcRequest accept(GrpcRequestFactory requestFactory,
                              Consumer<QueryProtos.SimulationStepResponse> successConsumer,
                              Consumer<Throwable> errorConsumer) {
        return requestFactory.create(this, successConsumer, errorConsumer);
    }
}
