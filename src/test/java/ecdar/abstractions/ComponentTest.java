package ecdar.abstractions;

import ecdar.Ecdar;
import ecdar.mutation.ComponentVerificationTransformer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;

import java.util.List;

import static ecdar.abstractions.Project.LOCATION;

public class ComponentTest {

    private int counter = 0;

    @BeforeAll
    static void setup() {
        Ecdar.setUpForTest();
    }

    @Test
    public void testCloneSameId() {
        final Component original = new Component(false, "test_comp");

        final Location loc1 = new Location();
        original.addLocation(loc1);
        final String id1 = loc1.getId();

        final Component clone = ComponentVerificationTransformer.cloneForVerification(original);

        // Clone has a location with the same id
        Assertions.assertNotNull(clone.findLocation(id1));
    }

    @Test
    public void testCloneChangeTargetOfOriginal() {
        final Component original = new Component(false, "test_comp");
        Ecdar.getProject().getComponents().add(original);

        final Location loc1 = new Location();
        loc1.initialize(getUniqueLocationId());
        original.addLocation(loc1);
        final String id1 = loc1.getId();

        final Location loc2 = new Location();
        loc2.initialize(getUniqueLocationId());
        original.addLocation(loc2);
        final String id2 = loc2.getId();

        final Edge edge1 = new Edge(loc1, EdgeStatus.INPUT);
        edge1.setTargetLocation(loc1);
        original.addEdge(edge1);

        final Component clone = ComponentVerificationTransformer.cloneForVerification(original);

        // The two ids should be different
        Assertions.assertNotEquals(id1, id2);

        Assertions.assertEquals(id1, original.getEdges().get(0).getTargetLocation().getId());
        Assertions.assertEquals(id1, clone.getEdges().get(0).getTargetLocation().getId());

        // Make original change target loc
        edge1.setTargetLocation(loc2);

        // Only original should change
        Assertions.assertEquals(id2, original.getEdges().get(0).getTargetLocation().getId());
        Assertions.assertEquals(id1, clone.getEdges().get(0).getTargetLocation().getId());
    }

    @Test
    public void testCloneChangeTargetOfClone() {
        final Component original = new Component(false, "test_comp");
        Ecdar.getProject().getComponents().add(original);

        final Location loc1 = new Location();
        loc1.initialize(getUniqueLocationId());
        original.addLocation(loc1);
        final String id1 = loc1.getId();

        final Location loc2 = new Location();
        loc2.initialize(getUniqueLocationId());
        original.addLocation(loc2);
        final String id2 = loc2.getId();

        final Edge edge1 = new Edge(loc1, EdgeStatus.INPUT);
        edge1.setTargetLocation(loc1);
        original.addEdge(edge1);

        final Component clone = ComponentVerificationTransformer.cloneForVerification(original);

        // The two ids should be different
        Assertions.assertNotEquals(id1, id2);

        Assertions.assertEquals(id1, original.getEdges().get(0).getTargetLocation().getId());
        Assertions.assertEquals(id1, clone.getEdges().get(0).getTargetLocation().getId());

        // Make clone change target loc
        clone.getEdges().get(0).setTargetLocation(loc2);

        // Only original should change
        Assertions.assertEquals(id1, original.getEdges().get(0).getTargetLocation().getId());
        Assertions.assertEquals(id2, clone.getEdges().get(0).getTargetLocation().getId());
    }

