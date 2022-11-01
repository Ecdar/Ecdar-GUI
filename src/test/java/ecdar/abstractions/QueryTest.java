package ecdar.abstractions;

import ecdar.Ecdar;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;

public class QueryTest {
    @BeforeAll
    static void setup() {
        Ecdar.setUpForTest();
    }

    @Test
    public void testGetQuery() {
        //Test that the query string from the query textfield is decoded correctly for the backend to use it
        final Query query = new Query("(Administration || Machine || Researcher) \u2264 Spec)", "comment", QueryState.RUNNING);

        String expected = "(Administration || Machine || Researcher) <= Spec)";
        String result = query.getQuery();

        Assertions.assertEquals(expected, result);
    }
}
