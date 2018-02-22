package ecdar.mutation.models;


import ecdar.mutation.MutationTestPlanController;
import ecdar.mutation.MutationTestingException;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A strategy for showing a non-refinement.
 */
public class NonRefinementStrategy {
    private final Map<String, List<StrategyRule>> rules = new HashMap<>();

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

    /**
     * Gets the first rule satisfying some specified conditions.
     * @param specificationLocation the id of the location of the specification
     * @param mutantLocation the id of the location of the mutant
     * @param specificationValues the values of variables in the specification
     * @param mutantValues the values of variables of the mutant
     * @return the first satisfying rule, or null if none satisfies the conditions
     */
    public StrategyRule getRule(final String specificationLocation, final String mutantLocation,
                                final Map<String, Double> specificationValues, final Map<String, Double> mutantValues) {
        System.out.println("Rule");
        final Map<String, Double> values = new HashMap<>();
        specificationValues.forEach((key, value) -> values.put(MutationTestPlanController.SPEC_NAME + "." + key, value));
        mutantValues.forEach((key, value) -> values.put(MutationTestPlanController.MUTANT_NAME + "." + key, value));


        final List<StrategyRule> ruleList = rules.get(MutationTestPlanController.SPEC_NAME + "." + specificationLocation + " " +
                MutationTestPlanController.MUTANT_NAME + "." + mutantLocation);

        if (ruleList == null) return null;

        return ruleList.stream().filter(rule -> rule.isSatisfied(values)).findFirst().orElse(null);
    }

}
