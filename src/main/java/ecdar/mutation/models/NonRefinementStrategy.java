package ecdar.mutation.models;


import ecdar.mutation.MutationTestingException;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * A strategy for showing a non-refinement.
 */
public class NonRefinementStrategy {
    private final Map<StrategyState, List<StrategyRule>> rules = new HashMap<>();

    /**
     * Constructs a strategy based on the result of verifytga.
     * @param lines lines of the strategy as taking from verifytga
     * @throws MutationTestingException if the lines were not understood
     */
    public NonRefinementStrategy(final List<String> lines) throws MutationTestingException {
        final Iterator<String> iterator =  lines.iterator();
        while (iterator.hasNext()) {
            final List<StrategyRule> ruleList = new ArrayList<>();
            rules.put(new StrategyState(iterator.next()), ruleList);

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
     * This method is based on to two component simulations,
     * each simulation each component.
     * @param sim1 simulation 1
     * @param sim2 simulation 2
     * @return the first satisfying rule, or null if none satisfies the conditions
     */
    public StrategyRule getRule(final ComponentSimulation sim1, final ComponentSimulation sim2) {
        return getRule(sim1.getName(), sim2.getName(), sim1.getCurrentLocId(), sim2.getCurrentLocId(),
                sim1.getLocalVariableValuations(), sim2.getLocalVariableValuations(),
                sim1.getClockValuations(), sim2.getClockValuations()
        );
    }

    /**
     * Gets the first rule satisfying some specified conditions.
     * @param c1Name name of component 1
     * @param c2Name name of component 2
     * @param c1Loc the id of the current location of component 1
     * @param c2Loc the id of the current location of component 2
     * @param c1Locals the valuations of local variables of component 1
     * @param c2Locals the valuations of local variables of component 2
     * @param c1Clocks the clock valuations of component 1
     * @param c2Clocks the clock valuations of component 2
     * @return the first satisfying rule, or null if none satisfies the conditions
     */
    public StrategyRule getRule(final String c1Name, final String c2Name, final String c1Loc, final String c2Loc,
                                final Map<String, Integer> c1Locals, final Map<String, Integer> c2Locals,
                                final Map<String, Double> c1Clocks, final Map<String, Double> c2Clocks) {
        final List<String> locals = c1Locals.entrySet().stream()
                .map(entry -> c1Name + "." + entry.getKey() + "=" + entry.getValue()).collect(Collectors.toList());
        locals.addAll(c2Locals.entrySet().stream()
                .map(entry -> c2Name + "." + entry.getKey() + "=" + entry.getValue()).collect(Collectors.toList())
        );

        final StrategyState matchingState = rules.keySet().stream()
                .filter(state -> state.matches(c1Name + "." + c1Loc, c2Name + "." + c2Loc, locals))
                .findFirst().orElse(null);

        if (matchingState == null) return null;

        final Map<String, Double> clocks = new HashMap<>();
        c1Clocks.forEach((key, value) -> clocks.put(c1Name + "." + key, value));
        c2Clocks.forEach((key, value) -> clocks.put(c2Name + "." + key, value));

        return rules.get(matchingState).stream().filter(rule -> rule.isSatisfied(clocks)).findFirst()
                .orElse(null);
    }
}
