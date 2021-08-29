package ecdar.presentations;

import ecdar.abstractions.Component;
import ecdar.controllers.EcdarController;
import ecdar.controllers.MainController;
import ecdar.utility.UndoRedoStack;
import ecdar.utility.colors.Color;
import ecdar.utility.helpers.LocationAware;
import com.jfoenix.controls.JFXTextField;
import javafx.application.Platform;
import javafx.beans.binding.When;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableBooleanValue;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
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
    double previousX;
    double previousY;
    boolean wasDragged;
    boolean hadInitialFocus = false;

    static double TAG_HEIGHT = 1.6 * GRID_SIZE;

    public TagPresentation() {
        new EcdarFXMLLoader().loadAndGetController("TagPresentation.fxml", this);

        initializeShape();
        initializeLabel();
        initializeMouseTransparency();
        initializeTextFocusHandler();
    }

    void initializeTextFocusHandler() {
        Platform.runLater(() -> {
            // When a label is loaded do not request focus initially
            textFieldFocusProperty().addListener((observable, oldValue, newValue) -> {
                if(!hadInitialFocus && newValue) {
                    hadInitialFocus = true;
                    MainController.getActiveCanvasPresentation().getController().leaveTextAreas();
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

        final Path shape = (Path) lookup("#shape");

        final MoveTo start = new MoveTo(0, 0);

        l2 = new LineTo(WIDTH, 0);
        l3 = new LineTo(WIDTH, HEIGHT);
        final LineTo l4 = new LineTo(0, HEIGHT);
        final LineTo l6 = new LineTo(0, 0);

        shape.getElements().addAll(start, l2, l3, l4, l6);

        shape.setFill(backgroundColor.getColor(backgroundColorIntensity));
        shape.setStroke(backgroundColor.getColor(backgroundColorIntensity.next(4)));

        final JFXTextField textField = (JFXTextField) lookup("#textField");
        shape.setCursor(Cursor.OPEN_HAND);

        shape.setOnMousePressed(event -> {
            previousX = getTranslateX();
            previousY = getTranslateY();
            shape.setCursor(Cursor.CLOSED_HAND);
        });

        this.setOnMouseDragged(event -> {
            event.consume();

            final double newX = MainController.getActiveCanvasPresentation().mouseTracker.gridXProperty().subtract(getComponent().getBox().getXProperty()).subtract(getLocationAware().xProperty()).doubleValue() - getMinWidth() / 2;
            setTranslateX(newX);

            final double newY = MainController.getActiveCanvasPresentation().mouseTracker.gridYProperty().subtract(getComponent().getBox().getYProperty()).subtract(getLocationAware().yProperty()).doubleValue() - getHeight() / 2;
            setTranslateY(newY - 2);

            // Tell the mouse release action that we can store an update
            wasDragged = true;
        });

        shape.setOnMouseReleased(event -> {
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
            } else if(event.getClickCount() == 2){
                textField.setMouseTransparent(false);
                textField.requestFocus();
                textField.requestFocus(); // This needs to be done twice because of reasons
            }

            //Handle the horizontal placement of the tag
            if(getTranslateX() + locationAware.getValue().getX() + textField.getWidth() * 2 > getComponent().getBox().getX() + getComponent().getBox().getWidth()) {
                setTranslateX(getComponent().getBox().getX() + getComponent().getBox().getWidth() - locationAware.getValue().getX() - textField.getWidth() * 2);
            } else if (getTranslateX() + locationAware.getValue().getX() < getComponent().getBox().getX()) {
                setTranslateX(getComponent().getBox().getX() - locationAware.getValue().getX());
            }

            //Handle the vertical placement of the tag
            if(getTranslateY() + locationAware.getValue().getY() + textField.getHeight() * 2 > getComponent().getBox().getY() + getComponent().getBox().getHeight()) {
                setTranslateY(getComponent().getBox().getY() + getComponent().getBox().getHeight() - locationAware.getValue().getY() - textField.getHeight() * 2);
            } else if (getTranslateY() + locationAware.getValue().getY() < getComponent().getBox().getY() + GRID_SIZE * 2) {
                setTranslateY(getComponent().getBox().getY() - locationAware.getValue().getY() + GRID_SIZE * 2);
            }

            shape.setCursor(Cursor.OPEN_HAND);
        });

        textField.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if(newValue) {
                shape.setCursor(Cursor.TEXT);
            } else {
                textField.setMouseTransparent(true);
                shape.setCursor(Cursor.OPEN_HAND);
            }
        });

        // When enter or escape is pressed release focus
        textField.setOnKeyPressed(MainController.getActiveCanvasPresentation().getController().getLeaveTextAreaKeyHandler());
    }

    void initializeLabel() {
        final Label label = (Label) lookup("#label");
        final JFXTextField textField = (JFXTextField) lookup("#textField");
        final Path shape = (Path) lookup("#shape");

        final Insets insets = new Insets(0,2,0,2);
        textField.setPadding(insets);
        label.setPadding(insets);

        final int padding = 0;

        label.layoutBoundsProperty().addListener((obs, oldBounds, newBounds) -> {
            double newWidth = Math.max(newBounds.getWidth(), 10);
            final double res = GRID_SIZE * 2 - (newWidth % (GRID_SIZE * 2));
            newWidth += res;

            textField.setMinWidth(newWidth);
            textField.setMaxWidth(newWidth);

            l2.setX(newWidth + padding);
            l3.setX(newWidth + padding);

            setMinWidth(newWidth + padding);
            setMaxWidth(newWidth + padding);

            textField.setMinHeight(TAG_HEIGHT);
            textField.setMaxHeight(TAG_HEIGHT);

            textField.focusedProperty().addListener((observable, oldFocused, newFocused) -> {
                if (newFocused) {
                    shape.setTranslateY(2);
                    textField.setTranslateY(2);
                }
            });

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

    public void setDisabledText(boolean bool){
        final JFXTextField textField = (JFXTextField) lookup("#textField");
        textField.setDisable(true);
    }
}
