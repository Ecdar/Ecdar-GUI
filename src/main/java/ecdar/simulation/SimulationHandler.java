package ecdar.simulation;

import ecdar.Ecdar;
import ecdar.abstractions.*;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Map;

/**
 * Handles state changes, updates of values / clocks, and keeps track of all the transitions that
 * have been taken throughout a simulation.
 */
public class SimulationHandler {
    public static final String QUERY_PREFIX = "Query: ";
    private ObjectProperty<SimulationState> currentConcreteState;
    private ObjectProperty<SimulationState> initialConcreteState;
    private ObjectProperty<BigDecimal> currentTime = new SimpleObjectProperty<>();
    private BigDecimal delay;
    private ArrayList<Edge> edgesSelected;
    private EcdarSystem system;
    private SimulationStateSuccessor successor;
    private int numberOfSteps;

    /**
     * A string to keep track what is currently being simulated
     * For now the string is prefixed with {@link #QUERY_PREFIX} when doing a query simulation
     * and kept empty when doing system simulations
     */
    private String currentSimulation = "";

    private final ObservableMap<String, BigDecimal> simulationVariables = FXCollections.observableHashMap();
    private final ObservableMap<String, BigDecimal> simulationClocks = FXCollections.observableHashMap();
    /**
     * For some reason the successor.getTransitions() only sometimes returns some of the transitions
     * that are available, when running the initial step.
     * That is why we need to keep track of the initial transitions.
     */
    private final ObservableList<Transition> initialTransitions = FXCollections.observableArrayList();
    public ObservableList<SimulationState> traceLog = FXCollections.observableArrayList();
    public ObservableList<Transition> availableTransitions = FXCollections.observableArrayList();

    /**
     * Empty constructor that should be used if the system or project has not be initialized yet
     */
    public SimulationHandler() {

    }

    /**
     * Initializes the default system (non-query system)
     */
    public void initializeDefaultSystem() {
        currentSimulation = "";
    }

    /**
     * Initializes the values and properties in the {@link SimulationHandler}.
     * Can also be used as a reset of the simulation.
     * THIS METHOD DOES NOT RESET THE ENGINE,
     */
    private void initializeSimulation() {
        // Initialization
        this.delay = new BigDecimal(0);
        this.edgesSelected = new ArrayList<>();
        this.numberOfSteps = 0;
        this.availableTransitions.clear();
        this.simulationVariables.clear();
        this.simulationClocks.clear();
        this.traceLog.clear();
        this.currentConcreteState = new SimpleObjectProperty<>(getInitialConcreteState());
        this.initialConcreteState = new SimpleObjectProperty<>(getInitialConcreteState());
        this.currentTime = new SimpleObjectProperty<>(BigDecimal.ZERO);

        //Preparation for the simulation
        this.system = getSystem();
        this.currentConcreteState.get().setTime(currentTime.getValue());
        this.initialTransitions.clear();
        this.successor = null;
    }

    /**
     * Reloads the whole simulation sets the initial transitions, states, etc
     */
    public void initialStep() {
        initializeSimulation();
        final SimulationState currentState = currentConcreteState.get();
        successor = getStateSuccessor();

        //Save the previous states, and get the new
        currentConcreteState.set(successor.getState());
        this.traceLog.add(currentState);
        numberOfSteps++;

        //Updates the transitions available
        availableTransitions.addAll(FXCollections.observableArrayList(successor.getTransitions()));
        initialTransitions.addAll(availableTransitions);
        updateAllValues();
    }

    /**
     * Resets the simulation to the initial location
     * where the <code>SimulationState</code> is the {@link SimulationHandler#initialConcreteState}, when there are
     * elements in the {@link SimulationHandler#traceLog}. Otherwise it calls {@link SimulationHandler#initialStep}
     */
    public void resetToInitialLocation() {
        //If the simulation has not begone
        if (traceLog.size() == 0)
            initialStep();
        else
            selectTransitionFromLog(initialConcreteState.get());
    }

