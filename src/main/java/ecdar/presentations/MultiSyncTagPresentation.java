package ecdar.presentations;

import com.jfoenix.controls.JFXTextField;
import ecdar.Ecdar;
import ecdar.abstractions.DisplayableEdge;
import ecdar.abstractions.Edge;
import ecdar.abstractions.GroupedEdge;
import ecdar.abstractions.Nail;
import ecdar.controllers.EcdarController;
import ecdar.controllers.MultiSyncTagController;
import ecdar.utility.UndoRedoStack;
import ecdar.utility.helpers.ItemDragHelper;
import ecdar.utility.helpers.SelectHelper;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ObservableBooleanValue;
import javafx.beans.value.ObservableDoubleValue;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.util.List;

public class MultiSyncTagPresentation extends TagPresentation {
    private final MultiSyncTagController controller;
    private String placeholder = "";
    private final GroupedEdge edge;
    private final DoubleProperty syncsWidth = new SimpleDoubleProperty(80);
    private final SyncTextFieldPresentation emptySyncTextField;

    public MultiSyncTagPresentation(GroupedEdge edge, Runnable updateIOStatusOfSubEdges) {
        this.controller = new EcdarFXMLLoader().loadAndGetController("MultiSyncTagPresentation.fxml", this);
        this.edge = edge;
        edge.ioStatus.addListener((observable) -> updateIOStatusOfSubEdges.run());

        Platform.runLater(() -> {
            // Added to avoid NullPointer exception for location aware and component in getDragBounds method
            edge.getNails().stream().filter(n -> n.getPropertyType().equals(DisplayableEdge.PropertyType.SYNCHRONIZATION)).findFirst().ifPresent(this::setLocationAware);
            setComponent(EcdarController.getActiveCanvasPresentation().getController().activeComponentPresentation.getController().getComponent());

            updateTopBorder();
            initializeMouseTransparency();
            initializeTextFocusHandler();
            initializeDragging();

            widthProperty().addListener((observable) -> updateTopBorder());
            heightProperty().addListener((observable) -> updateTopBorder());

            controller.syncList.setPadding(new Insets(0, 0, 10, 0));

            // Disable horizontal scroll
            lookup("#scrollPane").addEventFilter(ScrollEvent.SCROLL, event -> {
                if (event.getDeltaX() != 0) {
                    event.consume();
                }
            });
        });

        emptySyncTextField = addSyncTextField(null);

        if (!edge.getEdges().isEmpty() && !edge.getEdges().get(0).getSync().isEmpty()) {
            // When dragging edge to change source or target location, the MultiSyncTag is initialized with multiple sub-edges
            for (Edge subEdge : edge.getEdges()) {
                addSyncTextField(subEdge);
            }
        }

        edge.getEdges().addListener((ListChangeListener<Edge>) change -> {
            while (change.next()) {
                if (change.wasRemoved()) change.getRemoved().forEach(this::removeSyncTextField);
                if (change.wasAdded()) change.getAddedSubList().forEach(this::addSyncTextField);
            }

            // Handle height
            if (getHeight() > TAG_HEIGHT * 10) {
                double newHeight = TAG_HEIGHT * 10;
                final double resHeight = newHeight;
                newHeight += resHeight;

                l2.setY(newHeight);
                l3.setY(newHeight);
            }
        });
    }

    @Override
    public void setPlaceholder(final String placeholder) {
        this.placeholder = placeholder;
        List<Node> syncTextFields = controller.syncList.getChildren();

        for (Node child : syncTextFields) {
            ((SyncTextFieldPresentation) child).setPlaceholder(placeholder);
        }
    }

    @Override
    public void replaceSpace() {
        for (Node child : controller.syncList.getChildren()) {
            initializeTextAid(((SyncTextFieldPresentation) child).getController().textField);
        }
    }

    @Override
    public void requestTextFieldFocus() {
        Platform.runLater(() -> {
            this.emptySyncTextField.getController().textField.requestFocus();
            this.emptySyncTextField.getController().textField.end();
        });
    }

    @Override
    public ObservableBooleanValue textFieldFocusProperty() {
        return controller.textFieldFocus;
    }

    @Override
    public void setDisabledText(boolean bool){
        for (Node child : controller.syncList.getChildren()) {
            ((SyncTextFieldPresentation) child).getController().textField.setDisable(true);
        }
    }

