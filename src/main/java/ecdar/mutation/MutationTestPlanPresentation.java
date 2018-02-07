package ecdar.mutation;

import ecdar.Ecdar;
import ecdar.abstractions.Component;
import ecdar.presentations.EcdarFXMLLoader;
import ecdar.presentations.HighLevelModelPresentation;
import javafx.scene.control.Label;

/**
 * Presentation for a test plan with model-based mutation testing.
 */
public class MutationTestPlanPresentation extends HighLevelModelPresentation {
    private final MutationTestPlanController controller;

    /**
     * Constructs the presentation and initializes it.
     * @param testPlan the test plan to present
     */
    public MutationTestPlanPresentation(final MutationTestPlan testPlan) {
        controller = new EcdarFXMLLoader().loadAndGetController("MutationTestPlanPresentation.fxml", this);
        controller.setPlan(testPlan);

        initialize();
    }

    /**
     * Initializes this.
     */
    private void initialize() {
        // Fill test model picker with components
        for (final Component component : Ecdar.getProject().getComponents()) {
            final Label label = new Label(component.getName());
            controller.testModelPicker.getItems().add(label);

            // If component is the test model of the test plan, select it
            final String testModelId = controller.getPlan().getTestModelId();
            if (testModelId != null && testModelId.equals(component.getName()))
                controller.testModelPicker.setValue(label);
        }

        // If test model is not selected, disable test button
        if (controller.testModelPicker.getValue() == null) {
            controller.testButton.setDisable(true);

            // Enable it once user selects a test model
            controller.testModelPicker.valueProperty().addListener(((observable, oldValue, newValue) ->
                    controller.testButton.setDisable(false)));
        }

        // Bind test plan to test model picker
        controller.testModelPicker.valueProperty().addListener(((observable, oldValue, newValue) ->
                    controller.getPlan().setTestModelId(newValue.getText())));

        controller.mutantsText.textProperty().bind(controller.getPlan().mutantsTextProperty());
        controller.testCasesText.textProperty().bind(controller.getPlan().testCasesTextProperty());
    }
}
