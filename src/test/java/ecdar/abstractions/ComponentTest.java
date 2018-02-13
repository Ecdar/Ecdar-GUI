package ecdar.abstractions;

import com.bpodgursky.jbool_expressions.Expression;
import com.bpodgursky.jbool_expressions.Not;
import com.bpodgursky.jbool_expressions.Or;
import com.bpodgursky.jbool_expressions.Variable;
import com.bpodgursky.jbool_expressions.parsers.ExprParser;
import com.bpodgursky.jbool_expressions.rules.RuleSet;
import ecdar.Ecdar;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.reactfx.value.Var;

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
    public void fdgdfgd() {
        Expression<String> nonStandard = Not.of(Or.of(
                Variable.of("x<4")));
        System.out.println(nonStandard);

        Expression<String> posForm = RuleSet.toCNF(nonStandard);
        System.out.println(posForm.getExprType());
        System.out.println(posForm.toLexicographicString());
        System.out.println(posForm.toLexicographicString());
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

    @Test
    public void testAngelicCompletion() {
        final Component c = new Component();
        Ecdar.getProject().getComponents().add(c);

        // Has no outgoing edges
        final Location l1 = new Location();
        l1.initialize();
        c.addLocation(l1);

        // Has outgoing a input edge without guard
        final Location l2 = new Location();
        l2.initialize();
        c.addLocation(l2);

        // Has outgoing b input edge with guard x <= 3
        final Location l3 = new Location();
        l3.initialize();
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

        Assert.assertEquals(3, c.getLocations().size());
        Assert.assertEquals(3, c.getEdges().size());

        /*c.applyAngelicCompletion();

        Assert.assertEquals(3, c.getLocations().size());
        /*
        Assert.assertEquals(8, c.getEdges().size());

        // l1 should have two new input edges without guards
        Edge edge = c.getEdges().get(3);
        Assert.assertEquals(EdgeStatus.INPUT, edge.getStatus());
        Assert.assertEquals("a", edge.getSync());
        Assert.assertEquals("", edge.getGuard());
        Assert.assertEquals(l1, edge.getSourceLocation());
        Assert.assertEquals(l1, edge.getTargetLocation());

        edge = c.getEdges().get(4);
        Assert.assertEquals(EdgeStatus.INPUT, edge.getStatus());
        Assert.assertEquals("b", edge.getSync());
        Assert.assertEquals("", edge.getGuard());
        Assert.assertEquals(l1, edge.getSourceLocation());
        Assert.assertEquals(l1, edge.getTargetLocation());

        // l2 should have one new input edge without guard
        edge = c.getEdges().get(5);
        Assert.assertEquals(EdgeStatus.INPUT, edge.getStatus());
        Assert.assertEquals("b", edge.getSync());
        Assert.assertEquals("", edge.getGuard());
        Assert.assertEquals(l2, edge.getSourceLocation());
        Assert.assertEquals(l2, edge.getTargetLocation());

        // l3 should have two new input edges
        // one without guard
        edge = c.getEdges().get(6);
        Assert.assertEquals(EdgeStatus.INPUT, edge.getStatus());
        Assert.assertEquals("a", edge.getSync());
        Assert.assertEquals("", edge.getGuard());
        Assert.assertEquals(l3, edge.getSourceLocation());
        Assert.assertEquals(l3, edge.getTargetLocation());

        // and one with negated guard
        edge = c.getEdges().get(7);
        Assert.assertEquals(EdgeStatus.INPUT, edge.getStatus());
        Assert.assertEquals("b", edge.getSync());
        Assert.assertEquals("x>3", edge.getGuard());
        Assert.assertEquals(l3, edge.getSourceLocation());
        Assert.assertEquals(l3, edge.getTargetLocation());
*/ // TODO
    }
}