package ecdar.mutation.models;

import ecdar.mutation.SimpleComponentSimulation;

/**
 * A result of a model-based mutation test with respect to a single test-case.
 */
public class TestResult extends ExpandableContent {
    public enum Verdict {INCONCLUSIVE, PASS, FAIL}

    private final Verdict verdict;

    /**
     * Constructs.
     * @param id id of the test-case
     * @param description description of the test-case
     * @param reason reason for the verdict
     * @param testModelSimulation test model simulation
     * @param mutantSimulation mutant model simulation
     * @param verdict verdict of the test
     */
    public TestResult(final String id, final String description, final String reason,
                      final SimpleComponentSimulation testModelSimulation, final SimpleComponentSimulation mutantSimulation, final Verdict verdict) {
        super(description, "Id: " + id + ":\n" +
                "Reason: " + reason + "\n" +
                "Test model is in location: " + testModelSimulation.getCurrentLocation().getId() + " with values: " + testModelSimulation.getAllValuations() + "\n" +
                "Mutant is in location: " + mutantSimulation.getCurrentLocation().getId() + " with values: " + mutantSimulation.getAllValuations() + "\n" +
                "Trace: " + String.join(" -> ", testModelSimulation.getTrace()));

        this.verdict = verdict;
    }

    public Verdict getVerdict() {
        return verdict;
    }
}
