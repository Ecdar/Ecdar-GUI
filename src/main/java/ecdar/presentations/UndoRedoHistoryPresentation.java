package ecdar.presentations;

import javafx.scene.layout.AnchorPane;

public class UndoRedoHistoryPresentation extends AnchorPane {

    public UndoRedoHistoryPresentation() {
        new EcdarFXMLLoader().loadAndGetController("UndoRedoHistoryPresentation.fxml", this);
    }
}
