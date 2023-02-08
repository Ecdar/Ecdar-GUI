package ecdar.utility;

import ecdar.abstractions.Component;
import ecdar.abstractions.Location;
import ecdar.presentations.LocationPresentation;
import ecdar.utility.helpers.LocationPlacer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

public class LocationPlacerTest {
    static Component component;

    @BeforeAll
    static void setup() {
        component = new Component(false, "test_comp");
    }

    @Test
    public void testLocationCanBeAddedToNewlyGeneratedComponent() {
        LocationPresentation newLocationPresentation = LocationPlacer.ensureCorrectPlacementOfLocation(component, new ArrayList<>(), new Location(), (locationPresentation) -> Assertions.fail("Unable to add location to component with no location"));
        Assertions.assertNotNull(newLocationPresentation);
    }
}
