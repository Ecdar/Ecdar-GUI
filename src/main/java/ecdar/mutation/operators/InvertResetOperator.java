package ecdar.mutation.operators;

import com.google.common.collect.Lists;
import ecdar.abstractions.Component;
import ecdar.abstractions.Edge;
import ecdar.mutation.TextFlowBuilder;
import ecdar.mutation.models.MutationTestCase;

import java.util.ArrayList;
import java.util.List;

/**
 * Mutation operator that inverts a clock reset on an update property.
 */
public class InvertResetOperator extends MutationOperator {
    @Override
    public String getText() {
        return "Invert reset";
    }

    @Override
    public String getCodeName() {
        return "invertReset";
    }

    @Override
    public List<MutationTestCase> generateTestCases(final Component original) {
        final List<String> clocks = original.getClocks();

        final List<MutationTestCase> cases = new ArrayList<>();

        // For all edges in the original component
        for (int edgeIndex = 0; edgeIndex < original.getEdges().size(); edgeIndex++) {
            final Edge originalEdge = original.getEdges().get(edgeIndex);

            // Ignore if locked (e.g. if edge on the Inconsistent or Universal locations)
            if (originalEdge.getIsLocked().get()) continue;

            // For each clock
            final int finalEdgeIndex = edgeIndex;
            clocks.forEach(clock -> {
                final Component mutant = original.cloneForVerification();
                final Edge mutantEdge = mutant.getEdges().get(finalEdgeIndex);

                // Mutate
                invertClock(mutantEdge, clock);

                cases.add(new MutationTestCase(original, mutant,
                        getCodeName() + "_" + finalEdgeIndex + "_" + clock,
                        new TextFlowBuilder().text("Inverted ").boldText("clock reset").text(" of clock ")
                                .boldText(clock).text(" on guard of ").edgeLinks(originalEdge, original.getName())
                                .build()
                ));
            });
        }

        return cases;
    }

    /**
     * Inverts the reset of a clock on an edge.
     * @param mutantEdge the edge to change
     * @param clock the clock to invert
     */
    private static void invertClock(final Edge mutantEdge, final String clock) {
        // Collect each statement in the update property
        final List<String> statements;
        if (mutantEdge.getUpdate().trim().isEmpty()) statements = new ArrayList<>();
        else statements = Lists.newArrayList(mutantEdge.getUpdate().split(","));

        // Remove reset if exists
        final boolean removed = statements.removeIf(s -> s.matches("^\\s*" + clock + "\\s*:?=\\s*0\\s*$"));

        // If not found, add it
        if (!removed) statements.add(clock + " = 0");

        // Update property
        mutantEdge.setUpdate(String.join(", ", statements));
    }

    @Override
    public String getDescription() {
        return "Inverts a clock reset on an update property. " +
                "If the clock was originally reset, the reset will be removed. " +
                "Otherwise, a reset will be added.";
    }

    @Override
    public int getUpperLimit(final Component original) {
        return original.getEdges().size() * original.getClocks().size();

    }

    @Override
    public boolean isUpperLimitExact() {
        return false;
    }
}
