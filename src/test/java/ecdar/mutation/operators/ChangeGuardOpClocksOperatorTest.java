package ecdar.mutation.operators;

import ecdar.abstractions.Component;
import ecdar.abstractions.Edge;
import ecdar.abstractions.EdgeStatus;
import ecdar.abstractions.Location;
import ecdar.mutation.MutationTestingException;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collection;

public class ChangeGuardOpClocksOperatorTest {

    @Test
    public void generateMutants1() throws MutationTestingException {
        final Component component = new Component();
        component.setDeclarationsText("clock x;");

        component.addLocation(new Location());

        final Edge edge = new Edge(component.getLocations().get(0), EdgeStatus.INPUT);
        edge.setTargetLocation(component.getLocations().get(0));
        edge.setGuard("20<=x");
        component.addEdge(edge);

        final Collection<? extends Component> mutants = new ChangeGuardOpClocksOperator().generateMutants(component);

        Assert.assertEquals(1, mutants.size());
        Assert.assertTrue(mutants.stream().anyMatch(mutant -> mutant.getEdges().size() == 1 && mutant.getEdges().get(0).getGuard().equals("20>x")));

        // The original guard should not be present among the mutants
        Assert.assertTrue(mutants.stream().noneMatch(mutant -> mutant.getEdges().stream().anyMatch(e -> e.getGuard().equals("20<=x"))));
    }

    @Test
    public void generateMutants2() throws MutationTestingException {
        final Component component = new Component();
        component.setDeclarationsText("clock x;");

        component.addLocation(new Location());

        final Edge edge = new Edge(component.getLocations().get(0), EdgeStatus.INPUT);
        edge.setTargetLocation(component.getLocations().get(0));
        edge.setGuard("20<x");
        component.addEdge(edge);

        final Collection<? extends Component> mutants = new ChangeGuardOpClocksOperator().generateMutants(component);

        Assert.assertEquals(2, mutants.size());
        Assert.assertTrue(mutants.stream().anyMatch(mutant -> mutant.getEdges().size() == 1 && mutant.getEdges().get(0).getGuard().equals("20<=x")));
        Assert.assertTrue(mutants.stream().anyMatch(mutant -> mutant.getEdges().size() == 1 && mutant.getEdges().get(0).getGuard().equals("20>x")));

        // The original guard should not be present among the mutants
        Assert.assertTrue(mutants.stream().noneMatch(mutant -> mutant.getEdges().stream().anyMatch(e -> e.getGuard().equals("20<x"))));
    }

    @Test
    public void testComputeNoGuard() throws MutationTestingException {
        final Component component = new Component();
        component.setDeclarationsText("clock x;");

        component.addLocation(new Location());

        final Edge edge = new Edge(component.getLocations().get(0), EdgeStatus.INPUT);
        edge.setTargetLocation(component.getLocations().get(0));
        component.addEdge(edge);

        final Collection<? extends Component> mutants = new ChangeGuardOpClocksOperator().generateMutants(component);

        Assert.assertEquals(0, mutants.size());
    }
    @Test
    public void testComputeGuardNotEqual() throws MutationTestingException {
        final Component component = new Component();
        component.setDeclarationsText("clock x;");

        component.addLocation(new Location());

        final Edge edge = new Edge(component.getLocations().get(0), EdgeStatus.INPUT);
        edge.setTargetLocation(component.getLocations().get(0));
        edge.setGuard("20 != x");
        component.addEdge(edge);

        final Collection<? extends Component> mutants = new ChangeGuardOpClocksOperator().generateMutants(component);

        Assert.assertEquals(2, mutants.size());
        Assert.assertTrue(mutants.stream().anyMatch(mutant -> mutant.getEdges().size() == 1 && mutant.getEdges().get(0).getGuard().equals("20 <= x")));
        Assert.assertTrue(mutants.stream().anyMatch(mutant -> mutant.getEdges().size() == 1 && mutant.getEdges().get(0).getGuard().equals("20 > x")));

        // The original guard should not be present among the mutants
        Assert.assertTrue(mutants.stream().noneMatch(mutant -> mutant.getEdges().stream().anyMatch(e -> e.getGuard().equals("20 != x"))));
    }

    @Test
    public void testComputeConjunction() throws MutationTestingException {
        final Component component = new Component();
        component.setDeclarationsText("clock x, y;");

        component.addLocation(new Location());

        final Edge edge = new Edge(component.getLocations().get(0), EdgeStatus.INPUT);
        edge.setTargetLocation(component.getLocations().get(0));
        edge.setGuard("20 <= x && y == 2");
        component.addEdge(edge);

        final Collection<? extends Component> mutants = new ChangeGuardOpClocksOperator().generateMutants(component);

        Assert.assertEquals(3, mutants.size());
        Assert.assertTrue(mutants.stream().anyMatch(mutant -> mutant.getEdges().size() == 1 && mutant.getEdges().get(0).getGuard().equals("20 > x && y == 2")));
        Assert.assertTrue(mutants.stream().anyMatch(mutant -> mutant.getEdges().size() == 1 && mutant.getEdges().get(0).getGuard().equals("20 <= x && y > 2")));

        // The original guard should not be present among the mutants
        Assert.assertTrue(mutants.stream().noneMatch(mutant -> mutant.getEdges().stream().anyMatch(e -> e.getGuard().equals("20 <= x && y == 2"))));
    }

    /**
     * Try while using a variable that is not a clock (e.g. a local variable).
     * It should not generate mutants.
     * @throws MutationTestingException if an error occurs
     */
    @Test
    public void generateNoClocks() throws MutationTestingException {
        final Component component = new Component();

        component.addLocation(new Location());

        final Edge edge = new Edge(component.getLocations().get(0), EdgeStatus.INPUT);
        edge.setTargetLocation(component.getLocations().get(0));
        edge.setGuard("20 <= x");
        component.addEdge(edge);

        final Collection<? extends Component> mutants = new ChangeGuardOpClocksOperator().generateMutants(component);

        Assert.assertEquals(0, mutants.size());
    }

    /**
     * Here one simple guard is with a clock, and one is without a clock.
     * Only the first should be mutated.
     * It should not generate mutants.
     * @throws MutationTestingException if an error occurs
     */
    @Test
    public void generateClockAndNoClocks() throws MutationTestingException {
        final Component component = new Component();
        component.setDeclarationsText("clock x;");

        component.addLocation(new Location());

        final Edge edge = new Edge(component.getLocations().get(0), EdgeStatus.INPUT);
        edge.setTargetLocation(component.getLocations().get(0));
        edge.setGuard("20 <= x && a > 0");
        component.addEdge(edge);

        final Collection<? extends Component> mutants = new ChangeGuardOpClocksOperator().generateMutants(component);

        Assert.assertEquals(1, mutants.size());
        Assert.assertTrue(mutants.stream().anyMatch(mutant -> mutant.getEdges().size() == 1 && mutant.getEdges().get(0).getGuard().equals("20 > x && a > 0")));
    }
}