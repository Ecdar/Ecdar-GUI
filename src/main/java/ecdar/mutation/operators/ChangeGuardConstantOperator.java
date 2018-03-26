package ecdar.mutation.operators;

import ecdar.abstractions.Component;
import ecdar.abstractions.Edge;
import ecdar.mutation.TextFlowBuilder;
import ecdar.mutation.models.MutationTestCase;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Mutation operator that changes a constant in a guard.
 */
public class ChangeGuardConstantOperator extends MutationOperator {
    @Override
    public String getText() {
        return "Change guard constant";
    }

    @Override
    public String getCodeName() {
        return "changeGuardConstant";
    }

    @Override
    public List<MutationTestCase> generateTestCases(final Component original) {
        final List<MutationTestCase> cases = new ArrayList<>();

        // For all edges in the original component
        for (int edgeIndex = 0; edgeIndex < original.getEdges().size(); edgeIndex++) {
            final Edge originalEdge = original.getEdges().get(edgeIndex);

            // Ignore if locked (e.g. if edge on the Inconsistent or Universal locations)
            if (originalEdge.getIsLocked().get()) continue;

            final String originalGuard = originalEdge.getGuard();
            final Matcher matcher = Pattern.compile("(\\d+)").matcher(originalGuard);

            int index = 0;
            while (matcher.find()) {
                {
                    final Component mutant = original.cloneForVerification();
                    final Edge mutantEdge = mutant.getEdges().get(edgeIndex);
                    final int newNumber = Integer.parseInt(matcher.group(1)) + 1;

                    mutantEdge.setGuard(originalGuard.substring(0, matcher.start()) + newNumber + originalGuard.substring(matcher.end()));

                    cases.add(new MutationTestCase(original, mutant,
                            getCodeName() + "_" + edgeIndex + "_" + index + "_+1",
                            new TextFlowBuilder().text("Changed ").boldText("guard").text(" of ")
                                    .edgeLinks(originalEdge, original.getName()).text(" from ").boldText(originalGuard)
                                    .text(" to ").boldText(mutantEdge.getGuard()).build()
                    ));
                } {
                    final Component mutant = original.cloneForVerification();
                    final Edge mutantEdge = mutant.getEdges().get(edgeIndex);
                    final int newNumber = Integer.parseInt(matcher.group(1)) -1;

                    mutantEdge.setGuard(originalGuard.substring(0, matcher.start()) + newNumber + originalGuard.substring(matcher.end()));

                    cases.add(new MutationTestCase(original, mutant,
                            getCodeName() + "_" + edgeIndex + "_" + index + "_-1",
                            new TextFlowBuilder().text("Changed ").boldText("guard").text(" of ")
                                    .edgeLinks(originalEdge, original.getName()).text(" from ").boldText(originalGuard)
                                    .text(" to ").boldText(mutantEdge.getGuard()).build()
                    ));
                }

                index++;
            }
        }

        return cases;
    }

    @Override
    public String getDescription() {
        return "Adds or subtracts 1 to or from a constant in a guard.";
    }

    @Override
    public int getUpperLimit(final Component original) {
        int count = 0;

        // For all edges in the original component
        for (int edgeIndex = 0; edgeIndex < original.getEdges().size(); edgeIndex++) {
            final Edge originalEdge = original.getEdges().get(edgeIndex);

            // Ignore if locked (e.g. if edge on the Inconsistent or Universal locations)
            if (originalEdge.getIsLocked().get()) continue;

            final String originalGuard = originalEdge.getGuard();
            final Matcher matcher = Pattern.compile("(\\d+)").matcher(originalGuard);

            while (matcher.find()) count++;
        }

        return 2 * count;
    }

    @Override
    public boolean isUpperLimitExact() {
        return true;
    }
}
