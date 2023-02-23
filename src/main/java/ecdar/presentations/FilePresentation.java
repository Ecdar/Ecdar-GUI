package ecdar.presentations;

import ecdar.Ecdar;
import ecdar.abstractions.HighLevelModel;
import ecdar.controllers.FileController;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.layout.*;

public class FilePresentation extends AnchorPane {
    private final FileController controller;

    public FilePresentation(final HighLevelModel model) {
        controller = new EcdarFXMLLoader().loadAndGetController("FilePresentation.fxml", this);
        controller.setModel(model);

        // Ensure that the icons are scaled to current font scale
        Platform.runLater(() -> Ecdar.getPresentation().getController().scaleIcons(this));
    }

    public FileController getController() {
        return controller;
    }
}