    /**
     * Resets the simulation to the state after executing the given transition. <br />
     * This method also resets the state, variables, and clocks to the values they had after the given transition.
     * This also updates {@link SimulationHandler#availableTransitions} such that
     * it displays the available transitions after taking the given transition.
     *
     * @param transition the transition which the simulation should go back to
     */
    public void selectTransitionFromLog(final SimulationState transition) {
        final int indexInTrace = traceLog.indexOf(transition);
        final SimulationState selectedState;
        if (indexInTrace == -1) {
            System.out.println("Cannot find transition: " + transition);
            Ecdar.showToast("Cannot find transition: " + transition);
            return;
        } else if (indexInTrace == numberOfSteps - 1) {
            return; //you have selected the current system
        } else {
            selectedState = traceLog.get(indexInTrace);
        }
        final int sizeOfTraceLog = traceLog.size();
        final int maxRetries = 3;
        int numberOfRetries = 0;
        edgesSelected = new ArrayList<>();
        //In case that we fail we have to save the time we had before
        final BigDecimal tempTime = currentTime.get();

        currentTime.setValue(new BigDecimal(selectedState.getTime().doubleValue()));
        successor.getState().setTime(currentTime.getValue());

        while (numberOfRetries < maxRetries) {
            successor = getStateSuccessor();
            break;
        }
        currentConcreteState.set(selectedState);
        setSimVarAndClocks();
        traceLog.remove(indexInTrace + 1, sizeOfTraceLog);
        availableTransitions.clear();

        // If the user selected the initial/first state in the trace log, we do not trust the engine,
        // as it only gives us a subset of the available transitions, in some cases.
        if (indexInTrace == 0) availableTransitions.addAll(initialTransitions);
        else availableTransitions.addAll(successor.getTransitions());

        numberOfSteps = indexInTrace + 1;
    }

    /**
     * Take a step in the simulation.
     *
     * @param selectedTransitionIndex the index of the availableTransition that you want to take.
     * @param delay              the time which should pass after the transition.
     */
    public void nextStep(final int selectedTransitionIndex, final BigDecimal delay) {
        if (selectedTransitionIndex > availableTransitions.size()) {
            Ecdar.showToast("The selected transition index: " + selectedTransitionIndex + " is bigger than it should: " + availableTransitions);
            return;
        }

        final Transition selectedTransition = availableTransitions.get(selectedTransitionIndex);
        edgesSelected = new ArrayList<>();

        //Preparing for the step
        for (int i = 0; i < selectedTransition.getEdges().size(); i++) {
            edgesSelected.set(i, selectedTransition.getEdges().get(i));
        }

        final int maxRetries = 3;
        int numberOfRetries = 0;

        // getConcreteSuccessor may throw a "ProtocolException: Word expected" but in some cases calling the same
        // method again does not throw this exception, and actually gives us the expected result.
        // This loop calls the method a number of times (maxRetries)
        while (numberOfRetries < maxRetries) {
            successor = getStateSuccessor();
            // Break from the loop if the method call was a success
            break;
        }

        //Save the previous states, and get the new
        currentConcreteState.set(successor.getState());
        this.traceLog.add(currentConcreteState.get());

        // increments the number of steps taken during this simulation
        numberOfSteps++;

        //Updates the transitions available
        availableTransitions.clear();
        availableTransitions.setAll(successor.getTransitions());
        this.delay = delay;
        updateAllValues();
    }

    private SimulationStateSuccessor getStateSuccessor() {
        // ToDo: Implement
        return new SimulationStateSuccessor();
    }

    /**
     * An overload of {@link SimulationHandler#nextStep(int, BigDecimal)} where the delay is 0.
     *
     * @param selectedTransition the index of the availableTransition that you want to take.
     */
    public void nextStep(final int selectedTransition) {
        nextStep(selectedTransition, BigDecimal.ZERO);
    }

    public void nextStep(final Transition transition, final BigDecimal delay) {
        int index = availableTransitions.indexOf(transition);
        if (index != -1) {
            nextStep(index, delay);
        }
    }

    /**
     * Updates all values and clocks that are used doing the current simulation.
     * It also stores the variables in the {@link SimulationHandler#simulationVariables}
     * and the clocks in {@link SimulationHandler#simulationClocks}.
     */
    private void updateAllValues() {
        currentTime.set(currentTime.get().add(delay));
        successor.getState().setTime(currentTime.get());
        setSimVarAndClocks();
    }

    /**
     * Sets the value of simulation variables and clocks, based on {@link SimulationHandler#currentConcreteState}
     */
    private void setSimVarAndClocks() {
        // The variables and clocks are all found in the getVariables array
        // the array is always of the following order: variables, clocks.
        // The noOfVars variable thus also functions as an offset for the clocks in the getVariables array
//        final int noOfClocks = engine.getSystem().getNoOfClocks();
//        final int noOfVars = engine.getSystem().getNoOfVariables();

//        for (int i = 0; i < noOfVars; i++){
//            simulationVariables.put(engine.getSystem().getVariableName(i),
//                    currentConcreteState.get().getVariables()[i].getValue(BigDecimal.ZERO));
//        }

        // As the clocks values starts after the variables values in currentConcreteState.get().getVariables()
        // Then i needs to start where the variables ends.
        // j is needed to map the correct name with the value
//        for (int i = noOfVars, j = 0; i < noOfClocks + noOfVars ; i++, j++) {
//            simulationClocks.put(engine.getSystem().getClockName(j),
//                    currentConcreteState.get().getVariables()[i].getValue(BigDecimal.ZERO));
//        }
    }

