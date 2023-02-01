package ecdar.utility.helpers;

import ecdar.abstractions.Component;
import ecdar.abstractions.Location;
import ecdar.presentations.LocationPresentation;
import javafx.beans.value.ChangeListener;

import java.util.Collection;
import java.util.function.Consumer;

import static ecdar.presentations.Grid.GRID_SIZE;

public class LocationPlacer {
    public static LocationPresentation ensureCorrectPlacementOfLocation(Component component, Collection<LocationPresentation> existingLocations, Location originalLocation, Consumer<LocationPresentation> failedToFindPlacement) {
        // Create a new presentation, and register it on the map
        final LocationPresentation newLocationPresentation = new LocationPresentation(originalLocation, component);

        final ChangeListener<Number> locationPlacementChangedListener = (observable, oldValue, newValue) -> {
            final double offset = newLocationPresentation.getController().circle.getRadius() * 2 + GRID_SIZE;
            boolean hit = false;
            ItemDragHelper.DragBounds componentBounds = newLocationPresentation.getController().getDragBounds();

            //Define the x and y coordinates for the initial and final locations
            final double initialLocationX = component.getBox().getX() + newLocationPresentation.getController().circle.getRadius() * 2, initialLocationY = component.getBox().getY() + newLocationPresentation.getController().circle.getRadius() * 2, finalLocationX = component.getBox().getX() + component.getBox().getWidth() - newLocationPresentation.getController().circle.getRadius() * 2, finalLocationY = component.getBox().getY() + component.getBox().getHeight() - newLocationPresentation.getController().circle.getRadius() * 2;

            double latestHitRight = 0, latestHitDown = 0, latestHitLeft = 0, latestHitUp = 0;

            //Check to see if the location is placed on top of the initial location
            if (Math.abs(initialLocationX - (newLocationPresentation.getLayoutX())) < offset && Math.abs(initialLocationY - (newLocationPresentation.getLayoutY())) < offset) {
                hit = true;
                latestHitRight = initialLocationX;
                latestHitDown = initialLocationY;
                latestHitLeft = initialLocationX;
                latestHitUp = initialLocationY;
            }

            //Check to see if the location is placed on top of the final location
            else if (Math.abs(finalLocationX - (newLocationPresentation.getLayoutX())) < offset && Math.abs(finalLocationY - (newLocationPresentation.getLayoutY())) < offset) {
                hit = true;
                latestHitRight = finalLocationX;
                latestHitDown = finalLocationY;
                latestHitLeft = finalLocationX;
                latestHitUp = finalLocationY;
            }

            //Check to see if the location is placed on top of another location
            else {
                for (LocationPresentation entry : existingLocations) {
                    if (entry != newLocationPresentation && Math.abs(entry.getLayoutX() - (newLocationPresentation.getLayoutX())) < offset && Math.abs(entry.getLayoutY() - (newLocationPresentation.getLayoutY())) < offset) {
                        hit = true;
                        latestHitRight = entry.getLayoutX();
                        latestHitDown = entry.getLayoutY();
                        latestHitLeft = entry.getLayoutX();
                        latestHitUp = entry.getLayoutY();
                        break;
                    }
                }
            }

            //If the location is not placed on top of any other locations, do not do anything
            if (!hit) {
                return;
            }
            hit = false;

            //Find an unoccupied space for the location
            for (int i = 1; i < component.getBox().getWidth() / offset; i++) {

                //Check to see, if the location can be placed to the right of the existing locations
                if (componentBounds.trimX(latestHitRight + offset) == latestHitRight + offset) {

                    //Check if the location would be placed on the final location
                    if (Math.abs(finalLocationX - (latestHitRight + offset)) < offset && Math.abs(finalLocationY - (newLocationPresentation.getLayoutY())) < offset) {
                        hit = true;
                        latestHitRight = finalLocationX;
                    } else {
                        for (LocationPresentation entry : existingLocations) {
                            if (entry != newLocationPresentation && Math.abs(entry.getLayoutX() - (latestHitRight + offset)) < offset && Math.abs(entry.getLayoutY() - (newLocationPresentation.getLayoutY())) < offset) {
                                hit = true;
                                latestHitRight = entry.getLayoutX();
                                break;
                            }
                        }
                    }

                    if (!hit) {
                        newLocationPresentation.setLayoutX(latestHitRight + offset);
                        return;
                    }
                }
                hit = false;

                //Check to see, if the location can be placed below the existing locations
                if (componentBounds.trimY(latestHitDown + offset) == latestHitDown + offset) {

                    //Check if the location would be placed on the final location
                    if (Math.abs(finalLocationX - (newLocationPresentation.getLayoutX())) < offset && Math.abs(finalLocationY - (latestHitDown + offset)) < offset) {
                        hit = true;
                        latestHitDown = finalLocationY;
                    } else {
                        for (LocationPresentation entry : existingLocations) {
                            if (entry != newLocationPresentation && Math.abs(entry.getLayoutX() - (newLocationPresentation.getLayoutX())) < offset && Math.abs(entry.getLayoutY() - (latestHitDown + offset)) < offset) {
                                hit = true;
                                latestHitDown = entry.getLayoutY();
                                break;
                            }
                        }
                    }
                    if (!hit) {
                        newLocationPresentation.setLayoutY(latestHitDown + offset);
                        return;
                    }
                }
                hit = false;

                //Check to see, if the location can be placed to the left of the existing locations
                if (componentBounds.trimX(latestHitLeft - offset) == latestHitLeft - offset) {

                    //Check if the location would be placed on the initial location
                    if (Math.abs(initialLocationX - (latestHitLeft - offset)) < offset && Math.abs(initialLocationY - (newLocationPresentation.getLayoutY())) < offset) {
                        hit = true;
                        latestHitLeft = initialLocationX;
                    } else {
                        for (LocationPresentation entry : existingLocations) {
                            if (entry != newLocationPresentation && Math.abs(entry.getLayoutX() - (latestHitLeft - offset)) < offset && Math.abs(entry.getLayoutY() - (newLocationPresentation.getLayoutY())) < offset) {
                                hit = true;
                                latestHitLeft = entry.getLayoutX();
                                break;
                            }
                        }
                    }
                    if (!hit) {
                        newLocationPresentation.setLayoutX(latestHitLeft - offset);
                        return;
                    }
                }
                hit = false;

                //Check to see, if the location can be placed above the existing locations
                if (componentBounds.trimY(latestHitUp - offset) == latestHitUp - offset) {

                    //Check if the location would be placed on the initial location
                    if (Math.abs(initialLocationX - (newLocationPresentation.getLayoutX())) < offset && Math.abs(initialLocationY - (latestHitUp - offset)) < offset) {
                        hit = true;
                        latestHitUp = initialLocationY;
                    } else {
                        for (LocationPresentation entry : existingLocations) {
                            if (entry != newLocationPresentation && Math.abs(entry.getLayoutX() - (newLocationPresentation.getLayoutX())) < offset && Math.abs(entry.getLayoutY() - (latestHitUp - offset)) < offset) {
                                hit = true;
                                latestHitUp = entry.getLayoutY();
                                break;
                            }
                        }
                    }
                    if (!hit) {
                        newLocationPresentation.setLayoutY(latestHitUp - offset);
                        return;
                    }
                }
                hit = false;
            }

            failedToFindPlacement.accept(newLocationPresentation);
        };

        newLocationPresentation.layoutXProperty().addListener(locationPlacementChangedListener);
        newLocationPresentation.layoutYProperty().addListener(locationPlacementChangedListener);

        return newLocationPresentation;
    }
}
