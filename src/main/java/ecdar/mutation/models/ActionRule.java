package ecdar.mutation.models;

import ecdar.abstractions.EdgeStatus;
import ecdar.mutation.MutationTestingException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * An action rule in a strategy.
 */
public class ActionRule extends StrategyRule {
    private static final String REGEX_TRANSITION = "^\\w+\\.\\w+->\\w+\\.(\\w+) \\{ [^,]*, (\\w+)([?!]), (.*) }$";

    private final String endLocationName;
    private final String sync;
    private final EdgeStatus status;
    private final String updateProperty;

    public ActionRule(final String condition, final String transition) throws MutationTestingException {
        super(condition);

        final Matcher matcher = Pattern.compile(REGEX_TRANSITION).matcher(transition);

        if (!matcher.find()) throw new MutationTestingException("Strategy transition " + transition + " does not match " + REGEX_TRANSITION);

        endLocationName = matcher.group(1);
        sync = matcher.group(2);
        status = matcher.group(3).equals("?") ? EdgeStatus.INPUT : EdgeStatus.OUTPUT;

        if (matcher.group(4).equals("1")) updateProperty = "";
        else updateProperty = matcher.group(4);
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

    public EdgeStatus getStatus() {
        return status;
    }
}
