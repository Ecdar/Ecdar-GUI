package ecdar.simulation;

import ecdar.Ecdar;
import ecdar.abstractions.Component;
import ecdar.abstractions.Location;
import ecdar.backend.BackendHelper;
import ecdar.controllers.SimulationInitializationDialogController;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

public class ReachabilityTest {

    @BeforeAll
    static void setup() {
        Ecdar.setUpForTest();
    }

    @Test
    void reachabilityQuerySyntaxTestSuccess() {
        var regex = "([a-zA-Z]\\w*)([|][|][a-zA-Z]\\w*)*\\s+\\->\\s+\\[(\\w*)(,(\\w)*)*\\]\\([a-zA-Z0-9_<>=]*\\)(;\\[(\\w*)(,(\\w)*)\\]\\([a-zA-Z0-9_<>=]*\\))*";

        var location = new Location();
        location.setId("L1");
        var component = new Component();
        component.setName("C1");

        SimulationInitializationDialogController.ListOfComponents.clear();
        SimulationInitializationDialogController.ListOfComponents.add("C1");
        SimulationInitializationDialogController.ListOfComponents.add("C2");
        SimulationInitializationDialogController.ListOfComponents.add("C3");

        var result = BackendHelper.getLocationReachableQuery(location, component);
        assertTrue(result.matches(regex));
    }

    @Test
    void reachabilityQueryLocationPosition1TestSuccess() {
        var location = new Location();
        location.setId("L1");
        var component = new Component();
        component.setName("C1");

        SimulationInitializationDialogController.ListOfComponents.clear();
        SimulationInitializationDialogController.ListOfComponents.add("C1");
        SimulationInitializationDialogController.ListOfComponents.add("C2");
        SimulationInitializationDialogController.ListOfComponents.add("C3");

        var result = BackendHelper.getLocationReachableQuery(location, component);
        var indexOfLocation = result.indexOf('[') + 1;
        var output = result.charAt(indexOfLocation);
        assertEquals(output, location.getId().charAt(0));
    }

    @Test
    void reachabilityQueryLocationPosition2TestSuccess() {
        var location = new Location();
        location.setId("L1");
        var component = new Component();
        component.setName("C1");

        SimulationInitializationDialogController.ListOfComponents.clear();
        SimulationInitializationDialogController.ListOfComponents.add("C2");
        SimulationInitializationDialogController.ListOfComponents.add("C1");
        SimulationInitializationDialogController.ListOfComponents.add("C3");

        var result = BackendHelper.getLocationReachableQuery(location, component);
        var indexOfLocation = result.indexOf(',') + 1;
        var output = result.charAt(indexOfLocation);
        assertEquals(output, location.getId().charAt(0));
    }

    @Test
    void reachabilityQueryLocationPosition3TestSuccess() {
        var location = new Location();
        location.setId("L1");
        var component = new Component();
        component.setName("C1");

        SimulationInitializationDialogController.ListOfComponents.clear();
        SimulationInitializationDialogController.ListOfComponents.add("C2");
        SimulationInitializationDialogController.ListOfComponents.add("C3");
        SimulationInitializationDialogController.ListOfComponents.add("C1");

        var query = BackendHelper.getLocationReachableQuery(location, component);
        var indexOfLocation = query.indexOf(']') - 2;
        var output = query.charAt(indexOfLocation);
        assertEquals(output, location.getId().charAt(0));
    }

    @Test
    void reachabilityQueryNumberOfLocationsTestSuccess() {
        var location = new Location();
        location.setId("L1");
        var component = new Component();
        component.setName("C1");

        SimulationInitializationDialogController.ListOfComponents.clear();
        SimulationInitializationDialogController.ListOfComponents.add("C2");
        SimulationInitializationDialogController.ListOfComponents.add("C1");
        SimulationInitializationDialogController.ListOfComponents.add("C3");
        SimulationInitializationDialogController.ListOfComponents.add("C4");

        var query = BackendHelper.getLocationReachableQuery(location, component);
        int underscoreCount = 0;
        for (int i = 0; i < query.length(); i++) {
            if (query.charAt(i) == '_') {
                underscoreCount++;
            }
        }

        assertEquals(SimulationInitializationDialogController.ListOfComponents.size(), underscoreCount + 1);
    }
}
