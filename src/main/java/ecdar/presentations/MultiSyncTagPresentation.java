package ecdar.presentations;

import com.jfoenix.controls.JFXScrollPane;
import com.jfoenix.controls.JFXTextField;
import ecdar.controllers.EcdarController;
import javafx.application.Platform;
import javafx.beans.binding.When;
import javafx.beans.property.*;
import javafx.beans.value.ObservableBooleanValue;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.StrokeType;

import java.util.List;

import static ecdar.presentations.Grid.GRID_SIZE;

public class MultiSyncTagPresentation extends TagPresentation {

    private final JFXScrollPane textFieldList;
    private LineTo l2;
    private LineTo l3;
    private boolean hadInitialFocus = false;

    private static double TAG_HEIGHT = 1.6 * GRID_SIZE;

    public MultiSyncTagPresentation() {
        new EcdarFXMLLoader().loadAndGetController("MultiSyncTagPresentation.fxml", this);

        textFieldList = (JFXScrollPane) this.lookup("#textFieldScrollPane");
        ((StackPane) textFieldList.getChildren().get(1)).setMinHeight(((JFXTextField) lookup("#textField")).getHeight() + 20);
        ((StackPane) textFieldList.getChildren().get(1)).setPrefHeight(((JFXTextField) lookup("#textField")).getHeight() + 20);

        // addSyncTextField();
    }

    private void addSyncTextField() {
        final Label label = new Label();
        final JFXTextField textField = new JFXTextField();
        final Path shape = new Path();

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

    public void setAndBindStringList(final List<StringProperty> stringList) {
        Platform.runLater(() -> {
            int i = 1;
            for (StringProperty stringProperty : stringList) {
                StackPane textFieldContainer = new StackPane();
                textFieldContainer.setAlignment(Pos.CENTER_LEFT);

                Path path = new Path();
                path.setId("shape" + i);
                path.setStrokeType(StrokeType.INSIDE);

                Label label = new Label();
                label.setId("label" + i);
                label.getStyleClass().add("sub-caption");
                label.setVisible(false);

                final JFXTextField textField = new JFXTextField();
                textField.setId("textField" + i);
                textField.setAlignment(Pos.CENTER_LEFT);
                textField.getStyleClass().add("sub-caption");
                textField.setMouseTransparent(true);

                textField.textProperty().unbind();
                textField.setText(stringProperty.get());
                stringProperty.bind(textField.textProperty());

                textFieldContainer.getChildren().addAll(path, label, textField);

                textFieldList
                        .getChildren()
                        .add(textFieldContainer);
                i++;
            }
        });
    }

    @Override
    public void setPlaceholder(final String placeholder) {
        List<Node> textFieldContainers = ((VBox) ((ScrollPane) textFieldList.getChildren().get(0)).getContent()).getChildren();

        for (Node child : textFieldContainers) {
            if(!((StackPane) child).getChildren().isEmpty()) {
                JFXTextField textField = (JFXTextField) ((StackPane) child).getChildren().get(2);
                textField.setPromptText(placeholder);
            }
        }
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
        // ToDo: Handle
        final JFXTextField textField = (JFXTextField) lookup("#textField");
        return textField.focusedProperty();
    }

    @Override
    public void setDisabledText(boolean bool){
        // ToDo: Handle
        final JFXTextField textField = (JFXTextField) lookup("#textField");
        textField.setDisable(true);
    }
}
