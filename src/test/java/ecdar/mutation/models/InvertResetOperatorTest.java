package ecdar.mutation.models;

import ecdar.abstractions.Component;
import ecdar.abstractions.Edge;
import ecdar.abstractions.EdgeStatus;
import ecdar.abstractions.Location;
import ecdar.mutation.MutationTestingException;
import ecdar.mutation.operators.InvertResetOperator;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collection;

public class InvertResetOperatorTest {

    @Test
    public void numberOfMutants() throws MutationTestingException {
        final Component component = new Component();
        component.setDeclarationsText("clock x, y;");

        component.addLocation(new Location());

        Edge edge = new Edge(component.getLocations().get(0), EdgeStatus.INPUT);
        edge.setTargetLocation(component.getLocations().get(0));
        component.addEdge(edge);

        edge = new Edge(component.getLocations().get(0), EdgeStatus.INPUT);
        edge.setTargetLocation(component.getLocations().get(0));
        edge.addUpdateNail("x = 0");
        component.addEdge(edge);

        edge = new Edge(component.getLocations().get(0), EdgeStatus.INPUT);
        edge.setTargetLocation(component.getLocations().get(0));
        edge.addUpdateNail("x = 0,y = 0");
        component.addEdge(edge);

        final Collection<? extends Component> mutants = new InvertResetOperator().generateMutants(component);

        // 2 clocks, 3 edges, we except 2 * 3 = 6 mutants
        Assert.assertEquals(6, mutants.size());

        Assert.assertEquals(1, mutants.stream().filter(m -> m.getEdges().get(1).getUpdate().isEmpty()).count());
        Assert.assertEquals(1, mutants.stream().filter(m -> m.getEdges().get(1).getUpdate().equals("x = 0, y = 0")).count());

        Assert.assertEquals(0, mutants.stream().filter(m -> m.getEdges().get(2).getUpdate().isEmpty()).count());
    }
}