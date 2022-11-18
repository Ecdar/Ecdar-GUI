package ecdar.controllers;
import ecdar.abstractions.ComponentOperator;
import ecdar.abstractions.EcdarSystem;
import ecdar.abstractions.EcdarSystemEdge;
import ecdar.presentations.DropDownMenu;
import ecdar.presentations.MenuElement;
import com.jfoenix.controls.JFXPopup;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Controller for ComponentOperators
 */
public class ComponentOperatorController implements Initializable {
    public Polygon frame;
    public StackPane root;
    public Label label;

    private final ObjectProperty<EcdarSystem> system = new SimpleObjectProperty<>();
    private ComponentOperator operator;
    private final BooleanProperty hasParent = new SimpleBooleanProperty(false);
    private Circle dropDownMenuHelperCircle;


    // Properties

    public ComponentOperator getOperator() { return operator; }

    public void setOperator(final ComponentOperator operator) {
        this.operator = operator;
    }

    public void setSystem(final EcdarSystem system) {
        this.system.set(system);
    }

    public EcdarSystem getSystem() {
        return system.get();
    }


    // Initialize

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        system.addListener(((observable, oldValue, newValue) -> {
            initializeDropDownMenu();
        }));
    }

    private void initializeDropDownMenu() {
        dropDownMenuHelperCircle = new Circle(5);
        dropDownMenuHelperCircle.setOpacity(0);
        dropDownMenuHelperCircle.setMouseTransparent(true);
        root.getChildren().add(dropDownMenuHelperCircle);
    }

    /**
     * Shows a context menu.
     * This method creates the menu object itself
     * (rather than having it be created in an initialize method),
     * since the parent of the root is not yet defined when initializing.
     * @param mouseEvent TODO write this
     */
    private void showContextMenu(MouseEvent mouseEvent) {
        dropDownMenuHelperCircle.setLayoutX(mouseEvent.getX());
        dropDownMenuHelperCircle.setLayoutY(mouseEvent.getY());

        final DropDownMenu contextMenu = new DropDownMenu(dropDownMenuHelperCircle);

        contextMenu.addMenuElement(new MenuElement("Draw Edge")
                .setClickable(() -> {
                    final EcdarSystemEdge edge = new EcdarSystemEdge(operator);
                    getSystem().addEdge(edge);

                    contextMenu.hide();
                }));

        contextMenu.addSpacerElement();

        contextMenu.addMenuElement(new MenuElement("Delete")
                .setClickable(() -> {
                    getSystem().removeComponentOperator(getOperator());
                    contextMenu.hide();
                }));

        contextMenu.show(JFXPopup.PopupVPosition.TOP, JFXPopup.PopupHPosition.LEFT, 0, 0);
    }

    /**
     * Listens to an edge to update whether the operator has a parent edge.
     * @param parentEdge the edge to update with
     */
    private void updateHasParent(final EcdarSystemEdge parentEdge) {
        // The operator has a parent iff the supposed parent edge has the operator as a child
        parentEdge.getChildProperty().addListener(((observable, oldValue, newValue) -> hasParent.set(getOperator().equals(newValue))));
    }

    /**
     * Handles mouse clicked event on the view of the operator.
     * @param event the mouse event
     */
    @FXML
    private void onMouseClicked(final MouseEvent event) {
        event.consume();

        final EcdarSystemEdge unfinishedEdge = getSystem().getUnfinishedEdge();

        // if primary clicked and there is an unfinished edge, finish it with the system root as target
        if (unfinishedEdge != null && event.getButton().equals(MouseButton.PRIMARY)) {
            final boolean succeeded = unfinishedEdge.tryFinishWithOperator(getOperator());

            // If succeeded and the operator became a child, update parent property
            // (since operators are only allowed to have one parent)
            if (succeeded && unfinishedEdge.getChild().equals(getOperator())) {
                hasParent.set(true);
                updateHasParent(unfinishedEdge);
            }

            return;
        }

        // If secondary clicked, show context menu
        if (event.getButton().equals(MouseButton.SECONDARY)) {
            showContextMenu(event);
        }
    }
}
