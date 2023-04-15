package ecdar.mutation.operators;

import ecdar.abstractions.Component;
import ecdar.abstractions.Edge;
import ecdar.abstractions.Location;
import ecdar.mutation.ComponentVerificationTransformer;
import ecdar.mutation.TextFlowBuilder;
import ecdar.mutation.models.MutationTestCase;

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
    public String getCodeName() {
        return "changeSource";
    }

    @Override
    public List<MutationTestCase> generateTestCases(final Component original) {
        final List<MutationTestCase> cases = new ArrayList<>();

        // For all edges in the original component
        for (int edgeIndex = 0; edgeIndex < original.getEdges().size(); edgeIndex++) {
            final Edge originalEdge = original.getEdges().get(edgeIndex);

            // Ignore if locked (e.g. if edge on the Inconsistent or Universal locations)
            if (originalEdge.getIsLockedProperty().get()) continue;

            // Change the source of that edge to (almost) each of the locations
            for (final Location originalLocation : original.getLocations()) {
                // Ignore if location is source in original edge
                if (originalEdge.getSourceLocation() == originalLocation) continue;

                // Ignore if location is the Inconsistent or the Universal locations
                // We do not want to have those as a source location,
                // since it would break their behaviour
                if (originalLocation.getType().equals(Location.Type.INCONSISTENT) || originalLocation.getType().equals(Location.Type.UNIVERSAL))
                    continue;

                final Component mutant = ComponentVerificationTransformer.cloneForVerification(original);

                // Mutate
                final Edge mutantEdge = mutant.getEdges().get(edgeIndex);
                final String newLocId = originalLocation.getId();
                mutantEdge.setSourceLocation(mutant.findLocation(newLocId));

                cases.add(new MutationTestCase(original, mutant,
                        getCodeName() + "_" + edgeIndex + "_" + originalLocation.getId(),
                        new TextFlowBuilder().text("Changed ").boldText("source").text(" of ")
                                .edgeLinks(originalEdge, original.getName()).text(" to ")
                                .locationLink(newLocId, original.getName()).build()
                ));
            }
        }

        return cases;
    }

    @Override
    public String getDescription() {
        return "Changes the source location of an edge.";
    }

    @Override
    public int getUpperLimit(final Component original) {
        return (original.getLocations().size() - 1) * original.getDisplayableEdges().size();
    }

    @Override
    public boolean isUpperLimitExact() {
        return false;
    }
}
