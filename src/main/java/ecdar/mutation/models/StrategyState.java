package ecdar.mutation.models;

import ecdar.mutation.MutationTestingException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A state according to a non-refinement strategy.
 * A state is defined by its combination of current locations and values of local variables
 */
public class StrategyState {
    private final String loc1, loc2;
    private final List<String> localValuations = new ArrayList<>();

    /**
     * Constructs a state from a strategy line from verifytga.
     * Examples of strategy lines:
     * "State: ( S.L4 M.L4 ) S.sound=0 M.sound=0 [spoiler] "
     * "State: ( S.L4 M.L4 ) [spoiler] "
     * @param line strategy line
     * @throws MutationTestingException if the line cannot be parsed
     */
    public StrategyState(final String line) throws MutationTestingException {
        final String regex = "^State: \\( (\\S*) (\\S*) \\)(.*) \\[spoiler] $";
        final Matcher matcher = Pattern.compile(regex).matcher(line);

        if (!matcher.find()) throw new MutationTestingException("strategy line \"" + line + "\" does not match \"" + regex + "\"");

        loc1 = matcher.group(1);
        loc2 = matcher.group(2);

        if (!matcher.group(3).isEmpty())
            localValuations.addAll(Arrays.asList(matcher.group(3).trim().split(" ")));
    }

    private String getLoc1() {
        return loc1;
    }

    private String getLoc2() {
        return loc2;
    }

    private List<String> getLocalValuations() {
        return localValuations;
    }

    /**
     * Gets if the specified data matches this state.
     * The order of location does not matter.
     * @param location1 a location
     * @param location2 a location
     * @param localValuations valuations of local variables (with fully qualified names) as strings, e.g. the string "C.var=2"
     * @return true iff the data matches
     */
    public boolean matches(final String location1, final String location2, final List<String> localValuations) {
        return (getLoc1().equals(location1) || getLoc1().equals(location2)) && // First location should match
                (getLoc2().equals(location1) || getLoc2().equals(location2)) && // Second location should match
                localValuations.containsAll(getLocalValuations()); // All local valuations should match
    }
}
