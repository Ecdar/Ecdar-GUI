package ecdar.simulation;

import ecdar.Ecdar;
import ecdar.abstractions.Component;
import ecdar.abstractions.Location;
import ecdar.backend.BackendDriver;
import ecdar.backend.BackendHelper;
import ecdar.backend.SimulationHandler;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ReachabilityTest {

    @BeforeAll
    static void setup() {
        Ecdar.setUpForTest();
    }

    @Test
    void reachabilityQuerySyntaxTestSuccess() {
        var regex = "query\\s+\\->\\s+\\[(\\w*)(,(\\w)*)*\\]\\([a-zA-Z0-9_<>=]*\\)(;\\[(\\w*)(,(\\w)*)\\]\\([a-zA-Z0-9_<>=]*\\))*";

        var location = new Location();
        location.setId("L1");
        var component = new Component();
        component.setName("C1");

        SimulationHandler simulationHandler = new SimulationHandler(new BackendDriver());

        List<String> components = new ArrayList<>();
        components.add("C1");
        components.add("C2");
        components.add("C3");

        simulationHandler.setComponentsInSimulation(components);

        Ecdar.setSimulationHandler(simulationHandler);

        var result = BackendHelper.getLocationReachableQuery(location, component, "query");

        assertTrue(result.matches(regex));
    }

    @Test
    void reachabilityQueryLocationPosition1TestSuccess() {
        var location = new Location();
        location.setId("L1");
        var component = new Component();
        component.setName("C1");

        SimulationHandler simulationHandler = new SimulationHandler(new BackendDriver());

        List<String> components = new ArrayList<>();
        components.add("C1");
        components.add("C2");
        components.add("C3");

        simulationHandler.setComponentsInSimulation(components);

        Ecdar.setSimulationHandler(simulationHandler);

        var result = BackendHelper.getLocationReachableQuery(location, component, "query");

        int indexOfOpeningBracket = result.indexOf('[');
        int indexOfComma = 0;

        for (int i = 0; i < result.length(); i++) {
            if (result.charAt(i) == ',') {
               indexOfComma = i;
               break;
            }
        }

        var output = result.substring(indexOfOpeningBracket + 1, indexOfComma);
        assertEquals(output, location.getId());
    }

    @Test
    void reachabilityQueryLocationPosition2TestSuccess() {
        var location = new Location();
        location.setId("L123");
        var component = new Component();
        component.setName("C2");

        SimulationHandler simulationHandler = new SimulationHandler(new BackendDriver());

        List<String> components = new ArrayList<>();
        components.add("C1");
        components.add("C2");
        components.add("C3");

        simulationHandler.setComponentsInSimulation(components);

        Ecdar.setSimulationHandler(simulationHandler);

        var result = BackendHelper.getLocationReachableQuery(location, component, "query");

        int indexOfFirstComma = 0;
        int indexofSecondComma = 0;
        boolean commaSeen = false;

        for (int i = 0; i < result.length(); i++) {
            if (result.charAt(i) == ',') {
                if(commaSeen){
                    indexofSecondComma = i;
                }
                else {
                    indexOfFirstComma = i;
                    commaSeen = true;
                }
            }
        }

        var output = result.substring(indexOfFirstComma + 1,indexofSecondComma);
        assertEquals(output, location.getId());
    }

    @Test
    void reachabilityQueryLocationPosition3TestSuccess() {
        var location = new Location();
        location.setId("L12345");
        var component = new Component();
        component.setName("C3");

        SimulationHandler simulationHandler = new SimulationHandler(new BackendDriver());

        List<String> components = new ArrayList<>();
        components.add("C1");
        components.add("C2");
        components.add("C3");

        simulationHandler.setComponentsInSimulation(components);

        Ecdar.setSimulationHandler(simulationHandler);

        var query = BackendHelper.getLocationReachableQuery(location, component, "query");

        int indexOfLastComma = 0;

        for (int i = query.length()-1; i > 0; i--) {
            if (query.charAt(i) == ',') {
               indexOfLastComma = i;
               break;
            }
        }

        int indexOfClosingBracket = query.indexOf(']');

        var output = query.substring(indexOfLastComma + 1,indexOfClosingBracket);

        assertEquals(output, location.getId());
    }

    @Test
    void reachabilityQueryNumberOfLocationsTestSuccess() {
        var location = new Location();
        location.setId("L1");
        var component = new Component();
        component.setName("C1");

        SimulationHandler simulationHandler = new SimulationHandler(new BackendDriver());

        List<String> components = new ArrayList<>();
        components.add("C1");
        components.add("C2");
        components.add("C3");
        components.add("C4");

        simulationHandler.setComponentsInSimulation(components);

        Ecdar.setSimulationHandler(simulationHandler);

        var query = BackendHelper.getLocationReachableQuery(location, component, "query");
        int commaCount = 0;
        for (int i = 0; i < query.length(); i++) {
            if (query.charAt(i) == ',') {
                commaCount++;
            }
        }

        int expected = commaCount + 1;

        assertEquals(expected, Ecdar.getSimulationHandler().getComponentsInSimulation().size());
    }
}
