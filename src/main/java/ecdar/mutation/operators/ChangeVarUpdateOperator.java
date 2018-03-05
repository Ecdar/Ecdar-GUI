package ecdar.mutation.operators;

import ecdar.abstractions.Component;
import ecdar.abstractions.Edge;
import ecdar.mutation.MutationTestingException;
import ecdar.mutation.models.MutationTestCase;
import ecdar.utility.ExpressionHelper;
import org.apache.commons.lang3.tuple.Triple;

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
    public List<MutationTestCase> generateTestCases(final Component original) {
        final List<Triple<String, Integer, Integer>> locals = original.getLocalVariablesWithBounds();

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
            locals.forEach(local -> {
                // If variable is not assigned, add it
                if (sides.get(local.getLeft()) == null) {
                    // For each possible assignment of that variable
                    for (int value = local.getMiddle(); value <= local.getRight(); value++) {
                        final Component mutant = original.cloneForVerification();
                        final Edge mutantEdge = mutant.getEdges().get(finalEdgeIndex);

                        final List<String> newSimpleUpdates = new ArrayList<>();

                        sides.forEach((left, right) -> newSimpleUpdates.add(left + "=" + right));
                        newSimpleUpdates.add(local.getLeft() + "=" + value);

                        mutantEdge.setUpdate(String.join(",", newSimpleUpdates));

                        cases.add(new MutationTestCase(
                                original, mutant,
                                getCodeName() + "_" + finalEdgeIndex + "_" + local.getLeft() + "_" + value,
                                "Changed update of edge " + originalEdge.getSourceLocation().getId() +
                                        " -> " + originalEdge.getTargetLocation().getId() + " from " +
                                        originalEdge.getUpdate() + "to " + mutantEdge.getUpdate()
                        ));
                    }
                } else { // Otherwise, replace the assignment
                    // For each possible assignment of that variable
                    for (int value = local.getMiddle(); value <= local.getRight(); value++) {
                        // If this is already the original assignment, ignore
                        if (sides.get(local.getLeft()).equals(String.valueOf(value))) continue;

                        final Component mutant = original.cloneForVerification();
                        final Edge mutantEdge = mutant.getEdges().get(finalEdgeIndex);

                        final List<String> newSimpleUpdates = new ArrayList<>();

                        final int finalValue = value;
                        sides.forEach((left, right) -> {
                            if (left.equals(local.getLeft())) newSimpleUpdates.add(left + "=" + finalValue);
                            else newSimpleUpdates.add(left + "=" + right);
                        });

                        mutantEdge.setUpdate(String.join(",", newSimpleUpdates));

                        cases.add(new MutationTestCase(
                                original, mutant,
                                getCodeName() + "_" + finalEdgeIndex + "_" + local.getLeft() + "_" + value,
                                "Changed update of edge " + originalEdge.getSourceLocation().getId() +
                                        " -> " + originalEdge.getTargetLocation().getId() + " from " +
                                        originalEdge.getUpdate() + "to " + mutantEdge.getUpdate()
                        ));
                    }
                }
            });
        }

        return cases;
    }

    @Override
    public String getDescription() {
        return "Changes (or adds, if the variable is not assigned in this edge) the assignment of a local variable " +
                "in an update property to any of its defined values. " +
                "Creates up to [# of edges] * [# of variables] * [# of possible values in the type of the variables] mutants." +
                "This operator only considers variables of a custom type defined with typedef int[a, b] c, " +
                "where a and b are literals, and c is the type name. " +
                "This operator assumes that all right sides of variable assignments are literals.";
    }
}
