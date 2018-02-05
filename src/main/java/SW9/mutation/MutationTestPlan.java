package SW9.mutation;

import SW9.Ecdar;
import SW9.abstractions.Component;
import SW9.abstractions.HighLevelModelObject;

import java.util.HashSet;

/**
 * A test plan for conducting model-based mutation testing on a component.
 */
public class MutationTestPlan extends HighLevelModelObject {
    private static final String TEST = "Test";

    public MutationTestPlan() {
        generateName();
    }


    /**
     * Generate and sets a unique id for this system
     */
    private void generateName() {
        final HashSet<String> names = new HashSet<>();

        for (final MutationTestPlan plan : Ecdar.getProject().getTestPlans()){
            names.add(plan.getName());
        }

        for (int counter = 1; ; counter++) {
            if (!names.contains(TEST + counter)){
                setName((TEST + counter));
                return;
            }
        }
    }
}
