package ecdar.simulation;

import com.uppaal.engine.Engine;
import com.uppaal.engine.EngineException;
import com.uppaal.model.core2.Document;
import com.uppaal.model.system.SystemEdge;
import com.uppaal.model.system.UppaalSystem;
import com.uppaal.model.system.concrete.ConcreteState;
import com.uppaal.model.system.concrete.ConcreteSuccessor;
import com.uppaal.model.system.concrete.ConcreteTransition;
import ecdar.Ecdar;
import ecdar.backend.BackendException;
import ecdar.backend.BackendHelper;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

/**
 * Handles state changes, updates of values / clocks, and keeps track of all the transitions that
 * have been taken throughout a simulation.
 */
public class SimulationHandler {
    public static final String QUERY_PREFIX = "Query: ";
    private Engine engine;
    private ObjectProperty<ConcreteState> currentConcreteState;
    private ObjectProperty<ConcreteState> initialConcreteState;
    private ObjectProperty<BigDecimal> currentTime;
    private BigDecimal delay;
    private SystemEdge edgesSelected[];
    private UppaalSystem system;
    private ConcreteSuccessor successor;
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
    private final ObservableList<ConcreteTransition> initialTransitions = FXCollections.observableArrayList();
    public ObservableList<ConcreteState> traceLog = FXCollections.observableArrayList();
    public ObservableList<ConcreteTransition> availableTransitions = FXCollections.observableArrayList();

    /**
     * The constructor of the {@link SimulationHandler}.
     * @param engine an engine which has to be linked to system
     * @throws IllegalArgumentException if the {@link Engine#getSystem()} is null and
     *         a system has not been linked to the engine.
     */
    public SimulationHandler(final Engine engine) throws IllegalArgumentException {
        if (engine.getSystem() == null)
            throw new IllegalArgumentException("Could not find a system linked to the given engine " + engine);
        this.engine = engine;
        initializeSimulation();
    }

    /**
     * Empty constructor that should be used if the system or project has not be initialized yet
     */
    public SimulationHandler() {

    }

    /**
     * Initializes the default system (non-query system)
     */
    public void initializeDefaultSystem() throws BackendException {
        final Document doc = BackendHelper.getEcdarDocument();
        initializeUsingDocument(doc);
        currentSimulation = "";
    }

    /**
     * Helper method to initialize a simulation. Used for constructors that have documents but no engine
     * @param document the system document to base the simulation on
     * @throws BackendException.SystemNotFoundException if a system was not linked to the backend
     */
    private void initializeUsingDocument(final Document document) throws BackendException.SystemNotFoundException {
        final Engine engine = BackendHelper.getEngine();
        try {
            engine.getSystem(document, new Vector<>());
        } catch (EngineException | IOException e) {
            e.printStackTrace();
        }
        if (engine.getSystem() == null)
            throw new BackendException.SystemNotFoundException("Could not find a system linked to the given engine " + engine);
        this.engine = engine;
        initializeSimulation();
    }

    /**
     * Initializes the values and properties in the {@link SimulationHandler}.
     * Can also be used as a reset of the simulation.
     * THIS METHOD DOES NOT RESET THE ENGINE,
     */
    private void initializeSimulation() {
        // Initialization
        this.delay = new BigDecimal(0);
        this.edgesSelected = new SystemEdge[0];
        this.numberOfSteps = 0;
        this.availableTransitions.clear();
        this.simulationVariables.clear();
        this.simulationClocks.clear();
        this.traceLog.clear();
        this.currentConcreteState = new SimpleObjectProperty<>(engine.getSystem().getConcreteInitial());
        this.initialConcreteState = new SimpleObjectProperty<>(engine.getSystem().getConcreteInitial());
        this.currentTime = new SimpleObjectProperty<>(BigDecimal.ZERO);

        //Preparation for the simulation
        this.system = engine.getSystem();
        this.currentConcreteState.get().setTime(currentTime.getValue());
        this.initialTransitions.clear();
        this.successor = null;
    }

