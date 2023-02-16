package ecdar.utility.helpers;

import ecdar.Ecdar;
import ecdar.abstractions.Box;
import ecdar.presentations.LocationPresentation;
import javafx.geometry.Point2D;

import java.util.Collection;

import static ecdar.presentations.ModelPresentation.TOOLBAR_HEIGHT;

public class LocationPlacer {
    /**
     * Finds an unoccupied space within the provided box, starting from the preferred placement.
     * Returns null if no such space could be found given the existing locations
     * @param bounds bounds within which to find the unoccupied space
     * @param existingLocations array of coordinates that are occupied
     * @param preferredPlacement the preferred coordinates for the free space
     * @return a free space or null if unable to find one
     */
    public static Point2D getFreeCoordinatesForLocation(final Box bounds, final Collection<Point2D> existingLocations, final Point2D preferredPlacement) {
        final double offset = LocationPresentation.RADIUS * 2 + Ecdar.CANVAS_PADDING;
        boolean hit = false;

        double latestHitRight = 0,
                latestHitDown = 0,
                latestHitLeft = 0,
                latestHitUp = 0;

        //Check to see if the location is placed on top of another location
        for (Point2D entry : existingLocations) {
            if (Math.abs(entry.getX() - (preferredPlacement.getX())) < offset && Math.abs(entry.getY() - (preferredPlacement.getY())) < offset) {
                hit = true;
                latestHitRight = entry.getX();
                latestHitDown = entry.getY();
                latestHitLeft = entry.getX();
                latestHitUp = entry.getY();
                break;
            }
        }

        //If the location is not placed on top of any other locations, do not do anything
        if (!hit) {
            return preferredPlacement;
        }
        hit = false;

        //Find an unoccupied space for the location
        for (int i = 1; i < bounds.getWidth() / offset; i++) {
            //Check to see, if the location can be placed to the right of an existing location
            if (latestHitRight > offset && bounds.getWidth() > latestHitRight + offset) {
                for (Point2D entry : existingLocations) {
                    if (Math.abs(entry.getX() - (latestHitRight + offset)) < offset && Math.abs(entry.getY() - (preferredPlacement.getY())) < offset) {
                        hit = true;
                        latestHitRight = entry.getX();
                        break;
                    }
                }

                if (!hit) {
                    return new Point2D(latestHitRight + offset, preferredPlacement.getY());
                }
            }
            hit = false;

            //Check to see, if the location can be placed below an existing location
            if (latestHitDown > offset && bounds.getHeight() > latestHitDown + offset) {
                for (Point2D entry : existingLocations) {
                    if (Math.abs(entry.getX() - (preferredPlacement.getX())) < offset && Math.abs(entry.getY() - (latestHitDown + offset)) < offset) {
                        hit = true;
                        latestHitDown = entry.getY();
                        break;
                    }
                }

                if (!hit) {
                    return new Point2D(preferredPlacement.getX(), latestHitDown + offset);
                }
            }
            hit = false;

            //Check to see, if the location can be placed to the left of the existing location
            if (latestHitLeft > offset && bounds.getWidth() > latestHitLeft - offset) {
                for (Point2D entry : existingLocations) {
                    if (Math.abs(entry.getX() - (latestHitLeft - offset)) < offset && Math.abs(entry.getY() - (preferredPlacement.getY())) < offset) {
                        hit = true;
                        latestHitLeft = entry.getX();
                        break;
                    }
                }

                if (!hit) {
                    return new Point2D(latestHitLeft - offset, preferredPlacement.getY());
                }
            }
            hit = false;

            //Check to see, if the location can be placed above an existing location
            if (latestHitUp > offset + TOOLBAR_HEIGHT && bounds.getHeight() > latestHitUp - offset) {
                for (Point2D entry : existingLocations) {
                    if (Math.abs(entry.getX() - (preferredPlacement.getX())) < offset && Math.abs(entry.getY() - (latestHitUp - offset)) < offset) {
                        hit = true;
                        latestHitUp = entry.getY();
                        break;
                    }
                }

                if (!hit) {
                    return new Point2D(preferredPlacement.getX(), latestHitUp - offset);
                }
            }
            hit = false;
        }

        return null;
    }
}
