package ecdar.mutation.operators;

import ecdar.Ecdar;
import ecdar.abstractions.Component;
import ecdar.abstractions.Edge;
import ecdar.abstractions.EdgeStatus;
import ecdar.abstractions.Location;
import ecdar.utility.colors.EnabledColor;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;

public class ChangeTargetOperatorTest {

    @BeforeAll
    static void setup() {
        Ecdar.setUpForTest();
    }

    @Test
    public void testNumberOfMutants() {
        final Component component = new Component(EnabledColor.getDefault(), "test_comp");
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
        Assertions.assertEquals(15, new ChangeTargetOperator().generateTestCases(component).size());
    }
}