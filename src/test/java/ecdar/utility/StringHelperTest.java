package ecdar.utility;

import ecdar.abstractions.Query;
import ecdar.abstractions.QueryState;
import ecdar.utility.helpers.StringHelper;
import org.junit.Before;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.util.StringUtils;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class StringHelperTest {
    @Test
    public void ConvertSymbolsToUnicodeTest() {
        String stringToReplace = "2 >= 4";
        String result = StringHelper.ConvertSymbolsToUnicode(stringToReplace);
        String expected = "2 \u2265 4";

        assertEquals(expected, result);
    }
}

