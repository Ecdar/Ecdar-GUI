package ecdar.mutation.operators;

import java.util.ArrayList;
import java.util.List;

/**
 * Mutation operator that changes an one of the operators <, <=, >, >=, !=, == of a guard that do not use a clock.
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
                "where the left and right sides do not use a clock. " +
                "Creates up to 5 * [# of uses of operators in guards] mutants.";
    }
}
