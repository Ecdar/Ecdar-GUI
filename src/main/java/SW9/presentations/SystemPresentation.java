package SW9.presentations;

import SW9.abstractions.SystemModel;
import SW9.controllers.SystemModelController;
import javafx.fxml.FXMLLoader;
import javafx.fxml.JavaFXBuilderFactory;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.net.URL;

/**
 *
 */
public class SystemPresentation extends StackPane {
    private final SystemModelController controller;

    public SystemPresentation(final SystemModel systemModel) {
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
        setMinWidth(systemModel.getBox().getWidth());
        setMaxWidth(systemModel.getBox().getWidth());
        setMinHeight(systemModel.getBox().getHeight());
        setMaxHeight(systemModel.getBox().getHeight());
        minHeightProperty().bindBidirectional(systemModel.getBox().heightProperty());
        maxHeightProperty().bindBidirectional(systemModel.getBox().heightProperty());
        minWidthProperty().bindBidirectional(systemModel.getBox().widthProperty());
        maxWidthProperty().bindBidirectional(systemModel.getBox().widthProperty());

        controller = fxmlLoader.getController();
        controller.setSystemModel(systemModel);
    }
}
