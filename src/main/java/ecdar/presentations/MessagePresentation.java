package ecdar.presentations;

import ecdar.Ecdar;
import ecdar.abstractions.Component;
import ecdar.abstractions.Edge;
import ecdar.abstractions.Location;
import ecdar.code_analysis.CodeAnalysis;
import ecdar.code_analysis.Nearable;
import ecdar.controllers.EcdarController;
import ecdar.controllers.MainController;
import ecdar.utility.helpers.SelectHelper;
import javafx.beans.InvalidationListener;
import javafx.collections.FXCollections;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;

public class MessagePresentation extends HBox {

    private final CodeAnalysis.Message message;

    public MessagePresentation(final CodeAnalysis.Message message) {
        this.message = message;

        new EcdarFXMLLoader().loadAndGetController("MessagePresentation.fxml", this);

        // Initialize
        initializeMessage();
        initializeNearLabel();
    }

    private void initializeMessage() {
        final Label messageLabel = (Label) lookup("#messageLabel");
        messageLabel.textProperty().bind(message.messageProperty());
    }

    private void initializeNearLabel() {
        final InvalidationListener listener = observable -> {
            String nearString = "Near: ";

            final HBox nearLabels = (HBox) lookup("#nearLabels");
            nearLabels.getChildren().clear(); // Remove all children currently in the container

            if (message.getNearables().size() == 0) {
                nearString = ""; // Do not display any "near"
            } else {
                // Add all "near" strings
                for (final Nearable nearable : message.getNearables()) {
                    final Label newNearLabel = new Label(nearable.generateNearString());

                    final boolean isClickable = nearable instanceof Location
                            || nearable instanceof Edge
                            || nearable instanceof Component;

                    if (isClickable) {
                        // Set styling
                        newNearLabel.setStyle("-fx-underline: true;");
                        newNearLabel.getStyleClass().add("body1");

                        // On mouse entered/exited
                        newNearLabel.setOnMouseEntered(event -> setCursor(Cursor.HAND));
                        newNearLabel.setOnMouseExited(event -> setCursor(Cursor.DEFAULT));

                        // On mouse pressed
                        newNearLabel.setOnMousePressed(event -> {
                            final Component[] openComponent = {null};

                            // We are pressing a location, find the location and open the corresponding component
                            if (nearable instanceof Location) {
                                Ecdar.getProject().getComponents().forEach(component -> {
                                    if (component.getLocations().contains(nearable)) {
                                        openComponent[0] = component;
                                    }
                                });
                            } else if (nearable instanceof Edge) { // We are pressing an edge, find the edge and open the corresponding component
                                Ecdar.getProject().getComponents().forEach(component -> {
                                    if (component.getDisplayableEdges().contains(nearable)) {
                                        openComponent[0] = component;
                                    }
                                });
                            }

                            if (openComponent[0] != null) {
                                if (!MainController.getActiveCanvasPresentation().getController().getActiveModel().equals(openComponent[0])) {
                                    SelectHelper.elementsToBeSelected = FXCollections.observableArrayList();
                                    MainController.getActiveCanvasPresentation().getController().setActiveModel(openComponent[0]);
                                }

                                SelectHelper.clearSelectedElements();
                                SelectHelper.select(nearable);
                            }
                        });
                    }

                    nearLabels.getChildren().add(newNearLabel);

                    final Region spacer = new Region();
                    spacer.setMinWidth(10);

                    nearLabels.getChildren().add(spacer);
                }
            }

            final Label nearLabel = (Label) lookup("#nearLabel");
            nearLabel.setText(nearString);
        };

        // Run the listener now
        listener.invalidated(null);

        // Whenever the list is updated
        message.getNearables().addListener(listener);
    }

}
