package ecdar.mutation.operators;

import ecdar.Ecdar;
import ecdar.abstractions.Component;
import ecdar.abstractions.Edge;
import ecdar.mutation.TextFlowBuilder;
import ecdar.mutation.models.MutationTestCase;
import ecdar.utility.ExpressionHelper;
import org.apache.commons.lang3.tuple.Triple;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Mutation operator that changes the assignment of local variables.
 */
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
        final List<Triple<String, Integer, Integer>> variablesWithBounds = getLocalComponentVariablesWithBounds(original);

        final List<MutationTestCase> cases = new ArrayList<>();

        // For all edges in the original component
        for (int edgeIndex = 0; edgeIndex < original.getEdges().size(); edgeIndex++) {
            final Edge originalEdge = original.getEdges().get(edgeIndex);

            // Ignore if locked (e.g. if edge on the Inconsistent or Universal locations)
            if (originalEdge.getIsLockedProperty().get()) continue;

            final String oldUpdate = originalEdge.getUpdate();
            final Map<String, String> sides = ExpressionHelper.getUpdateSides(oldUpdate);

            // For each variable
            final int finalEdgeIndex = edgeIndex;
            variablesWithBounds.forEach(local -> {
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
                                new TextFlowBuilder().text("Changed ").boldText("update").text(" of ")
                                        .edgeLinks(originalEdge, original.getName()).text(" from ")
                                        .boldText(originalEdge.getUpdate()).text(" to ")
                                        .boldText(mutantEdge.getUpdate()).build()
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
                                new TextFlowBuilder().text("Changed ").boldText("update").text(" of ")
                                        .edgeLinks(originalEdge, original.getName()).text(" from ")
                                        .boldText(originalEdge.getUpdate()).text(" to ")
                                        .boldText(mutantEdge.getUpdate()).build()
                        ));
                    }
                }
            });
        }

        return cases;
    }

    /**
     * Gets the local variables defined in the declarations text.
     * Also gets the lower and upper bounds for these variables.
     *
     * @return Triples containing (left) name of the variable, (middle) lower bound, (right) upper bound
     */
    private List<Triple<String, Integer, Integer>> getLocalComponentVariablesWithBounds(Component component) {
        final List<Triple<String, Integer, Integer>> typedefs = Ecdar.getProject().getGlobalDeclarations().getTypedefs();

        final List<Triple<String, Integer, Integer>> locals = new ArrayList<>();

        Arrays.stream(component.getDeclarationsText().split(";")).forEach(statement -> {
            final Matcher matcher = Pattern.compile("^\\s*(\\w+)\\s+(\\w+)(\\W|$)").matcher(statement);
            if (!matcher.find()) return;

            final Optional<Triple<String, Integer, Integer>> typedef = typedefs.stream()
                    .filter(def -> def.getLeft().equals(matcher.group(1))).findAny();
            if (typedef.isEmpty()) return;

            locals.add(Triple.of(matcher.group(2), typedef.get().getMiddle(), typedef.get().getRight()));
        });

        return locals;
    }

    @Override
    public String getDescription() {
        return "Changes the assignment (or adds, if the variable is not assigned in corresponding edge) of a local variable " +
                "in an update property to any of its defined values. " +
                "This operator only considers variables of a custom type defined with typedef int[a, b] c, " +
                "where a and b are literals, and c is the type name. " +
                "This operator assumes that all right sides of variable assignments are literals.";
    }

    @Override
    public int getUpperLimit(final Component original) {
        // Get the sum of valuations of each variable
        final int varValueCount = getLocalComponentVariablesWithBounds(original).stream().mapToInt(local -> local.getRight() - local.getMiddle() + 1).sum();

        return original.getDisplayableEdges().size() * varValueCount;
    }

    @Override
    public boolean isUpperLimitExact() {
        return false;
    }
}