    @Test
    public void testAngelicCompletion() {
        final Component c = new Component();
        Ecdar.getProject().getComponents().add(c);

        // Has no outgoing edges
        final Location l1 = new Location();
        l1.initialize(getUniqueLocationId());
        c.addLocation(l1);

        // Has outgoing a input edge without guard
        final Location l2 = new Location();
        l2.initialize(getUniqueLocationId());
        c.addLocation(l2);

        // Has outgoing b input edge with guard x <= 3
        final Location l3 = new Location();
        l3.initialize(getUniqueLocationId());
        c.addLocation(l3);

        final Edge e1 = new Edge(l2, EdgeStatus.INPUT);
        e1.setTargetLocation(l1);
        e1.setSync("a");
        c.addEdge(e1);

        final Edge e2 = new Edge(l3, EdgeStatus.INPUT);
        e2.setTargetLocation(l2);
        e2.setSync("b");
        e2.setGuard("x <= 3");
        c.addEdge(e2);

        // Outputs should not have effect
        final Edge e3 = new Edge(l3, EdgeStatus.OUTPUT);
        e3.setTargetLocation(l2);
        e3.setSync("c");
        e3.setGuard("x <= 2");
        c.addEdge(e3);

        c.updateIOList();

        Assertions.assertEquals(3, c.getLocations().size());
        Assertions.assertEquals(3, c.getEdges().size());

        ComponentVerificationTransformer.applyAngelicCompletionForComponent(c);

        Assertions.assertEquals(3, c.getLocations().size());

        Assertions.assertEquals(8, c.getEdges().size());

        // l1 should have two new input edges without guards
        Edge edge = c.getEdges().get(3);
        Assertions.assertEquals(EdgeStatus.INPUT, edge.getStatus());
        Assertions.assertEquals("a", edge.getSync());
        Assertions.assertEquals("", edge.getGuard());
        Assertions.assertEquals(l1, edge.getSourceLocation());
        Assertions.assertEquals(l1, edge.getTargetLocation());

        edge = c.getEdges().get(4);
        Assertions.assertEquals(EdgeStatus.INPUT, edge.getStatus());
        Assertions.assertEquals("b", edge.getSync());
        Assertions.assertEquals("", edge.getGuard());
        Assertions.assertEquals(l1, edge.getSourceLocation());
        Assertions.assertEquals(l1, edge.getTargetLocation());

        // l2 should have one new input edge without guard
        edge = c.getEdges().get(5);
        Assertions.assertEquals(EdgeStatus.INPUT, edge.getStatus());
        Assertions.assertEquals("b", edge.getSync());
        Assertions.assertEquals("", edge.getGuard());
        Assertions.assertEquals(l2, edge.getSourceLocation());
        Assertions.assertEquals(l2, edge.getTargetLocation());

        // l3 should have two new input edges
        // one without guard
        edge = c.getEdges().get(6);
        Assertions.assertEquals(EdgeStatus.INPUT, edge.getStatus());
        Assertions.assertEquals("a", edge.getSync());
        Assertions.assertEquals("", edge.getGuard());
        Assertions.assertEquals(l3, edge.getSourceLocation());
        Assertions.assertEquals(l3, edge.getTargetLocation());

        // and one with negated guard
        edge = c.getEdges().get(7);
        Assertions.assertEquals(EdgeStatus.INPUT, edge.getStatus());
        Assertions.assertEquals("b", edge.getSync());
        Assertions.assertEquals("x > 3", edge.getGuard());
        Assertions.assertEquals(l3, edge.getSourceLocation());
        Assertions.assertEquals(l3, edge.getTargetLocation());
    }

    @Test
    public void testAngelicCompletionConjunction() {
        final Component c = new Component();

        final Location l1 = new Location();
        l1.initialize(getUniqueLocationId());
        c.addLocation(l1);

        final Edge e1 = new Edge(l1, EdgeStatus.INPUT);
        e1.setTargetLocation(l1);
        e1.setSync("a");
        e1.setGuard("x <= 3 && x > 1");
        c.addEdge(e1);

        c.updateIOList();

        Assertions.assertEquals(1, c.getLocations().size());
        Assertions.assertEquals(1, c.getEdges().size());

        ComponentVerificationTransformer.applyAngelicCompletionForComponent(c);

        Assertions.assertEquals(1, c.getLocations().size());
        Assertions.assertEquals(3, c.getEdges().size());

        Edge edge = c.getEdges().get(1);
        Assertions.assertEquals(EdgeStatus.INPUT, edge.getStatus());
        Assertions.assertEquals("a", edge.getSync());
        Assertions.assertEquals("x <= 1", edge.getGuard());
        Assertions.assertEquals(l1, edge.getSourceLocation());
        Assertions.assertEquals(l1, edge.getTargetLocation());

        edge = c.getEdges().get(2);
        Assertions.assertEquals(EdgeStatus.INPUT, edge.getStatus());
        Assertions.assertEquals("a", edge.getSync());
        Assertions.assertEquals("x > 3", edge.getGuard());
        Assertions.assertEquals(l1, edge.getSourceLocation());
        Assertions.assertEquals(l1, edge.getTargetLocation());
    }

