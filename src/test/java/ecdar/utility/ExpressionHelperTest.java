package ecdar.utility;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;

import java.util.HashMap;
import java.util.Map;

public class ExpressionHelperTest {

    @Test
    public void parseUpdateLocal() {
        final Map<String, Integer> locals = new HashMap<>();
        locals.put("a", 2);

        final Map<String, Integer> result = ExpressionHelper.parseUpdate("a=a+1", locals);

        Assertions.assertEquals(1, result.size());
        Assertions.assertTrue(result.containsKey("a"));
        Assertions.assertTrue(result.containsValue(3));
    }

    @Test
    public void parseUpdateClock() {
        final Map<String, Integer> result = ExpressionHelper.parseUpdate("x=0", new HashMap<>());

        Assertions.assertEquals(1, result.size());
        Assertions.assertTrue(result.containsKey("x"));
        Assertions.assertTrue(result.containsValue(0));
    }

    @Test
    public void parseUpdateClocks() {
        final Map<String, Integer> result = ExpressionHelper.parseUpdate("x=0,\ny=0", new HashMap<>());

        Assertions.assertEquals(2, result.size());
        Assertions.assertTrue(result.containsKey("x"));
        Assertions.assertTrue(result.containsKey("y"));
        Assertions.assertTrue(result.values().stream().allMatch(v -> v == 0));
    }
}