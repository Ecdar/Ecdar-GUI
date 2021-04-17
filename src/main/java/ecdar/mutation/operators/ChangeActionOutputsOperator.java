package ecdar.mutation.operators;

import ecdar.abstractions.Component;
import ecdar.abstractions.Edge;
import ecdar.abstractions.EdgeStatus;
import ecdar.mutation.models.MutationTestCase;

import java.util.ArrayList;
import java.util.List;


/**
 * Mutation operator for changing an action to a (different) output.
 */
public class ChangeActionOutputsOperator extends ChangeActionOperator {
    @Override
    public String getText() {
        return "Change action, outputs";
    }

    @Override
    public String getCodeName() {
        return "changeActionOutputs";
    }

    @Override
    public List<MutationTestCase> generateTestCases(final Component original) {
        final List<MutationTestCase> cases = new ArrayList<>();

        // For all edges in the original component
        for (int edgeIndex = 0; edgeIndex < original.getSubEdges().size(); edgeIndex++) {
            final Edge originalEdge = original.getSubEdges().get(edgeIndex);

            // Ignore if locked (e.g. if edge on the Inconsistent or Universal locations)
            if (originalEdge.getIsLocked().get()) continue;

            // Change the action of that edge to another action
            final int finalEdgeIndex = edgeIndex;
            original.getOutputStrings().forEach(output -> {
                final MutationTestCase testCase = generateTestCase(original, finalEdgeIndex, output, EdgeStatus.OUTPUT);
                if (testCase != null) cases.add(testCase);
            });
        }

        return cases;
    }

    @Override
    public String getDescription() {
        return "Changes the action of an edge to an arbitrary output action.";
    }

    @Override
    public int getUpperLimit(final Component original) {
        return original.getInputEdges().size() * original.getOutputStrings().size() +
                original.getOutputEdges().size() * (original.getOutputStrings().size() - 1);
    }

    @Override
    public boolean isUpperLimitExact() {
        return false;
    }
}
