package ecdar.controllers;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXComboBox;
import javafx.fxml.Initializable;

import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SimulationInitializationDialogController implements Initializable {
    public JFXComboBox<String> simulationComboBox;
    public JFXButton cancelButton;
    public JFXButton startButton;

    public static List<String> ListOfComponents = new ArrayList<>();
    /**
     * Function gets list of components to simulation
     * and saves it in the public static ListOfComponents
     */
    public void setSimulationData(){
        // set simulation query
        SimulatorController.setSimulationQuery(simulationComboBox.getSelectionModel().getSelectedItem());

        // set list of components involved in simulation
        ListOfComponents.clear();
        // pattern filters out all components by ignoring operators.
        Pattern pattern = Pattern.compile("([\\w]*)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(SimulatorController.getSimulationQuery());
        List<String> listOfComponentsToSimulate = new ArrayList<>();
        //Adds all found components to list.
        while(matcher.find()){
            if(matcher.group().length() != 0) {
                listOfComponentsToSimulate.add(matcher.group());
            }
        }
        ListOfComponents = listOfComponentsToSimulate;
    }

    public void initialize(URL location, ResourceBundle resources) {

    }
}
