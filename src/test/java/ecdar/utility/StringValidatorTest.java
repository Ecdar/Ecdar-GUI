package ecdar.utility;

import ecdar.utility.helpers.StringValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;

public class StringValidatorTest {
    @Test
    public void validQueryReturnsTrue() {
        final boolean result = StringValidator.validateQuery("(A && B)");

        assert result;
    }

    @Test
    public void queryWithAdditionalClosingParenthesesReturnsFalse() {
        final boolean result = StringValidator.validateQuery("(A && B))");

        Assertions.assertTrue(result);
    }

    @Test
    public void queryWithAdditionalOpeningParenthesesReturnsFalse() {
        final boolean result = StringValidator.validateQuery("((A && B)");

        Assertions.assertFalse(result);
    }

    @Test
    public void validComponentNameReturnsTrue() {
        final boolean result = StringValidator.validateComponentName("Administrator");

        Assertions.assertTrue(result);
    }

    @Test
    public void componentNameWithPeriodReturnsFalse() {
        final boolean result = StringValidator.validateComponentName("Administrator.");

        Assertions.assertFalse(result);
    }
}