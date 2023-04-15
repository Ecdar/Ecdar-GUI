package ecdar.mutation.operators;

import ecdar.abstractions.Component;
import ecdar.abstractions.Location;
import ecdar.mutation.ComponentVerificationTransformer;
import ecdar.mutation.TextFlowBuilder;
import ecdar.mutation.models.MutationTestCase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Mutation operator that changes an invariant.
 * It adds 1 to the right side of an invariant part.
 * Parts are divided by conjunction.
 */
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
                final Component mutant = ComponentVerificationTransformer.cloneForVerification(original);

                final List<String> newParts = new ArrayList<>(invariantParts);
                newParts.set(partIndex, newParts.get(partIndex) + " + 1");

                final String invariant = String.join(" && ", newParts);
                mutant.getLocations().get(locationIndex).setInvariant(invariant);

                cases.add(new MutationTestCase(original, mutant,
                        getCodeName() + "_" + originalLocation.getId() + "_" + partIndex,
                        new TextFlowBuilder().text("Changed ").boldText("invariant").text(" of ")
                                .locationLink(originalLocation.getId(), original.getName()).text(" from ")
                                .boldText(originalLocation.getInvariant()).text(" to ").boldText(invariant).build()
                ));
            }

        }

        return cases;
    }

    @Override
    public String getDescription() {
        return "Adds 1 to the right side of an invariant part. " +
                "Invariant parts are divided by conjunction.";
    }

    @Override
    public int getUpperLimit(final Component original) {
        int count = 0;

        // For all locations in the original component
        for (int locationIndex = 0; locationIndex < original.getLocations().size(); locationIndex++) {
            final Location originalLocation = original.getLocations().get(locationIndex);

            if (originalLocation.getInvariant().trim().isEmpty()) continue;

            count += originalLocation.getInvariant().split("&&").length;
        }

        return count;
    }

    @Override
    public boolean isUpperLimitExact() {
        return true;
    }
}
