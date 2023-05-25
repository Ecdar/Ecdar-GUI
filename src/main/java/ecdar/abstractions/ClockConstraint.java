package ecdar.abstractions;

import javafx.util.Pair;

public class ClockConstraint {
    public final Pair<String, String> clocks;
    public final char comparator;
    public final int constant;
    public final boolean isStrict;

    public ClockConstraint(String left_clock, String right_clock, int constant, char comparator, boolean isStrict) {
        this.clocks = new Pair<>(left_clock, right_clock);
        this.constant = constant;
        this.comparator = comparator;
        this.isStrict = isStrict;
    }

    @Override
    public String toString() {
        if (clocks.getValue() != null) {
            return clocks.getKey() + " - " +
                    clocks.getValue() + " " +
                    comparator + (isStrict ? "= " : " ") +
                    constant;
        } else {
            return clocks.getKey() + " " +
                    comparator + (isStrict ? "= " : " ") +
                    constant;
        }
    }
}