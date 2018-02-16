package ecdar.mutation.models;

import ecdar.abstractions.Component;
import ecdar.abstractions.Edge;
import ecdar.abstractions.Location;

import java.util.ArrayList;
import java.util.List;

/**
 * Generates mutants from a component.
 * Each mutant has a changed source location on an edge.
 */
class ChangeSourceOperator extends MutationOperator {
    @Override
    public String getText() {
        return "Change source";
    }

    @Override
    public String getJsonName() {
        return "changeSource";
    }

    @Override
    public List<Component> generate(final Component original) {
        final List<Component> mutants = new ArrayList<>();

        // For all edges in the original component
        for (int edgeIndex = 0; edgeIndex < original.getEdges().size(); edgeIndex++) {
            final Edge originalEdge = original.getEdges().get(edgeIndex);

            // Ignore if locked (e.g. if edge on the Inconsistent or Universal locations)
            if (originalEdge.getIsLocked().get()) continue;

            // Change the source of that edge to (almost) each of the locations
            for (final Location location : original.getLocations()) {
                // Ignore if location is source in original edge
                if (originalEdge.getSourceLocation() == location) continue;

                // Ignore if location is the Inconsistent or the Universal locations
                // We do not want to have those as a source location,
                // since it would break their behaviour
                if (location.getType().equals(Location.Type.INCONSISTENT) || location.getType().equals(Location.Type.UNIVERSAL))
                    continue;

                final Component mutant = original.cloneForVerification();

                // Mutate
                final Edge mutantEdge = mutant.getEdges().get(edgeIndex);
                final String newLocId = location.getId();
                mutantEdge.setSourceLocation(mutant.findLocation(newLocId));

                mutants.add(mutant);
            }
        }

        return mutants;
    }

    @Override
    public String getDescription() {
        return "Changes the source location of an edge. " +
               "Creates up to ([# of locations] - 1) * [# of edges] mutants.";
    }
}
