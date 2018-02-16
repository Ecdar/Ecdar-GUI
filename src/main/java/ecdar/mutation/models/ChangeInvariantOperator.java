package ecdar.mutation.models;

import ecdar.abstractions.Component;
import ecdar.abstractions.Location;

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
    public String getJsonName() {
        return "changeInvariant";
    }

    @Override
    public List<Component> generate(final Component original) {
        final List<Component> mutants = new ArrayList<>();

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

                mutant.getLocations().get(locationIndex).setInvariant(String.join(" && ", newParts));

                mutants.add(mutant);
            }

        }

        return mutants;
    }

    @Override
    public String getDescription() {
        return "Adds 1 to the right side of an invariant part. " +
                "Invariant parts are divided by conjunction. " +
                "Creates [# of invariant parts] mutants.";
    }
}
