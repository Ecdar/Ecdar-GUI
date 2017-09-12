package SW9.abstractions;

import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 */
public class LocationTest {
    @Test
    public void getNickname() throws Exception {
        final Location location = new Location();
        location.setNickname("test");
        Assert.assertEquals("test", location.getNickname());
    }
}