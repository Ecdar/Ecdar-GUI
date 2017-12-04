package SW9.controllers;

import SW9.abstractions.HighLevelModelObject;
import SW9.abstractions.SystemModel;
import javafx.fxml.FXML;
import javafx.scene.input.MouseEvent;
import javafx.scene.shape.Line;

/**
 *
 */
public class SystemController extends ModelController {
    private SystemModel system;
    public Line topRightLine;

    public SystemModel getSystem() {
        return system;
    }

    public void setSystem(SystemModel system) {
        this.system = system;
    }

    @FXML
    private void modelContainerPressed(final MouseEvent event) {

    }

    @Override
    public HighLevelModelObject getModel() {
        return system;
    }
}
