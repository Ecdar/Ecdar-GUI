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
import ecdar.abstractions.Edge;
import ecdar.backend.BackendException;
import ecdar.backend.BackendHelper;
import javafx.beans.Observable;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.util.Pair;
import models.Transition;

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

    /**
     * For some reason the successor.getTransitions() only sometimes returns some of the transitions
     * that are available, when running the initial step.
     * That is why we need to keep track of the initial transitions.
     */
    public ObservableList<EcdarSystemState> traceLog = FXCollections.observableArrayList();

    /**
     * The constructor of the {@link EcdarSimulationHandler}.
     * @throws IllegalArgumentException if the {@link Engine#getSystem()} is null and
     *         a system has not been linked to the engine.
     */
    public EcdarSimulationHandler() {
        initializeSimulation();
        currentState = getInitialStep().getKey();
    }

    /**
     * Initializes the values and properties in the {@link EcdarSimulationHandler}.
     * Can also be used as a reset of the simulation.
     * THIS METHOD DOES NOT RESET THE ENGINE,
     */
    private void initializeSimulation() {
    }

    /**
     * Reloads the whole simulation sets the initial transitions, states, etc
     */
    public Pair<EcdarSystemState, Vector<Edge>> getInitialStep() {
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