package ecdar.mutation;

import ecdar.abstractions.Component;
import ecdar.abstractions.Location;
import ecdar.mutation.models.ActionRule;
import org.junit.Assert;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.*;

public class ComponentSimulationTest {

    @Test
    public void delay() {
        final Component c = new Component();
        c.setDeclarationsText("clock x;");

        final Location l1 = new Location();
        l1.setType(Location.Type.INITIAL);
        c.addLocation(l1);

        final ComponentSimulation s = new ComponentSimulation(c);

        Assert.assertEquals(1, s.getValuations().size());
        Assert.assertTrue(s.getValuations().containsKey("x"));
        Assert.assertTrue(s.getValuations().containsValue(0.0));

        s.delay(1.2);

        Assert.assertEquals(1, s.getValuations().size());
        Assert.assertTrue(s.getValuations().containsKey("x"));
        Assert.assertTrue(s.getValuations().containsValue(1.2));

        s.delay(0.3);

        Assert.assertEquals(1, s.getValuations().size());
        Assert.assertTrue(s.getValuations().containsKey("x"));
        Assert.assertTrue(s.getValuations().containsValue(1.5));
    }

    @Test
    public void resetClock() throws MutationTestingException {
        final Component c = new Component();
        c.setDeclarationsText("clock x;");

        final Location l1 = new Location();
        l1.setType(Location.Type.INITIAL);
        l1.idProperty().setValue("L0");
        c.addLocation(l1);

        final ComponentSimulation s = new ComponentSimulation(c);

        Assert.assertEquals(1, s.getValuations().size());
        Assert.assertTrue(s.getValuations().containsKey("x"));
        Assert.assertTrue(s.getValuations().containsValue(0.0));

        s.delay(1.2);

        Assert.assertEquals(1, s.getValuations().size());
        Assert.assertTrue(s.getValuations().containsKey("x"));
        Assert.assertTrue(s.getValuations().containsValue(1.2));

        final ActionRule r = new ActionRule("1==1", "A.a->B.L0 { 1, a?, x=0 }");
        s.runActionRule(r);

        Assert.assertEquals(1, s.getValuations().size());
        Assert.assertTrue(s.getValuations().containsKey("x"));
        Assert.assertTrue(s.getValuations().containsValue(0.0));
    }

    @Test
    public void switchCurrentLocation() throws MutationTestingException {
        final Component c = new Component();
        c.setDeclarationsText("clock x;");

        Location l = new Location();
        l.setType(Location.Type.INITIAL);
        l.idProperty().setValue("L0");
        c.addLocation(l);

        l = new Location();
        l.idProperty().setValue("L1");
        c.addLocation(l);

        final ComponentSimulation s = new ComponentSimulation(c);

        Assert.assertEquals(1, s.getValuations().size());
        Assert.assertEquals("L0", s.getCurrentLocation().getId());

        final ActionRule r = new ActionRule("1==1", "A.a->B.L1 { 1, a?, x=0 }");
        s.runActionRule(r);

        Assert.assertEquals(1, s.getValuations().size());
        Assert.assertEquals("L1", s.getCurrentLocation().getId());
    }
}