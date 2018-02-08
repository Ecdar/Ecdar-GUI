package ecdar.mutation;

import ecdar.abstractions.Component;

import java.util.List;

/**
 * A test case for model-based mutation testing.
 */
class MutationTestCase {
    private Component testModel, mutant;
    private List<String> strategy;

    MutationTestCase(final Component testModel, final Component mutant, final List<String> strategy) {
        this.testModel = testModel;
        this.mutant = mutant;
        this.strategy = strategy;
    }
}
