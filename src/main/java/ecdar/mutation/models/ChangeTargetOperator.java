package ecdar.mutation.models;

import ecdar.abstractions.Component;
import ecdar.abstractions.Edge;
import ecdar.abstractions.Location;

import java.util.ArrayList;
import java.util.List;

/**
 * Generates mutants from a component.
 * Each mutant has a changed target location on an edge.
 * Generates # of edges * (# of locations - 1) mutants.
 */
public class ChangeTargetOperator extends MutationOperator {
    /**
     * Computes mutants.
     * @return the computed mutants
     */
    @Override
    public List<Component> compute(final Component original) {
        final List<Component> mutants = new ArrayList<>();

        // For all edges in the original component
        for (int edgeIndex = 0; edgeIndex < original.getEdges().size(); edgeIndex++) {
            final Edge originalEdge = original.getEdges().get(edgeIndex);

            // Ignore if locked (e.g. if edge on the Inconsistent or Universal locations)
            if (originalEdge.getIsLocked().get()) continue;

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

    @Override
    public String getText() {
        return "Change target.";
    }

    @Override
    String getJsonName() {
        return "changeTarget";
    }
}
