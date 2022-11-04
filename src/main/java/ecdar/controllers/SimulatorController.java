package ecdar.controllers;

import ecdar.Ecdar;
import ecdar.abstractions.*;
import ecdar.presentations.SimulationInitializationDialogPresentation;
import ecdar.simulation.SimulationHandler;
import ecdar.presentations.SimulatorOverviewPresentation;
import ecdar.simulation.SimulationState;
import ecdar.simulation.Transition;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.Initializable;
import javafx.scene.layout.StackPane;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.ResourceBundle;

public class SimulatorController implements Initializable {
    public StackPane root;
    public SimulatorOverviewPresentation overviewPresentation;
    public StackPane toolbar;

    private boolean firstTimeInSimulator;
    private final static DoubleProperty width = new SimpleDoubleProperty(),
            height = new SimpleDoubleProperty();
    private static ObjectProperty<Transition> selectedTransition = new SimpleObjectProperty<>();
    private static ObjectProperty<SimulationState> selectedState = new SimpleObjectProperty<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        root.widthProperty().addListener((observable, oldValue, newValue) -> width.setValue(newValue));
        root.heightProperty().addListener((observable, oldValue, newValue) -> height.setValue(newValue));
        firstTimeInSimulator = true;
    }

    /**
     * Prepares the simulator to be shown.<br />
     * It also prepares the processes to be shown in the {@link SimulatorOverviewPresentation} by: <br />
     * - Building the system if it has been updated or have never been created.<br />
     * - Adding the components which are going to be used in the simulation to
     */
    public void willShow() {
        final SimulationHandler sm = Ecdar.getSimulationHandler();
        boolean shouldSimulationBeReset = true;

        if (sm.getCurrentState() == null) sm.initialStep(); // ToDo NIELS: Find better solution

        //Have the user left a trace or is he simulating a query
        if (sm.traceLog.size() >= 2 || sm.getCurrentSimulation().contains(SimulationHandler.QUERY_PREFIX)) {
            shouldSimulationBeReset = false;
        }

        if (!firstTimeInSimulator && !new HashSet<>(overviewPresentation.getController().getComponentObservableList())
                .containsAll(findComponentsInCurrentSimulation(SimulationInitializationDialogController.ListOfComponents))) {
            shouldSimulationBeReset = true;
        }

        if (shouldSimulationBeReset || firstTimeInSimulator) {

            resetSimulation();
            sm.resetToInitialLocation();
        }
        overviewPresentation.getController().addProcessesToGroup();
        overviewPresentation.getController().highlightProcessState(sm.getCurrentState());
    }

    /**
     * Resets the current simulation, and prepares for a new simulation by clearing the
     * {@link SimulatorOverviewController#processContainer} and adding the processes of the new simulation.
     */
    private void resetSimulation() {
        final SimulationHandler sm = Ecdar.getSimulationHandler();
        sm.initializeDefaultSystem();

        overviewPresentation.getController().clearOverview();
        overviewPresentation.getController().getComponentObservableList().clear();
        overviewPresentation.getController().getComponentObservableList().addAll(findComponentsInCurrentSimulation(SimulationInitializationDialogController.ListOfComponents));
        firstTimeInSimulator = false;
    }

    /**
     * Finds the components that are used in the current simulation by looking at the component found in
     * {@link Project#getComponents()} and compare them to the processes declared in the {@link SimulationHandler#getSystem()}
     * <p>
     * TODO This does currently not work if the same component is used multiple times.
     *
     * @return all the components used in the current simulation
     */
    private List<Component> findComponentsInCurrentSimulation(List<String> queryComponents) {
        //Show components from the system
        final SimulationHandler sm = Ecdar.getSimulationHandler();
        List<Component> components = new ArrayList<>();
        components = Ecdar.getProject().getComponents();

        //Matches query components against with existing components and adds them to simulation
        List<Component> SelectedComponents = new ArrayList<>();
        for(Component comp:components) {
            for(String componentInQuery : queryComponents) {
                if((comp.getName().equals(componentInQuery))) {
                    SelectedComponents.add(comp);
                }
            }
        }
        return SelectedComponents;
    }

    /**
     * Resets the simulation and prepares the view for showing the new simulation to the user
     */
    public void resetCurrentSimulation() {
        overviewPresentation.getController().removeProcessesFromGroup();
        resetSimulation();
        Ecdar.getSimulationHandler().resetToInitialLocation();
        overviewPresentation.getController().addProcessesToGroup();
    }

    public void willHide() {
        overviewPresentation.getController().removeProcessesFromGroup();
        overviewPresentation.getController().getComponentObservableList().forEach(component -> {
            // Previously reset coordinates of component box
        });
        overviewPresentation.getController().unhighlightProcesses();
    }

    public static DoubleProperty getWidthProperty() {
        return width;
    }

    public static DoubleProperty getHeightProperty() {
        return height;
    }


    public static ObjectProperty<Transition> getSelectedTransitionProperty() {
        return selectedTransition;
    }

    public static void setSelectedTransition(Transition selectedTransition) {
        SimulatorController.selectedTransition.set(selectedTransition);
    }

    public static ObjectProperty<SimulationState> getSelectedStateProperty() {
        return selectedState;
    }

    public static void setSelectedState(SimulationState selectedState) {
        SimulatorController.selectedState.set(selectedState);
    }
}
