package ecdar.controllers;

import ecdar.Ecdar;
import ecdar.abstractions.*;
import ecdar.backend.SimulationHandler;
import ecdar.presentations.SimulatorOverviewPresentation;
import ecdar.simulation.SimulationState;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.Initializable;
import javafx.scene.layout.StackPane;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class SimulatorController implements Initializable {
    public StackPane root;
    private SimulationHandler simulationHandler;
    public SimulatorOverviewPresentation overviewPresentation;
    public StackPane toolbar;

    private boolean firstTimeInSimulator;
    private final static DoubleProperty width = new SimpleDoubleProperty(),
            height = new SimpleDoubleProperty();
    private static ObjectProperty<SimulationState> selectedState = new SimpleObjectProperty<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        root.widthProperty().addListener((observable, oldValue, newValue) -> width.setValue(newValue));
        root.heightProperty().addListener((observable, oldValue, newValue) -> height.setValue(newValue));
        firstTimeInSimulator = true;
        simulationHandler = Ecdar.getSimulationHandler();
    }

    /**
     * Prepares the simulator to be shown.<br />
     * It also prepares the processes to be shown in the {@link SimulatorOverviewPresentation} by: <br />
     * - Building the system if it has been updated or have never been created.<br />
     * - Adding the components which are going to be used in the simulation to
     */
    public void willShow() {
        // If the user left a trace, continue from that trace
        boolean shouldSimulationBeReset = simulationHandler.traceLog.size() < 2;

        // If the composition is not the same as previous simulation, reset the simulation
        if (!(overviewPresentation.getController().getComponentObservableList().hashCode() ==
                findComponentsInCurrentSimulation(simulationHandler.getComponentsInSimulation()).hashCode())) {
            shouldSimulationBeReset = true;
        }
        
        if (shouldSimulationBeReset || firstTimeInSimulator || simulationHandler.currentState.get() == null) {
            resetSimulation();
            simulationHandler.initialStep();
        }

        overviewPresentation.getController().addProcessesToGroup();

        // If the simulation continues, highligt the current state and available edges
        if (simulationHandler.currentState.get() != null && !shouldSimulationBeReset) {
            overviewPresentation.getController().highlightProcessState(simulationHandler.currentState.get());
            overviewPresentation.getController().highlightAvailableEdges(simulationHandler.currentState.get());
        }

    }

    /**
     * Resets the current simulation, and prepares for a new simulation by clearing the
     * {@link SimulatorOverviewController#processContainer} and adding the processes of the new simulation.
     */
    private void resetSimulation() {
        List<Component> listOfComponentsForSimulation = findComponentsInCurrentSimulation(simulationHandler.getComponentsInSimulation());
        overviewPresentation.getController().clearOverview();
        overviewPresentation.getController().getComponentObservableList().clear();
        overviewPresentation.getController().getComponentObservableList().addAll(listOfComponentsForSimulation);
        firstTimeInSimulator = false;
    }
    
    /**
     * Finds the components that are used in the current simulation by looking at the components found in
     * Ecdar.getProject.getComponents() and compares them to the components found in the queryComponents list
     *
     * @return all the components used in the current simulation
     */
    private List<Component> findComponentsInCurrentSimulation(List<String> queryComponents) {
        //Show components from the system
        List<Component> components = Ecdar.getProject().getComponents();

        //Matches query components against with existing components and adds them to simulation
        List<Component> SelectedComponents = new ArrayList<>();
        for(Component comp : components) {
            for(String componentInQuery : queryComponents) {
                if((comp.getName().equals(componentInQuery))) {
                    Component temp = new Component(comp.serialize());
                    SelectedComponents.add(temp);
                }
            }
        }
        simulationHandler.setSimulationComponents((ArrayList<Component>) SelectedComponents);
        return SelectedComponents;
    }

    /**
     * Resets the simulation and prepares the view for showing the new simulation to the user
     */
    public void resetCurrentSimulation() {
        overviewPresentation.getController().removeProcessesFromGroup();
        resetSimulation();
        simulationHandler.resetToInitialLocation();
        overviewPresentation.getController().addProcessesToGroup();
    }

    public void willHide() {
        overviewPresentation.getController().removeProcessesFromGroup();
    }

    public static DoubleProperty getWidthProperty() {
        return width;
    }

    public static DoubleProperty getHeightProperty() {
        return height;
    }

    public static void setSelectedState(SimulationState selectedState) {
        SimulatorController.selectedState.set(selectedState);
    }
}
