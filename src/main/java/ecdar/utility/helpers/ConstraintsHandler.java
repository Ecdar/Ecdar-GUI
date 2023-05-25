package ecdar.utility.helpers;

import EcdarProtoBuf.ObjectProtos;
import ecdar.abstractions.ClockConstraint;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class ConstraintsHandler {
    public static String getStateClockConstraintsString(ObjectProtos.State protoState) {
        List<ClockConstraint> refinedConstraints = new ArrayList<>();
        for (var constraint : protoState.getZone().getConjunctions(0).getConstraintsList()) {
            refinedConstraints.add(getClockConstraintLine(constraint));
        }

//        ToDo NIELS: Check for combinable constraints
//        StringBuilder clockConstraintsString = new StringBuilder();
//        List<Integer> mergedConstraints = new ArrayList<>();
//
//        for (int i = 0; i < refinedConstraints.size(); i++) {
//            if (mergedConstraints.contains(i)) continue; // 'i' has already been merged
//
//            for (int j = 1; j < refinedConstraints.size(); j++) {
//                if (i == j || mergedConstraints.contains(j)) continue; // 'j' has already been merged
//
//                ClockConstraint c1 = refinedConstraints.get(i);
//                ClockConstraint c2 = refinedConstraints.get(j);
//
//                if (constraintClocksMatch(c1, c2)) {
//                    clockConstraintsString.append(getMergedConstraintString(c1, c2)).append("\n");
//                    mergedConstraints.add(i);
//                    mergedConstraints.add(j);
//                }
//            }
//        }
//
//        for (int i = 0; i < refinedConstraints.size(); i++) {
//            if (!mergedConstraints.contains(i)) {
//                clockConstraintsString.append(refinedConstraints.get(i).toString()).append("\n");
//            }
//        }
//
//        // Remove trailing newline
//        clockConstraintsString.setLength(clockConstraintsString.length() - 1);

        return refinedConstraints.stream().map(ClockConstraint::toString).collect(Collectors.joining("\n"));
    }

    private static String getMergedConstraintString(ClockConstraint c1, ClockConstraint c2) {
        StringBuilder mergedConstraint = new StringBuilder();

        var smallestConstraint = c1.constant < c2.constant ? c1 : c2;
        var largestConstraint = c1.equals(smallestConstraint) ? c2 : c1;

        // Left constant
        mergedConstraint.append(smallestConstraint.constant).append(" ").append(smallestConstraint.comparator).append(smallestConstraint.isStrict ? "= " : " ");

        // Clocks
        mergedConstraint.append(smallestConstraint.clocks.getKey());
        if (smallestConstraint.clocks.getValue() == null) {
            mergedConstraint.append(" - ").append(smallestConstraint.clocks.getValue());
        }
        mergedConstraint.append(" ");

        // Right constant
        mergedConstraint.append(largestConstraint.comparator).append(largestConstraint.isStrict ? "= " : " ").append(largestConstraint.constant);

        return mergedConstraint.toString();
    }

    private static boolean constraintClocksMatch(ClockConstraint c1, ClockConstraint c2) {
        return (Objects.equals(c1.clocks.getValue(), c2.clocks.getValue()) && Objects.equals(c1.clocks.getKey(), c2.clocks.getKey())) ||
                (Objects.equals(c1.clocks.getValue(), c2.clocks.getKey()) && Objects.equals(c1.clocks.getKey(), c2.clocks.getValue()));
    }

    private static ClockConstraint getClockConstraintLine(ObjectProtos.Constraint constraint) {
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
