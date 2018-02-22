package ecdar.mutation;

import ecdar.Ecdar;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class TestDriverCommunicationTest {

    @Before
    public void setup() {
        Ecdar.setUpForTest();
    }

    @Test
    @Ignore
    public void testSimpleCommunication() {
        Assert.assertEquals("", "");
    }
}