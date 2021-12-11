package ecdar.mutation.operators;

import ecdar.abstractions.Component;
import ecdar.abstractions.Edge;
import ecdar.abstractions.EdgeStatus;
import ecdar.mutation.models.MutationTestCase;

import java.util.ArrayList;
import java.util.List;

/**
 * Mutation operator that changes synchronization actions to (other) input actions.
 */
public class ChangeActionInputsOperator extends ChangeActionOperator {
    @Override
    public String getText() {
        return "Change action, inputs";
    }

    @Override
    public String getCodeName() {
        return "changeActionInputs";
    }

    @Override
    public List<MutationTestCase> generateTestCases(final Component original) {
        final List<MutationTestCase> cases = new ArrayList<>();

        // For all edges in the original component
        for (int edgeIndex = 0; edgeIndex < original.getEdges().size(); edgeIndex++) {
            final Edge originalEdge = original.getEdges().get(edgeIndex);

            // Ignore if locked (e.g. if edge on the Inconsistent or Universal locations)
            if (originalEdge.getIsLockedProperty().get()) continue;

            // Change the action of that edge to other action
            final int finalEdgeIndex = edgeIndex;
            original.getInputStrings().forEach(input -> {
                final MutationTestCase testCase = generateTestCase(original, finalEdgeIndex, input, EdgeStatus.INPUT);
                if (testCase != null) cases.add(testCase);
            });
        }

        return cases;
    }

    @Override
    public String getDescription() {
        return "Changes the action of an edge to an arbitrary input action.";
    }

    @Override
    public int getUpperLimit(final Component original) {
        return original.getInputEdges().size() * (original.getInputStrings().size() - 1) +
                original.getOutputEdges().size() * original.getInputStrings().size();
    }

    @Override
    public boolean isUpperLimitExact() {
        return false;
    }
}
