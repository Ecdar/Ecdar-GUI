
package ecdar.presentations;

import javafx.scene.layout.AnchorPane;

public class BackgroundThreadPresentation extends AnchorPane {

    public BackgroundThreadPresentation() {
        new EcdarFXMLLoader().loadAndGetController("BackgroundThreadPresentation.fxml", this);
    }
}
