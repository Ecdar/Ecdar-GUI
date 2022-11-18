package ecdar.controllers;

import ecdar.presentations.TracePaneElementPresentation;
import ecdar.presentations.TransitionPaneElementPresentation;
import javafx.fxml.Initializable;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import java.net.URL;
import java.util.ResourceBundle;

public class LeftSimPaneController implements Initializable {
    public StackPane root;
    public ScrollPane scrollPane;
    public VBox scrollPaneVbox;

    public TransitionPaneElementPresentation transitionPanePresentation;
    public TracePaneElementPresentation tracePanePresentation;

    @Override
    public void initialize(URL location, ResourceBundle resources) {

    }
}
