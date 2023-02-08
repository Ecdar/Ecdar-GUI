package ecdar.controllers;

import ecdar.Ecdar;
import ecdar.abstractions.Component;
import ecdar.code_analysis.CodeAnalysis;
import ecdar.presentations.MessagePresentation;
import ecdar.utility.colors.Color;
import javafx.beans.InvalidationListener;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.fxml.Initializable;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;

import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class MessageCollectionController implements Initializable {
    public VBox root;
    public Circle indicator;
    public Label headline;
    public Line line;
    public VBox messageBox;

    private Component component;
    private final ObservableList<CodeAnalysis.Message> messages = new SimpleListProperty<>();
    private Map<CodeAnalysis.Message, MessagePresentation> messageMessagePresentationMap;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initializeHeadline(component);
        initializeLine();
        initializeErrorsListener();
    }

    private void initializeErrorsListener() {
        messageMessagePresentationMap = new HashMap<>();

        final Consumer<CodeAnalysis.Message> addMessage = (message) -> {
            final MessagePresentation messagePresentation = new MessagePresentation(message);
            messageMessagePresentationMap.put(message, messagePresentation);
            messageBox.getChildren().add(messagePresentation);
        };

        messages.forEach(addMessage);
        messages.addListener((ListChangeListener<CodeAnalysis.Message>) c -> {
            while (c.next()) {
                c.getAddedSubList().forEach(addMessage);

                c.getRemoved().forEach(message -> {
                    messageBox.getChildren().remove(messageMessagePresentationMap.get(message));
                    messageMessagePresentationMap.remove(message);
                });
            }
        });
    }

    private void initializeLine() {
        messageBox.getChildren().addListener((InvalidationListener) observable -> line.setEndY(messageBox.getChildren().size() * 23 + 8));
    }

    private void initializeHeadline(final Component component) {
        line.setStroke(Color.GREY.getColor(Color.Intensity.I400));

        // This is an project wide message that is not specific to a component
        if (component == null) {
            headline.setText("Project");
            return;
        }

        headline.setText(component.getName());
        headline.textProperty().bind(component.nameProperty());

        final EventHandler<MouseEvent> onMouseEntered = event -> {
            root.setCursor(Cursor.HAND);
            headline.setStyle("-fx-underline: true;");
        };

        final EventHandler<MouseEvent> onMouseExited = event -> {
            root.setCursor(Cursor.DEFAULT);
            headline.setStyle("-fx-underline: false;");
        };

        final EventHandler<MouseEvent> onMousePressed = event -> Ecdar.getPresentation().getController().setActiveModelPresentationForActiveCanvas(
                Ecdar.getPresentation().getController().
                        projectPane.getController().getComponentPresentations().
                        stream().filter(componentPresentation -> componentPresentation.getController().getComponent().equals(component))
                        .findFirst().orElse(null));

        headline.setOnMouseEntered(onMouseEntered);
        headline.setOnMouseExited(onMouseExited);
        headline.setOnMousePressed(onMousePressed);
        indicator.setOnMouseEntered(onMouseEntered);
        indicator.setOnMouseExited(onMouseExited);
        indicator.setOnMousePressed(onMousePressed);

        final BiConsumer<Color, Color.Intensity> updateColor = (color, intensity) -> {
            indicator.setFill(color.getColor(component.getColorIntensity()));
        };

        updateColor.accept(component.getColor(), component.getColorIntensity());
        component.colorProperty().addListener((observable, oldColor, newColor) -> updateColor.accept(newColor, component.getColorIntensity()));
    }

    public void setComponent(Component newComponent) {
        component = newComponent;
    }

    public void setMessages(List<CodeAnalysis.Message> newMessages) {
        if (!newMessages.isEmpty()) messages.setAll(newMessages);
    }
}
