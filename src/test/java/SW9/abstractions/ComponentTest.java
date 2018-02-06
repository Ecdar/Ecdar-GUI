package SW9.abstractions;

import SW9.Ecdar;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ComponentTest {

    @Before
    public void setup() {
        Ecdar.setUpForTest();
    }

    @Test
    public void testCloneSameId() {
        final Component original = new Component(false);

        final Location loc1 = new Location();
        original.addLocation(loc1);
        final String id1 = loc1.getId();

        final Component clone = original.cloneForVerification();

        // Clone has a location with the same id
        Assert.assertNotNull(clone.findLocation(id1));
    }

    @Test
    public void testCloneChangeTargetOfOriginal() {
        final Component original = new Component(false);
        Ecdar.getProject().getComponents().add(original);

        final Location loc1 = new Location();
        loc1.initialize();
        original.addLocation(loc1);
        final String id1 = loc1.getId();

        final Location loc2 = new Location();
        loc2.initialize();
        original.addLocation(loc2);
        final String id2 = loc2.getId();

        final Edge edge1 = new Edge(loc1, EdgeStatus.INPUT);
        edge1.setTargetLocation(loc1);
        original.addEdge(edge1);

        final Component clone = original.cloneForVerification();

        // The two ids should be different
        Assert.assertNotEquals(id1, id2);

        Assert.assertEquals(id1, original.getEdges().get(0).getTargetLocation().getId());
        Assert.assertEquals(id1, clone.getEdges().get(0).getTargetLocation().getId());

        // Make original change target loc
        edge1.setTargetLocation(loc2);

        // Only original should change
        Assert.assertEquals(id2, original.getEdges().get(0).getTargetLocation().getId());
        Assert.assertEquals(id1, clone.getEdges().get(0).getTargetLocation().getId());
    }

    @Test
    public void testCloneChangeTargetOfClone() {
        final Component original = new Component(false);
        Ecdar.getProject().getComponents().add(original);

        final Location loc1 = new Location();
        loc1.initialize();
        original.addLocation(loc1);
        final String id1 = loc1.getId();

        final Location loc2 = new Location();
        loc2.initialize();
        original.addLocation(loc2);
        final String id2 = loc2.getId();

        final Edge edge1 = new Edge(loc1, EdgeStatus.INPUT);
        edge1.setTargetLocation(loc1);
        original.addEdge(edge1);

        final Component clone = original.cloneForVerification();

        // The two ids should be different
        Assert.assertNotEquals(id1, id2);

        Assert.assertEquals(id1, original.getEdges().get(0).getTargetLocation().getId());
        Assert.assertEquals(id1, clone.getEdges().get(0).getTargetLocation().getId());

        // Make clone change target loc
        clone.getEdges().get(0).setTargetLocation(loc2);

        // Only original should change
        Assert.assertEquals(id1, original.getEdges().get(0).getTargetLocation().getId());
        Assert.assertEquals(id2, clone.getEdges().get(0).getTargetLocation().getId());
    }
}