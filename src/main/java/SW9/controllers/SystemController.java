package SW9.controllers;

import SW9.abstractions.HighLevelModelObject;
import SW9.abstractions.SystemModel;
import SW9.presentations.DropDownMenu;
import com.jfoenix.controls.JFXPopup;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.input.MouseEvent;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Controller for a system.
 */
public class SystemController extends ModelController implements Initializable {
    private final ObjectProperty<SystemModel> system;
    public Line topRightLine;

    private Circle dropDownMenuHelperCircle;
    private DropDownMenu contextMenu;

    public SystemController() {
        system = new SimpleObjectProperty<>();
    }

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        system.addListener((observable, oldValue, newValue) -> initializeSystemContextMenu());
    }

    public SystemModel getSystem() {
        return system.get();
    }

    public void setSystem(final SystemModel system) {
        this.system.setValue(system);
    }

    @FXML
    private void modelContainerPressed(final MouseEvent event) {
        event.consume();
        CanvasController.leaveTextAreas();

        if (event.isSecondaryButtonDown()) {
            dropDownMenuHelperCircle.setLayoutX(event.getX());
            dropDownMenuHelperCircle.setLayoutY(event.getY());
            DropDownMenu.x = event.getX();
            DropDownMenu.y = event.getY();

            contextMenu.show(JFXPopup.PopupVPosition.TOP, JFXPopup.PopupHPosition.LEFT, 0, 0);
        }
    }

    @Override
    public HighLevelModelObject getModel() {
        return getSystem();
    }

    private void initializeSystemContextMenu() {
        dropDownMenuHelperCircle = new Circle(5);
        dropDownMenuHelperCircle.setOpacity(0);
        dropDownMenuHelperCircle.setMouseTransparent(true);

        root.getChildren().add(dropDownMenuHelperCircle);

        contextMenu = new DropDownMenu(root, dropDownMenuHelperCircle, 230, true);

        contextMenu.addColorPicker(system.get(), system.get()::dye);
    }

    /**
     * Hides the border and background.
     */
    @Override
    void hideBorderAndBackground() {
        super.hideBorderAndBackground();
        topRightLine.setVisible(false);
    }

    /**
     * Shows the border and background.
     */
    @Override
    void showBorderAndBackground() {
        super.showBorderAndBackground();
        topRightLine.setVisible(true);
    }
}
