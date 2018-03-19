package ecdar.mutation;

import com.google.gson.GsonBuilder;
import ecdar.Ecdar;
import ecdar.abstractions.Component;
import ecdar.abstractions.Project;
import ecdar.abstractions.SimpleComponentsSystemDeclarations;
import ecdar.backend.BackendException;
import ecdar.backend.UPPAALDriver;
import ecdar.mutation.models.MutationTestCase;
import ecdar.mutation.operators.MutationOperator;
import ecdar.mutation.models.MutationTestPlan;
import javafx.application.Platform;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Handles generation and export of mutants.
 */
class ExportHandler {
    private final MutationTestPlan plan;

    private final Component testModel;

    ExportHandler(final MutationTestPlan plan, final Component testModel) {
        this.plan = plan;
        this.testModel = testModel;
    }

    private Component getTestModel() {
        return testModel;
    }

    private MutationTestPlan getPlan() {
        return plan;
    }

    private Consumer<Text> getProgressWriter() {
        return text -> getPlan().writeProgress(text);
    }

    /**
     * Starts the export.
     */
    void start() {
        getPlan().setStatus(MutationTestPlan.Status.WORKING);

        final Instant start = Instant.now();

        // Mutate with selected operators
        final List<MutationTestCase> cases = new ArrayList<>();
        try {
            for (final MutationOperator operator : getPlan().getSelectedMutationOperators())
                cases.addAll(operator.generateTestCases(getTestModel()));
        } catch (final MutationTestingException e) {
            handleException(e);
            return;
        }

        cases.stream().map(MutationTestCase::getMutant).forEach(mutant -> {
            // Name them the same name as the test model
            mutant.setName(getTestModel().getName());

            mutant.updateIOList();
        });

        // Apply angelic completion if selected
        if (getPlan().isAngelicWhenExport())
            cases.stream().map(MutationTestCase::getMutant).forEach(Component::applyAngelicCompletion);

        getPlan().setMutantsText("Mutants: " + cases.size() + " - Execution time: " + MutationTestPlanPresentation.readableFormat(Duration.between(start, Instant.now())));


        try {
            final String path;

            if (getPlan().getFormat().equals("XML")) {
                path = Ecdar.getRootDirectory() + File.separator + "mutants" + File.separator + "xml";

                FileUtils.forceMkdir(new File(path));
                FileUtils.cleanDirectory(new File(path));

                for (final MutationTestCase aCase : cases) {
                    storeMutantXml(aCase);
                }
            } else {
                path = Ecdar.getRootDirectory() + File.separator + "mutants" + File.separator + "json";

                FileUtils.forceMkdir(new File(path));
                FileUtils.cleanDirectory(new File(path));

                for (final MutationTestCase aCase : cases) {
                    storeMutantJson(aCase);
                }
            }

            final Text text = new Text("Exported to " + path);
            text.setFill(Color.GREEN);
            getProgressWriter().accept(text);
            getPlan().setStatus(MutationTestPlan.Status.IDLE);
        } catch (IOException | BackendException | URISyntaxException e) {
            e.printStackTrace();
            final String message = "Error: " + e.getMessage();
            final Text text = new Text(message);
            text.setFill(Color.RED);
            getProgressWriter().accept(text);
            Ecdar.showToast(message);
            getPlan().setStatus(MutationTestPlan.Status.IDLE);
        }
    }

    /**
     * Stores a mutant as an XML file.
     * @param testCase test-case containing the mutant
     * @throws BackendException if an error occurs during generation of backend XML
     * @throws IOException if an error occurs during storing of the file
     * @throws URISyntaxException if an error occurs when getting the URL of the root directory
     */
    private static void storeMutantXml(final MutationTestCase testCase) throws BackendException, IOException, URISyntaxException {
        final Component mutant = testCase.getMutant();

        // make a project with the mutant
        final Project project = new Project();
        project.getComponents().add(mutant);
        project.setGlobalDeclarations(Ecdar.getProject().getGlobalDeclarations());
        mutant.updateIOList(); // Update io in order to get the right system declarations for the mutant
        project.setSystemDeclarations(new SimpleComponentsSystemDeclarations(mutant));

        UPPAALDriver.storeBackendModel(project, "mutants" + File.separator + "xml", testCase.getId());
    }

    /**
     * Stores a mutant as a JSON file.
     * @param testCase test-case containing the mutant
     * @throws IOException if an error occurs during storing of the file
     * @throws URISyntaxException if an error occurs when getting the URL of the root directory
     */
    private static void storeMutantJson(final MutationTestCase testCase) throws URISyntaxException, IOException {
        final FileWriter writer = new FileWriter(Ecdar.getRootDirectory() + File.separator + "mutants" + File.separator + "json" + File.separator + testCase.getId() + ".json");

        new GsonBuilder().setPrettyPrinting().create().toJson(testCase.getMutant().serialize(), writer);
        writer.close();
    }

    /**
     * Handles a mutation test exception.
     * Displays a message to the user.
     * @param e the exception
     */
    private void handleException(final MutationTestingException e) {
        e.printStackTrace();

        // Only show error if the process is not already being stopped
        if (getPlan().getStatus().equals(MutationTestPlan.Status.WORKING)) {
            getPlan().setStatus(MutationTestPlan.Status.STOPPING);
            Platform.runLater(() -> {
                final String message = "Error while generating test-cases: " + e.getMessage();
                final Text text = new Text(message);
                text.setFill(Color.RED);
                getProgressWriter().accept(text);
                Ecdar.showToast(message);
            });
        }
    }
}
