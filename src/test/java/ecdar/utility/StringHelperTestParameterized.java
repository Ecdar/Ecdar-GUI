package ecdar.utility;

import ecdar.utility.helpers.StringHelper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertEquals;

@RunWith(Parameterized.class)
public class StringHelperTestParameterized {
    private String inputString;
    private String expectedResult;

    // Each parameter should be placed as an argument here
    // Every time runner triggers, it will pass the arguments
    // from parameters we defined in primeNumbers() method

    public StringHelperTestParameterized(String inputString, String expectedResult) {
        this.inputString = inputString;
        this.expectedResult = expectedResult;
    }

    @Parameterized.Parameters
    public static Collection input() {
        return Arrays.asList(new Object[][]{
                {"2 >= 4", "2 \u2265 4"},
                {"2 <= 4", "2 \u2264 4"},
                {"2 == 4", "2 == 4"},
                {"2 > 4", "2 \u2265 4"},
                {"2 < 4", "2 \u2264 4"}
        });
    }

    @Test
    public void testStringHelperConversionToUnicode() {
        System.out.println(inputString + " bliver sammenlignet med " + expectedResult);
        assertEquals(expectedResult,
                StringHelper.ConvertSymbolsToUnicode(inputString));
    }
}
