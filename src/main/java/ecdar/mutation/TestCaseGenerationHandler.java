package ecdar.mutation;

import ecdar.Ecdar;
import ecdar.abstractions.Component;
import ecdar.abstractions.Project;
import ecdar.abstractions.SimpleComponentsSystemDeclarations;
import ecdar.backend.BackendException;
import ecdar.backend.UPPAALDriver;
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
import java.util.function.Supplier;
import java.util.stream.Collectors;

class TestCaseGenerationHandler {
    private final static String TEST_MODEL_NAME = "S";
    private final static String MUTANT_NAME = "M";

    private final MutationTestPlan plan;

    private Component getTestModel() {
        return testModel;
    }
    private Supplier<Integer> getMaxThreadsSupplier() {
        return maxThreadsSupplier;
    }
    private Consumer<Text> getProgressWriter() {
        return progressWriter;
    }

    private final Component testModel;
    private final Supplier<Integer> maxThreadsSupplier;
    private List<Component> mutants;
    private List<MutationTestCase> testCases;

    // Progress fields
    private final Consumer<Text> progressWriter;
    private Instant generationStart;
    private String queryFilePath;
    private int generationJobsStarted;
    private int generationJobsEnded;

    /**
     * Constructs the handler.
     * @param plan the test plan containing options for generation
     * @param testModel the tet model to use
     * @param progressWriter object to write progress with
     * @param maxThreadsSupplier supplier for getting the maximum allowed threads while generating
     */
    TestCaseGenerationHandler(final MutationTestPlan plan, final Component testModel, final Consumer<Text> progressWriter, final Supplier<Integer> maxThreadsSupplier) {
        this.plan = plan;
        this.testModel = testModel;
        this.progressWriter = progressWriter;
        this.maxThreadsSupplier = maxThreadsSupplier;
    }

    private MutationTestPlan getPlan() {
        return plan;
    }

    void start() {
        getPlan().setStatus(MutationTestPlan.Status.WORKING);

        testModel.setName(TEST_MODEL_NAME);
        testModel.updateIOList();

        final Instant start = Instant.now();

        // Mutate with selected operators
        mutants = new ArrayList<>();
        for (final MutationOperator operator : getPlan().getSelectedMutationOperators()) mutants.addAll(operator.compute(getTestModel()));

        mutants.forEach(Component::applyAngelicCompletion);

        getPlan().setMutantsText("Mutants: " + mutants.size() + " - Execution time: " + MutationTestPlanPresentation.readableFormat(Duration.between(start, Instant.now())));

        // If chosen, apply demonic completion
        if (getPlan().isDemonic()) testModel.applyDemonicCompletion();

        // Create test cases
        generationJobsStarted = 0;
        generationJobsEnded = 0;
        generationStart = Instant.now();
        testCases = Collections.synchronizedList(new ArrayList<>()); // use synchronized to be thread safe

        try {
            queryFilePath = UPPAALDriver.storeQuery("refinement: " + MUTANT_NAME + "<=" + TEST_MODEL_NAME, "query");
        } catch (URISyntaxException | IOException e) {
            e.printStackTrace();
            Ecdar.showToast("Error: " + e.getMessage());
            return;
        }

        updateGenerationJobs();
    }

    /**
     * Updates what test-case generation jobs to run.
     */
    private synchronized void updateGenerationJobs() {
        if (getPlan().getStatus().equals(MutationTestPlan.Status.STOPPING)) {
            if (getGenerationJobsRunning() == 0) {
                getPlan().setStatus(MutationTestPlan.Status.IDLE);
            }

            return;
        }

        // If we are done, clean up and move on
        if (generationJobsEnded == mutants.size()) {
            try {
                FileUtils.cleanDirectory(new File(UPPAALDriver.getTempDirectoryAbsolutePath()));
            } catch (IOException | URISyntaxException e) {
                e.printStackTrace();
                Ecdar.showToast("Error: " + e.getMessage());
                return;
            }

            final Text text = new Text("Done");
            text.setFill(Color.GREEN);
            getProgressWriter().accept(text);
            getPlan().setStatus(MutationTestPlan.Status.IDLE);

            return;
        }

        writeProgress("Generating test-cases... (" + generationJobsEnded + "/" + mutants.size() + " mutants processed)");

        // while we have not reach the maximum allowed threads and there are still jobs to start
        while (getGenerationJobsRunning() < getMaxThreadsSupplier().get() &&
                generationJobsStarted < mutants.size()) {
            generateTestCase(testModel, mutants.get(generationJobsStarted), generationJobsStarted);
            generationJobsStarted++;
        }
    }

    private void writeProgress(final String message) {
        final Text text = new Text(message);
        text.setFill(Color.web("#333333"));
        getProgressWriter().accept(text);
    }

    /**
     * Gets the number of generation jobs currently running.
     * @return the number of jobs running
     */
    private synchronized int getGenerationJobsRunning() {
        return generationJobsStarted - generationJobsEnded;
    }

