package ecdar.controllers;

import com.jfoenix.controls.JFXTextField;
import ecdar.abstractions.Edge;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;

import java.net.URL;
import java.util.ResourceBundle;

public class SyncTextFieldController implements Initializable {
    public Label label;
    public JFXTextField textField;
    public Edge connectedEdge;

    @Override
    public void initialize(URL location, ResourceBundle resources) {

    }
}
