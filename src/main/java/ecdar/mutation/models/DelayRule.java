package ecdar.mutation.models;

/**
 * A delay rule in a strategy.
 */
public class DelayRule extends StrategyRule {
    /**
     * Constructs.
     * @param condition the conduction of a the rule
     */
    public DelayRule(final String condition) {
        super(condition);
    }
}
