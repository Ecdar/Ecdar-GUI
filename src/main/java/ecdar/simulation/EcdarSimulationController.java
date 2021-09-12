package ecdar.simulation;

import com.uppaal.model.system.concrete.ConcreteState;
import com.uppaal.model.system.concrete.ConcreteTransition;
import ecdar.Ecdar;
import ecdar.abstractions.*;
import ecdar.backend.BackendException;
import ecdar.backend.BackendHelper;
import ecdar.presentations.LeftSimPanePresentation;
import ecdar.presentations.RightSimPanePresentation;
import ecdar.presentations.SimulatorOverviewPresentation;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class EcdarSimulationController implements Initializable, Presentable {

    public StackPane root;
    public SimulatorOverviewPresentation overviewPresentation;
    public StackPane toolbar;
    public Label rightPaneFillerElement;
    public Label leftPaneFillerElement;
    public Rectangle bottomFillerElement;
    public RightSimPanePresentation rightSimPane;
    public LeftSimPanePresentation leftSimPane;
    private boolean firstTimeInSimulator;

    private final static DoubleProperty width = new SimpleDoubleProperty(),
            height = new SimpleDoubleProperty();
    private static ObjectProperty<ConcreteTransition> selectedTransition = new SimpleObjectProperty<>();
    private static ObjectProperty<ConcreteState> selectedState = new SimpleObjectProperty<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        root.widthProperty().addListener((observable, oldValue, newValue) -> width.setValue(newValue));
        root.heightProperty().addListener((observable, oldValue, newValue) -> height.setValue(newValue));
        firstTimeInSimulator = true;
    }

    /**
     * Prepares the simulator to be shown.<br />
     * It also prepares the processes to be shown in the {@link SimulatorOverviewPresentation} by: <br />
     *  - Building the system if it has been updated or never have been created.<br />
     *  - Adding the components which are going to be used in the simulation to
     */
    @Override
    public void willShow() {
        final EcdarSimulationHandler sm = Ecdar.getSimulationHandler();
        boolean shouldSimulationBeReset = true;

        // ToDo NIELS: Handle the below check to check if the user has actually left
        // Have the user left a trace or is he simulating a query
        if (false) {
            shouldSimulationBeReset = false;
        }

        if(!firstTimeInSimulator && !overviewPresentation.getController().getComponentObservableList()
                .containsAll(findComponentsInCurrentSimulation())) {
            shouldSimulationBeReset = true;
        }

        if (shouldSimulationBeReset || firstTimeInSimulator) {
            try {
                resetSimulation();
            } catch (final BackendException.SystemNotFoundException ex) {
                overviewPresentation.getController().addProcessesToGroup();
                return;
            }
        }
        overviewPresentation.getController().addProcessesToGroup();
        //overviewPresentation.getController().highlightProcessState(sm.getCurrentConcreteState());
    }

    /**
     * Resets the current simulation, and prepares for a new simulation by clearing the
     * {@link EcdarSimulatorOverviewController#processContainer} and adding the processes of the new simulation.
     *
     * @throws BackendException.SystemNotFoundException if the system is not declared or contains syntax errors.
     */
    private void resetSimulation() throws BackendException.SystemNotFoundException {
        final EcdarSimulationHandler sm = Ecdar.getSimulationHandler();
        try {
            BackendHelper.buildEcdarDocument();
          //  sm.initializeDefaultSystem();
        } catch (final BackendException.SystemNotFoundException ex){
            Ecdar.showToast("A system is not declared, or contains syntax errors");
            throw ex;
        } catch (final BackendException ex) {
            //Something is wrong with the system
            System.out.println(ex);
            return;
        }
        overviewPresentation.getController().clearOverview();
        overviewPresentation.getController().getComponentObservableList().clear();
        overviewPresentation.getController().getComponentObservableList().addAll(findComponentsInCurrentSimulation());
        firstTimeInSimulator = false;
    }

    /**
     * Finds the components that are used in the current simulation by looking at the component found in
     *
     * TODO This does currently not work if the same component is used multiple times.
     *
     * @return all the components used in the current simulation
     */
    private List<Component> findComponentsInCurrentSimulation() {
        //Show components from the system
        final EcdarSimulationHandler sm = Ecdar.getSimulationHandler();
        List<Component> components = new ArrayList<>();
//        for (int i = 0; i < sm.getSystem().getNoOfProcesses(); i++) {
//            final int finalI = i; // when using a var in lambda it has to be final
//            final List<Component> filteredList = Ecdar.getProject().getComponents().filtered(component -> {
//                return component.getName().contentEquals(sm.getSystem().getProcess(finalI).getName());
//            });
//            components.addAll(filteredList);
//        }
        return components;
    }

    /**
     * Resets the simulation and prepares the view for showing the new simulation to the user
     */
    public void resetCurrentSimulation() {
        overviewPresentation.getController().removeProcessesFromGroup();
        try {
            resetSimulation();
        }
        catch (final BackendException.SystemNotFoundException ex){
            //system is not declared or contains syntax errors
            overviewPresentation.getController().addProcessesToGroup();
            return;
        }
        overviewPresentation.getController().addProcessesToGroup();
    }

    @Override
    public void willHide() {
        overviewPresentation.getController().removeProcessesFromGroup();
        overviewPresentation.getController().getComponentObservableList().forEach(component -> {
            component.getBox().setX(5.00);
            component.getBox().setY(5.00);
        });
        overviewPresentation.getController().unhighlightProcesses();
    }
    public static DoubleProperty getWidthProperty() {
        return width;
    }

    public static DoubleProperty getHeightProperty() {
        return height;
    }


    public static ObjectProperty<ConcreteTransition> getSelectedTransitionProperty() {
        return selectedTransition;
    }

    public static void setSelectedTransition(ConcreteTransition selectedTransition) {
        EcdarSimulationController.selectedTransition.set(selectedTransition);
    }

    public static ObjectProperty<ConcreteState> getSelectedStateProperty() {
        return selectedState;
    }

    public static void setSelectedState(ConcreteState selectedState) {
        EcdarSimulationController.selectedState.set(selectedState);
    }
}
