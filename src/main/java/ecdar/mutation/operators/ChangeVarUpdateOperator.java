package ecdar.mutation.operators;

import ecdar.abstractions.Component;
import ecdar.abstractions.Edge;
import ecdar.mutation.MutationTestingException;
import ecdar.mutation.models.MutationTestCase;
import ecdar.utility.ExpressionHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ChangeVarUpdateOperator extends MutationOperator {
    @Override
    public String getText() {
        return "Change variable update";
    }

    @Override
    public String getCodeName() {
        return "changeVarUpdate";
    }

    @Override
    public List<MutationTestCase> generateTestCases(final Component original) throws MutationTestingException {
        final List<String> vars = original.getLocalVariables();

        final List<MutationTestCase> cases = new ArrayList<>();

        // For all edges in the original component
        for (int edgeIndex = 0; edgeIndex < original.getEdges().size(); edgeIndex++) {
            final Edge originalEdge = original.getEdges().get(edgeIndex);

            // Ignore if locked (e.g. if edge on the Inconsistent or Universal locations)
            if (originalEdge.getIsLocked().get()) continue;

            final String oldUpdate = originalEdge.getUpdate();
            final Map<String, String> sides = ExpressionHelper.getUpdateSides(oldUpdate);

            // For each variable
            final int finalEdgeIndex = edgeIndex;
            vars.forEach(var -> {

                if (sides.containsKey(var)) {
                    {
                        final Component mutant = original.cloneForVerification();
                        final Edge mutantEdge = mutant.getEdges().get(finalEdgeIndex);

                        final String change = "-1";
                        final List<String> newSimpleUpdates = new ArrayList<>();

                        sides.forEach((left, right) -> {
                            if (left.equals(var)) newSimpleUpdates.add(left + "=" + right + change);
                            else newSimpleUpdates.add(left + "=" + right);
                        });

                        cases.add(generateCase(
                                original, originalEdge, finalEdgeIndex, var,
                                mutant, mutantEdge, change, newSimpleUpdates
                        ));
                    } {
                        final Component mutant = original.cloneForVerification();
                        final Edge mutantEdge = mutant.getEdges().get(finalEdgeIndex);

                        final String change = "+1";
                        final List<String> newSimpleUpdates = new ArrayList<>();

                        sides.forEach((left, right) -> {
                            if (left.equals(var)) newSimpleUpdates.add(left + "=" + right + change);
                            else newSimpleUpdates.add(left + "=" + right);
                        });

                        cases.add(generateCase(
                                original, originalEdge, finalEdgeIndex, var,
                                mutant, mutantEdge, change, newSimpleUpdates
                        ));
                    }
                } else {
                    {
                        final Component mutant = original.cloneForVerification();
                        final Edge mutantEdge = mutant.getEdges().get(finalEdgeIndex);

                        final String change = "-1";
                        final List<String> newSimpleUpdates = new ArrayList<>();

                        sides.forEach((left, right) -> newSimpleUpdates.add(left + "=" + right));

                        newSimpleUpdates.add(var + "=" + var + change);

                        cases.add(generateCase(
                                original, originalEdge, finalEdgeIndex, var,
                                mutant, mutantEdge, change, newSimpleUpdates
                        ));
                    } {

                        final Component mutant = original.cloneForVerification();
                        final Edge mutantEdge = mutant.getEdges().get(finalEdgeIndex);
                        final String change = "+1";
                        final List<String> newSimpleUpdates = new ArrayList<>();

                        sides.forEach((left, right) -> newSimpleUpdates.add(left + "=" + right));

                        newSimpleUpdates.add(var + "=" + var + change);

                        cases.add(generateCase(
                                original, originalEdge, finalEdgeIndex, var,
                                mutant, mutantEdge, change, newSimpleUpdates
                        ));
                    }
                }
            });
        }

        return cases;
    }

    /**
     * Generates a test-case.
     * @param original original component
     * @param originalEdge original edge to mutate
     * @param edgeIndex index of the edge to mutate
     * @param var local variable to mutate with
     * @param mutant mutant component
     * @param mutantEdge edge to mutate
     * @param change an id of what is changed
     * @param newSimpleUpdates the new update property, as a list of simple expressions
     * @return the generated test-case
     */
    private MutationTestCase generateCase(final Component original, final Edge originalEdge, final int edgeIndex,
                                          final String var, final Component mutant, final Edge mutantEdge,
                                          final String change, final List<String> newSimpleUpdates) {

        mutantEdge.setUpdate(String.join(",", newSimpleUpdates));

        return new MutationTestCase(
                original, mutant,
                getCodeName() + "_" + edgeIndex + "_" + var + "_" + change,
                "Changed update of edge " + originalEdge.getSourceLocation().getId() + " -> " +
                        originalEdge.getTargetLocation().getId() + " from " +
                        originalEdge.getUpdate() + "to " + mutantEdge.getUpdate()
        );
    }

    @Override
    public String getDescription() {
        return "Changes the assignment of a local variable " +
                "in an update property to +/- 1 of the original assignment. " +
                "If the variable is not assigned in this property," +
                "the variable will be assigned to 1 more/less that its previous value.";
    }
}
