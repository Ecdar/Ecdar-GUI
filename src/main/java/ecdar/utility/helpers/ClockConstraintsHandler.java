package ecdar.utility.helpers;

import ecdar.abstractions.ClockConstraint;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ClockConstraintsHandler {
    /**
     * Generates a string representation of the clock constraints of the provided state
     *
     * @param clockConstraints list of the clock constraints represent in the string
     * @return a prettified string representation of the clock constraints
     */
    public String getStateClockConstraintsString(List<ClockConstraint> clockConstraints) {
        if (clockConstraints.isEmpty()) return "";

        StringBuilder clockConstraintsString = new StringBuilder();
        List<Integer> mergedConstraints = new ArrayList<>();

        for (int i = 0; i < clockConstraints.size(); i++) {
            if (mergedConstraints.contains(i)) continue; // 'i' has already been merged

            for (int j = 1; j < clockConstraints.size(); j++) {
                if (i == j || mergedConstraints.contains(j)) continue; // 'j' has already been merged

                ClockConstraint c1 = clockConstraints.get(i);
                ClockConstraint c2 = clockConstraints.get(j);

                if (constraintClocksMatch(c1, c2)) {
                    clockConstraintsString.append(getMergedConstraintString(c1, c2)).append("\n");
                    mergedConstraints.add(i);
                    mergedConstraints.add(j);
                }
            }
        }

        for (int i = 0; i < clockConstraints.size(); i++) {
            if (!mergedConstraints.contains(i)) {
                clockConstraintsString.append(clockConstraints.get(i).toString()).append("\n");
            }
        }

        // Remove trailing newline
        clockConstraintsString.setLength(clockConstraintsString.length() - 1);

        return clockConstraintsString.toString();
    }

    /**
     * Merges two clock constraints into a combined string.
     * If the two constraints are incompatible, this returns the two individual string representations split by `\n`
     *
     * @param c1 first clock constraint
     * @param c2 second clock constraint
     * @return string representation of the merged clock constraint if compatible, or the two separate representations otherwise
     */
    private String getMergedConstraintString(ClockConstraint c1, ClockConstraint c2) {
        StringBuilder mergedConstraint = new StringBuilder();

        if ((c1.comparator == '<' && c2.comparator == '>') ||
                (c1.comparator == '>' && c2.comparator == '<')) {
            var smallestConstraint = c1.comparator == '>' ? c1 : c2;
            var largestConstraint = c1.equals(smallestConstraint) ? c2 : c1;

            // Lower bound
            mergedConstraint.append(smallestConstraint.constant)
                    .append(" <")
                    .append(smallestConstraint.isStrict ? "= " : " ");

            // Clock relation
            mergedConstraint.append(smallestConstraint.clocks.getKey());
            if (smallestConstraint.clocks.getValue() != null) {
                mergedConstraint.append(" - ").append(smallestConstraint.clocks.getValue());
            }
            mergedConstraint.append(" ");

            // Upper bound
            mergedConstraint.append("<")
                    .append(largestConstraint.isStrict ? "= " : " ")
                    .append(largestConstraint.constant);

            return mergedConstraint.toString();
        } else if (Objects.equals(c1.clocks.getValue(), c2.clocks.getKey()) &&
                Objects.equals(c1.clocks.getKey(), c2.clocks.getValue())) {
            if (c1.constant == 0 && c2.constant == 0) {
                return mergedConstraint.append(c1.clocks.getKey())
                        .append(" = ")
                        .append(c1.clocks.getValue()).toString();
            }
        }

        return mergedConstraint.append(c1).append("\n").append(c2).toString();
    }

    /**
     * Checks if the tro constraints can be merged
     *
     * @param c1 first clock constraint
     * @param c2 second clock constraint
     * @return true if the two clock constraints are compatible, false otherwise
     */
    private boolean constraintClocksMatch(ClockConstraint c1, ClockConstraint c2) {
        return (Objects.equals(c1.clocks.getValue(), c2.clocks.getValue()) && Objects.equals(c1.clocks.getKey(), c2.clocks.getKey())) || (Objects.equals(c1.clocks.getValue(), c2.clocks.getKey()) && Objects.equals(c1.clocks.getKey(), c2.clocks.getValue()));
    }
}
