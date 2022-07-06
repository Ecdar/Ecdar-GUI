package ecdar.presentations;

import ecdar.abstractions.Component;
import ecdar.controllers.EcdarController;
import ecdar.utility.UndoRedoStack;
import ecdar.utility.colors.Color;
import ecdar.utility.helpers.ItemDragHelper;
import ecdar.utility.helpers.LocationAware;
import com.jfoenix.controls.JFXTextField;
import ecdar.utility.helpers.SelectHelper;
import javafx.application.Platform;
import javafx.beans.binding.When;
import javafx.beans.property.*;
import javafx.beans.value.ObservableBooleanValue;
import javafx.beans.value.ObservableDoubleValue;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.robot.Robot;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;

import java.util.function.BiConsumer;

import static ecdar.presentations.Grid.GRID_SIZE;
import static javafx.scene.paint.Color.TRANSPARENT;

public class TagPresentation extends StackPane {
    final static Color backgroundColor = Color.GREY;
    final static Color.Intensity backgroundColorIntensity = Color.Intensity.I50;

    final ObjectProperty<Component> component = new SimpleObjectProperty<>(null);
    final ObjectProperty<LocationAware> locationAware = new SimpleObjectProperty<>(null);

    LineTo l2;
    LineTo l3;
    boolean hadInitialFocus = false;

    static double TAG_HEIGHT = 1.6 * GRID_SIZE;

    public TagPresentation() {
        new EcdarFXMLLoader().loadAndGetController("TagPresentation.fxml", this);

        initializeShape();
        initializeLabel();
        initializeMouseTransparency();
        initializeTextFocusHandler();

        layoutXProperty().addListener((observable, oldValue, newValue) -> {
            final double trimmedX = getDragBounds().trimX(newValue.doubleValue());
            if (trimmedX != newValue.doubleValue()) {
                layoutXProperty().set(trimmedX);
            }
        });

        layoutYProperty().addListener((observable, oldValue, newValue) -> {
            final double trimmedY = getDragBounds().trimY(newValue.doubleValue());
            if (trimmedY != newValue.doubleValue()) {
                layoutXProperty().set(trimmedY);
            }
        });
    }

    void initializeTextFocusHandler() {
        Platform.runLater(() -> {
            // When a label is loaded do not request focus initially
            textFieldFocusProperty().addListener((observable, oldValue, newValue) -> {
                if (!hadInitialFocus && newValue) {
                    hadInitialFocus = true;
                    EcdarController.getActiveCanvasPresentation().getController().leaveTextAreas();
                }
            });
        });
    }

    void initializeMouseTransparency() {
        mouseTransparentProperty().bind(opacityProperty().isEqualTo(0, 0.00f));
    }

    void initializeTextAid(JFXTextField textField) {
        textField.textProperty().addListener((obs, oldText, newText) -> {
            if (newText.contains(" ")) {
                final String updatedString = newText.replace(" ", "_");
                textField.setText(updatedString);
            }
        });
    }

