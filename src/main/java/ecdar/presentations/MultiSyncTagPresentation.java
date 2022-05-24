package ecdar.presentations;

import ecdar.abstractions.Edge;
import ecdar.abstractions.GroupedEdge;
import ecdar.controllers.EcdarController;
import ecdar.controllers.MultiSyncTagController;
import ecdar.utility.UndoRedoStack;
import javafx.application.Platform;
import javafx.beans.value.ObservableBooleanValue;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.util.List;

import static ecdar.presentations.Grid.GRID_SIZE;

public class MultiSyncTagPresentation extends TagPresentation {
    private final MultiSyncTagController controller;
    private String placeholder = "";
    private Label widestLabel;
    private final GroupedEdge edge;
    private double widthNeededForSyncs = 60;
    private final SyncTextFieldPresentation emptySyncTextField;

    public MultiSyncTagPresentation(GroupedEdge edge, Runnable updateIOStatusOfSubEdges) {
        this.controller = new EcdarFXMLLoader().loadAndGetController("MultiSyncTagPresentation.fxml", this);

        this.edge = edge;
        edge.ioStatus.addListener((observable) -> updateIOStatusOfSubEdges.run());

        Platform.runLater(() -> {
            updateTopBorder();
            initializeMouseTransparency();
            initializeTextFocusHandler();

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
                if (change.wasRemoved()) {
                    change.getRemoved().forEach(this::removeSyncTextField);
                }

                if (change.wasAdded()) {
                    change.getAddedSubList().forEach(this::addSyncTextField);
                }
            }

            // Handle height
            if (getHeight() > TAG_HEIGHT * 10) {
                double newHeight = TAG_HEIGHT * 10;
                final double resHeight = GRID_SIZE * 2 - (newHeight % (GRID_SIZE * 2));
                newHeight += resHeight;

                l2.setY(newHeight);
                l3.setY(newHeight);

                setMinHeight(newHeight);
                setMaxHeight(newHeight);
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

    public MultiSyncTagController getController() {
        return controller;
    }

    private void updateTopBorder() {
        updateColorAndMouseShape();
        updateMouseInteractions();

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

    private void updateMouseInteractions() {
        controller.frame.setOnMousePressed(event -> {
            previousX = getTranslateX();
            previousY = getTranslateY();

            controller.frame.setCursor(Cursor.CLOSED_HAND);
        });

        controller.frame.setOnMouseDragged(event -> {
            event.consume();

            final double newX = EcdarController.getActiveCanvasPresentation().mouseTracker.gridXProperty().subtract(getComponent().getBox().getXProperty()).subtract(getLocationAware().xProperty()).subtract(getWidth() / 2).doubleValue();
            setTranslateX(newX);

            final double newY = EcdarController.getActiveCanvasPresentation().mouseTracker.gridYProperty().subtract(getComponent().getBox().getYProperty()).subtract(getLocationAware().yProperty()).subtract(controller.topbar.getHeight() / 2).doubleValue();
            setTranslateY(newY - 2);

            // Tell the mouse release action that we can store an update
            wasDragged = true;
        });

        controller.frame.setOnMouseReleased(event -> {
            if (wasDragged) {
                // Add to undo redo stack
                final double currentX = getTranslateX();
                final double currentY = getTranslateY();
                final double storePreviousX = previousX;
                final double storePreviousY = previousY;

                UndoRedoStack.pushAndPerform(
                        () -> {
                            setTranslateX(currentX);
                            setTranslateY(currentY);
                        },
                        () -> {
                            setTranslateX(storePreviousX);
                            setTranslateY(storePreviousY);
                        },
                        String.format("Moved tag from (%f,%f) to (%f,%f)", currentX, currentY, storePreviousX, storePreviousY),
                        "pin-drop"
                );

                // Reset the was dragged boolean
                wasDragged = false;
            }

            controller.frame.setCursor(Cursor.OPEN_HAND);

            Platform.runLater(this::ensureTagIsWithinComponent);
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
        syncTextField.getController().label.layoutBoundsProperty().addListener((obs, oldBounds, newBounds) -> {
            final int padding = 5;
            double newWidth = Math.max(newBounds.getWidth(), 60);
            final double resWidth = GRID_SIZE * 2 - (newWidth % (GRID_SIZE * 2));
            newWidth += resWidth;

            this.updateNeededWidth();

            if (syncTextField.getController().label.equals(widestLabel) && newWidth + padding > widthNeededForSyncs) {
                setMinWidth(newWidth + padding);
                setMaxWidth(newWidth + padding);

                controller.syncList.setMinWidth(newWidth + padding);
                controller.syncList.setMaxWidth(newWidth + padding);

                Platform.runLater(this::ensureTagIsWithinComponent);
            }

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
        // Added to avoid null pointer when first sync is added
        if (controller.syncList == null || controller.syncList.getParent() == null || controller.syncList.getParent().getParent() == null) {
            return;
        }

        //Handle the horizontal placement of the tag
        double syncListWidth = ((ScrollPane) controller.syncList.getParent().getParent().getParent()).getViewportBounds().getWidth();
        if(getTranslateX() + locationAware.getValue().getX() + syncListWidth + GRID_SIZE * 2 > getComponent().getBox().getX() + getComponent().getBox().getWidth()) {
            setTranslateX(Grid.snap(getComponent().getBox().getX() + getComponent().getBox().getWidth() - locationAware.getValue().getX() - syncListWidth - GRID_SIZE * 2));
        } else if (getTranslateX() + locationAware.getValue().getX() < getComponent().getBox().getX()) {
            setTranslateX(Grid.snap(getComponent().getBox().getX() - locationAware.getValue().getX()));
        }

        //Handle the vertical placement of the tag
        double syncListHeight = ((ScrollPane) controller.syncList.getParent().getParent().getParent()).getViewportBounds().getHeight();
        if(getTranslateY() + locationAware.getValue().getY() + syncListHeight + TAG_HEIGHT * 3 > getComponent().getBox().getY() + getComponent().getBox().getHeight()) {
            setTranslateY(Grid.snap(getComponent().getBox().getY() + getComponent().getBox().getHeight() - locationAware.getValue().getY() - (syncListHeight + TAG_HEIGHT * 3)));
        } else if (getTranslateY() + locationAware.getValue().getY() < getComponent().getBox().getY() + GRID_SIZE * 2) {
            setTranslateY(Grid.snap(getComponent().getBox().getY() - locationAware.getValue().getY() + GRID_SIZE * 2));
        }
    }

    private void placeEmptySyncBelowCurrent(SyncTextFieldPresentation currentTextField) {
        // Move empty sync below current sync and request focus
        controller.syncList.getChildren().remove(this.emptySyncTextField);
        controller.syncList.getChildren().add(controller.syncList.getChildren().indexOf(currentTextField) + 1, this.emptySyncTextField);
        this.requestTextFieldFocus();
    }

    private void updateNeededWidth() {
        double neededLengthForTextField = 0;
        for (Node child : controller.syncList.getChildren()) {
            Label currentLabel = ((SyncTextFieldPresentation) child).getController().label;

            if (currentLabel.getWidth() > neededLengthForTextField) {
                neededLengthForTextField = currentLabel.getWidth();
                widestLabel = currentLabel;
            }
        }

        widthNeededForSyncs = neededLengthForTextField;
    }
}
