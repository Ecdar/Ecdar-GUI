package ecdar.presentations;

import com.jfoenix.controls.JFXTextField;
import com.uppaal.model.system.concrete.ConcreteTransitionRecord;
import ecdar.controllers.MultiSyncTagController;
import javafx.application.Platform;
import javafx.beans.binding.When;
import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableBooleanValue;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.LineTo;
import javafx.scene.text.TextAlignment;

import java.util.List;
import java.util.Stack;

import static ecdar.presentations.Grid.GRID_SIZE;

public class MultiSyncTagPresentation extends TagPresentation {

    private final MultiSyncTagController controller;
    private final LineTo l2;
    private final LineTo l3;
    private boolean hadInitialFocus = false;

    private static final double TAG_HEIGHT = 1.6 * GRID_SIZE;

    public MultiSyncTagPresentation() {
        controller = new EcdarFXMLLoader().loadAndGetController("MultiSyncTagPresentation.fxml", this);

        l2 = new LineTo(0, 0);
        l3 = new LineTo(0, TAG_HEIGHT);

        initializeShape();
        initializeLabel();
        initializeMouseTransparency();
        //ToDo NIELS: initializeTextFocusHandler();

        controller.syncList.setPadding(new Insets(0, 0, 10, 0));
        controller.syncList.setMinWidth(5000);

        // Disable horizontal scroll
        // ToDo NIELS: Set border to enable drag
        lookup("#scrollPane").addEventFilter(ScrollEvent.SCROLL,new EventHandler<ScrollEvent>() {
            @Override
            public void handle(ScrollEvent event) {
                if (event.getDeltaX() != 0) {
                    event.consume();
                }
            }
        });
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

            l2.setX(newWidth + padding);
            l3.setX(newWidth + padding);

            setMinWidth(newWidth + padding);
            setMaxWidth(newWidth + padding);

            textField.focusedProperty().addListener((observable, oldFocused, newFocused) -> {
                if (newFocused) {
                    textField.setTranslateY(2);
                }
            });

            if (getWidth() >= 1000) {
                setWidth(newWidth);;
                textField.setTranslateY(-1);
            }

            // Fixes the jumping of the shape when the text field is empty
            if (textField.getText().isEmpty()) {
                setWidth(-1);
            }
        });

        label.textProperty().bind(new When(textField.textProperty().isNotEmpty()).then(textField.textProperty()).otherwise(textField.promptTextProperty()));

        StackPane container = new StackPane();
        container.getChildren().add(label);
        container.getChildren().add(textField);
        container.setAlignment(Pos.TOP_LEFT);

        controller.syncList.getChildren().add(container);

        return textField;
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

    private void clearTextFields() {
        controller.syncList.getChildren().clear();
    }

    @Override
    public void setPlaceholder(final String placeholder) {
        List<Node> textFieldContainers = controller.syncList.getChildren();

        for (Node child : textFieldContainers) {
            if (child instanceof StackPane && !((StackPane) child).getChildren().isEmpty()) {
                JFXTextField textField = (JFXTextField) ((StackPane) child).getChildren().get(1);
                textField.setPromptText(placeholder);
            }
        }
    }

    public MultiSyncTagController getController() {
        return controller;
    }

    @Override
    public void replaceSpace() {
        initializeTextAid();
    }

    @Override
    public void requestTextFieldFocus() {
        // ToDo: Handle
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
}
