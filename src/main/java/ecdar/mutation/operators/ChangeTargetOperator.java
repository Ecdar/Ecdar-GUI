package ecdar.mutation.operators;

import ecdar.abstractions.Component;
import ecdar.abstractions.Edge;
import ecdar.abstractions.Location;
import ecdar.mutation.models.MutationTestCase;

import java.util.ArrayList;
import java.util.List;

/**
 * Generates mutants from a component.
 * Each mutant has a changed target location on an edge.
 */
public class ChangeTargetOperator extends MutationOperator {
    @Override
    public String getText() {
        return "Change target";
    }

    @Override
    public String getCodeName() {
        return "changeTarget";
    }

    @Override
    public List<MutationTestCase> generateTestCases(final Component original) {
        final List<MutationTestCase> cases = new ArrayList<>();

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

                cases.add(new MutationTestCase(original, mutant,
                        getCodeName() + "_" + edgeIndex + "_" + originalLocation.getId(),
                        "Changed target of edge " + originalEdge.getSourceLocation().getId() + " -> " +
                                originalEdge.getTargetLocation().getId() + " to " + newLocId)
                );
            }
        }

        return cases;
    }

    @Override
    public String getDescription() {
        return "Changes the target location of an edge. " +
                "Creates up to ([# of locations] - 1) * [# of edges] mutants.";
    }
}
