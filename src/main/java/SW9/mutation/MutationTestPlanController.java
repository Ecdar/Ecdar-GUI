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

            final Project project = new Project();
            mutants.get(0).setName(MUTANT_NAME);
            project.getComponents().addAll(testModel, mutants.get(0));

            try {
                UPPAALDriver.storeBackendModel(project);
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
