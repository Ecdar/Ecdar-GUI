package ecdar.controllers;

import com.jfoenix.controls.JFXRippler;
import ecdar.presentations.CanvasPresentation;
import ecdar.utility.helpers.ZoomHelper;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;

import java.net.URL;
import java.util.ResourceBundle;

public class CanvasShellController implements Initializable {
    public Pane root;

    public StackPane toolbar;
    public JFXRippler zoomIn;
    public JFXRippler zoomOut;
    public JFXRippler zoomToFit;
    public JFXRippler resetZoom;

    public final ZoomHelper zoomHelper = new ZoomHelper();

    public CanvasPresentation canvasPresentation;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        canvasPresentation = new CanvasPresentation();
        zoomHelper.setCanvas(this.canvasPresentation);
        zoomHelper.setGrid(this.canvasPresentation.getGrid());
        //ToDo: bind width and height of canvasPresentation to the width and height of this
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
