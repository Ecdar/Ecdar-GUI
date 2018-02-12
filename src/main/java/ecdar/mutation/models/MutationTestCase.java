package ecdar.mutation.models;

import ecdar.abstractions.Component;
import ecdar.mutation.MutationTestingException;
import ecdar.mutation.models.NonRefinementStrategy;

import java.util.List;

/**
 * A test case for model-based mutation testing.
 */
public class MutationTestCase {
    private Component testModel, mutant;
    private NonRefinementStrategy strategy;

    public MutationTestCase(final Component testModel, final Component mutant, final List<String> strategy) throws MutationTestingException {
        this.testModel = testModel;
        this.mutant = mutant;
        this.strategy = new NonRefinementStrategy(strategy);
    }
}