    /**
     * Generates a test-case.
     * @param testModel test model
     * @param mutant mutant used for generating
     * @param mutationIndex index of the mutant used for generating
     */
    private void generateTestCase(final Component testModel, final Component mutant, final int mutationIndex) {
        // make a project with the test model and the mutant
        final Project project = new Project();
        mutant.setName(MUTANT_NAME);
        project.getComponents().addAll(testModel, mutant);
        project.setGlobalDeclarations(Ecdar.getProject().getGlobalDeclarations());
        mutant.updateIOList(); // Update io in order to get the right system declarations for the mutant
        project.setSystemDeclarations(new SimpleComponentsSystemDeclarations(testModel, mutant));

        new Thread(() -> {
            try {
                final Process process = startVerifytgaProcess(mutationIndex, project);
                if (process == null) return;

                List<String> lines = getVerifytgaInputLines(process);
                if (lines == null) return;

                // If refinement, no test-case to generate.
                // I use endsWith rather than contains,
                // since verifytga sometimes output some weird symbols at the start of this line.
                if (lines.stream().anyMatch(line -> line.endsWith(" -- Property is satisfied."))) {
                    Platform.runLater(this::onGenerationJobDone);
                    return;
                }

                // Verifytga should output that the property is not satisfied
                // If it does not, then this is an error
                if (lines.stream().noneMatch(line -> line.endsWith(" -- Property is NOT satisfied."))) {
                    throw new MutationTestingException("Output from verifytga not understood: " + String.join("\n", lines) + "\n" +
                            "Mutation index: " + mutationIndex);
                }

                int strategyIndex = lines.indexOf("Strategy for the attacker:");

                // If no such index, error
                if (strategyIndex < 0) {
                    throw new MutationTestingException("Output from verifytga not understood: " + String.join("\n", lines) + "\n" +
                            "Mutation index: " + mutationIndex);
                }

                List<String> strategy = lines.subList(strategyIndex + 2, lines.size());

                testCases.add(new MutationTestCase(testModel, mutant, strategy));
            } catch (MutationTestingException e) {
                e.printStackTrace();

                // Only show error if the process is not already being stopped
                if (getPlan().getStatus().equals(MutationTestPlan.Status.WORKING)) {
                    getPlan().setStatus(MutationTestPlan.Status.STOPPING);
                    Platform.runLater(() -> {
                        final String message = "Error while generating test-cases: " + e.getMessage();
                        final Text text = new Text(message);
                        text.setFill(Color.RED);
                        progressWriter.accept(text);
                        Ecdar.showToast(message);
                    });
                }
            }

            // JavaFX elements cannot be updated in another thread, so make it run in a JavaFX thread at some point
            Platform.runLater(this::onGenerationJobDone);
        }).start();
    }

    /**
     * Starts varifytga to fetch a strategy.
     * @param mutationIndex the inputs of the mutant to pass to verifytga
     * @param project the backend XML project containing the test model and the mutant
     * @return the started process, or null if an error occurs
     */
    private Process startVerifytgaProcess(final int mutationIndex, final Project project) throws MutationTestingException {
        final Process process;

        try {
            // Store the project and the refinement query as backend XML
            final String modelPath = UPPAALDriver.storeBackendModel(project, "model" + mutationIndex);

            // Run verifytga to check refinement and to fetch strategy if non-refinement
            final ProcessBuilder builder = new ProcessBuilder(UPPAALDriver.findVerifytgaAbsolutePath(), "-t0", modelPath, queryFilePath);
            process = builder.start();

        } catch (BackendException | IOException | URISyntaxException e) {
            throw new MutationTestingException("Error while starting verifytga", e);
        }

        return process;
    }

    /**
     * Gets the lines from the input stream of verifytga.
     * If an error occurs, this method tells the user and signals this controller to stop.
     * @param process the process running verifytga
     * @return the input lines. Each line is without the newline character.
     * @throws MutationTestingException if an I/O error occurs
     */
    private static List<String> getVerifytgaInputLines(final Process process) throws MutationTestingException {
        List<String> lines;

        try (BufferedReader inputReader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            lines = inputReader.lines().collect(Collectors.toList());
            if (!handlePotentialErrorsFromVerifytga(process)) lines = null;
        } catch (final IOException e) {
            throw new MutationTestingException("I/O exception while reading from verifytga");
        }

        return lines;
    }

    /**
     * Checks for errors in a process.
     * The input stream must be completely read before calling this.
     * Otherwise, we rick getting stuck while reading the error stream.
     * If an error occurs, this method tells the user and signals this controller to stop.
     * @param process process to check for
     * @return true iff process was successful (e.g. no errors was found)
     * @throws IOException if an I/O error occurs
     */
    private static boolean handlePotentialErrorsFromVerifytga(final Process process) throws IOException, MutationTestingException {
        try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
            //final String errorLine = errorReader.readLine();
            final List<String> errorLines = errorReader.lines().collect(Collectors.toList());

            if (!errorLines.isEmpty()) {
                throw new MutationTestingException("Error from the error stream of verifytga: " + String.join("\n", errorLines));
            }
        }
        return true;
    }

    /**
     * Is triggered when a test-case generation attempt is done.
     * It updates UI labels to tell user about the progress.
     * Once all test-case generation attempts are done,
     * this method executes the test-cases (not done)
     *
     * This method should be called in a JavaFX thread, since it updates JavaFX elements.
     */
    private synchronized void onGenerationJobDone() {
        generationJobsEnded++;

        getPlan().setTestCasesText("Test-cases: " + testCases.size() + " - Generation time: " + MutationTestPlanPresentation.readableFormat(Duration.between(generationStart, Instant.now())));

        updateGenerationJobs();
    }

}