    /**
     * Getter for the current concrete state
     *
     * @return the current {@link SimulationState}
     */
    public SimulationState getCurrentState() {
        return currentConcreteState.get();
    }

    /**
     * The way to get the time in the current state of a simulation
     *
     * @return the time in the current state
     */
    public BigDecimal getCurrentTime() {
        return currentTime.get();
    }

    public ObjectProperty<BigDecimal> currentTimeProperty() {
        return currentTime;
    }

    /**
     * The way to get the delay of the latest step in the simulation
     *
     * @return the delay of the latest step in the in the simulation
     */
    public BigDecimal getDelay() {
        return delay;
    }

    /**
     * The number of total steps taken in the current simulation
     *
     * @return the number of steps
     */
    public int getNumberOfSteps() {
        return numberOfSteps;
    }

    /**
     * All the transitions taken in this simulation
     *
     * @return an {@link ObservableList} of all the transitions taken in this simulation so far
     */
    public ObservableList<SimulationState> getTraceLog() {
        return traceLog;
    }

    /**
     * All the available transitions in this state
     *
     * @return an {@link ObservableList} of all the currently available transitions in this state
     */
    public ObservableList<Transition> getAvailableTransitions() {
        return availableTransitions;
    }

    /**
     * All the variables connected to the current simulation.
     * This does not return any clocks, if you need please use {@link SimulationHandler#getSimulationClocks()} instead
     *
     * @return a {@link Map} where the name (String) is the key, and a {@link BigDecimal} is the value
     */
    public ObservableMap<String, BigDecimal> getSimulationVariables() {
        return simulationVariables;
    }

    /**
     * All the clocks connected to the current simulation.
     *
     * @return a {@link Map} where the name (String) is the key, and a {@link BigDecimal} is the clock value
     * @see SimulationHandler#getSimulationVariables()
     */
    public ObservableMap<String, BigDecimal> getSimulationClocks() {
        return simulationClocks;
    }

    /**
     * The initial state of the current simulation
     *
     * @return the initial {@link SimulationState} of this simulation
     */
    public SimulationState getInitialConcreteState() {
        return Ecdar.getBackendDriver().getInitialSimulationState();
    }

    public ObjectProperty<SimulationState> initialConcreteStateProperty() {
        return initialConcreteState;
    }

    /**
     * Prints all available transitions to {@link System#out}.
     * This is very useful for debugging.
     * If a string representation is needed please use {@link SimulationHandler#getAvailableTransitionsAsStrings()}
     * instead.
     */
    public void printAvailableTransitions() {
        System.out.println("---------------------------------");

        System.out.println(numberOfSteps + " Successor state " + currentConcreteState.toString() + " Entry time " + currentTime);
        System.out.print("Available transitions: ");
        availableTransitions.forEach(
                Transition -> System.out.println(Transition.getLabel() + " "));

        if (!availableTransitions.isEmpty()) {
            for (int i = 0; i < availableTransitions.get(0).getEdges().size(); i++) {
                // ToDo: Implement
//                System.out.println("Edges: " +
//                        availableTransitions.get(0).getEdges().get(i).getEdge().getSource().getPropertyValue("name") +
//                        "." + availableTransitions.get(0).getEdges().get(i).getName() + " --> " +
//                        availableTransitions.get(0).getEdges().get(i).getEdge().getTarget().getPropertyValue("name"));
            }
        }

        System.out.println("---------------------------------");
    }

    /**
     * To get all available transitions as strings
     *
     * @return an ArrayList<String> of all the enabled transitions
     */
    public ArrayList<String> getAvailableTransitionsAsStrings() {
        final ArrayList<String> transitions = new ArrayList<>();
        for (final Transition Transition : availableTransitions) {
            transitions.add(Transition.getLabel());
        }
        return transitions;
    }

    public EcdarSystem getSystem() {
        return system;
    }

    public String getCurrentSimulation() {
        return currentSimulation;
    }
}