package ecdar.utility.helpers;

import ecdar.controllers.EcdarController;
import ecdar.presentations.ComponentOperatorPresentation;
import ecdar.presentations.ComponentPresentation;
import ecdar.utility.UndoRedoStack;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ObservableDoubleValue;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import javafx.util.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static ecdar.presentations.Grid.GRID_SIZE;

public class ItemDragHelper {

    public static class DragBounds {
        private ObservableDoubleValue minX;
        private ObservableDoubleValue maxX;
        private ObservableDoubleValue minY;
        private ObservableDoubleValue maxY;

        public DragBounds(final ObservableDoubleValue minX, final ObservableDoubleValue maxX, final ObservableDoubleValue minY, final ObservableDoubleValue maxY) {
            this.minX = minX;
            this.maxX = maxX;
            this.minY = minY;
            this.maxY = maxY;
        }

        public DragBounds(final List<DragBounds> dragBoundses) {
             minX = new SimpleDoubleProperty(Double.MIN_VALUE);
             maxX = new SimpleDoubleProperty(Double.MAX_VALUE);
             minY = new SimpleDoubleProperty(Double.MIN_VALUE);
             maxY = new SimpleDoubleProperty(Double.MAX_VALUE);

            for (final DragBounds dragBounds : dragBoundses) {
                if (dragBounds.minX.get() > minX.get()) {
                    minX = dragBounds.minX;
                }

                if (dragBounds.maxX.get() < maxX.get()) {
                    maxX = dragBounds.maxX;
                }

                if (dragBounds.minY.get() > minY.get()) {
                    minY = dragBounds.minY;
                }

                if (dragBounds.maxY.get() < maxY.get()) {
                    maxY = dragBounds.maxY;
                }
            }

        }

        public static DragBounds generateLooseDragBounds() {
            return new ItemDragHelper.DragBounds(new SimpleDoubleProperty(Double.MIN_VALUE), new SimpleDoubleProperty(Double.MAX_VALUE),new SimpleDoubleProperty(Double.MIN_VALUE), new SimpleDoubleProperty(Double.MAX_VALUE));
        }

        public double trimX(final ObservableDoubleValue x) {
            return trimX(x.get());
        }

        public double trimX(final double x) {
            return trim(x, minX.get(), maxX.get());
        }

        public double trimY(final ObservableDoubleValue y) {
            return trimY(y.get());
        }

        public double trimY(final double y) {
            return trim(y, minY.get(), maxY.get());
        }

        private double trim(final double v, final double min, final double max) {
            if(v < min) {
                return min;
            } else if(v > max) {
                return max;
            } else {
                return v;
            }
        }

    }

