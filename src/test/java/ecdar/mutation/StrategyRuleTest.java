package ecdar.mutation;

import ecdar.mutation.models.DelayRule;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class StrategyRuleTest {

    @Test
    public void testIsSatisfied1() {
        Assert.assertTrue(new DelayRule("true").isSatisfied(new HashMap<>()));
    }

    @Test
    public void testIsSatisfied2() {
        final Map<String, Double> values = new HashMap<>();
        values.put("M.e", 20.231);
        Assert.assertTrue(new DelayRule("(20<M.e)").isSatisfied(values));
    }

    @Test
    public void testIsSatisfied3() {
        final Map<String, Double> values = new HashMap<>();
        values.put("M.e", 10.231);
        Assert.assertFalse(new DelayRule("(20<M.e)").isSatisfied(values));
    }

    @Test
    public void testIsSatisfied4() {
        final Map<String, Double> values = new HashMap<>();
        values.put("M.e", 20.231);
        values.put("S.f", 0.0);
        values.put("M.f", 0.0);
        Assert.assertTrue(new DelayRule("(20<M.e && S.f==M.f && M.f==0)").isSatisfied(values));
    }

    @Test
    public void testIsSatisfied5() {
        final Map<String, Double> values = new HashMap<>();
        values.put("M.e", 20.231);
        values.put("S.f", 0.2);
        values.put("M.f", 0.0);
        Assert.assertFalse(new DelayRule("(20<M.e && S.f==M.f && M.f==0)").isSatisfied(values));
    }
}