    /**
     * Reloads the whole simulation sets the initial transitions, states, etc
     */
    public void initialStep() {
        initializeSimulation();
        final ConcreteState currentState = currentConcreteState.get();

        try {
            successor = engine.getConcreteSuccessor(system, currentState,
                    edgesSelected, currentTime.getValue(), BigDecimal.ZERO);
        } catch (final EngineException e) {
            e.printStackTrace();
            Ecdar.showToast("Could not perform initial step: " + e.getMessage());
            return;
        }

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
     * Resets the simulation to the initial location using {@link SimulationHandler#selectTransitionFromLog(ConcreteState)}
     * where the <code>ConcreteState</code> is the {@link SimulationHandler#initialConcreteState}, when there are
     * elements in the {@link SimulationHandler#traceLog}. Otherwise it calls {@link SimulationHandler#initialStep}
     */
    public void resetToInitialLocation(){
        //If the simulation has not begone
        if(traceLog.size() == 0)
            initialStep();
        else
            selectTransitionFromLog(initialConcreteState.get());
    }

    /**
     * Resets the simulation to the state after executing the given transition. <br />
     * This method also resets the state, variables, and clocks to the values they had after the given transition.
     * This also updates {@link SimulationHandler#availableTransitions} such that
     * it displays the available transitions after taking the given transition.
     * @param transition the transition which the simulation should go back to
     */
    public void selectTransitionFromLog(final ConcreteState transition) {
        final int indexInTrace = traceLog.indexOf(transition);
        final ConcreteState selectedState;
        if(indexInTrace  == -1) {
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
        edgesSelected = new SystemEdge[0];
        //In case that we fail we have to save the time we had before
        final BigDecimal tempTime = currentTime.get();

        currentTime.setValue(new BigDecimal(selectedState.getTime().doubleValue()));
        successor.getState().setTime(currentTime.getValue());

        while (numberOfRetries < maxRetries) {
            try {
                successor = engine.getConcreteSuccessor(system, selectedState,
                        edgesSelected, currentTime.getValue(), BigDecimal.ZERO);
                break;
            } catch (final EngineException e) {
                numberOfRetries++;

                if(numberOfRetries == maxRetries) { // We finally give up and accept the Exception
                    e.printStackTrace();
                    Ecdar.showToast("Could not take next step: " + e.getMessage());
                    //Reset the clock to the previous value
                    currentTime.setValue(tempTime);
                    return;
                }
            }
        }
        currentConcreteState.set(selectedState);
        setSimVarAndClocks();
        traceLog.remove(indexInTrace + 1, sizeOfTraceLog);
        availableTransitions.clear();

        // If the user selected the initial/first state in the trace log, we do not trust the engine,
        // as it only gives us a subset of the available transitions, in some cases.
        if(indexInTrace == 0) availableTransitions.addAll(initialTransitions);
        else availableTransitions.addAll(FXCollections.observableArrayList(successor.getTransitions()));

        numberOfSteps = indexInTrace + 1;
    }

    /**
     * Take a step in the simulation.
     * @param selectedTransition the index of the availableTransition that you want to take.
     * @param delay the time which should pass after the transition.
     */
    public void nextStep(final int selectedTransition, final BigDecimal delay) {
        if(selectedTransition > availableTransitions.size()) {
            Ecdar.showToast("The selected transition index: " + selectedTransition + " is bigger than it should: "+ availableTransitions);
            return;
        }

        final ConcreteTransition selectedConcreteTransition = availableTransitions.get(selectedTransition);
        edgesSelected = new SystemEdge[selectedConcreteTransition.getEdges().length];

        //Preparing for the step
        for (int i = 0; i < selectedConcreteTransition.getEdges().length; i++) {
            edgesSelected[i] = selectedConcreteTransition.getEdges()[i];
        }

        final int maxRetries = 3;
        int numberOfRetries = 0;

        // getConcreteSuccessor may throw a "ProtocolException: Word expected" but in some cases calling the same
        // method again does not throw this exception, and actually gives us the expected result.
        // This loop calls the method a number of times (maxRetries)
        while (numberOfRetries < maxRetries) {
            try {
                successor = engine.getConcreteSuccessor(system, currentConcreteState.get(),
                        edgesSelected, currentTime.getValue(), delay);
                // Break from the loop if the method call was a success
                break;
            } catch (final EngineException e) {
                // Increment the number of retries so we don't get stuck in this loop
                numberOfRetries++;

                if(numberOfRetries == maxRetries) { // We finally give up and accept the Exception
                    e.printStackTrace();
                    Ecdar.showToast("Could not take next step: " + e.getMessage());
                    return;
                }
            }
        }

        //Save the previous states, and get the new
        currentConcreteState.set(successor.getState());
        this.traceLog.add(currentConcreteState.get());

        // increments the number of steps taken during this simulation
        numberOfSteps++;

        //Updates the transitions available
        availableTransitions.clear();
        availableTransitions.setAll(FXCollections.observableArrayList(successor.getTransitions()));
        this.delay = delay;
        updateAllValues();
    }

    /**
     * An overload of {@link SimulationHandler#nextStep(int, BigDecimal)} where the delay is 0.
     * @param selectedTransition the index of the availableTransition that you want to take.
     */
    public void nextStep(final int selectedTransition) {
        nextStep(selectedTransition, BigDecimal.ZERO);
    }

    public void nextStep(final ConcreteTransition transition, final BigDecimal delay) {
        int index = availableTransitions.indexOf(transition);
        if(index != -1) {
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
     *
     */
    private void setSimVarAndClocks() {
        // The variables and clocks are all found in the getVariables array
        // the array is always of the following order: variables, clocks.
        // The noOfVars variable thus also functions as an offset for the clocks in the getVariables array
        final int noOfClocks = engine.getSystem().getNoOfClocks();
        final int noOfVars = engine.getSystem().getNoOfVariables();

        for (int i = 0; i < noOfVars; i++){
            simulationVariables.put(engine.getSystem().getVariableName(i),
                    currentConcreteState.get().getVariables()[i].getValue(BigDecimal.ZERO));
        }

        // As the clocks values starts after the variables values in currentConcreteState.get().getVariables()
        // Then i needs to start where the variables ends.
        // j is needed to map the correct name with the value
        for (int i = noOfVars, j = 0; i < noOfClocks + noOfVars ; i++, j++) {
            simulationClocks.put(engine.getSystem().getClockName(j),
                    currentConcreteState.get().getVariables()[i].getValue(BigDecimal.ZERO));
        }
    }

    /**
     * Getter for the current concrete state
     * @return the current {@link ConcreteState}
     */
    public ConcreteState getCurrentConcreteState() {
        return currentConcreteState.get();
    }

    /**
     * The way to get the time in the current state of a simulation
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
     * @return the delay of the latest step in the in the simulation
     */
    public BigDecimal getDelay() {
        return delay;
    }

    /**
     * The number of total steps taken in the current simulation
     * @return the number of steps
     */
    public int getNumberOfSteps() {
        return numberOfSteps;
    }

    /**
     * All the transitions taken in this simulation
     * @return an {@link ObservableList} of all the transitions taken in this simulation so far
     */
    public ObservableList<ConcreteState> getTraceLog() {
        return traceLog;
    }

    /**
     * All the available transitions in this state
     * @return an {@link ObservableList} of all the currently available transitions in this state
     */
    public ObservableList<ConcreteTransition> getAvailableTransitions() {
        return availableTransitions;
    }

    /**
     * All the variables connected to the current simulation.
     * This does not return any clocks, if you need please use {@link SimulationHandler#getSimulationClocks()} instead
     * @return a {@link Map} where the name (String) is the key, and a {@link BigDecimal} is the value
     */
    public ObservableMap<String, BigDecimal> getSimulationVariables() {
        return simulationVariables;
    }

    /**
     * All the clocks connected to the current simulation.
     * @see SimulationHandler#getSimulationVariables()
     * @return a {@link Map} where the name (String) is the key, and a {@link BigDecimal} is the clock value
     */
    public ObservableMap<String, BigDecimal> getSimulationClocks() {
        return simulationClocks;
    }

    /**
     * The initial state of the current simulation
     * @return the initial {@link ConcreteState} of this simulation
     */
    public ConcreteState getInitialConcreteState() {
        return initialConcreteState.get();
    }

    public ObjectProperty<ConcreteState> initialConcreteStateProperty() {
        return initialConcreteState;
    }

    /**
     * Prints all available transitions to {@link java.lang.System#out}.
     * This is very useful for debugging.
     * If a string representation is needed please use {@link SimulationHandler#getAvailableTransitionsAsStrings()}
     * instead.
     */
    public void printAvailableTransitions() {
        System.out.println("---------------------------------");

        System.out.println(numberOfSteps + " Successor state " + currentConcreteState.toString() + " Entry time " + currentTime);
        java.lang.System.out.print("Available transitions: ");
        availableTransitions.forEach(
                concreteTransition -> System.out.println(concreteTransition.getLabel() + " "));

        if(!availableTransitions.isEmpty()) {
            for (int i = 0; i < availableTransitions.get(0).getEdges().length; i++) {
                System.out.println("Edges: " +
                        availableTransitions.get(0).getEdges()[i].getEdge().getSource().getPropertyValue("name") +
                        "." + availableTransitions.get(0).getEdges()[i].getName() + " --> " +
                        availableTransitions.get(0).getEdges()[i].getEdge().getTarget().getPropertyValue("name"));
            }
        }

        System.out.println("---------------------------------");
    }

    /**
     * To get all available transitions as strings
     * @return an ArrayList<String> of all the enabled transitions
     */
    public ArrayList<String> getAvailableTransitionsAsStrings(){
        final ArrayList<String> transitions = new ArrayList<>();
        for (final ConcreteTransition concreteTransition: availableTransitions) {
            transitions.add(concreteTransition.getLabel());
        }
        return transitions;
    }

    public UppaalSystem getSystem() {
        return system;
    }

    public String getCurrentSimulation() {
        return currentSimulation;
    }
}