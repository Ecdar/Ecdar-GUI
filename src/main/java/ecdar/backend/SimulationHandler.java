package ecdar.backend;

import EcdarProtoBuf.ComponentProtos;
import EcdarProtoBuf.ObjectProtos;
import EcdarProtoBuf.QueryProtos;
import ecdar.Ecdar;
import ecdar.abstractions.*;
import ecdar.simulation.SimulationState;
import ecdar.simulation.SimulationStateSuccessor;
import io.grpc.stub.StreamObserver;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import EcdarProtoBuf.QueryProtos.SimulationStepResponse;

/**
 * Handles state changes, updates of values / clocks, and keeps track of all the transitions that
 * have been taken throughout a simulation.
 */
public class SimulationHandler {
    public static final String QUERY_PREFIX = "Query: ";
    private String composition;
    private ObjectProperty<SimulationState> currentConcreteState = new SimpleObjectProperty<>();
    private ObjectProperty<SimulationState> initialConcreteState = new SimpleObjectProperty<>();
    private ObjectProperty<BigDecimal> currentTime = new SimpleObjectProperty<>();
    private BigDecimal delay;
    private ArrayList<Edge> edgesSelected;
    private EcdarSystem system;
    private SimulationStateSuccessor successor;
    private int numberOfSteps;

    private final ObservableMap<String, BigDecimal> simulationVariables = FXCollections.observableHashMap();
    private final ObservableMap<String, BigDecimal> simulationClocks = FXCollections.observableHashMap();
    /**
     * For some reason the successor.getTransitions() only sometimes returns some of the transitions
     * that are available, when running the initial step.
     * That is why we need to keep track of the initial transitions.
     */
    private final ObservableList<ecdar.simulation.Transition> initialTransitions = FXCollections.observableArrayList();
    public ObservableList<SimulationState> traceLog = FXCollections.observableArrayList();
    public ObservableList<ecdar.simulation.Transition> availableTransitions = FXCollections.observableArrayList();
    private final BackendDriver backendDriver;
    private final ArrayList<BackendConnection> connections = new ArrayList<>();

    /**
     * Empty constructor that should be used if the system or project has not be initialized yet
     */
    public SimulationHandler(BackendDriver backendDriver) {
        this.backendDriver = backendDriver;
    }

    /**
     * Initializes the default system (non-query system)
     */

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
        this.currentConcreteState.set(getInitialConcreteState());
        this.initialConcreteState.set(getInitialConcreteState());
        this.currentTime = new SimpleObjectProperty<>(BigDecimal.ZERO);
        
        //Preparation for the simulation
        this.system = getSystem();
        //this.currentConcreteState.get().setTime(currentTime.getValue());
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
        
        GrpcRequest request = new GrpcRequest(backendConnection -> {
            StreamObserver<SimulationStepResponse> responseObserver = new StreamObserver<>() {
                @Override
                public void onNext(QueryProtos.SimulationStepResponse value) {
                    System.out.println(value);
                }
                
                @Override
                public void onError(Throwable t) {
                    System.out.println(t.getMessage());
                    Ecdar.showToast("Could not start simulation");
                    
                    // Release backend connection
                    backendDriver.addBackendConnection(backendConnection);
                    connections.remove(backendConnection);
                }

                @Override
                public void onCompleted() {
                    // Release backend connection
                    backendDriver.addBackendConnection(backendConnection);
                    connections.remove(backendConnection);
                }
            };

            var comInfo = ComponentProtos.ComponentsInfo.newBuilder();
            for (Component c : Ecdar.getProject().getComponents()) {
                comInfo.addComponents(ComponentProtos.Component.newBuilder().setJson(c.serialize().toString()).build());
            }
            comInfo.setComponentsHash(comInfo.getComponentsList().hashCode());
            var simStartRequest = QueryProtos.SimulationStartRequest.newBuilder();
            var simInfo = QueryProtos.SimulationInfo.newBuilder()
                    .setComponentComposition(composition)
                    .setComponentsInfo(comInfo);
            simStartRequest.setSimulationInfo(simInfo);
            backendConnection.getStub().withDeadlineAfter(this.backendDriver.getResponseDeadline(), TimeUnit.MILLISECONDS)
                    .startSimulation(simStartRequest.build(), responseObserver);
        }, BackendHelper.getDefaultBackendInstance());
        
        backendDriver.addRequestToExecutionQueue(request);
        
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
     * elements in the {@link SimulationHandler#traceLog}. Otherwise, it calls {@link SimulationHandler#initialStep}
     */
    public void resetToInitialLocation() {
        //If the simulation has not begun
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

        final ecdar.simulation.Transition selectedTransition = availableTransitions.get(selectedTransitionIndex);
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

    public void nextStep(final ecdar.simulation.Transition transition, final BigDecimal delay) {
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
        //successor.getState().setTime(currentTime.get());
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
    public ObservableList<ecdar.simulation.Transition> getAvailableTransitions() {
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
        // ToDo: Implement
        return initialConcreteState.get();
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
        for (final ecdar.simulation.Transition Transition : availableTransitions) {
            transitions.add(Transition.getLabel());
        }
        return transitions;
    }

    public EcdarSystem getSystem() {
        return system;
    }

    public String getComposition() { return composition;}

    public void setComposition(String composition) {this.composition = composition;}

    public boolean isSimulationRunning() {
        return false; // ToDo: Implement
    }

    /**
     * Close all open backend connection and kill all locally running processes
     *
     * @throws IOException if any of the sockets do not respond
     */
    public void closeAllBackendConnections() throws IOException {
        for (BackendConnection con : connections) {
            con.close();
        }
    }
}