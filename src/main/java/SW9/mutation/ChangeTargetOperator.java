package SW9.mutation;

import SW9.abstractions.Component;
import SW9.abstractions.Edge;
import SW9.abstractions.Location;

import java.util.ArrayList;
import java.util.List;

/**
 * Generates mutants from a component.
 * Each mutant has a changed target location on an edge.
 * Generates # of edges * (# of locations - 1) mutants.
 */
public class ChangeTargetOperator {
    private final Component original;

    /**
     * Constructor.
     * @param original component to mutate
     */
    public ChangeTargetOperator(final Component original) {
        this.original = original;
    }

    /**
     * Computes mutants.
     * @return the computed mutants
     */
    public List<Component> computeMutants() {
        final List<Component> mutants = new ArrayList<>();

        // For all edges in the original component
        for (int edgeIndex = 0; edgeIndex < original.getEdges().size(); edgeIndex++) {
            final Edge originalEdge = original.getEdges().get(edgeIndex);

            for (final Location originalLocation : original.getLocations()) {
                // Ignore if location is target in original edge
                if (originalEdge.getTargetLocation() == originalLocation) continue;

                final Component mutant = original.cloneForVerification();

                // Mutate
                final Edge mutantEdge = mutant.getEdges().get(edgeIndex);
                final String newLocId = originalLocation.getId();
                mutantEdge.setTargetLocation(mutant.findLocation(newLocId));

                mutants.add(mutant);
            }
        }

        return mutants;
    }
}
