package ecdar.mutation.models;

import ecdar.abstractions.Component;
import ecdar.mutation.MutationTestingException;

import java.util.List;

/**
 * A test case for model-based mutation testing.
 */
public class MutationTestCase {
    private Component testModel, mutant;
    private NonRefinementStrategy strategy;
    private String id;

    public MutationTestCase(final Component testModel, final Component mutant, final List<String> strategy, final String id) throws MutationTestingException {
        this.testModel = testModel;
        this.mutant = mutant;
        this.strategy = new NonRefinementStrategy(strategy);
        this.id = id;
    }

    public String getId() {
        return id;
    }
}
