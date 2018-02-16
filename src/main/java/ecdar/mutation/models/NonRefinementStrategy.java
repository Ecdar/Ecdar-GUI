package ecdar.mutation.models;


import ecdar.mutation.MutationTestingException;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A strategy for showing a non-refinement.
 */
public class NonRefinementStrategy {
    private Map<String, List<StrategyRule>> rules = new HashMap<>();

    /**
     * Constructs a strategy based on the result of verifytga.
     * @param lines lines of the strategy as taking from verifytga
     * @throws MutationTestingException if the lines were not understood
     */
    public NonRefinementStrategy(final List<String> lines) throws MutationTestingException {
        final Iterator<String> iterator =  lines.iterator();
        while (iterator.hasNext()) {
            final String locationsLine = iterator.next();
            final String locationsRegex = "^State: \\( (.*) \\) \\[spoiler] $";
            final Matcher locationsMatcher = Pattern.compile(locationsRegex).matcher(locationsLine);

            if (!locationsMatcher.find()) throw new MutationTestingException("strategy line \"" + locationsLine + "\" does not match \"" + locationsRegex + "\"");

            final List<StrategyRule> ruleList = new ArrayList<>();
            rules.put(locationsMatcher.group(1), ruleList);

            String line;
            while (iterator.hasNext() && !(line = iterator.next()).isEmpty()) {
                final String delayRegex = "^While you are in\\s(.*), wait.$";
                final Matcher delayMatcher = Pattern.compile(delayRegex).matcher(line);

                if (delayMatcher.find()) {
                    ruleList.add(new DelayRule(delayMatcher.group(1)));
                    continue;
                }

                final String actionRegex = "^When you are in\\s(.*), take transition (.*) \\[SKIP]$";
                final Matcher actionMatcher = Pattern.compile(actionRegex).matcher(line);

                if (actionMatcher.find()) {
                    ruleList.add(new ActionRule(actionMatcher.group(1), actionMatcher.group(2)));
                    continue;
                }

                throw new MutationTestingException("Strategy line \"" + line + "\" does not match \"" + delayRegex + "\" or \"" + actionRegex + "\"");
            }
        }
    }


}
