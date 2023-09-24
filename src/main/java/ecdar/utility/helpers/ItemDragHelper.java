package ecdar.utility.helpers;

import ecdar.Ecdar;
import ecdar.controllers.EcdarController;
import ecdar.controllers.EdgeController;
import ecdar.presentations.ComponentOperatorPresentation;
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
            } else return Math.min(v, max);
        }

    }

    public static void makeDraggable(final Node mouseSubject,
                                     final Supplier<DragBounds> getDragBounds) {
        makeDraggable(mouseSubject, mouseSubject, getDragBounds);
    }

    public static void makeDraggable(final Node mouseSubject, final Node draggable,
                                     final Supplier<DragBounds> getDragBounds) {
        final DoubleProperty draggablePreviousX = new SimpleDoubleProperty();
        final DoubleProperty draggablePreviousY = new SimpleDoubleProperty();
        final DoubleProperty dragOffsetX = new SimpleDoubleProperty();
        final DoubleProperty dragOffsetY = new SimpleDoubleProperty();
        final BooleanProperty wasDragged = new SimpleBooleanProperty();

        final ArrayList<Pair<Double, Double>> previousLocations = new ArrayList<>();

        mouseSubject.addEventHandler(MouseEvent.MOUSE_PRESSED, event -> {
            if(!event.isPrimaryButtonDown()) return;
            event.consume();

            draggablePreviousX.set(draggable.getLayoutX());
            draggablePreviousY.set(draggable.getLayoutY());
            dragOffsetX.set(event.getSceneX());
            dragOffsetY.set(event.getSceneY());

            previousLocations.clear();

            for (SelectHelper.ItemSelectable item : SelectHelper.getSelectedElements()) {
                previousLocations.add(new Pair<>(item.getX(), item.getY()));
            }
        });

        mouseSubject.addEventHandler(MouseEvent.MOUSE_DRAGGED, event -> {
            if(!event.isPrimaryButtonDown()) return;
            event.consume();

            final DragBounds dragBounds = getDragBounds.get();

            final double dragDistanceX = (event.getSceneX() - dragOffsetX.get()) / Ecdar.getPresentation().getController().getEditorPresentation().getController().getActiveCanvasZoomFactor().get();
            final double dragDistanceY = (event.getSceneY() - dragOffsetY.get()) / Ecdar.getPresentation().getController().getEditorPresentation().getController().getActiveCanvasZoomFactor().get();
            double draggableNewX = dragBounds.trimX(draggablePreviousX.get() + dragDistanceX);
            double draggableNewY = dragBounds.trimY(draggablePreviousY.get() + dragDistanceY);

            draggable.setLayoutX(draggableNewX);
            draggable.setLayoutY(draggableNewY);

            int numberOfSelectedItems = SelectHelper.getSelectedElements().size();

            for (int i = 0; i < numberOfSelectedItems; i++) {
                SelectHelper.ItemSelectable item = SelectHelper.getSelectedElements().get(i);
                if (item instanceof EdgeController) continue;

                if (!item.equals(mouseSubject)) {
                    final double itemNewX = dragBounds.trimX(previousLocations.get(i).getKey() + dragDistanceX);
                    final double itemNewY = dragBounds.trimY(previousLocations.get(i).getValue() + dragDistanceY);

                    // The x and y properties of any ComponentOperatorPresentation is bound and must therefore be set this way instead
                    if (item instanceof ComponentOperatorPresentation) {
                        ((ComponentOperatorPresentation) item).layoutXProperty().set(item.getDragBounds().trimX(itemNewX));
                        ((ComponentOperatorPresentation) item).layoutYProperty().set(item.getDragBounds().trimY(itemNewY));
                    } else {
                        item.xProperty().set(item.getDragBounds().trimX(itemNewX));
                        item.yProperty().set(item.getDragBounds().trimY(itemNewY));
                    }
                }
            }

            wasDragged.set(true);
        });

        mouseSubject.addEventHandler(MouseEvent.MOUSE_RELEASED, event -> {
            event.consume();
            final double draggableCurrentX = draggable.getLayoutX();
            final double draggableCurrentY = draggable.getLayoutY();
            final double previousX = draggablePreviousX.get();
            final double previousY = draggablePreviousY.get();

            // Copying the coordinates and selected items is necessary for the undo/redo stack to function properly
            final ArrayList<SelectHelper.ItemSelectable> selectedItems = new ArrayList<>(SelectHelper.getSelectedElements());
            final ArrayList<Pair<Double, Double>> currentLocations = new ArrayList<>();
            for (SelectHelper.ItemSelectable item : selectedItems) {
                currentLocations.add(new Pair<>(item.getX(), item.getY()));
            }
            final ArrayList<Pair<Double, Double>> savedLocations = new ArrayList<>(previousLocations);

            if(draggableCurrentX != previousX || draggableCurrentY != previousY) {
                UndoRedoStack.pushAndPerform(
                        () -> {
                            placeSelectedItems(mouseSubject, draggableCurrentX, draggableCurrentY, currentLocations, selectedItems);
                        },
                        () -> {
                            placeSelectedItems(mouseSubject, previousX, previousY, savedLocations, selectedItems);
                        },
                        String.format("Moved " + draggable.getClass() + " from (%f,%f) to (%f,%f)", draggableCurrentX, draggableCurrentY, previousX, previousY),
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
            if (item instanceof EdgeController) continue;

            if (!item.equals(mouseSubject)) {
                // The x and y properties of any ComponentOperatorPresentation is bound and must therefore be set this way instead
                if (item instanceof ComponentOperatorPresentation) {
                    ((ComponentOperatorPresentation) item).setLayoutX(item.getDragBounds().trimX(currentLocations.get(i).getKey()));
                    ((ComponentOperatorPresentation) item).setLayoutY(item.getDragBounds().trimY(currentLocations.get(i).getValue()));
                } else {
                    item.xProperty().set(item.getDragBounds().trimX(currentLocations.get(i).getKey()));
                    item.yProperty().set(item.getDragBounds().trimY(currentLocations.get(i).getValue()));
                }
            }
        }
    }
}
