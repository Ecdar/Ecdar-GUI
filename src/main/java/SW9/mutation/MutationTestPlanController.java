package SW9.mutation;

import SW9.Ecdar;
import SW9.abstractions.Component;
import SW9.abstractions.Project;
import SW9.backend.BackendException;
import SW9.backend.UPPAALDriver;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXComboBox;
import javafx.scene.control.Label;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

public class MutationTestPlanController {
    private final static String TEST_MODEL_NAME = "S";
    private final static String MUTANT_NAME = "M";

    public JFXComboBox<Label> testModelPicker;
    public JFXButton testButton;

    private MutationTestPlan plan;

    public void setPlan(final MutationTestPlan plan) {
        this.plan = plan;

        initialize();
    }

    private void initialize() {
        testButton.setOnAction(event -> {
            final Component testModel = Ecdar.getProject().findComponent(testModelPicker.getValue().getText());
            testModel.setName(TEST_MODEL_NAME);

            final List<Component> mutants = new ChangeSourceOperator(testModel).computeMutants();

            // make a project with the test model and the mutant
            final Project project = new Project();
            mutants.get(0).setName(MUTANT_NAME);
            project.getComponents().addAll(testModel, mutants.get(0));
            project.setGlobalDeclarations(Ecdar.getProject().getGlobalDeclarations());
            mutants.get(0).updateIOList(); // Update io in order to get the right system declarations for the mutant
            project.setSystemDeclarations(new TwoComponentSystemDeclarations(testModel, mutants.get(0)));

            // Store the project and the refinement query as backend XML
            try {
                UPPAALDriver.storeBackendModel(project);
                UPPAALDriver.storeQuery("refinement: " + MUTANT_NAME + "<=" + TEST_MODEL_NAME);
            } catch (BackendException | IOException | URISyntaxException e) {
                Ecdar.showToast("Error: " + e.getMessage());
                e.printStackTrace();
                return;
            }


        });
    }

    public MutationTestPlan getPlan() {
        return plan;
    }
}