    @Override
    public ItemDragHelper.DragBounds getDragBounds() {
        final JFXTextField textField = (JFXTextField) lookup("#textField");
        double syncListWidth;
        double syncListHeight;

        // Added to avoid null pointer when first sync is added
        if (controller.syncList == null || controller.syncList.getParent() == null || controller.syncList.getParent().getParent() == null) {
            syncListWidth = textField.getWidth() + Ecdar.CANVAS_PADDING * 2;
            syncListHeight = textField.getHeight() + Ecdar.CANVAS_PADDING;
        } else {
            syncListWidth = ((ScrollPane) controller.syncList.getParent().getParent().getParent()).getViewportBounds().getWidth();
            syncListHeight = ((ScrollPane) controller.syncList.getParent().getParent().getParent()).getViewportBounds().getHeight();
        }

        final ObservableDoubleValue minX = locationAware.get().xProperty().multiply(-1).add(Ecdar.CANVAS_PADDING);
        final ObservableDoubleValue maxX = getComponent().getBox().getWidthProperty()
                .subtract(locationAware.get().xProperty().add(syncListWidth + Ecdar.CANVAS_PADDING));

        final ObservableDoubleValue minY = locationAware.get().yProperty().multiply(-1).add(textField.heightProperty().multiply(2));
        final ObservableDoubleValue maxY = getComponent().getBox().getHeightProperty()
                .subtract(locationAware.get().yProperty().add(syncListHeight + TAG_HEIGHT * 2));

        return new ItemDragHelper.DragBounds(minX, maxX, minY, maxY);
    }

    public MultiSyncTagController getController() {
        return controller;
    }

    private void updateTopBorder() {
        updateColorAndMouseShape();

        for (Node child : controller.syncList.getChildren()) {
            ((SyncTextFieldPresentation) child).getController().textField
                    .setOnKeyPressed(EcdarController.getActiveCanvasPresentation().getController()
                            .getLeaveTextAreaKeyHandler((keyEvent) -> {

                if (keyEvent.getCode().equals(KeyCode.ENTER)) {
                    if (!child.equals(this.emptySyncTextField)) {
                        this.placeEmptySyncBelowCurrent((SyncTextFieldPresentation) child);
                    }

                    this.requestTextFieldFocus();
                }

            }));
        }
    }

    private void updateColorAndMouseShape() {
        EcdarController.getActiveCanvasPresentation().getController().activeComponentPresentation.getController().getComponent().colorProperty().addListener((observable, oldValue, newValue) -> {
            controller.frame.setBackground(new Background(new BackgroundFill(newValue.getColor(
                    EcdarController.getActiveCanvasPresentation().getController().activeComponentPresentation.getController().getComponent().getColorIntensity()), new CornerRadii(0), Insets.EMPTY)));
        });

        Color color = EcdarController.getActiveCanvasPresentation().getController().activeComponentPresentation.getController().getComponent().getColor().getColor(
                EcdarController.getActiveCanvasPresentation().getController().activeComponentPresentation.getController().getComponent().getColorIntensity());

        controller.frame.setBackground(new Background(new BackgroundFill(color, new CornerRadii(0), Insets.EMPTY)));
        controller.frame.setCursor(Cursor.OPEN_HAND);
    }

    private void initializeDragging() {
        final DoubleProperty draggablePreviousX = new SimpleDoubleProperty();
        final DoubleProperty draggablePreviousY = new SimpleDoubleProperty();
        final DoubleProperty dragOffsetX = new SimpleDoubleProperty();
        final DoubleProperty dragOffsetY = new SimpleDoubleProperty();
        final BooleanProperty wasDragged = new SimpleBooleanProperty();

        controller.frame.addEventHandler(MouseEvent.MOUSE_PRESSED, event -> {
            event.consume();

            SelectHelper.clearSelectedElements();
            draggablePreviousX.set(getTranslateX());
            draggablePreviousY.set(getTranslateY());
            dragOffsetX.set(event.getSceneX());
            dragOffsetY.set(event.getSceneY());

            controller.frame.setCursor(Cursor.CLOSED_HAND);
        });

        controller.frame.addEventHandler(MouseEvent.MOUSE_DRAGGED, event -> {
            event.consume();

            Platform.runLater(() -> {
                final double dragDistanceX = (event.getSceneX() - dragOffsetX.get()) / EcdarController.getActiveCanvasZoomFactor().get();
                final double dragDistanceY = (event.getSceneY() - dragOffsetY.get()) / EcdarController.getActiveCanvasZoomFactor().get();
                double draggableNewX = getDragBounds().trimX(draggablePreviousX.get() + dragDistanceX);
                double draggableNewY = getDragBounds().trimY(draggablePreviousY.get() + dragDistanceY);

                setTranslateX(draggableNewX);
                setTranslateY(draggableNewY);
            });

            wasDragged.set(true);
        });

        controller.frame.addEventHandler(MouseEvent.MOUSE_RELEASED, event -> {
            event.consume();
            final double draggableCurrentX = getTranslateX();
            final double draggableCurrentY = getTranslateY();
            final double previousX = draggablePreviousX.get();
            final double previousY = draggablePreviousY.get();


            if(draggableCurrentX != previousX || draggableCurrentY != previousY) {
                UndoRedoStack.pushAndPerform(
                        () -> {
                            setTranslateX(draggableCurrentX);
                            setTranslateY(draggableCurrentY);
                        },
                        () -> {
                            setTranslateX(previousX);
                            setTranslateY(previousY);
                        },
                        String.format("Moved " + this.getClass() + " from (%f,%f) to (%f,%f)", draggableCurrentX, draggableCurrentY, previousX, previousY),
                        "pin-drop"
                );
            }

            // Reset the was dragged boolean
            wasDragged.set(false);
            controller.frame.setCursor(Cursor.OPEN_HAND);
        });
    }

