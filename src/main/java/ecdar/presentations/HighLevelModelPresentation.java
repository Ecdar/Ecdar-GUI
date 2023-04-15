package ecdar.presentations;

import ecdar.controllers.HighLevelModelController;
import javafx.scene.layout.StackPane;

/**
 *
 */
public abstract class HighLevelModelPresentation extends StackPane {
    abstract public HighLevelModelController getController();
}
