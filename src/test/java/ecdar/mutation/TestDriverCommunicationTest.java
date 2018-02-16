package ecdar.mutation;

import ecdar.Ecdar;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TestDriverCommunicationTest {

    @Before
    public void setup() {
        Ecdar.setUpForTest();
    }

    @Test
    public void testSimpleCommunication() {
        TestDriver testDriver = new TestDriver();
        Assert.assertEquals("", "");
    }
}