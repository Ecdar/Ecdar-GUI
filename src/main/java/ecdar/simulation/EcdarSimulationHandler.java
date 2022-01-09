package ecdar.simulation;

import ecdar.Ecdar;
import ecdar.abstractions.Component;
import ecdar.abstractions.Edge;
import ecdar.backend.BackendException;
import ecdar.backend.BackendHelper;
import ecdar.controllers.EcdarController;
import javafx.beans.Observable;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.util.Pair;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Map;
import java.util.Vector;

/**
 * Handles state changes, updates of values / clocks, and keeps track of all the transitions that
 * have been taken throughout a simulation.
 */
public class EcdarSimulationHandler {
    /**
     * A string to keep track what is currently being simulated
     * For now the string is prefixed with QUERY_PREFIX when doing a query simulation ToDo NIELS: QUERY_PREFIX used to be a hyperlink
     * and kept empty when doing system simulations
     */
    private String currentSimulation = "";
    private final ObservableMap<String, BigDecimal> simulationVariables = FXCollections.observableHashMap();
    private final ObservableMap<String, BigDecimal> simulationClocks = FXCollections.observableHashMap();
    private EcdarSystemState currentState;
    private ArrayList<Component> components = new ArrayList<>();

    /**
     * For some reason the successor.getTransitions() only sometimes returns some of the transitions
     * that are available, when running the initial step.
     * That is why we need to keep track of the initial transitions.
     */
    public ObservableList<EcdarSystemState> traceLog = FXCollections.observableArrayList();

    /**
     * The constructor of the {@link EcdarSimulationHandler}.
     */
    public EcdarSimulationHandler() {
        initializeSimulation();
        // currentState = getInitialStep().getKey();
    }

    /**
     * Initializes the values and properties in the {@link EcdarSimulationHandler}.
     * Can also be used as a reset of the simulation.
     * THIS METHOD DOES NOT RESET THE ENGINE,
     * ToDo NIELS: Probably delete
     */
    private void initializeSimulation() {
    }

    public void initializeNewSimulation(ArrayList<Component> components) {
        this.components = components;

        // ToDo NIELS: Call backend with components to initialize new simulation
    }

    /**
     * Reloads the whole simulation sets the initial transitions, states, etc
     */
    public Pair<EcdarSystemState, Vector<Edge>> getInitialStep() {
        // ToDo NIELS: Execute backend query to fetch initial step
        return null;
    }

    /**
     * Take a step in the simulation.
     */
    public Pair<EcdarSystemState, Vector<Edge>> getSuccessor(EcdarSystemState state, Edge transition) {
        // ToDo NIELS: Get successor state and save it to the currentState before returning
        return null;
    }

    /**
     * Returns the current state, which is the last state returned by getSuccessor
     */
    public EcdarSystemState getCurrentState() {
        return currentState;
    }

    public ObservableMap<String, BigDecimal> getSimulationVariables() {
        return simulationVariables;
    }

    public ObservableMap<String, BigDecimal> getSimulationClocks() {
        return simulationClocks;
    }
}