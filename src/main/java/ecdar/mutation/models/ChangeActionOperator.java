package ecdar.mutation.models;

import ecdar.abstractions.Component;
import ecdar.mutation.MutationTestingException;

import java.util.List;

public class ChangeActionOperator extends MutationOperator {
    @Override
    public String getText() {
        return "Change action";
    }

    @Override
    public String getCodeName() {
        return "changeAction";
    }

    @Override
    public List<MutationTestCase> generateTestCases(Component original) throws MutationTestingException {
        return null;
    }

    @Override
    public String getDescription() {
        return null;
    }
}
