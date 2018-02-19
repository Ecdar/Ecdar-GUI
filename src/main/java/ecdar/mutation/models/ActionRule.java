package ecdar.mutation.models;

import ecdar.mutation.MutationTestingException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * An action rule in a strategy.
 */
public class ActionRule extends StrategyRule {
    public static final String REGEX_TRANSITION = "^\\w+\\.\\w+->\\w+\\.(\\w)+ \\{ [^,]*, [^,]*, (.*) }$";

    private String endLocationName, updateProperty;

    public ActionRule(final String condition, final String transition) throws MutationTestingException {
        super(condition);

        final Matcher matcher = Pattern.compile(REGEX_TRANSITION).matcher(transition);

        if (!matcher.find()) throw new MutationTestingException("Strategy transition " + transition + " does not match " + REGEX_TRANSITION);

        endLocationName = matcher.group(1);

        if (matcher.group(2).equals("1")) updateProperty = "";
        else updateProperty = matcher.group(2);
    }

    public String getEndLocationName() {
        return endLocationName;
    }

    public String getUpdateProperty() {
        return updateProperty;
    }
}
