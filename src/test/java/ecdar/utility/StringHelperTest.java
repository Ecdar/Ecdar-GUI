package ecdar.utility;

import ecdar.utility.helpers.StringHelper;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class StringHelperTest {
    @ParameterizedTest
    @ValueSource(strings = {
            "2 >= 4;2 \u2265 4",
            "2 <= 4;2 \u2264 4",
            "2 == 4;2 == 4",
            "2 > 4;2 > 4",
            "2 < 4;2 < 4",
    })
    void convertSymbolsToUnicode(String inputAndExpectedOutput) {
        var split = inputAndExpectedOutput.split(";");
        var input = split[0];
        var expectedOutput = split[1];

        var actualOutput = StringHelper.ConvertSymbolsToUnicode(input);

        assertEquals(expectedOutput, actualOutput);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "2 \u2265 4;2 >= 4",
            "2 \u2264 4;2 <= 4",
            "2 == 4;2 == 4",
            "2 > 4;2 > 4",
            "2 < 4;2 < 4"
    })
    void convertUnicodeToSymbols(String inputAndExpectedOutput) {
        var split = inputAndExpectedOutput.split(";");
        var input = split[0];
        var expectedOutput = split[1];

        var actualOutput = StringHelper.ConvertUnicodeToSymbols(input);

        assertEquals(expectedOutput, actualOutput);
    }
}
