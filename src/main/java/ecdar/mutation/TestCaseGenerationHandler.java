package ecdar.mutation;

import ecdar.Ecdar;
import ecdar.abstractions.Component;
import ecdar.backend.BackendException;
import ecdar.backend.BackendHelper;
import ecdar.mutation.models.MutationTestCase;
import ecdar.mutation.models.MutationTestPlan;
import javafx.application.Platform;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import org.apache.commons.io.FileUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Handler for generating test-cases.
 */
class TestCaseGenerationHandler implements ConcurrentJobsHandler {
    private final MutationTestPlan plan;
    private final Consumer<List<MutationTestCase>> testCasesConsumer;

    private final Component testModel;

    private final List<MutationTestCase> potentialTestCases;
    private List<MutationTestCase> finishedTestCases;

    private ConcurrentJobsDriver jobsDriver;

    // Progress fields
    private Instant generationStart;
    private String queryFilePath;


    /* Constructors */

    /**
     * Constructs the handler.
     *
     * @param plan               the test plan containing options for generation
     * @param testModel          the tet model to use
     * @param potentialTestCases potential test-cases containing the mutants
     * @param testCasesConsumer  consumer to be called when all test-cases are generated
     */
    TestCaseGenerationHandler(final MutationTestPlan plan, final Component testModel, final List<MutationTestCase> potentialTestCases, final Consumer<List<MutationTestCase>> testCasesConsumer) {
        this.plan = plan;
        this.testModel = testModel;
        this.testCasesConsumer = testCasesConsumer;
        this.potentialTestCases = potentialTestCases;
    }


    /* Getters and setters */

    private MutationTestPlan getPlan() {
        return plan;
    }

    private Component getTestModel() {
        return testModel;
    }

    /* Other methods */

    /**
     * Generates mutants and test-cases from them.
     */
    void start() {
        generationStart = Instant.now();
        finishedTestCases = Collections.synchronizedList(new ArrayList<>()); // use synchronized to be thread safe

        try {
            queryFilePath = BackendHelper.storeQuery("refinement: " + MutationTestPlanController.MUTANT_NAME + "<=" + MutationTestPlanController.SPEC_NAME, "query");
        } catch (final URISyntaxException | IOException e) {
            e.printStackTrace();
            Ecdar.showToast("Error: " + e.getMessage());
            return;
        }

        jobsDriver = new ConcurrentJobsDriver(this);
        jobsDriver.addJobs(potentialTestCases.stream().map(testCase -> (Runnable) () -> generateTestCase(testCase, getPlan().getBackendTries())).collect(Collectors.toList()));
    }


    @Override
    public boolean shouldStop() {
        return getPlan().shouldStop();
    }

    @Override
    public void onStopped() {
        Platform.runLater(() -> getPlan().setStatus(MutationTestPlan.Status.IDLE));
    }

    @Override
    public void onAllJobsSuccessfullyDone() {
        try {
            FileUtils.cleanDirectory(new File(BackendHelper.getTempDirectoryAbsolutePath()));
        } catch (final IOException | URISyntaxException e) {
            e.printStackTrace();
            Ecdar.showToast("Error: " + e.getMessage());
            return;
        }

        final Text text = new Text("Done");
        text.setFill(Color.GREEN);
        getPlan().writeProgress(text);
        testCasesConsumer.accept(finishedTestCases);
    }

    @Override
    public void onProgressRemaining(final int remaining) {
        Platform.runLater(() -> getPlan().writeProgress(
                "Generating test-cases... (" + remaining + " mutant" + (remaining == 1 ? "" : "s") + " remaining)"
        ));
    }

    @Override
    public int getMaxConcurrentJobs() {
        return getPlan().getConcurrentGenerationThreads();
    }