    private void removeSyncTextField(Edge removedEdge) {
        for(int i = 0; i < controller.syncList.getChildren().size(); i++) {
            if(((SyncTextFieldPresentation) controller.syncList.getChildren().get(i)).getController().textField.getText().equals(removedEdge.getSync())) {
                controller.syncList.getChildren().remove((controller.syncList.getChildren().get(i)));
                break;
            }
        }
    }

    private SyncTextFieldPresentation addSyncTextField(Edge edge) {
        final SyncTextFieldPresentation syncTextFieldPresentation = new SyncTextFieldPresentation(placeholder, edge);

        controller.syncList.getChildren().add(Math.max(controller.syncList.getChildren().indexOf(emptySyncTextField), 0), syncTextFieldPresentation);

        Platform.runLater(() -> {
            initializeTextFieldForSync(syncTextFieldPresentation);
            initializeLabelForSync(syncTextFieldPresentation);
            ensureTagIsWithinComponent();
        });

        return syncTextFieldPresentation;
    }

    private void initializeTextFieldForSync(SyncTextFieldPresentation syncTextField) {
        syncTextField.getController().textField.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if(!newValue) {
                if(syncTextField.equals(this.emptySyncTextField) && !this.emptySyncTextField.getController().textField.getText().isEmpty()) {
                    // Create a new sub-edge with the sync and reset the empty sync text field
                    Edge newEdge = edge.getBaseSubEdge();
                    newEdge.setSync(syncTextField.getController().textField.getText());
                    syncTextField.getController().textField.setText("");

                    UndoRedoStack.pushAndPerform(
                            () -> {
                                this.edge.addEdgeToGroup(newEdge);
                            },
                            () -> {
                                this.edge.getEdges().remove(newEdge);
                            },
                            "New sync added to multi-sync edge",
                            "list_alt"
                    );
                } else if (!syncTextField.equals(this.emptySyncTextField) || !this.emptySyncTextField.getController().textField.getText().isEmpty()) {
                    if (syncTextField.getController().textField.getText().isEmpty()) {
                        this.edge.getEdges().remove(syncTextField.getController().connectedEdge);
                    }

                    this.requestTextFieldFocus();
                }
            }
        });
    }

    private void initializeLabelForSync(SyncTextFieldPresentation syncTextField) {
        // Bind the width of the label and add 2 for padding
        syncTextField.getController().textField.minWidthProperty().bind(syncsWidth.add(2));
        syncTextField.getController().textField.maxWidthProperty().bind(syncsWidth.add(2));

        syncTextField.getController().label.layoutBoundsProperty().addListener((obs, oldBounds, newBounds) -> {
            // Set limit for minimum width and add 2 for padding (text is not using full width without padding)
            double newWidth = Math.max(newBounds.getWidth() + 2, 80);
            this.updateNeededWidth(newWidth);
            Platform.runLater(this::ensureTagIsWithinComponent);

            if (getWidth() >= 1000) {
                setWidth(newWidth);
                syncTextField.getController().textField.setTranslateY(-1);
            }

            // Fixes the jumping of the shape when the text field is empty
            if (syncTextField.getController().textField.getText().isEmpty()) {
                setWidth(-1);
            }
        });
    }

    private void ensureTagIsWithinComponent() {
        setTranslateX(getDragBounds().trimX(getTranslateX()));
        setTranslateY(getDragBounds().trimY(getTranslateY()));
    }

    private void placeEmptySyncBelowCurrent(SyncTextFieldPresentation currentTextField) {
        // Move empty sync below current sync and request focus
        controller.syncList.getChildren().remove(this.emptySyncTextField);
        controller.syncList.getChildren().add(controller.syncList.getChildren().indexOf(currentTextField) + 1, this.emptySyncTextField);
        this.requestTextFieldFocus();
    }

    private void updateNeededWidth(double newWidth) {
        double neededLengthForTextField = newWidth;
        for (Node child : controller.syncList.getChildren()) {
            Label currentLabel = ((SyncTextFieldPresentation) child).getController().label;
            if (currentLabel.getWidth() > neededLengthForTextField) {
                neededLengthForTextField = currentLabel.getWidth();
            }
        }

        syncsWidth.set(neededLengthForTextField);
    }
}
