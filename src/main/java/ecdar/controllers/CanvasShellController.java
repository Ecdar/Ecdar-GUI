package ecdar.controllers;

import com.jfoenix.controls.JFXRippler;

import ecdar.abstractions.Component;
import ecdar.abstractions.EcdarSystem;
import ecdar.presentations.CanvasPresentation;
import ecdar.presentations.Grid;
import ecdar.utility.helpers.ZoomHelper;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;

import java.net.URL;
import java.util.ResourceBundle;

public class CanvasShellController implements Initializable {
    public Pane root;
    public HBox toolbar;
    public CanvasPresentation canvasPresentation;
    public Grid grid;

    public JFXRippler zoomIn;
    public JFXRippler zoomOut;
    public JFXRippler zoomToFit;
    public JFXRippler resetZoom;

    public final ZoomHelper zoomHelper = new ZoomHelper();
    // This is whether to allow the user to turn on/off the grid.
    // While this is false, the grid is always hidden, no matter the user option.
    private final BooleanProperty allowGrid = new SimpleBooleanProperty(true);

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        canvasPresentation.getController().activeComponentProperty().addListener(((observable, oldValue, newValue) -> {
            boolean shouldZoomBeActive = newValue instanceof Component || newValue instanceof EcdarSystem;
            toolbar.setVisible(shouldZoomBeActive);
            zoomHelper.setActive(shouldZoomBeActive);
        }));
    }

    /**
     * Allows the user to turn the grid on/off.
     * If the user has currently chosen on, then this method also shows the grid.
     */
    public void allowGrid() {
        allowGrid.set(true);
    }

    /**
     * Disallows the user to turn the grid on/off.
     * Also hides the grid.
     */
    public void disallowGrid() {
        allowGrid.set(false);
    }

    public BooleanProperty allowGridProperty() {
        return allowGrid;
    }

    public boolean isGridAllowed() {
        return allowGrid.get();
    }

    @FXML
    private void zoomInClicked() {
        zoomHelper.zoomIn();
    }

    @FXML
    private void zoomOutClicked() {
        zoomHelper.zoomOut();
    }

    @FXML
    private void zoomToFitClicked() {
        zoomHelper.zoomToFit();
    }

    @FXML
    private void resetZoomClicked() {
        zoomHelper.resetZoom();
    }
}
