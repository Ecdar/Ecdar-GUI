package ecdar.mutation.models;

import ecdar.abstractions.Component;
import ecdar.abstractions.Edge;
import ecdar.abstractions.EdgeStatus;
import ecdar.abstractions.Location;
import ecdar.mutation.MutationTestingException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class SinkLocationOperator extends MutationOperator {
    @Override
    public String getText() {
        return "Sink location";
    }

    @Override
    String getJsonName() {
        return "sinkLocation";
    }

    @Override
    public Collection<? extends Component> generate(final Component original) {
        final List<Component> mutants = new ArrayList<>();

        // For all edges in the original component
        for (int edgeIndex = 0; edgeIndex < original.getEdges().size(); edgeIndex++) {
            final Edge originalEdge = original.getEdges().get(edgeIndex);

            // Ignore if locked (e.g. if edge on the Inconsistent or Universal locations)
            if (originalEdge.getIsLocked().get()) continue;

            final Component mutant = original.cloneForVerification();

            // Mutate
            final Edge mutantEdge = mutant.getEdges().get(edgeIndex);
            mutantEdge.setTargetLocation(createSinkLocation(mutant));

            mutants.add(mutant);
        }

        return mutants;
    }

    /**
     * Adds a sink location to a component that accepts (but ignores) all inputs and allowed time to pass.
     * @param component the component to add the sink to
     * @return the sink location
     */
    private static Location createSinkLocation(final Component component) {
        final Location sink = new Location();
        component.addLocation(sink);

        component.updateIOList();

        component.getInputStrings().forEach(input -> {
            final Edge edge = new Edge(sink, EdgeStatus.INPUT);
            edge.setTargetLocation(sink);
            edge.addSyncNail(input);
            component.addEdge(edge);
        });

        return sink;
    }
}
