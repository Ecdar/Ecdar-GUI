package SW9.presentations;

import javafx.fxml.FXMLLoader;
import javafx.fxml.JavaFXBuilderFactory;
import javafx.scene.layout.AnchorPane;

import java.io.IOException;
import java.net.URL;

public class UndoRedoHistoryPresentation extends AnchorPane {

    public UndoRedoHistoryPresentation() {
        new EcdarFXMLLoader().loadAndGetController("UndoRedoHistoryPresentation.fxml", this);
    }
}
