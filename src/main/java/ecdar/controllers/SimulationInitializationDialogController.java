package ecdar.controllers;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXComboBox;
import ecdar.Ecdar;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;

import java.net.URL;
import java.util.ResourceBundle;

public class SimulationInitializationDialogController implements Initializable {
    public JFXComboBox<String> simulationComboBox;
    public JFXButton cancelButton;
    public JFXButton startButton;

    public void initialize(URL location, ResourceBundle resources) {

    }
}
