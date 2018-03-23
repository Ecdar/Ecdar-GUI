package ecdar.mutation.operators;

import ecdar.abstractions.Component;
import ecdar.abstractions.Edge;
import ecdar.mutation.MutationTestingException;
import ecdar.mutation.TextFlowBuilder;
import ecdar.mutation.models.MutationTestCase;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Mutation operator that changes an one of the operators <, <=, >, >=, !=, == of a guard.
 * If a guard is a conjunction, more mutants will be generated from that guard.
 */
public abstract class ChangeGuardOpOperator extends MutationOperator {
    public abstract List<String> getOperators();

    /**
     * Gets if we should mutate with guard pats with clocks or without clocks.
     * @return true if we should mutate with clocks. False if we should mutate without clocks
     */
    public abstract boolean shouldContainClocks();

    @Override
    public List<MutationTestCase> generateTestCases(final Component original) throws MutationTestingException {
        final List<MutationTestCase> testCases = new ArrayList<>();

        // Do not use != as this is not allowed for timing constrains
        final List<String> operators = getOperators();

        final List<String> clocks = original.getClocks();

        // For all edges in the original component
        for (int edgeIndex = 0; edgeIndex < original.getEdges().size(); edgeIndex++) {
            final Edge originalEdge = original.getEdges().get(edgeIndex);

            // Ignore if locked (e.g. if edge on the Inconsistent or Universal locations)
            if (originalEdge.getIsLocked().get()) continue;

            // Ignore if guard is empty
            if (originalEdge.getGuard().isEmpty()) continue;

            // For all parts in the conjunction
            final String[] guardParts = originalEdge.getGuard().split("&&");
            for (int partIndex = 0; partIndex < guardParts.length; partIndex++) {
                final String part = guardParts[partIndex];

                // If it does not contain clocks, ignore
                if (containsVar(part, clocks) != shouldContainClocks()) continue;

                final String REGEX_SIMPLE_GUARD = "^([^<>=!]+)(<|<=|>|>=|==|!=)([^<>=!]+)$";
                final Matcher matcher = Pattern.compile(REGEX_SIMPLE_GUARD).matcher(part);

                if (!matcher.find())
                    throw new MutationTestingException("Guard part " + part + " does not match " + REGEX_SIMPLE_GUARD);

                final String originalOperator = matcher.group(2);

                // Create a mutant for each other operator
                for (int operatorIndex = 0; operatorIndex < operators.size(); operatorIndex++) {
                    final String newOperator = operators.get(operatorIndex);

                    // If operator is the same as with the original, ignore
                    if (originalOperator.equals(newOperator)) continue;

                    final Component mutant = createMutant(original, guardParts,
                            matcher.group(1) + newOperator + matcher.group(3),
                            partIndex, edgeIndex);

                    testCases.add(new MutationTestCase(original, mutant,
                            getCodeName() + "_" + edgeIndex + "_" + partIndex + "_" + operatorIndex,
                            new TextFlowBuilder().text("Changed ").boldText("guard").text(" of ")
                                    .edgeLinks(originalEdge, original.getName()).text(" from ")
                                    .boldText(originalEdge.getGuard()).text(" to ")
                                    .boldText(mutant.getEdges().get(edgeIndex).getGuard()).build()
                    ));
                }
            }
        }

        return testCases;
    }

    /**
     * Gets if an expression contains at least on of some specified variables.
     * @param expr the expression to check
     * @param vars the variables
     * @return true iff the expression contains at least on of the variables
     */
    private static boolean containsVar(final String expr, final List<String> vars) {
        for (final String var : vars)
            if (Pattern.compile("(.*\\W)?" + var + "(\\W.*)?").matcher(expr).find()) return true;

        return false;
    }

    /**
     * Creates a mutant with a changed operator.
     * @param original component to mutate
     * @param originalSimpleGuards the original simple guards (e.g. x < 2) that the whole original guard is a conjunction of
     * @param newSimpleGuard new simple guard to replace one of the original simple guards
     * @param simpleGuardIndex the index of the simple guards that should be changed
     * @param edgeIndex the index of the edge containing the guard to mutate
     * @return the created mutant
     */
    private static Component createMutant(final Component original, final String[] originalSimpleGuards,
                                          final String newSimpleGuard, final int simpleGuardIndex, final int edgeIndex) {
        final Component mutant = original.cloneForVerification();

        final String[] newSimpleGuards = originalSimpleGuards.clone();
        newSimpleGuards[simpleGuardIndex] = newSimpleGuard;

        mutant.getEdges().get(edgeIndex).setGuard(String.join("&&", newSimpleGuards));

        return mutant;
    }
}
