package ecdar.mutation.models;

import ecdar.abstractions.Component;
import ecdar.abstractions.Edge;
import ecdar.abstractions.EdgeStatus;
import ecdar.abstractions.Location;

import java.util.ArrayList;
import java.util.List;

/**
 * A mutation operator that changes an edge to go to a new sink location.
 * A sink location accepts (but ignores) all inputs and allows for time to pass.
 */
public class SinkLocationOperator extends MutationOperator {
    @Override
    public String getText() {
        return "Sink location";
    }

    @Override
    public String getCodeName() {
        return "sinkLocation";
    }

    @Override
    public List<MutationTestCase> generate(final Component original) {
        final List<MutationTestCase> mutants = new ArrayList<>();

        // For all edges in the original component
        for (int edgeIndex = 0; edgeIndex < original.getEdges().size(); edgeIndex++) {
            final Edge originalEdge = original.getEdges().get(edgeIndex);

            // Ignore if locked (e.g. if edge on the Inconsistent or Universal locations)
            if (originalEdge.getIsLocked().get()) continue;

            final Component mutant = original.cloneForVerification();

            // Mutate
            final Edge mutantEdge = mutant.getEdges().get(edgeIndex);
            mutantEdge.setTargetLocation(addSinkLocation(mutant));

            mutants.add(new MutationTestCase(original, mutant,
                    getCodeName() + "_" + edgeIndex,
                    "Changed target of edge " + originalEdge.getSourceLocation().getId() + " -> " +
                            originalEdge.getTargetLocation().getId() + " to a new sink location"));
        }

        return mutants;
    }

    @Override
    public String getDescription() {
        return "Changes the target location of an edge to a new sink location. " +
               "Sink locations accept, but ignore, all inputs. " +
               "Creates up to [# of edges] mutants.";
    }

    /**
     * Adds a sink location to a component that accepts (but ignores) all inputs and allowed time to pass.
     * @param component the component to add the sink to
     * @return the sink location
     */
    private static Location addSinkLocation(final Component component) {
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
