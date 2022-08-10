package ecdar.mutation.operators;

import ecdar.abstractions.Component;
import ecdar.abstractions.Edge;
import ecdar.abstractions.EdgeStatus;
import ecdar.abstractions.Location;
import ecdar.mutation.MutationTestingException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;

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
        Assertions.assertEquals(6, mutants.size());

        Assertions.assertEquals(1, mutants.stream().filter(m -> m.getDisplayableEdges().get(1).getUpdate().isEmpty()).count());
        Assertions.assertEquals(1, mutants.stream().filter(m -> m.getDisplayableEdges().get(1).getUpdate().equals("x = 0, y = 0")).count());

        Assertions.assertEquals(0, mutants.stream().filter(m -> m.getDisplayableEdges().get(2).getUpdate().isEmpty()).count());
    }
}