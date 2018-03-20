package ecdar.mutation.models;

import ecdar.abstractions.Component;

/**
 * A test case for model-based mutation testing.
 */
public class MutationTestCase {
    private final Component testModel;
    private final Component mutant;
    private NonRefinementStrategy strategy;
    private final String id;
    private final String description;

    public MutationTestCase(final Component testModel, final Component mutant, final String id, final String description) {
        this.testModel = testModel;
        this.mutant = mutant;
        this.id = id;
        this.description = description;
    }

    public String getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }
    public Component getTestModel() {
        return testModel;
    }

    public Component getMutant() {
        return mutant;
    }

    public NonRefinementStrategy getStrategy() {
        return strategy;
    }

    public void setStrategy(final NonRefinementStrategy strategy) {
        this.strategy = strategy;
    }
}
