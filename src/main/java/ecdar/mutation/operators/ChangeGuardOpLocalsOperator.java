package ecdar.mutation.operators;

import ecdar.abstractions.Component;
import ecdar.abstractions.Edge;

import java.util.ArrayList;
import java.util.List;

/**
 * Mutation operator that changes an one of the operators {@code <, <=, >, >=, !=, ==} of a guard that do not use a clock.
 * If a guard is a conjunction, more mutants will be generated from that guard.
 */
public class ChangeGuardOpLocalsOperator extends ChangeGuardOpOperator {
    @Override
    public List<String> getOperators() {
        final List<String> operators = new ArrayList<>();

        operators.add("<=");
        operators.add("<");
        operators.add("==");
        operators.add("!=");
        operators.add(">=");
        operators.add(">");

        return operators;
    }

    @Override
    public boolean shouldContainClocks() {
        return false;
    }

    @Override
    public String getText() {
        return "Change guard operator, locals";
    }

    @Override
    public String getCodeName() {
        return "changeGuardOpLocals";
    }

    @Override
    public String getDescription() {
        return "Changes one of the operators <, <=, >, >=, !=, == of a guard, " +
                "where the left and right sides do not use clocks.";
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

        return 5 * count;
    }

    @Override
    public boolean isUpperLimitExact() {
        return false;
    }
}
