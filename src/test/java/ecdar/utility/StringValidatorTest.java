package ecdar.utility;

import ecdar.utility.helpers.StringValidator;
import org.junit.Assert;
import org.junit.Test;

public class StringValidatorTest {
    @Test
    public void validQueryReturnsTrue() {
        final boolean result = StringValidator.validateString("(A && B)", StringValidator.queryValidation);

        Assert.assertTrue(result);
    }

    @Test
    public void queryWithAdditionalClosingParenthesesReturnsFalse() {
        final boolean result = StringValidator.validateString("(A && B))", StringValidator.queryValidation);

        Assert.assertFalse(result);
    }

    @Test
    public void queryWithAdditionalOpeningParenthesesReturnsFalse() {
        final boolean result = StringValidator.validateString("((A && B)", StringValidator.queryValidation);

        Assert.assertFalse(result);
    }

    @Test
    public void validComponentNameReturnsTrue() {
        final boolean result = StringValidator.validateString("Administrator", StringValidator.componentNameValidation);

        Assert.assertTrue(result);
    }

    @Test
    public void componentNameWithPeriodReturnsFalse() {
        final boolean result = StringValidator.validateString("Administrator.", StringValidator.componentNameValidation);

        Assert.assertFalse(result);
    }
}