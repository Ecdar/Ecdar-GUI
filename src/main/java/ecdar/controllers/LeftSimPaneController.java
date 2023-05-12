package ecdar.controllers;

import ecdar.presentations.StatePresentation;
import ecdar.presentations.TracePanePresentation;
import ecdar.presentations.StatePanePresentation;
import ecdar.utility.colors.Color;
import javafx.collections.ObservableList;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;

import java.net.URL;
import java.util.ResourceBundle;

public class LeftSimPaneController implements Initializable {
    public StackPane root;
    public ScrollPane scrollPane;
    public VBox scrollPaneVbox;

    public StatePanePresentation statePanePresentation;
    public TracePanePresentation tracePanePresentation;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initializeBackground();
        initializeRightBorder();
    }

    private void initializeBackground() {
        scrollPaneVbox.setBackground(new Background(new BackgroundFill(
                Color.GREY.getColor(Color.Intensity.I200),
                CornerRadii.EMPTY,
                Insets.EMPTY
        )));
    }

    /**
     * Initializes the thin border on the right side of the transition toolbar
     */
    private void initializeRightBorder() {
        statePanePresentation.getController().toolbar.setBorder(new Border(new BorderStroke(
                Color.GREY_BLUE.getColor(Color.Intensity.I900),
                BorderStrokeStyle.SOLID,
                CornerRadii.EMPTY,
                new BorderWidths(0, 1, 0, 0)
        )));
    }

    public void setTraceLog(ObservableList<StatePresentation> traceLog) {
        statePanePresentation.getController().setTraceLog(traceLog);
        tracePanePresentation.getController().setTraceLog(traceLog);
    }
}
