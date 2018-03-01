package ecdar.mutation.operators;

import ecdar.abstractions.Component;
import ecdar.abstractions.Edge;
import ecdar.abstractions.EdgeStatus;
import ecdar.abstractions.Location;
import ecdar.mutation.MutationTestingException;
import ecdar.mutation.operators.ChangeGuardOperator;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collection;

public class ChangeGuardOperatorTest {

    @Test
    public void testComputeGuard() throws MutationTestingException {
        final Component component = new Component();

        component.addLocation(new Location());

        final Edge edge = new Edge(component.getLocations().get(0), EdgeStatus.INPUT);
        edge.setTargetLocation(component.getLocations().get(0));
        edge.setGuard("20<x");
        component.addEdge(edge);

        final Collection<? extends Component> mutants = new ChangeGuardOperator().generateMutants(component);

        Assert.assertEquals(4, mutants.size());
        Assert.assertTrue(mutants.stream().anyMatch(mutant -> mutant.getEdges().size() == 1 && mutant.getEdges().get(0).getGuard().equals("20>=x")));

        // The original guard should not be present among the mutants
        Assert.assertTrue(mutants.stream().noneMatch(mutant -> mutant.getEdges().stream().anyMatch(e -> e.getGuard().equals("20<x"))));
    }

    @Test
    public void testComputeNoGuard() throws MutationTestingException {
        final Component component = new Component();

        component.addLocation(new Location());

        final Edge edge = new Edge(component.getLocations().get(0), EdgeStatus.INPUT);
        edge.setTargetLocation(component.getLocations().get(0));
        component.addEdge(edge);

        final Collection<? extends Component> mutants = new ChangeGuardOperator().generateMutants(component);

        Assert.assertEquals(0, mutants.size());
    }
    @Test
    public void testComputeGuardNotEqual() throws MutationTestingException {
        final Component component = new Component();

        component.addLocation(new Location());

        final Edge edge = new Edge(component.getLocations().get(0), EdgeStatus.INPUT);
        edge.setTargetLocation(component.getLocations().get(0));
        edge.setGuard("20 != x");
        component.addEdge(edge);

        final Collection<? extends Component> mutants = new ChangeGuardOperator().generateMutants(component);

        Assert.assertEquals(5, mutants.size());
        Assert.assertTrue(mutants.stream().anyMatch(mutant -> mutant.getEdges().size() == 1 && mutant.getEdges().get(0).getGuard().equals("20 == x")));

        // The original guard should not be present among the mutants
        Assert.assertTrue(mutants.stream().noneMatch(mutant -> mutant.getEdges().stream().anyMatch(e -> e.getGuard().equals("20 != x"))));
    }

    @Test
    public void testComputeConjunction() throws MutationTestingException {
        final Component component = new Component();

        component.addLocation(new Location());

        final Edge edge = new Edge(component.getLocations().get(0), EdgeStatus.INPUT);
        edge.setTargetLocation(component.getLocations().get(0));
        edge.setGuard("20 < x && y == 2");
        component.addEdge(edge);

        final Collection<? extends Component> mutants = new ChangeGuardOperator().generateMutants(component);

        Assert.assertEquals(8, mutants.size());
        Assert.assertTrue(mutants.stream().anyMatch(mutant -> mutant.getEdges().size() == 1 && mutant.getEdges().get(0).getGuard().equals("20 < x && y < 2")));

        // The original guard should not be present among the mutants
        Assert.assertTrue(mutants.stream().noneMatch(mutant -> mutant.getEdges().stream().anyMatch(e -> e.getGuard().equals("20 < x && y == 2"))));
    }
}