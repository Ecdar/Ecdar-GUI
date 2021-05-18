package ecdar.presentations;

import com.jfoenix.controls.JFXTextField;
import ecdar.controllers.EcdarController;
import ecdar.controllers.MultiSyncTagController;
import ecdar.utility.UndoRedoStack;
import javafx.application.Platform;
import javafx.beans.binding.When;
import javafx.beans.property.*;
import javafx.beans.value.ObservableBooleanValue;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;

import java.util.ArrayList;
import java.util.List;

import static ecdar.presentations.Grid.GRID_SIZE;

public class MultiSyncTagPresentation extends TagPresentation {

    private MultiSyncTagController controller;
    private String placeholder = "";

    public MultiSyncTagPresentation() {
        updateTopBorder();
        initializeLabel();
        initializeMouseTransparency();
        initializeTextFocusHandler();

        widthProperty().addListener((observable) -> updateTopBorder());
        heightProperty().addListener((observable) -> updateTopBorder());

        controller.syncList.setPadding(new Insets(0, 0, 10, 0));

        // Disable horizontal scroll
        lookup("#scrollPane").addEventFilter(ScrollEvent.SCROLL,new EventHandler<ScrollEvent>() {
            @Override
            public void handle(ScrollEvent event) {
                if (event.getDeltaX() != 0) {
                    event.consume();
                }
            }
        });
    }

    private void updateTopBorder() {
        if (controller == null) {
            this.controller = new EcdarFXMLLoader().loadAndGetController("MultiSyncTagPresentation.fxml", this);
        }

        EcdarController.getActiveCanvasPresentation().getController().activeComponentPresentation.getController().getComponent().colorProperty().addListener((observable, oldValue, newValue) -> {
            controller.frame.setBackground(new Background(new BackgroundFill(newValue.getColor(
                    EcdarController.getActiveCanvasPresentation().getController().activeComponentPresentation.getController().getComponent().getColorIntensity()), new CornerRadii(0), Insets.EMPTY)));
        });

        Color color = EcdarController.getActiveCanvasPresentation().getController().activeComponentPresentation.getController().getComponent().getColor().getColor(
                EcdarController.getActiveCanvasPresentation().getController().activeComponentPresentation.getController().getComponent().getColorIntensity());
        controller.frame.setBackground(new Background(new BackgroundFill(color, new CornerRadii(0), Insets.EMPTY)));

        final List<JFXTextField> textFields = new ArrayList<>();
        for (Node child : controller.syncList.getChildren()) {
            if (child instanceof StackPane && !((StackPane) child).getChildren().isEmpty()) {
                textFields.add((JFXTextField) ((StackPane) child).getChildren().get(1));
            }
        }

        controller.frame.setCursor(Cursor.OPEN_HAND);

        controller.frame.setOnMousePressed(event -> {
            previousX = getTranslateX();
            previousY = getTranslateY();
        });

        controller.frame.setOnMouseDragged(event -> {
            event.consume();

            //ToDo NIELS: The position is relative to the nail, which means that the nail position must be included in the calculation, if the tag is to be kept within the component

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
        });

        // When enter or escape is pressed release focus
        textFields.forEach(textField -> textField.setOnKeyPressed(EcdarController.getActiveCanvasPresentation().getController().getLeaveTextAreaKeyHandler()));
    }

    @Override
    public void setPlaceholder(final String placeholder) {
        this.placeholder = placeholder;
        List<Node> textFieldContainers = controller.syncList.getChildren();

        for (Node child : textFieldContainers) {
            if (child instanceof StackPane && !((StackPane) child).getChildren().isEmpty()) {
                JFXTextField textField = (JFXTextField) ((StackPane) child).getChildren().get(1);
                textField.setPromptText(placeholder);
            }
        }
    }

    @Override
    public void replaceSpace() {
        for (Node child : controller.syncList.getChildren()) {
            if (child instanceof StackPane && !((StackPane) child).getChildren().isEmpty()) {
                initializeTextAid((JFXTextField) ((StackPane) child).getChildren().get(1));
            }
        }
    }

