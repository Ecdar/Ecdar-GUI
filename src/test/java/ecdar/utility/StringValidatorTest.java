package ecdar.utility;

import ecdar.utility.helpers.StringValidator;
import org.junit.Assert;
import org.junit.Test;

public class StringValidatorTest {
    @Test
    public void validQueryReturnsTrue() {
        final boolean result = StringValidator.validateQuery("(A && B)");

        Assert.assertTrue(result);
    }

    @Test
    public void queryWithAdditionalClosingParenthesesReturnsFalse() {
        final boolean result = StringValidator.validateQuery("(A && B))");

        Assert.assertFalse(result);
    }

    @Test
    public void queryWithAdditionalOpeningParenthesesReturnsFalse() {
        final boolean result = StringValidator.validateQuery("((A && B)");

        Assert.assertFalse(result);
    }

    @Test
    public void validComponentNameReturnsTrue() {
        final boolean result = StringValidator.validateComponentName("Administrator");

        Assert.assertTrue(result);
    }

    @Test
    public void componentNameWithPeriodReturnsFalse() {
        final boolean result = StringValidator.validateComponentName("Administrator.");

        Assert.assertFalse(result);
    }
}