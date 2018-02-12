package ecdar.mutation;

import org.springframework.expression.spel.standard.SpelExpressionParser;

import java.util.Map;

public abstract class StrategyRule {
    private String condition;

    /**
     * Constructs a rule.
     * Examples of conditions:
     * true
     * (20<M.e)
     * (20<M.e && S.f==M.f && M.f==0)
     * @param condition
     */
    StrategyRule(final String condition) {
        this.condition = condition;
    }

    boolean isSatisfied(final Map<String, Double> values) {
        final String[] conditionCopy = {condition};
        values.forEach((key, value) -> conditionCopy[0] = conditionCopy[0].replace(key, String.valueOf(value)));

        return new SpelExpressionParser().parseExpression(conditionCopy[0]).getValue(Boolean.class);
    }
}