    @Test
    public void testAngelicCompletionDisjunction() {
        final Component c = new Component();

        final Location l1 = new Location();
        l1.initialize(getUniqueLocationId());
        c.addLocation(l1);

        final Edge e1 = new Edge(l1, EdgeStatus.INPUT);
        e1.setTargetLocation(l1);
        e1.setSync("a");
        e1.setGuard("x > 3");
        c.addEdge(e1);

        final Edge e2 = new Edge(l1, EdgeStatus.INPUT);
        e2.setTargetLocation(l1);
        e2.setSync("a");
        e2.setGuard("x <= 1");
        c.addEdge(e2);

        c.updateIOList();

        Assertions.assertEquals(1, c.getLocations().size());
        Assertions.assertEquals(2, c.getEdges().size());

        ComponentVerificationTransformer.applyAngelicCompletionForComponent(c);

        Assertions.assertEquals(1, c.getLocations().size());
        Assertions.assertEquals(3, c.getEdges().size());

        final Edge edge = c.getEdges().get(2);
        Assertions.assertEquals(EdgeStatus.INPUT, edge.getStatus());
        Assertions.assertEquals("a", edge.getSync());
        Assertions.assertEquals("x <= 3&&x > 1", edge.getGuard());
        Assertions.assertEquals(l1, edge.getSourceLocation());
        Assertions.assertEquals(l1, edge.getTargetLocation());
    }

    @Test
    public void testAngelicCompletionMathInGuard() {
        final Component c = new Component();

        final Location l1 = new Location();
        l1.initialize(getUniqueLocationId());
        c.addLocation(l1);

        final Edge e1 = new Edge(l1, EdgeStatus.INPUT);
        e1.setTargetLocation(l1);
        e1.setSync("a");
        e1.setGuard("x - y > 3 + n % 5");
        c.addEdge(e1);

        c.updateIOList();

        Assertions.assertEquals(1, c.getLocations().size());
        Assertions.assertEquals(1, c.getEdges().size());

        ComponentVerificationTransformer.applyAngelicCompletionForComponent(c);

        Assertions.assertEquals(1, c.getLocations().size());
        Assertions.assertEquals(2, c.getEdges().size());

        final Edge edge = c.getEdges().get(1);
        Assertions.assertEquals(EdgeStatus.INPUT, edge.getStatus());
        Assertions.assertEquals("a", edge.getSync());
        Assertions.assertEquals("x - y <= 3 + n % 5", edge.getGuard());
        Assertions.assertEquals(l1, edge.getSourceLocation());
        Assertions.assertEquals(l1, edge.getTargetLocation());
    }

    @Test
    public void testGetClock() {
        final Component c = new Component();
        c.setDeclarationsText("clock a;");

        final List<String> clocks = c.getClocks();

        Assertions.assertEquals(1, clocks.size());
        Assertions.assertEquals("a", clocks.get(0));
    }

    @Test
    public void testGet2Clocks() {
        final Component c = new Component();
        c.setDeclarationsText("clock a, b;");

        final List<String> clocks = c.getClocks();

        Assertions.assertEquals(2, clocks.size());
        Assertions.assertEquals("a", clocks.get(0));
        Assertions.assertEquals("b", clocks.get(1));
    }

    @Test
    public void testGetClocksEmpty() {
        final Component c = new Component();
        c.setDeclarationsText("");

        final List<String> clocks = c.getClocks();

        Assertions.assertEquals(0, clocks.size());
    }

    @Test
    public void testGetClocksNoClock() {
        final Component c = new Component();
        c.setDeclarationsText("int i = 0;\nint n = 2;");

        final List<String> clocks = c.getClocks();

        Assertions.assertEquals(0, clocks.size());
    }

    @Test
    public void testGetClocksWithNoise() {
        final Component c = new Component();
        c.setDeclarationsText("clock x;\n" +
                "\n" +
                "sound_t sound;");

        final List<String> clocks = c.getClocks();

        Assertions.assertEquals(1, clocks.size());
        Assertions.assertEquals("x", clocks.get(0));
    }

    @Test
    public void getLocalVariablesBool() {
        final Component c = new Component();
        c.setDeclarationsText("clock x;\n\nbool sound;");

        final List<String> vars = c.getLocalVariables();

        Assertions.assertEquals(1, vars.size());
        Assertions.assertEquals("sound", vars.get(0));
    }

    @Test
    public void getLocalVariablesCustomType() {
        final Component c = new Component();
        c.setDeclarationsText("// Place local declarations here.\n" +
                "clock x;\n" +
                "id_t cur;");

        final List<String> vars = c.getLocalVariables();

        Assertions.assertEquals(1, vars.size());
        Assertions.assertEquals("cur", vars.get(0));
    }

    @Test
    public void getLocalVariablesBoolAssignment() {
        final Component c = new Component();
        c.setDeclarationsText("clock x;\n\nbool sound = 1;");

        final List<String> vars = c.getLocalVariables();

        Assertions.assertEquals(1, vars.size());
        Assertions.assertEquals("sound", vars.get(0));
    }

    private String getUniqueLocationId() {
        counter++;
        return LOCATION + counter;
    }
}