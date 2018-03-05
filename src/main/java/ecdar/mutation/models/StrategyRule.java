package ecdar.mutation.models;

import ecdar.utility.ExpressionHelper;

import java.util.Map;

/**
 * A rule in a non-refinement strategy.
 */
public abstract class StrategyRule {
    private final String condition;

    /**
     * Constructs a rule.
     * Examples of conditions:
     * true
     * (20<M.e)
     * (20<M.e && S.f==M.f && M.f==0)
     * (20<=S.c && 20<M.e && M.c<20) || (20<=S.c && M.c<=M.e && M.e<=20) || (S.c<20 && M.c<=M.e)
     * @param condition a condition for when the rule is valid
     */
    StrategyRule(final String condition) {
        this.condition = condition;
    }

    /**
     * Gets if the conditions is satisfied given some valuations.
     * @param values valuations of variables. These must include (but not necessarily limited to) all variables used in the condition
     * @return true iff the condition is satisfied
     */
    public boolean isSatisfied(final Map<String, Double> values) {
        return ExpressionHelper.evaluateBooleanExpressionFromDoubles(condition, values);
    }
}
