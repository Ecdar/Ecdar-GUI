package ecdar.mutation;

public class ActionRule extends StrategyRule {
    private String transition;

    public ActionRule(final String condition, final String transition) {
        super(condition);

        this.transition = transition;
    }
}
