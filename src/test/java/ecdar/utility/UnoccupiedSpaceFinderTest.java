package ecdar.utility;

import ecdar.Ecdar;
import ecdar.abstractions.Box;
import ecdar.presentations.LocationPresentation;
import ecdar.utility.helpers.UnoccupiedSpaceFinder;
import javafx.geometry.Point2D;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

public class UnoccupiedSpaceFinderTest {
    @Test
    public void testUnoccupiedPointIsGivenAndReturned() {
        Box box = new Box();
        box.setWidth(300);
        box.setHeight(300);
        List<Point2D> locations = new ArrayList<>();
        locations.add(new Point2D(100, 40));
        locations.add(new Point2D(40, 100));
        locations.add(new Point2D(100, 100));
        locations.add(new Point2D(150, 150));
        Point2D preferredPlacement = new Point2D(200, 200);

        Point2D freePoint = UnoccupiedSpaceFinder.getUnoccupiedSpace(box, locations, preferredPlacement, LocationPresentation.RADIUS * 2 + Ecdar.CANVAS_PADDING);

        Assertions.assertEquals(preferredPlacement, freePoint);
    }

    @Test
    public void testPlacementOnOccupiedPointReturnsFreePoint() {
        Box box = new Box();
        box.setWidth(300);
        box.setHeight(300);
        Point2D preferredPlacement = new Point2D(100, 100);

        Point2D freePoint = UnoccupiedSpaceFinder.getUnoccupiedSpace(box, new ArrayList<>(List.of(preferredPlacement)), preferredPlacement, LocationPresentation.RADIUS * 2 + Ecdar.CANVAS_PADDING);

        Assertions.assertNotNull(freePoint);
        Assertions.assertNotEquals(preferredPlacement, freePoint);
    }

    @Test
    public void testPlacementInFullBoxReturnsNull() {
        Box box = new Box();
        box.setWidth(100);
        box.setHeight(100);
        List<Point2D> locations = new ArrayList<>();

        // Fill box
        for (int i = 0; i < box.getWidth(); i += LocationPresentation.RADIUS * 2) {
            for (int j = 0; j < box.getHeight(); j += LocationPresentation.RADIUS * 2) {
                locations.add(new Point2D(i, j));
            }
        }
        Point2D preferredPlacement = new Point2D(50, 50);

        Point2D freePoint = UnoccupiedSpaceFinder.getUnoccupiedSpace(box, locations, preferredPlacement, LocationPresentation.RADIUS * 2 + Ecdar.CANVAS_PADDING);

        Assertions.assertNull(freePoint);
    }
}
