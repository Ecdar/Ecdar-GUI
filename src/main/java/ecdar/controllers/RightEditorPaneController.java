package ecdar.controllers;

import ecdar.presentations.QueryPanePresentation;
import javafx.fxml.Initializable;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Controller class for the right pane in the editor
 */
public class RightEditorPaneController implements Initializable {
    public StackPane root;
    public VBox scrollPaneVbox;
    public QueryPanePresentation queryPane;

    @Override
    public void initialize(URL location, ResourceBundle resources) {

    }
}