    private void initializeShape() {
        final int WIDTH = 5000;
        final double HEIGHT = TAG_HEIGHT;

        final JFXTextField textField = (JFXTextField) lookup("#textField");
        final Path shape = (Path) lookup("#shape");
        final MoveTo start = new MoveTo(0, 0);

        l2 = new LineTo(WIDTH, 0);
        l3 = new LineTo(WIDTH, HEIGHT);
        final LineTo l4 = new LineTo(0, HEIGHT);
        final LineTo l6 = new LineTo(0, 0);

        shape.getElements().addAll(start, l2, l3, l4, l6);

        shape.setFill(backgroundColor.getColor(backgroundColorIntensity));
        shape.setStroke(backgroundColor.getColor(backgroundColorIntensity.next(4)));
        shape.setCursor(Cursor.OPEN_HAND);

        initializeDragging(textField, shape);

        textField.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                shape.setCursor(Cursor.TEXT);
            } else {
                textField.setMouseTransparent(true);
                shape.setCursor(Cursor.OPEN_HAND);
            }
        });

        // When enter or escape is pressed release focus
        textField.setOnKeyPressed(EcdarController.getActiveCanvasPresentation().getController().getLeaveTextAreaKeyHandler());
    }

    private void initializeDragging(JFXTextField textField, Path shape) {
        final DoubleProperty draggablePreviousX = new SimpleDoubleProperty();
        final DoubleProperty draggablePreviousY = new SimpleDoubleProperty();
        final DoubleProperty dragOffsetX = new SimpleDoubleProperty();
        final DoubleProperty dragOffsetY = new SimpleDoubleProperty();
        final BooleanProperty wasDragged = new SimpleBooleanProperty();

        shape.addEventHandler(MouseEvent.MOUSE_PRESSED, event -> {
            event.consume();

            SelectHelper.clearSelectedElements();

            draggablePreviousX.set(getTranslateX());
            draggablePreviousY.set(getTranslateY());
            dragOffsetX.set(event.getSceneX());
            dragOffsetY.set(event.getSceneY());

            // Abandon edge, if drag edge is registered instead of tag drag ToDo: Fix so this is not needed
            Platform.runLater(() -> {
                Robot robot = new Robot();
                robot.keyType(KeyCode.ESCAPE);
            });

            shape.setCursor(Cursor.CLOSED_HAND);
        });

        shape.addEventHandler(MouseEvent.MOUSE_DRAGGED, event -> {
            event.consume();

            final double dragDistanceX = Grid.snap(event.getSceneX() - dragOffsetX.get()) / EcdarController.getActiveCanvasZoomFactor().get();
            final double dragDistanceY = Grid.snap(event.getSceneY() - dragOffsetY.get()) / EcdarController.getActiveCanvasZoomFactor().get();
            double draggableNewX = getDragBounds().trimX(draggablePreviousX.get() + dragDistanceX);
            double draggableNewY = getDragBounds().trimY(draggablePreviousY.get() + dragDistanceY);

            setTranslateX(draggableNewX);
            setTranslateY(draggableNewY);

            wasDragged.set(true);
        });

        shape.addEventHandler(MouseEvent.MOUSE_RELEASED, event -> {
            event.consume();
            if (wasDragged.get()) {
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
                shape.setCursor(Cursor.OPEN_HAND);
            }

            if (event.getClickCount() == 2) {
                textField.setMouseTransparent(false);
                textField.requestFocus();
                textField.requestFocus(); // This needs to be done twice because of reasons
            }
        });
    }

    void initializeLabel() {
        final Label label = (Label) lookup("#label");
        final JFXTextField textField = (JFXTextField) lookup("#textField");
        final Path shape = (Path) lookup("#shape");

        final Insets insets = new Insets(0, 2, 0, 2);
        textField.setPadding(insets);
        label.setPadding(insets);

        textField.setMinHeight(TAG_HEIGHT);
        textField.setMaxHeight(TAG_HEIGHT);
        textField.focusedProperty().addListener((observable, oldFocused, newFocused) -> {
            if (newFocused) {
                shape.setTranslateY(2);
                textField.setTranslateY(2);
            }
        });

        label.layoutBoundsProperty().addListener((obs, oldBounds, newBounds) -> {
            // Set limit for minimum width and add 2 for padding (text is not using full width without padding)
            double newWidth = Math.max(newBounds.getWidth() + 2, 20);

            textField.setMinWidth(newWidth);
            textField.setMaxWidth(newWidth);

            if (getWidth() >= 1000) {
                setWidth(newWidth);
                setHeight(TAG_HEIGHT);
                shape.setTranslateY(-1);
                textField.setTranslateY(-1);
            }

            // Fixes the jumping of the shape when the text field is empty
            if (textField.getText().isEmpty()) {
                shape.setLayoutX(0);
            }
        });

        label.textProperty().bind(new When(textField.textProperty().isNotEmpty()).then(textField.textProperty()).otherwise(textField.promptTextProperty()));

        // Ensure that the line to the nail is anchored to the center of the tag
        l2.xProperty().bind(textField.widthProperty());
        l3.xProperty().bind(textField.widthProperty());
    }

    public void bindToColor(final ObjectProperty<Color> color, final ObjectProperty<Color.Intensity> intensity) {
        bindToColor(color, intensity, false);
    }

    public void bindToColor(final ObjectProperty<Color> color, final ObjectProperty<Color.Intensity> intensity, final boolean doColorBackground) {
        final BiConsumer<Color, Color.Intensity> recolor = (newColor, newIntensity) -> {
            final JFXTextField textField = (JFXTextField) lookup("#textField");
            textField.setUnFocusColor(TRANSPARENT);
            textField.setFocusColor(newColor.getColor(newIntensity));

            if (doColorBackground) {
                final Path shape = (Path) lookup("#shape");
                shape.setFill(newColor.getColor(newIntensity.next(-1)));
                shape.setStroke(newColor.getColor(newIntensity.next(-1).next(2)));

                textField.setStyle("-fx-prompt-text-fill: rgba(255, 255, 255, 0.6); -fx-text-fill: " + newColor.getTextColorRgbaString(newIntensity) + ";");
                textField.setFocusColor(newColor.getTextColor(newIntensity));
            } else {
                textField.setStyle("-fx-prompt-text-fill: rgba(0, 0, 0, 0.6);");
            }
        };

        color.addListener(observable -> recolor.accept(color.get(), intensity.get()));
        intensity.addListener(observable -> recolor.accept(color.get(), intensity.get()));

        recolor.accept(color.get(), intensity.get());
    }

    public void setAndBindString(final StringProperty stringProperty) {
        final JFXTextField textField = (JFXTextField) lookup("#textField");
        textField.textProperty().unbind();
        textField.setText(stringProperty.get());
        stringProperty.bind(textField.textProperty());
    }

    public void setPlaceholder(final String placeholder) {
        final JFXTextField textField = (JFXTextField) lookup("#textField");
        textField.setPromptText(placeholder);
    }

    public void replaceSpace() {
        initializeTextAid((JFXTextField) lookup("#textField"));
    }

    public void requestTextFieldFocus() {
        final JFXTextField textField = (JFXTextField) lookup("#textField");
        Platform.runLater(textField::requestFocus);
    }

    public ObservableBooleanValue textFieldFocusProperty() {
        final JFXTextField textField = (JFXTextField) lookup("#textField");
        return textField.focusedProperty();
    }

    public Component getComponent() {
        return component.get();
    }

    public void setComponent(final Component component) {
        this.component.set(component);
    }

    public ObjectProperty<Component> componentProperty() {
        return component;
    }

    public LocationAware getLocationAware() {
        return locationAware.get();
    }

    public ObjectProperty<LocationAware> locationAwareProperty() {
        return locationAware;
    }

    public void setLocationAware(LocationAware locationAware) {
        this.locationAware.set(locationAware);
    }

    public void setDisabledText(boolean bool) {
        final JFXTextField textField = (JFXTextField) lookup("#textField");
        textField.setDisable(true);
    }

    /**
     * Get the drag bounds for the tag within the Component
     * @return drag bounds of the tag within the Component
     */
    public ItemDragHelper.DragBounds getDragBounds() {
        final JFXTextField textField = (JFXTextField) lookup("#textField");

        final ObservableDoubleValue minX = locationAware.get().xProperty().multiply(-1).add(GRID_SIZE);
        final ObservableDoubleValue maxX = getComponent().getBox().getWidthProperty()
                .subtract(locationAware.get().xProperty().add(textField.widthProperty()).add(GRID_SIZE));
        final ObservableDoubleValue minY = locationAware.get().yProperty().multiply(-1).add(GRID_SIZE * 2);
        final ObservableDoubleValue maxY = getComponent().getBox().getHeightProperty()
                .subtract(locationAware.get().yProperty().add(textField.heightProperty()).add(GRID_SIZE));

        return new ItemDragHelper.DragBounds(minX, maxX, minY, maxY);
    }

    /**
     * Clears the string value of the tag
     */
    public void clear() {
        ((JFXTextField) lookup("#textField")).clear();
    }
}
