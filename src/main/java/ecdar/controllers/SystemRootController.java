package ecdar.controllers;

import ecdar.abstractions.SystemEdge;
import ecdar.abstractions.EcdarSystem;
import ecdar.abstractions.SystemRoot;
import ecdar.presentations.DropDownMenu;
import ecdar.presentations.MenuElement;
import com.jfoenix.controls.JFXPopup;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Group;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Polygon;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Controller for a system root.
 */
public class SystemRootController implements Initializable {
    public StackPane presentationRoot;
    public Polygon shape;
    public StackPane contextMenuContainer;
    public Group contextMenuSource;

    private SystemRoot systemRoot;
    private final ObjectProperty<EcdarSystem> system = new SimpleObjectProperty<>();

    private DropDownMenu contextMenu;
    private final BooleanProperty hasEdge = new SimpleBooleanProperty(false);


    // Properties

    public SystemRoot getSystemRoot() {
        return systemRoot;
    }

    public void setSystemRoot(final SystemRoot systemRoot) {
        this.systemRoot = systemRoot;
    }

    public EcdarSystem getSystem() {
        return system.get();
    }

    public void setSystem(final EcdarSystem system) {
        this.system.set(system);
    }


    public boolean hasEdge() {
        return hasEdge.get();
    }

    // Initialization

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        system.addListener(((observable, oldValue, newValue) -> {
            initializeDropDownMenu(newValue);
        }));
    }

    private void initializeDropDownMenu(final EcdarSystem system) {
        contextMenu = new DropDownMenu(contextMenuSource);

        contextMenu.addMenuElement(new MenuElement("Draw Edge")
                .setClickable(() -> {
                    createNewSystemEdge();

                    contextMenu.hide();
                })
                .setDisableable(hasEdge));
    }

    /**
     * Listens to an edge to update whether the root has an edge.
     * @param edge the edge to update with
     */
    private void handleHasEdge(final SystemEdge edge) {
        edge.getTempNodeProperty().addListener((observable -> updateHasEdge(edge)));
        edge.getChildProperty().addListener((observable -> updateHasEdge(edge)));
        edge.getParentProperty().addListener((observable -> updateHasEdge(edge)));
    }

    /**
     * Update has edge property to whether the root is in a given edge.
     * @param edge the given edge
     */
    private void updateHasEdge(final SystemEdge edge) {
        hasEdge.set(edge.isInEdge(getSystemRoot()));
    }

    @FXML
    private void onMouseClicked(final MouseEvent event) {
        event.consume();

        final SystemEdge unfinishedEdge = getSystem().getUnfinishedEdge();

        if ((event.isShiftDown() && event.getButton().equals(MouseButton.PRIMARY)) || event.getButton().equals(MouseButton.MIDDLE)) {
            // If shift click or middle click a component instance, create a new edge

            // Component instance must not already have an edge and there cannot be any other unfinished edges in the system
            if (!hasEdge.get() && unfinishedEdge == null) {
                createNewSystemEdge();
            }
        }

        // if primary clicked and there is an unfinished edge, finish it with the system root as target
        if (unfinishedEdge != null && event.getButton().equals(MouseButton.PRIMARY)) {
            final boolean succeeded = unfinishedEdge.tryFinishWithRoot(this);
            if (succeeded) {
                hasEdge.set(true);
                handleHasEdge(unfinishedEdge);
            }
            return;
        }

        // If secondary clicked, show context menu
        if (event.getButton().equals(MouseButton.SECONDARY)) {
            contextMenu.show(JFXPopup.PopupVPosition.TOP, JFXPopup.PopupHPosition.LEFT, 0, 0);
        }
    }

    /***
     * Helper method to create a new SystemEdge and add it to the current system and system root
     * @return The newly created SystemEdge
     */
    private SystemEdge createNewSystemEdge() {
        final SystemEdge edge = new SystemEdge(systemRoot);
        getSystem().addEdge(edge);
        hasEdge.set(true);
        handleHasEdge(edge);
        
        return edge;
    }
}
