package ecdar.mutation.operators;

import ecdar.abstractions.Component;
import ecdar.abstractions.Edge;
import ecdar.abstractions.EdgeStatus;
import ecdar.abstractions.Location;
import ecdar.mutation.operators.SinkLocationOperator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;

public class SinkLocationOperatorTest {

    @Test
    public void testNumberOfMutants() {
        final Component component = new Component();

        component.addLocation(new Location());

        Edge edge = new Edge(component.getLocations().get(0), EdgeStatus.INPUT);
        edge.setTargetLocation(component.getLocations().get(0));
        edge.setSync("a");
        component.addEdge(edge);

        edge = new Edge(component.getLocations().get(0), EdgeStatus.INPUT);
        edge.setTargetLocation(component.getLocations().get(0));
        edge.setSync("b");
        component.addEdge(edge);

        edge = new Edge(component.getLocations().get(0), EdgeStatus.INPUT);
        edge.setTargetLocation(component.getLocations().get(0));
        edge.setSync("b");
        component.addEdge(edge);

        edge = new Edge(component.getLocations().get(0), EdgeStatus.OUTPUT);
        edge.setTargetLocation(component.getLocations().get(0));
        edge.setSync("c");
        component.addEdge(edge);

        edge = new Edge(component.getLocations().get(0), EdgeStatus.OUTPUT);
        edge.setTargetLocation(component.getLocations().get(0));
        edge.setSync("a");
        component.addEdge(edge);

        // 5 edge. Expect 5 mutants
        Assertions.assertEquals(5, new SinkLocationOperator().generateTestCases(component).size());
    }
}