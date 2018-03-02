package ecdar.utility;

import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class ExpressionHelperTest {

    @Test
    public void parseUpdateLocal() {
        final Map<String, Integer> locals = new HashMap<>();
        locals.put("a", 2);

        final Map<String, Integer> result = ExpressionHelper.parseUpdate("a=a+1", locals);

        Assert.assertEquals(1, result.size());
        Assert.assertTrue(result.containsKey("a"));
        Assert.assertTrue(result.containsValue(3));
    }

    @Test
    public void parseUpdateClock() {
        final Map<String, Integer> result = ExpressionHelper.parseUpdate("x=0", new HashMap<>());

        Assert.assertEquals(1, result.size());
        Assert.assertTrue(result.containsKey("x"));
        Assert.assertTrue(result.containsValue(0));
    }

    @Test
    public void parseUpdateClocks() {
        final Map<String, Integer> result = ExpressionHelper.parseUpdate("x=0,\ny=0", new HashMap<>());

        Assert.assertEquals(2, result.size());
        Assert.assertTrue(result.containsKey("x"));
        Assert.assertTrue(result.containsKey("y"));
        Assert.assertTrue(result.values().stream().allMatch(v -> v == 0));
    }
}