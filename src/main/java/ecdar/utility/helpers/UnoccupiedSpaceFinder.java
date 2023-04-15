package ecdar.utility.helpers;

import ecdar.abstractions.Box;
import javafx.geometry.Point2D;

import java.util.Collection;

import static ecdar.presentations.ModelPresentation.TOOLBAR_HEIGHT;

public class UnoccupiedSpaceFinder {
    /**
     * Finds an unoccupied space within the provided box, starting from the preferred placement.
     * Returns null if no such space could be found given the existing locations
     * @param bounds bounds within which to find the unoccupied space
     * @param occupiedSpaces array of coordinates that are occupied
     * @param preferredPlacement the preferred coordinates for the free space
     * @param spacing the spacing between any two points and any point and the edges of the box
     * @return a free space or null if unable to find one
     */
    public static Point2D getUnoccupiedSpace(final Box bounds, final Collection<Point2D> occupiedSpaces, final Point2D preferredPlacement, final double spacing) {
        boolean hit = false;

        double latestHitRight = 0,
                latestHitDown = 0,
                latestHitLeft = 0,
                latestHitUp = 0;

        //Check to see if the location is placed on top of another location
        for (Point2D entry : occupiedSpaces) {
            if (Math.abs(entry.getX() - (preferredPlacement.getX())) < spacing && Math.abs(entry.getY() - (preferredPlacement.getY())) < spacing) {
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
        for (int i = 1; i < bounds.getWidth() / spacing; i++) {
            //Check to see, if the location can be placed to the right of an existing location
            if (latestHitRight > spacing && bounds.getWidth() > latestHitRight + spacing) {
                for (Point2D entry : occupiedSpaces) {
                    if (Math.abs(entry.getX() - (latestHitRight + spacing)) < spacing && Math.abs(entry.getY() - (preferredPlacement.getY())) < spacing) {
                        hit = true;
                        latestHitRight = entry.getX();
                        break;
                    }
                }

                if (!hit) {
                    return new Point2D(latestHitRight + spacing, preferredPlacement.getY());
                }
            }
            hit = false;

            //Check to see, if the location can be placed below an existing location
            if (latestHitDown > spacing && bounds.getHeight() > latestHitDown + spacing) {
                for (Point2D entry : occupiedSpaces) {
                    if (Math.abs(entry.getX() - (preferredPlacement.getX())) < spacing && Math.abs(entry.getY() - (latestHitDown + spacing)) < spacing) {
                        hit = true;
                        latestHitDown = entry.getY();
                        break;
                    }
                }

                if (!hit) {
                    return new Point2D(preferredPlacement.getX(), latestHitDown + spacing);
                }
            }
            hit = false;

            //Check to see, if the location can be placed to the left of the existing location
            if (latestHitLeft > spacing && bounds.getWidth() > latestHitLeft - spacing) {
                for (Point2D entry : occupiedSpaces) {
                    if (Math.abs(entry.getX() - (latestHitLeft - spacing)) < spacing && Math.abs(entry.getY() - (preferredPlacement.getY())) < spacing) {
                        hit = true;
                        latestHitLeft = entry.getX();
                        break;
                    }
                }

                if (!hit) {
                    return new Point2D(latestHitLeft - spacing, preferredPlacement.getY());
                }
            }
            hit = false;

            //Check to see, if the location can be placed above an existing location
            if (latestHitUp > spacing + TOOLBAR_HEIGHT && bounds.getHeight() > latestHitUp - spacing) {
                for (Point2D entry : occupiedSpaces) {
                    if (Math.abs(entry.getX() - (preferredPlacement.getX())) < spacing && Math.abs(entry.getY() - (latestHitUp - spacing)) < spacing) {
                        hit = true;
                        latestHitUp = entry.getY();
                        break;
                    }
                }

                if (!hit) {
                    return new Point2D(preferredPlacement.getX(), latestHitUp - spacing);
                }
            }
            hit = false;
        }

        return null;
    }
}
