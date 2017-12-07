package SW9.abstractions;

import SW9.Ecdar;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 */
public class LocationTest {
    @Test
    public void getId() {
        Location location = new Location("test");
        Assert.assertEquals("test", location.getId());
    }
}