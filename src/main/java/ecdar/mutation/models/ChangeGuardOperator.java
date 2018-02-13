package ecdar.mutation.models;

import ecdar.abstractions.Component;
import ecdar.abstractions.Edge;
import ecdar.mutation.MutationTestingException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Mutation operator that changes the operator of guards.
 * This only works for simple guards.
 * They must be empty of match this regex:
 * ^\s*(\w+)\s*(<|<=|==|>|>=)\s*(\w+)\s*$
 * For instance, no conjunctions in guards.
 */
public class ChangeGuardOperator extends MutationOperator {
    private static final String REGEX_SIMPLE_GUARD = "^([^<>=!]+)(<|<=|>|>=|==|!=)([^<>=!]+)$";

    @Override
    public String getText() {
        return "Change guard";
    }

    @Override
    String getJsonName() {
        return "changeGuard";
    }

    @Override
    public Collection<? extends Component> compute(final Component original) throws MutationTestingException {
        final List<Component> mutants = new ArrayList<>();

        // Do not use != as this is not allowed for timing constrains
        final List<String> operators = new ArrayList<>();
        operators.add("<");
        operators.add("<=");
        operators.add("==");
        operators.add(">");
        operators.add(">=");

        // For all edges in the original component
        for (int edgeIndex = 0; edgeIndex < original.getEdges().size(); edgeIndex++) {
            final int finalEdgeIndex = edgeIndex;
            final Edge originalEdge = original.getEdges().get(edgeIndex);

            // Ignore if locked (e.g. if edge on the Inconsistent or Universal locations)
            if (originalEdge.getIsLocked().get()) continue;

            // Ignore if guard is empty
            if (originalEdge.getGuard().isEmpty()) continue;

            final String[] simpleGuards = originalEdge.getGuard().split("&&");
            for (int simpleGuardIndex = 0; simpleGuardIndex < simpleGuards.length; simpleGuardIndex++) {
                final String simpleGuard = simpleGuards[simpleGuardIndex];

                final Matcher matcher = Pattern.compile(REGEX_SIMPLE_GUARD).matcher(simpleGuard);

                if (!matcher.find()) {
                    throw new MutationTestingException("Guard " + simpleGuard + " does not match pattern " + REGEX_SIMPLE_GUARD);
                }

                // Create a mutant for each other operator
                final int finalSimpleGuardIndex = simpleGuardIndex;
                operators.forEach(operator -> {
                    // If operator is the same as with the original, ignore
                    if (matcher.group(2).equals(operator)) return;

                    mutants.add(createMutant(original, simpleGuards, matcher.group(1) + operator + matcher.group(3), finalSimpleGuardIndex, finalEdgeIndex));
                });
            }
        }

        return mutants;
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
