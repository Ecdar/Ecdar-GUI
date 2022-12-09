package ecdar.controllers;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXComboBox;
import ecdar.Ecdar;
import ecdar.backend.SimulationHandler;
import javafx.fxml.Initializable;

import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SimulationInitializationDialogController implements Initializable {
    public JFXComboBox<String> simulationComboBox;
    public JFXButton cancelButton;
    public JFXButton startButton;

    private SimulationHandler simulationHandler;

    /**
     * Function extracts data from simulation initialization (query and list of components to simulation)
     * and saves it
     */
    public void setSimulationData(){
        // set simulation query
        simulationHandler.setSimulationQuery(simulationComboBox.getSelectionModel().getSelectedItem());

        // set list of components involved in simulation
        simulationHandler.clearComponentsInSimulation();
        // pattern filters out all components by ignoring operators.
        Pattern pattern = Pattern.compile("([\\w]*)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(simulationHandler.getSimulationQuery());
        List<String> listOfComponentsToSimulate = new ArrayList<>();
        //Adds all found components to list.
        while(matcher.find()){
            if(matcher.group().length() != 0) {
                listOfComponentsToSimulate.add(matcher.group());
            }
        }
        simulationHandler.setComponentsInSimulation(listOfComponentsToSimulate);
    }

    public void initialize(URL location, ResourceBundle resources) {
        simulationHandler = Ecdar.getSimulationHandler();
    }
}
