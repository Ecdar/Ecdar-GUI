package SW9.presentations;

import SW9.abstractions.System;
import SW9.controllers.ComponentController;
import SW9.controllers.SystemController;
import javafx.fxml.FXMLLoader;
import javafx.fxml.JavaFXBuilderFactory;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.net.URL;

/**
 *
 */
public class SystemPresentation extends StackPane {
    private final SystemController controller;

    public SystemPresentation(final System system) {
        final URL url = this.getClass().getResource("ComponentPresentation.fxml");

        final FXMLLoader fxmlLoader = new FXMLLoader();
        fxmlLoader.setLocation(url);
        fxmlLoader.setBuilderFactory(new JavaFXBuilderFactory());

        fxmlLoader.setRoot(this);
        try {
            fxmlLoader.load(url.openStream());
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        }

        // Set the width and the height of the view to the values in the abstraction
        setMinWidth(system.getWidth());
        setMaxWidth(system.getWidth());
        setMinHeight(system.getHeight());
        setMaxHeight(system.getHeight());
        minHeightProperty().bindBidirectional(system.heightProperty());
        maxHeightProperty().bindBidirectional(system.heightProperty());
        minWidthProperty().bindBidirectional(system.widthProperty());
        maxWidthProperty().bindBidirectional(system.widthProperty());

        controller = fxmlLoader.getController();
        controller.setSystem(system);
    }
}
