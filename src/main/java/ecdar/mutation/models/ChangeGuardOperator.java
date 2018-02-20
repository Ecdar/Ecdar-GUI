package ecdar.mutation.models;

import ecdar.abstractions.Component;
import ecdar.abstractions.Edge;
import ecdar.mutation.MutationTestingException;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Mutation operator that changes an one of the operators <, <=, >, >=, !=, == of a guard.
 * If a guard is a conjunction, more mutants will be generated from than guard.
 * This class replaces an operator with one of the operators <, <=, >, >=, ==.
 * We do not replace with !=, since the engine does not allow this for timing constrains.
 */
public class ChangeGuardOperator extends MutationOperator {
    private static final String REGEX_SIMPLE_GUARD = "^([^<>=!]+)(<|<=|>|>=|==|!=)([^<>=!]+)$";

    @Override
    public String getText() {
        return "Change guard";
    }

    @Override
    public String getCodeName() {
        return "changeGuard";
    }

    @Override
    public List<MutationTestCase> generate(final Component original) throws MutationTestingException {
        final List<MutationTestCase> testCases = new ArrayList<>();

        // Do not use != as this is not allowed for timing constrains
        final List<String> operators = new ArrayList<>();
        operators.add("<");
        operators.add("<=");
        operators.add("==");
        operators.add(">");
        operators.add(">=");

        // For all edges in the original component
        for (int edgeIndex = 0; edgeIndex < original.getEdges().size(); edgeIndex++) {
            final Edge originalEdge = original.getEdges().get(edgeIndex);

            // Ignore if locked (e.g. if edge on the Inconsistent or Universal locations)
            if (originalEdge.getIsLocked().get()) continue;

            // Ignore if guard is empty
            if (originalEdge.getGuard().isEmpty()) continue;

            final String[] guardParts = originalEdge.getGuard().split("&&");
            for (int partIndex = 0; partIndex < guardParts.length; partIndex++) {
                final String part = guardParts[partIndex];

                final Matcher matcher = Pattern.compile(REGEX_SIMPLE_GUARD).matcher(part);

                if (!matcher.find()) {
                    throw new MutationTestingException("Guard part " + part + " does not match pattern " + REGEX_SIMPLE_GUARD);
                }

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
                            "Changed guard of edge " + originalEdge.getSourceLocation().getId() + " -> " +
                                    originalEdge.getTargetLocation().getId() + " to " +
                                    mutant.getEdges().get(edgeIndex).getGuard()));
                }
            }
        }

        return testCases;
    }

    @Override
    public String getDescription() {
        return "Changes one of the operators <, <=, >, >=, !=, == of a guard " +
                "to one of the operators <, <=, >, >=, ==. " +
                "Creates up to 5 * [# of uses of operators in guards] mutants.";
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
    private static Component createMutant(final Component original, final String[] originalSimpleGuards, final String newSimpleGuard, final int simpleGuardIndex, final int edgeIndex) {
        final Component mutant = original.cloneForVerification();

        final String[] newSimpleGuards = originalSimpleGuards.clone();
        newSimpleGuards[simpleGuardIndex] = newSimpleGuard;

        mutant.getEdges().get(edgeIndex).setGuard(String.join("&&", newSimpleGuards));

        return mutant;
    }
}
