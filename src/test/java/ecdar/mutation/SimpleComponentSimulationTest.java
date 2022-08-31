package ecdar.mutation;

import ecdar.abstractions.Component;
import ecdar.abstractions.Edge;
import ecdar.abstractions.EdgeStatus;
import ecdar.abstractions.Location;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;

public class SimpleComponentSimulationTest {

    @Test
    public void delay() {
        final Component c = new Component();
        c.setDeclarationsText("clock x;");

        final Location l1 = new Location();
        l1.setType(Location.Type.INITIAL);
        c.addLocation(l1);

        final SimpleComponentSimulation s = new SimpleComponentSimulation(c);

        Assertions.assertEquals(1, s.getClockValuations().size());
        Assertions.assertTrue(s.getClockValuations().containsKey("x"));
        Assertions.assertTrue(s.getClockValuations().containsValue(0.0));

        boolean result = s.delay(1.2);

        Assertions.assertEquals(true, result);
        Assertions.assertEquals(1, s.getClockValuations().size());
        Assertions.assertTrue(s.getClockValuations().containsKey("x"));
        Assertions.assertTrue(s.getClockValuations().containsValue(1.2));

        result = s.delay(0.3);

        Assertions.assertEquals(true, result);
        Assertions.assertEquals(1, s.getClockValuations().size());
        Assertions.assertTrue(s.getClockValuations().containsKey("x"));
        Assertions.assertTrue(s.getClockValuations().containsValue(1.5));
    }

    @Test
    public void resetClock() throws MutationTestingException {
        final Component c = new Component();
        c.setDeclarationsText("clock x;");

        final Location l1 = new Location();
        l1.setType(Location.Type.INITIAL);
        l1.idProperty().setValue("L0");
        c.addLocation(l1);

        final Edge e = new Edge(l1, EdgeStatus.INPUT);
        e.setUpdate("x=0");
        e.setSync("a");
        e.setTargetLocation(l1);
        c.addEdge(e);

        final SimpleComponentSimulation s = new SimpleComponentSimulation(c);

        Assertions.assertEquals(1, s.getClockValuations().size());
        Assertions.assertTrue(s.getClockValuations().containsKey("x"));
        Assertions.assertTrue(s.getClockValuations().containsValue(0.0));

        s.delay(1.2);

        Assertions.assertEquals(1, s.getClockValuations().size());
        Assertions.assertTrue(s.getClockValuations().containsKey("x"));
        Assertions.assertTrue(s.getClockValuations().containsValue(1.2));

        s.runInputAction("a");

        Assertions.assertEquals(1, s.getClockValuations().size());
        Assertions.assertTrue(s.getClockValuations().containsKey("x"));
        Assertions.assertTrue(s.getClockValuations().containsValue(0.0));
    }

    @Test
    public void switchCurrentLocation() throws MutationTestingException {
        final Component c = new Component();
        c.setDeclarationsText("clock x;");

        final Location l1 = new Location();
        l1.setType(Location.Type.INITIAL);
        l1.idProperty().setValue("L0");
        c.addLocation(l1);

        final Location l2 = new Location();
        l2.idProperty().setValue("L1");
        c.addLocation(l2);

        final Edge e = new Edge(l1, EdgeStatus.INPUT);
        e.setSync("a");
        e.setTargetLocation(l2);
        c.addEdge(e);

        final SimpleComponentSimulation s = new SimpleComponentSimulation(c);

        Assertions.assertEquals(1, s.getClockValuations().size());
        Assertions.assertEquals("L0", s.getCurrentLocation().getId());

        s.runInputAction("a");

        Assertions.assertEquals(1, s.getClockValuations().size());
        Assertions.assertEquals("L1", s.getCurrentLocation().getId());
    }
}