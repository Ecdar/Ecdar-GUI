package SW9.mutation;

import SW9.presentations.EcdarFXMLLoader;
import SW9.presentations.HighLevelModelPresentation;

public class MutationTestPlanPresentation extends HighLevelModelPresentation {
    private final MutationTestPlanController controller;

    public MutationTestPlanPresentation(final MutationTestPlan testPlan) {
        controller = new EcdarFXMLLoader().loadAndGetController("MutationTestPlanPresentation.fxml", this);
        controller.setPlan(testPlan);
    }
}
