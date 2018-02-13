package ecdar.mutation.models;

import ecdar.abstractions.Component;
import ecdar.abstractions.Edge;
import ecdar.abstractions.Location;
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

            final Matcher matcher = Pattern.compile("^\\s*(\\w+)\\s*(<|<=|==|>|>=)\\s*(\\w+)\\s*$").matcher(originalEdge.getGuard());

            if (!matcher.find()) {
                throw new MutationTestingException("Guard \"" + originalEdge.getGuard() + "\" does not match pattern \"^\\s*(\\w+)\\s*(<|<=|==|>|>=)\\s*(\\w+)\\s*$\"");
            }

            // Create a mutant for each other operator
            operators.forEach(operator -> {
                // If operator is the same as with the original, ignore
                if (matcher.group(2).equals(operator)) return;

                final Component mutant = original.cloneForVerification();
                mutant.getEdges().get(finalEdgeIndex).setGuard(matcher.group(1) + operator + matcher.group(3));
                mutants.add(mutant);
            });
        }

        return mutants;
    }
}
