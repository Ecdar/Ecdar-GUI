package ecdar.utility.helpers;

import EcdarProtoBuf.ObjectProtos;
import ecdar.abstractions.ClockConstraint;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ConstraintsHandler {
    /**
     * Generates a string representation of the clock constraints of the provided state
     *
     * @param protoState state containing the clock constraints to represent in the string
     * @return a prettified string representation of the state's clock constraints
     */
    public static String getStateClockConstraintsString(ObjectProtos.State protoState) {
        List<ClockConstraint> refinedConstraints = new ArrayList<>();
        for (var constraint : protoState.getZone().getConjunctions(0).getConstraintsList()) {
            refinedConstraints.add(generateClockConstraint(constraint));
        }

        StringBuilder clockConstraintsString = new StringBuilder();
        List<Integer> mergedConstraints = new ArrayList<>();

        for (int i = 0; i < refinedConstraints.size(); i++) {
            if (mergedConstraints.contains(i)) continue; // 'i' has already been merged

            for (int j = 1; j < refinedConstraints.size(); j++) {
                if (i == j || mergedConstraints.contains(j)) continue; // 'j' has already been merged

                ClockConstraint c1 = refinedConstraints.get(i);
                ClockConstraint c2 = refinedConstraints.get(j);

                if (constraintClocksMatch(c1, c2)) {
                    clockConstraintsString.append(getMergedConstraintString(c1, c2)).append("\n");
                    mergedConstraints.add(i);
                    mergedConstraints.add(j);
                }
            }
        }

        for (int i = 0; i < refinedConstraints.size(); i++) {
            if (!mergedConstraints.contains(i)) {
                clockConstraintsString.append(refinedConstraints.get(i).toString()).append("\n");
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
    private static String getMergedConstraintString(ClockConstraint c1, ClockConstraint c2) {
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
    private static boolean constraintClocksMatch(ClockConstraint c1, ClockConstraint c2) {
        return (Objects.equals(c1.clocks.getValue(), c2.clocks.getValue()) && Objects.equals(c1.clocks.getKey(), c2.clocks.getKey())) || (Objects.equals(c1.clocks.getValue(), c2.clocks.getKey()) && Objects.equals(c1.clocks.getKey(), c2.clocks.getValue()));
    }

    /**
     * Generates a ClockConstraint object from a ProtoBuf constraint
     *
     * @param constraint the ProtoBuf constraint to generate from
     * @return the generated ClockConstraint
     */
    private static ClockConstraint generateClockConstraint(ObjectProtos.Constraint constraint) {
        var clock1 = constraint.getX().getComponentClock().getClockName();
        var clock2 = constraint.getY().getComponentClock().getClockName();
        var constant = constraint.getC();
        var strict = constraint.getStrict();

        if (clock1.equals("")) {
            // The first clock is the zero-clock
            return new ClockConstraint(clock2, null, Math.abs(constant), '>', strict);
        } else if (clock2.equals("")) {
            // The second clock is the zero-clock
            return new ClockConstraint(clock1, null, Math.abs(constant), '<', strict);
        } else {
            // None of the clocks are the zero-clock
            if (constant >= 0) {
                return new ClockConstraint(clock1, clock2, Math.abs(constant), '<', strict);
            } else {
                return new ClockConstraint(clock2, clock1, Math.abs(constant), '>', strict);
            }
        }
    }
}
