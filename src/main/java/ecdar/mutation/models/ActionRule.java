package ecdar.mutation.models;

import ecdar.mutation.MutationTestingException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * An action rule in a strategy.
 */
public class ActionRule extends StrategyRule {
    private static final String REGEX_TRANSITION = "^\\w+\\.\\w+->\\w+\\.(\\w+) \\{ [^,]*, (\\w+)[?!], (.*) }$";

    private final String endLocationName;
    private final String updateProperty;
    private final String sync;

    public ActionRule(final String condition, final String transition) throws MutationTestingException {
        super(condition);

        final Matcher matcher = Pattern.compile(REGEX_TRANSITION).matcher(transition);

        if (!matcher.find()) throw new MutationTestingException("Strategy transition " + transition + " does not match " + REGEX_TRANSITION);

        endLocationName = matcher.group(1);
        sync = matcher.group(2);

        if (matcher.group(3).equals("1")) updateProperty = "";
        else updateProperty = matcher.group(3);
    }


    /* Getters and setters */

    public String getEndLocationName() {
        return endLocationName;
    }

    public String getUpdateProperty() {
        return updateProperty;
    }

    /**
     * Gets the synchronisation property with ? or !.
     * @return the property
     */
    public String getSync() {
        return sync;
    }
}
