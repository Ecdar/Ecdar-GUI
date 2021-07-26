package ecdar.mutation.operators;

import ecdar.abstractions.Component;
import ecdar.abstractions.Edge;

import java.util.ArrayList;
import java.util.List;

/**
 * Mutation operator that changes an one of the operators {@code <, <=, >, >=, !=, ==} of a guard that uses a clock.
 * If a guard is a conjunction, more mutants will be generated from that guard.
 * This class replaces an operator with one of the operators {@code <=, >}.
 * We only use those operators for clocks, since these cover all practical scenarios.
 * E.g. It is practical impossible to get {@code x == C} for clock x and constant C, since time is continuous.
 */
public class ChangeGuardOpClocksOperator extends ChangeGuardOpOperator {
    @Override
    public String getText() {
        return "Change guard operator, clocks";
    }

    @Override
    public String getCodeName() {
        return "changeGuardOpClocks";
    }

    @Override
    public List<String> getOperators() {
        final List<String> operators = new ArrayList<>();

        operators.add("<=");
        operators.add(">");

        return operators;
    }

    @Override
    public boolean shouldContainClocks() {
        return true;
    }

    @Override
    public String getDescription() {
        return "Changes one of the operators <, <=, >, >=, !=, == of a guard, " +
                "where the left or right side uses a clock, " +
                "to one of the operators <=, >.";
    }

    @Override
    public int getUpperLimit(final Component original) {
        int count = 0;

        // For all edges in the original component
        for (int edgeIndex = 0; edgeIndex < original.getEdges().size(); edgeIndex++) {
            final Edge originalEdge = original.getEdges().get(edgeIndex);

            // Ignore if locked (e.g. if edge on the Inconsistent or Universal locations)
            if (originalEdge.getIsLockedProperty().get()) continue;

            // Ignore if guard is empty
            if (originalEdge.getGuard().isEmpty()) continue;

            final String[] guardParts = originalEdge.getGuard().split("&&");

            count += guardParts.length;
        }

        return 2 * count;
    }

    @Override
    public boolean isUpperLimitExact() {
        return false;
    }
}
