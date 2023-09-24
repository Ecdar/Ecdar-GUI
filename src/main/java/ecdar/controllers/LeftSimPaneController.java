package ecdar.controllers;

import ecdar.presentations.StatePresentation;
import ecdar.presentations.TracePanePresentation;
import ecdar.utility.colors.Color;
import javafx.collections.ObservableList;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.layout.*;

import java.net.URL;
import java.util.ResourceBundle;

public class LeftSimPaneController implements Initializable {
    public StackPane root;
    public VBox content;

    public TracePanePresentation tracePanePresentation;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initializeBackground();
        initializeRightBorder();
    }

    private void initializeBackground() {
        content.setBackground(new Background(new BackgroundFill(
                Color.GREY.getColor(Color.Intensity.I200),
                CornerRadii.EMPTY,
                Insets.EMPTY
        )));
    }

    /**
     * Initializes the thin border on the right side of the transition toolbar
     */
    private void initializeRightBorder() {
        tracePanePresentation.getController().toolbar.setBorder(new Border(new BorderStroke(
                Color.GREY_BLUE.getColor(Color.Intensity.I900),
                BorderStrokeStyle.SOLID,
                CornerRadii.EMPTY,
                new BorderWidths(0, 1, 0, 0)
        )));
    }

    public void setTraceLog(ObservableList<StatePresentation> traceLog) {
        tracePanePresentation.getController().setTraceLog(traceLog);
    }
}
