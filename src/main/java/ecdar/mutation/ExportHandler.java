package ecdar.mutation;

import com.google.gson.GsonBuilder;
import ecdar.Ecdar;
import ecdar.abstractions.Component;
import ecdar.abstractions.Project;
import ecdar.abstractions.SimpleComponentsSystemDeclarations;
import ecdar.backend.BackendException;
import ecdar.backend.UPPAALDriver;
import ecdar.mutation.models.MutationOperator;
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

public class ExportHandler {
    private final MutationTestPlan plan;

    public Component getTestModel() {
        return testModel;
    }

    private final Component testModel;
    private final Consumer<Text> progressWriter;

    ExportHandler(final MutationTestPlan plan, final Component testModel, final Consumer<Text> progressWriter) {
        this.plan = plan;
        this.testModel = testModel;
        this.progressWriter = progressWriter;
    }

    public MutationTestPlan getPlan() {
        return plan;
    }

    void start() {
        getPlan().setStatus(MutationTestPlan.Status.WORKING);

        final Instant start = Instant.now();

        // Mutate with selected operators
        final List<Component> mutants = new ArrayList<>();
        try {
            for (final MutationOperator operator : getPlan().getSelectedMutationOperators()) mutants.addAll(operator.compute(getTestModel()));
        } catch (final MutationTestingException e) {
            handleException(e);
            return;
        }

        for (final Component mutant : mutants) {
            // Name them the same name as the test model
            mutant.setName(getTestModel().getName());

            mutant.updateIOList();
        }

        // Apply angelic completion if selected
        if (getPlan().isAngelicWhenExport())
            mutants.forEach(Component::applyAngelicCompletion);

        getPlan().setMutantsText("Mutants: " + mutants.size() + " - Execution time: " + MutationTestPlanPresentation.readableFormat(Duration.between(start, Instant.now())));


        try {
            final String path;

            if (getPlan().getFormat().equals("XML")) {
                path = Ecdar.getRootDirectory() + File.separator + "mutants" + File.separator + "xml";

                FileUtils.forceMkdir(new File(path));
                FileUtils.cleanDirectory(new File(path));

                for (int i = 0; i < mutants.size(); i++) {
                    storeMutantXml(mutants.get(i), i);
                }
            } else {
                path = Ecdar.getRootDirectory() + File.separator + "mutants" + File.separator + "json";

                FileUtils.forceMkdir(new File(path));
                FileUtils.cleanDirectory(new File(path));

                for (int i = 0; i < mutants.size(); i++) {
                    storeMutantJson(mutants.get(i), i);
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

    private void storeMutantXml(final Component mutant, final int index) throws BackendException, IOException, URISyntaxException {
        // make a project with the mutant
        final Project project = new Project();
        project.getComponents().add(mutant);
        project.setGlobalDeclarations(Ecdar.getProject().getGlobalDeclarations());
        mutant.updateIOList(); // Update io in order to get the right system declarations for the mutant
        project.setSystemDeclarations(new SimpleComponentsSystemDeclarations(mutant));

        UPPAALDriver.storeBackendModel(project, "mutants" + File.separator + "xml", "model" + index);
    }

    private void storeMutantJson(final Component component, final int index) throws URISyntaxException, IOException {
        final FileWriter writer = new FileWriter(Ecdar.getRootDirectory() + File.separator + "mutants" + File.separator + "json" + File.separator + "model" + index + ".json");

        new GsonBuilder().setPrettyPrinting().create().toJson(component.serialize(), writer);
        writer.close();
    }

    public Consumer<Text> getProgressWriter() {
        return progressWriter;
    }

    private void handleException(final MutationTestingException e) {
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
}