    /**
     * Generates a test-case. ToDo: Reimplement for new engines, kept for future reference
     *
     * @param testCase potential test-case containing the test model, the mutant, and an id
     * @param tries    number of tries with empty response from the backend before giving up
     */
    private void generateTestCase(final MutationTestCase testCase, final int tries) {
//        final Component mutant = testCase.getMutant();
//
//        // Make a project with the test model and the mutant
//        final Project project = new Project();
//        mutant.setName(MutationTestPlanController.MUTANT_NAME);
//        project.getComponents().addAll(testModel, mutant);
//        project.setGlobalDeclarations(Ecdar.getProject().getGlobalDeclarations());
//        mutant.updateIOList(); // Update io in order to get the right system declarations for the mutant
//
//        new Thread(() -> {
//            try {
//                // If refinement, no test-case to generate.
//                // ToDo (Might not be an issue after switching away from verifytga):
//                // I use endsWith rather than contains,
//                // since verifytga sometimes output some weird symbols at the start of this line.
//                if (lines.stream().anyMatch(line -> line.endsWith(" -- Property is satisfied."))) {
//                    Platform.runLater(this::onGenerationJobDone);
//                    return;
//                }
//
//                // ToDo (Might not be an issue after switching away from verifytga):
//                // Verifytga should output that the property is not satisfied
//                // If it does not, then this is an error
//                if (lines.stream().noneMatch(line -> line.endsWith(" -- Property is NOT satisfied."))) {
//                    if (lines.isEmpty()) {
//                        if (tries > 1) {
//                            final int newTries = tries - 1;
//                            Ecdar.showToast("Empty response from backend with " + testCase.getId() +
//                                    ". We will try again. " + newTries + " tr" + (newTries == 1 ? "y" : "ies") +
//                                    " left.");
//                            generateTestCase(testCase, tries - 1);
//                            return;
//                        } else {
//                            throw new MutationTestingException("Output from backend is empty. Model: " + modelPath);
//                        }
//                    }
//
//                    throw new MutationTestingException("Output from backend not understood: " + String.join("\n", lines) + "\n" +
//                            "Model: " + modelPath);
//                }
//
//                int strategyIndex = lines.indexOf("Strategy for the attacker:");
//
//                // If no such index, error
//                if (strategyIndex < 0) {
//                    throw new MutationTestingException("Output from backend not understood: " + String.join("\n", lines) + "\n" +
//                            "Model: " + modelPath);
//                }
//
//                testCase.setStrategy(new NonRefinementStrategy(lines.subList(strategyIndex + 2, lines.size())));
//
//                finishedTestCases.add(testCase);
//            } catch (MutationTestingException | IOException | BackendException e) {
//                e.printStackTrace();
//
//                // Only show error if the process is not already being stopped
//                if (getPlan().getStatus().equals(MutationTestPlan.Status.WORKING)) {
//                    getPlan().setStatus(MutationTestPlan.Status.ERROR);
//                    Platform.runLater(() -> {
//                        final String message = "Error while generating test-case " + testCase.getId() + ": " + e.getMessage();
//                        final Text text = new Text(message);
//                        text.setFill(Color.RED);
//                        getPlan().writeProgress(text);
//                        Ecdar.showToast(message);
//                    });
//                }
//
//                jobsDriver.onJobDone();
//                return;
//            }
//
//            // JavaFX elements cannot be updated in another thread, so make it run in a JavaFX thread at some point
//            Platform.runLater(this::onGenerationJobDone);
//        }).start();
    }

    /**
     * Starts process to fetch a strategy.
     *
     * @param modelPath the path to the backend XML project containing the test model and the mutant
     * @return the started process, or null if an error occurs
     * @throws BackendException if the backend encounters an error
     */
    private Process startProcessToFetchStrategy(final String modelPath) throws BackendException {
        // Run backend to check refinement and to fetch strategy if non-refinement
        // return ((jECDARDriver) BackendDriverManager.getInstance(BackendHelper.BackendNames.jEcdar)).getJEcdarProcessForRefinementCheckAndStrategyIfNonRefinement(modelPath, queryFilePath);
        return null;
    }

    /**
     * Gets the lines from the input stream of the process.
     * If an error occurs, this method tells the user and signals this controller to stop.
     *
     * @param process the process running the backend
     * @return the input lines. Each line is without the newline character.
     * @throws MutationTestingException if the process has a non-empty error stream
     * @throws IOException              if an IO error occurs
     */
    private static List<String> getProcessInputLines(final Process process) throws MutationTestingException, IOException {
        final List<String> lines;

        try (final BufferedReader inputReader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            lines = inputReader.lines().collect(Collectors.toList());
        }

        checkProcessErrorStream(process);

        return lines;
    }

    /**
     * Checks for errors in a process.
     * The input stream must be completely read before calling this.
     * Otherwise, we risk getting stuck while reading the error stream.
     * If an error occurs, this method throws an exception
     *
     * @param process process to check for
     * @throws IOException              if an I/O error occurs
     * @throws MutationTestingException if the process has a non-empty error stream
     */
    private static void checkProcessErrorStream(final Process process) throws IOException, MutationTestingException {
        try (final BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
            final List<String> errorLines = errorReader.lines().collect(Collectors.toList());

            if (!errorLines.isEmpty()) {
                throw new MutationTestingException("Error from the error stream of the process: " + String.join("\n", errorLines));
            }
        }
    }

    /**
     * Is triggered when a test-case generation attempt is done.
     * It updates UI labels to tell user about the progress.
     * Once all test-case generation attempts are done,
     * this method executes the test-cases (not done)
     * <p>
     * This method should be called in a JavaFX thread, since it updates JavaFX elements.
     */
    private synchronized void onGenerationJobDone() {
        getPlan().setTestCasesText("Test-cases: " + finishedTestCases.size() + " - Generation time: " +
                MutationTestPlanPresentation.readableFormat(Duration.between(generationStart, Instant.now())));

        jobsDriver.onJobDone();
    }
}
