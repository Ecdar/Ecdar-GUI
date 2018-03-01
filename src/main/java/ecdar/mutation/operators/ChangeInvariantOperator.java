package ecdar.mutation.operators;

import ecdar.abstractions.Component;
import ecdar.abstractions.Location;
import ecdar.mutation.models.MutationTestCase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ChangeInvariantOperator extends MutationOperator {
    @Override
    public String getText() {
        return "Change invariant";
    }

    @Override
    public String getCodeName() {
        return "changeInvariant";
    }

    @Override
    public List<MutationTestCase> generateTestCases(final Component original) {
        final List<MutationTestCase> cases = new ArrayList<>();

        // For all locations in the original component
        for (int locationIndex = 0; locationIndex < original.getLocations().size(); locationIndex++) {
            final Location originalLocation = original.getLocations().get(locationIndex);

            if (originalLocation.getInvariant().trim().isEmpty()) continue;

            final List<String> invariantParts = Arrays.stream(originalLocation.getInvariant()
                    .split("&&")).map(String::trim).collect(Collectors.toList());
            for (int partIndex = 0; partIndex < invariantParts.size(); partIndex++) {
                final Component mutant = original.cloneForVerification();

                final List<String> newParts = new ArrayList<>(invariantParts);
                newParts.set(partIndex, newParts.get(partIndex) + " + 1");

                final String invariant = String.join(" && ", newParts);
                mutant.getLocations().get(locationIndex).setInvariant(invariant);

                cases.add(new MutationTestCase(original, mutant,
                        getCodeName() + "_" + originalLocation.getId() + "_" + partIndex,
                        "Changed invariant of " + originalLocation.getId() + " to " + invariant));
            }

        }

        return cases;
    }

    @Override
    public String getDescription() {
        return "Adds 1 to the right side of an invariant part. " +
                "Invariant parts are divided by conjunction. " +
                "Creates [# of invariant parts] mutants.";
    }
}
