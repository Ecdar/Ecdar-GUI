package ecdar.mutation;

import ecdar.Ecdar;
import ecdar.abstractions.Component;
import ecdar.abstractions.Location;
import ecdar.mutation.models.MutationTestCase;
import ecdar.mutation.models.MutationTestPlan;
import ecdar.mutation.operators.MutationOperator;
import javafx.application.Platform;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Handler for mutating.
 */
public class MutationHandler {
    private final Component testModel;
    private final MutationTestPlan plan;
    private final Consumer<List<MutationTestCase>> consumer;

    /**
     * Constructs.
     * @param testModel test model
     * @param plan test plan
     * @param consumer consumer to be called
     */
    public MutationHandler(final Component testModel, final MutationTestPlan plan, final Consumer<List<MutationTestCase>> consumer) {
        this.testModel = testModel;
        this.plan = plan;
        this.consumer = consumer;
    }


    /* Properties */

    private Component getTestModel() {
        return testModel;
    }

    private MutationTestPlan getPlan() {
        return plan;
    }

    private Consumer<List<MutationTestCase>> getConsumer() {
        return consumer;
    }


    /* Other */

    /**
     * Starts.
     */
    public void start() {
        getPlan().writeProgress("Generating mutants...");
        getPlan().setStatus(MutationTestPlan.Status.WORKING);

        new Thread(() -> {
            testModel.setName(MutationTestPlanController.SPEC_NAME);
            testModel.updateIOList();

            final Instant start = Instant.now();

            // Mutate with selected operators
            List<MutationTestCase> cases = new ArrayList<>();
            try {
                for (final MutationOperator operator : getPlan().getSelectedMutationOperators())
                    cases.addAll(operator.generateTestCases(getTestModel()));
            } catch (final MutationTestingException e) {
                e.printStackTrace();

                getPlan().setStatus(MutationTestPlan.Status.IDLE);
                Platform.runLater(() -> {
                    final String message = "Error while generating mutants: " + e.getMessage();
                    final Text text = new Text(message);
                    text.setFill(Color.RED);
                    getPlan().writeProgress(text);
                    Ecdar.showToast("Error");
                });
                return;
            }

            cases.forEach(testCase -> testCase.getMutant().applyAngelicCompletion());

            Platform.runLater(() -> getPlan().setMutantsText("Mutants: " + cases.size() + " - Mutation time: " +
                    MutationTestPlanPresentation.readableFormat(Duration.between(start, Instant.now())))
            );

            // If chosen, apply demonic completion
            if (getPlan().isDemonic()) testModel.applyDemonicCompletion();

            //Rename universal and inconsistent locations
            testModel.getLocations().forEach(location -> {
                if(location.getType().equals(Location.Type.UNIVERSAL)) {
                    location.setId("Universal");
                } else if(location.getType().equals(Location.Type.INCONSISTENT)) {
                    location.setId("Inconsistent");
                }
            });

            cases.forEach(testCase -> testCase.getMutant().getLocations().forEach(location -> {
                if(location.getType().equals(Location.Type.UNIVERSAL)) {
                    location.setId("Universal");
                } else if(location.getType().equals(Location.Type.INCONSISTENT)) {
                    location.setId("Inconsistent");
                }
            }));

            Platform.runLater(() -> getConsumer().accept(cases));
        }).start();
    }
}