    public static void makeDraggable(final Node mouseSubject,
                                     final Supplier<DragBounds> getDragBounds) {
        final DoubleProperty previousX = new SimpleDoubleProperty();
        final DoubleProperty previousY = new SimpleDoubleProperty();
        final BooleanProperty wasDragged = new SimpleBooleanProperty();

        final ArrayList<Pair<Double, Double>> previousLocations = new ArrayList<>();

        final DoubleProperty xDiff = new SimpleDoubleProperty();
        final DoubleProperty yDiff = new SimpleDoubleProperty();

        mouseSubject.addEventHandler(MouseEvent.MOUSE_PRESSED, event -> {
            if(!event.isPrimaryButtonDown()) return;

            event.consume();
            
            previousX.set(mouseSubject.getLayoutX());
            previousY.set(mouseSubject.getLayoutY());
            xDiff.set(event.getX());
            yDiff.set(event.getY());

            previousLocations.clear();

            for (SelectHelper.ItemSelectable item : SelectHelper.getSelectedElements()) {
                previousLocations.add(new Pair<>(item.getX(), item.getY()));
            }
        });

        mouseSubject.addEventHandler(MouseEvent.MOUSE_DRAGGED, event -> {
            if(!event.isPrimaryButtonDown()) return;

            event.consume();

            final DragBounds dragBounds = getDragBounds.get();

            final double newX = EcdarController.getActiveCanvasPresentation().mouseTracker.getGridX() - ((Boxed) EcdarController.getActiveCanvasPresentation().getController().getActiveModel()).getBox().getX();
            final double newY = EcdarController.getActiveCanvasPresentation().mouseTracker.getGridY() - ((Boxed) EcdarController.getActiveCanvasPresentation().getController().getActiveModel()).getBox().getY();

            final double unRoundedX = dragBounds.trimX(newX - xDiff.get());
            final double unRoundedY = dragBounds.trimY(newY - yDiff.get());
            double finalNewX = unRoundedX - unRoundedX % GRID_SIZE;
            double finalNewY = unRoundedY - unRoundedY % GRID_SIZE;

            if (mouseSubject instanceof ComponentPresentation) {
                finalNewX -= 0.5 * GRID_SIZE;
                finalNewY -= 0.5 * GRID_SIZE;
            }

            mouseSubject.setLayoutX(finalNewX);
            mouseSubject.setLayoutY(finalNewY);

            int numberOfSelectedItems = SelectHelper.getSelectedElements().size();

            for (int i = 0; i < numberOfSelectedItems; i++) {
                SelectHelper.ItemSelectable item = SelectHelper.getSelectedElements().get(i);

                if (!item.equals(mouseSubject)) {
                    final double itemNewX = previousLocations.get(i).getKey() + finalNewX - previousX.doubleValue();
                    final double itemNewY = previousLocations.get(i).getValue() + finalNewY - previousY.doubleValue();

                    final double itemUnRoundedX = dragBounds.trimX(itemNewX);
                    final double itemUnRoundedY = dragBounds.trimY(itemNewY);
                    double itemFinalNewX = itemUnRoundedX - itemUnRoundedX % GRID_SIZE;
                    double itemFinalNewY = itemUnRoundedY - itemUnRoundedY % GRID_SIZE;

                    if (item instanceof ComponentPresentation) {
                        itemFinalNewX -= 0.5 * GRID_SIZE;
                        itemFinalNewY -= 0.5 * GRID_SIZE;
                    }

                    // The x and y properties of any ComponentOperatorPresentation is bound and must therefore be set this way instead
                    if (item instanceof ComponentOperatorPresentation) {
                        ((ComponentOperatorPresentation) item).layoutXProperty().set(itemFinalNewX);
                        ((ComponentOperatorPresentation) item).layoutYProperty().set(itemFinalNewY);
                    } else {
                        item.xProperty().set(itemFinalNewX);
                        item.yProperty().set(itemFinalNewY);
                    }
                }
            }

            wasDragged.set(true);
        });

        mouseSubject.addEventHandler(MouseEvent.MOUSE_RELEASED, event -> {
            final double currentX = mouseSubject.getLayoutX();
            final double currentY = mouseSubject.getLayoutY();
            final double storePreviousX = previousX.get();
            final double storePreviousY = previousY.get();

            // Copying the coordinates and selected items is necessary for the undo/redo stack to function properly
            final ArrayList<SelectHelper.ItemSelectable> selectedItems = new ArrayList<>(SelectHelper.getSelectedElements());
            final ArrayList<Pair<Double, Double>> currentLocations = new ArrayList<>();
            for (SelectHelper.ItemSelectable item : selectedItems) {
                currentLocations.add(new Pair<>(item.getX(), item.getY()));
            }
            final ArrayList<Pair<Double, Double>> savedLocations = new ArrayList<>(previousLocations);

            if(currentX != storePreviousX || currentY != storePreviousY) {
                UndoRedoStack.pushAndPerform(
                        () -> {
                            placeSelectedItems(mouseSubject, currentX, currentY, currentLocations, selectedItems);
                        },
                        () -> {
                            placeSelectedItems(mouseSubject, storePreviousX, storePreviousY, savedLocations, selectedItems);
                        },
                        String.format("Moved " + mouseSubject.getClass() + " from (%f,%f) to (%f,%f)", currentX, currentY, storePreviousX, storePreviousY),
                        "pin-drop"
                );
            }

            // Reset the was dragged boolean
            wasDragged.set(false);
        });

    }

    private static void placeSelectedItems(Node mouseSubject, double currentX, double currentY, ArrayList<Pair<Double, Double>> currentLocations, ArrayList<SelectHelper.ItemSelectable> selectedItems) {
        mouseSubject.setLayoutX(currentX);
        mouseSubject.setLayoutY(currentY);

        int numberOfSelectedItems = selectedItems.size();

        for (int i = 0; i < numberOfSelectedItems; i++) {
            SelectHelper.ItemSelectable item = selectedItems.get(i);

            if (!item.equals(mouseSubject)) {
                // The x and y properties of any ComponentOperatorPresentation is bound and must therefore be set this way instead
                if (item instanceof ComponentOperatorPresentation) {
                    ((ComponentOperatorPresentation) item).layoutXProperty().set(currentLocations.get(i).getKey());
                    ((ComponentOperatorPresentation) item).layoutYProperty().set(currentLocations.get(i).getValue());
                } else {
                    item.xProperty().set(currentLocations.get(i).getKey());
                    item.yProperty().set(currentLocations.get(i).getValue());
                }
            }
        }
    }
}
