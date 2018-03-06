package ecdar.mutation.operators;

import java.util.ArrayList;
import java.util.List;

/**
 * Mutation operator that changes an one of the operators <, <=, >, >=, !=, == of a guard that uses a clock.
 * If a guard is a conjunction, more mutants will be generated from that guard.
 * This class replaces an operator with one of the operators <=, >.
 * We only use those operators for clocks, since these cover all practical scenarios.
 * E.g. It is practical impossible to get x == C for clock x and constant C, since time is continuous.
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
                "to one of the operators <=, >. " +
                "Creates up to 2 * [# of uses of operators in guards] mutants.";
    }
}
