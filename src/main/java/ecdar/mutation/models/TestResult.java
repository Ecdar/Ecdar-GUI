package ecdar.mutation.models;

import ecdar.mutation.SimpleComponentSimulation;

public class TestResult extends ExpandableContent {
    public enum Verdict {INCONCLUSIVE, PASS, FAIL}

    private final Verdict verdict;

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
