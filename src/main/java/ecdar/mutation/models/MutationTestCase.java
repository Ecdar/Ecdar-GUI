package ecdar.mutation.models;

import ecdar.abstractions.Component;
import javafx.scene.text.TextFlow;

/**
 * A test case for model-based mutation testing.
 */
public class MutationTestCase {
    private final Component testModel;
    private final Component mutant;
    private NonRefinementStrategy strategy;
    private final String id;
    private final TextFlow description;

    /**
     * Constructs.
     * @param testModel test model
     * @param mutant mutant model
     * @param id id of the test-case
     * @param description description of the test-case
     */
    public MutationTestCase(final Component testModel, final Component mutant, final String id, final TextFlow description) {
        this.testModel = testModel;
        this.mutant = mutant;
        this.id = id;
        this.description = description;
    }


    /* Properties */

    public String getId() {
        return id;
    }

    public TextFlow getDescription() {
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
