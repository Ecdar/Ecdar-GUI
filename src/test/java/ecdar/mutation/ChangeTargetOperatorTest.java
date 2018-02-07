package ecdar.mutation;

import ecdar.Ecdar;
import ecdar.abstractions.Component;
import ecdar.abstractions.Edge;
import ecdar.abstractions.EdgeStatus;
import ecdar.abstractions.Location;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class ChangeTargetOperatorTest {

    @Before
    public void setup() {
        Ecdar.setUpForTest();
    }

    @Test
    public void testNumberOfMutants() {
        final Component component = new Component(false);
        Ecdar.getProject().getComponents().add(component);

        // 3 locations in addition to the already created initial location
        component.addLocation(new Location());
        component.addLocation(new Location());
        component.addLocation(new Location());

        // 5 edges
        Edge edge = new Edge(component.getLocations().get(0), EdgeStatus.INPUT);
        edge.setTargetLocation(component.getLocations().get(0));
        component.addEdge(edge);

        edge = new Edge(component.getLocations().get(0), EdgeStatus.INPUT);
        edge.setTargetLocation(component.getLocations().get(1));
        component.addEdge(edge);

        edge = new Edge(component.getLocations().get(2), EdgeStatus.INPUT);
        edge.setTargetLocation(component.getLocations().get(1));
        component.addEdge(edge);

        edge = new Edge(component.getLocations().get(2), EdgeStatus.INPUT);
        edge.setTargetLocation(component.getLocations().get(2));
        component.addEdge(edge);

        edge = new Edge(component.getLocations().get(2), EdgeStatus.INPUT);
        edge.setTargetLocation(component.getLocations().get(3));
        component.addEdge(edge);

        // 5 edges, 4 locations. Expect 5 * (4 - 1) = 15 mutants
        Assert.assertEquals(15, new ChangeTargetOperator(component).computeMutants().size());
    }
}