package ecdar.controllers;

import com.jfoenix.controls.JFXRippler;
import javafx.fxml.Initializable;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Controller for a file in the project pane.
 */
public class FileController implements Initializable {
    public JFXRippler moreInformation;
    public ImageView fileImage;
    public StackPane filePane;

    @Override
    public void initialize(URL location, ResourceBundle resources) {

    }
}
