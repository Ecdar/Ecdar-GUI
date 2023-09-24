package ecdar.abstractions;

import EcdarProtoBuf.ObjectProtos;
import EcdarProtoBuf.QueryProtos;
import ecdar.backend.GrpcRequest;
import ecdar.backend.GrpcRequestFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class Decision implements RequestSource<QueryProtos.SimulationStepResponse> {
    public final String composition;
    public final List<String> edgeIds;
    public final String action;
    public final List<ClockConstraint> clockConstraints;
    public ObjectProtos.Decision protoDecision;

    public Decision(String composition, List<String> edgeIds, String action, List<ClockConstraint> clockConstraints, ObjectProtos.Decision protoDecision) {
        this.composition = composition;
        this.edgeIds = edgeIds;
        this.action = action;
        this.clockConstraints = clockConstraints;
        this.protoDecision = protoDecision;
    }

    /**
     * This constructed is used to represent the decision submitted for the initial state
     * ToDo NIELS: refactor to use a factory pattern for constructing new decisions
     */
    public Decision(String composition) {
        this(composition, new ArrayList<>(), null, null, null);
    }

    public boolean isInitial() {
        return protoDecision == null;
    }

    @Override
    public GrpcRequest accept(GrpcRequestFactory requestFactory,
                              Consumer<QueryProtos.SimulationStepResponse> successConsumer,
                              Consumer<Throwable> errorConsumer) {
        return requestFactory.create(this, successConsumer, errorConsumer);
    }
}