    @Override
    public void requestTextFieldFocus() {
        // ToDO NIELS: Handle
        final JFXTextField textField = (JFXTextField) lookup("#textField");
        Platform.runLater(textField::requestFocus);
    }

    @Override
    public ObservableBooleanValue textFieldFocusProperty() {
        return controller.textFieldFocus;
    }

    @Override
    public void setDisabledText(boolean bool){
        for (Node child : controller.syncList.getChildren()) {
            if (child instanceof StackPane && !((StackPane) child).getChildren().isEmpty()) {
                ((StackPane) child).getChildren().get(1).setDisable(true);
            }
        }
    }

    public MultiSyncTagController getController() {
        return controller;
    }

    public void setAndBindStringList(final List<StringProperty> stringList) {
        Platform.runLater(() -> {
            clearTextFields();

            for (StringProperty stringProperty : stringList) {
                JFXTextField textField = addSyncTextField();
                textField.textProperty().unbind();
                textField.setText(stringProperty.get());
                stringProperty.bind(textField.textProperty());
            }
        });

        if (getHeight() > TAG_HEIGHT * 10) {
            double newHeight = TAG_HEIGHT * 10;
            final double resHeight = GRID_SIZE * 2 - (newHeight % (GRID_SIZE * 2));
            newHeight += resHeight;

            l2.setY(newHeight);
            l3.setY(newHeight);

            setMinHeight(newHeight);
            setMaxHeight(newHeight);
        }
    }

    private JFXTextField addSyncTextField() {
        final Label label = new Label();
        final JFXTextField textField = new JFXTextField();

        label.getStyleClass().add("sub-caption");
        label.setAlignment(Pos.CENTER_LEFT);
        label.setTextAlignment(TextAlignment.LEFT);
        label.setVisible(false);

        textField.getStyleClass().add("sub-caption");
        textField.setAlignment(Pos.CENTER_LEFT);

        final Insets insets = new Insets(0,2,0,2);
        textField.setPadding(insets);
        label.setPadding(insets);

        final int padding = 5;

        label.layoutBoundsProperty().addListener((obs, oldBounds, newBounds) -> {
            double newWidth = Math.max(newBounds.getWidth(), 60);
            final double resWidth = GRID_SIZE * 2 - (newWidth % (GRID_SIZE * 2));
            newWidth += resWidth;

            double neededLengthForTextField = getNeededLengthForTextField();

            if (newWidth + padding > neededLengthForTextField) {
                setMinWidth(newWidth + padding);
                setMaxWidth(newWidth + padding);
            }

            if (getWidth() >= 1000) {
                setWidth(newWidth);;
                textField.setTranslateY(-1);
            }

            // Fixes the jumping of the shape when the text field is empty
            if (textField.getText().isEmpty()) {
                setWidth(-1);
            }
        });

        textField.focusedProperty().addListener((observable, oldFocused, newFocused) -> {
            if (newFocused) {
                textField.setTranslateY(2);
            } else {
                textField.setTranslateY(0);
                EcdarController.getActiveCanvasPresentation().getController().leaveTextAreas();
            }
        });

        textField.setPromptText(this.placeholder);

        label.textProperty().bind(new When(textField.textProperty().isNotEmpty()).then(textField.textProperty()).otherwise(textField.promptTextProperty()));

        StackPane container = new StackPane();
        container.getChildren().add(label);
        container.getChildren().add(textField);
        container.setAlignment(Pos.TOP_LEFT);

        controller.syncList.getChildren().add(container);

        return textField;
    }

    private double getNeededLengthForTextField() {
        double neededLengthForTextField = 0;
        for (Node child : controller.syncList.getChildren()) {
            double numberOfCharactersInChild = ((Label) ((StackPane) child).getChildren().get(0)).getWidth();

            if (numberOfCharactersInChild > neededLengthForTextField) {
                neededLengthForTextField = numberOfCharactersInChild;
            }
        }

        return neededLengthForTextField;
    }

    private void clearTextFields() {
        controller.syncList.getChildren().clear();
    }
}
