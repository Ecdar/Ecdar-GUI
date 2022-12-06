package ecdar.backend;

import EcdarProtoBuf.ComponentProtos;
import EcdarProtoBuf.ObjectProtos;
import EcdarProtoBuf.QueryProtos;
import EcdarProtoBuf.ObjectProtos.Decision;
import ecdar.Ecdar;
import ecdar.abstractions.*;
import ecdar.simulation.SimulationState;
import io.grpc.stub.StreamObserver;
import javafx.application.Platform;
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
import java.util.concurrent.TimeUnit;

import EcdarProtoBuf.QueryProtos.SimulationInfo;
import EcdarProtoBuf.QueryProtos.SimulationStepRequest;
import EcdarProtoBuf.QueryProtos.SimulationStepResponse;

/**
 * Handles state changes, updates of values / clocks, and keeps track of all the transitions that
 * have been taken throughout a simulation.
 */
public class SimulationHandler {
    public static final String QUERY_PREFIX = "Query: ";
    private String composition;
    public ObjectProperty<SimulationState> currentState = new SimpleObjectProperty<>();
    public ObjectProperty<SimulationState> initialState = new SimpleObjectProperty<>();
    public ObjectProperty<Edge> selectedEdge = new SimpleObjectProperty<>();
    private EcdarSystem system;
    private int numberOfSteps;

    private final ObservableMap<String, BigDecimal> simulationVariables = FXCollections.observableHashMap();
    private final ObservableMap<String, BigDecimal> simulationClocks = FXCollections.observableHashMap();
    public ObservableList<SimulationState> traceLog = FXCollections.observableArrayList();
    private final BackendDriver backendDriver;
    private final ArrayList<BackendConnection> connections = new ArrayList<>();

    /**
     * Empty constructor that should be used if the system or project has not be initialized yet
     */
    public SimulationHandler(BackendDriver backendDriver) {
        this.backendDriver = backendDriver;
    }


    /**
     * Initializes the values and properties in the {@link SimulationHandler}.
     * Can also be used as a reset of the simulation.
     * THIS METHOD DOES NOT RESET THE ENGINE,
     */
    private void initializeSimulation() {
        // Initialization
        this.numberOfSteps = 0;
        this.simulationVariables.clear();
        this.simulationClocks.clear();
        this.currentState.set(null);
        this.selectedEdge.set(null);
        this.traceLog.clear();
        
        this.system = getSystem();
    }


    /**
     * Reloads the whole simulation sets the initial transitions, states, etc
     */
    public void initialStep() {
        initializeSimulation();

        GrpcRequest request = new GrpcRequest(backendConnection -> {
            StreamObserver<SimulationStepResponse> responseObserver = new StreamObserver<>() {
                @Override
                public void onNext(QueryProtos.SimulationStepResponse value) {
                    // TODO this is temp solution to compile but should be fixed to handle ambiguity
                    currentState.set(new SimulationState(value.getNewDecisionPoints(0)));
                    Platform.runLater(() -> traceLog.add(currentState.get()));
                }
                
                @Override
                public void onError(Throwable t) {
                    Ecdar.showToast("Could not start simulation:\n" + t.getMessage());
                    
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
        this.traceLog.add(currentState.get());
        numberOfSteps++;
    
        //Updates the transitions available
        updateAllValues();
        
    }
    
    /**
     * Resets the simulation to the initial location
     */
    public void resetToInitialLocation() {
        initialStep();
    }

    /**
     * Take a step in the simulation.
     */
    public void nextStep() {
        GrpcRequest request = new GrpcRequest(backendConnection -> {
            StreamObserver<SimulationStepResponse> responseObserver = new StreamObserver<>() {
                @Override
                public void onNext(QueryProtos.SimulationStepResponse value) {
                    // TODO this is temp solution to compile but should be fixed to handle ambiguity
                    currentState.set(new SimulationState(value.getNewDecisionPoints(0)));
                    Platform.runLater(() -> traceLog.add(currentState.get()));
                }
                
                @Override
                public void onError(Throwable t) {
                    Ecdar.showToast("Could not take next step in simulation\nError: " + t.getMessage());
                    
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
            var simStepRequest = SimulationStepRequest.newBuilder();
            var simInfo = SimulationInfo.newBuilder()
                    .setComponentComposition(composition)
                    .setComponentsInfo(comInfo);
            simStepRequest.setSimulationInfo(simInfo);
            var source = currentState.get().getState();
            var specComp = ObjectProtos.SpecificComponent.newBuilder().setComponentName(getComponentName(selectedEdge.get())).setComponentIndex(getComponentIndex(selectedEdge.get()));
            var edge = EcdarProtoBuf.ObjectProtos.Edge.newBuilder().setId(selectedEdge.get().getId()).setSpecificComponent(specComp);
            var decision = Decision.newBuilder().setEdge(edge).setSource(source);
            simStepRequest.setChosenDecision(decision);

            backendConnection.getStub().withDeadlineAfter(this.backendDriver.getResponseDeadline(), TimeUnit.MILLISECONDS)
                    .takeSimulationStep(simStepRequest.build(), responseObserver);
        }, BackendHelper.getDefaultBackendInstance());
        
        backendDriver.addRequestToExecutionQueue(request);
        
        
        // increments the number of steps taken during this simulation
        numberOfSteps++;
        
        
        updateAllValues();
    }

    private String getComponentName(Edge edge) {
        var components = Ecdar.getProject().getComponents();
        for (var component : components) {
            for (var e : component.getEdges()) {
                if (e.getId().equals(edge.getId())) {
                    return component.getName();
                }
            }
        }
        throw new RuntimeException("Could not find component name for edge with id " + edge.getId());
    }

    private int getComponentIndex (Edge edge) {
        for (int i = 0; i < Ecdar.getProject().getComponents().size(); i++) {
            if (Ecdar.getProject().getComponents().get(i).getEdges().stream().anyMatch(p -> p.getId() == edge.getId())) {
                return i;
            }
        };
        throw new IllegalArgumentException("Edge does not belong to any component");
    }
    
    
    /**
     * Updates all values and clocks that are used doing the current simulation.
     * It also stores the variables in the {@link SimulationHandler#simulationVariables}
     * and the clocks in {@link SimulationHandler#simulationClocks}.
     */
    private void updateAllValues() {
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
     * @return 
     *
     * @return an {@link ObservableList} of all the currently available transitions in this state
     */
    public ArrayList<Pair<String, String>> getAvailableTransitions() {
        return currentState.get().getEdges();
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
    public SimulationState getInitialState() {
        // ToDo: Implement
        return initialState.get();
    }

    public ObjectProperty<SimulationState> initialStateProperty() {
        return initialState;
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


    /**
     * Sets the current state of the simulation to the given state from the trace log
     */
    public void selectStateFromLog(SimulationState state) {
        while (traceLog.get(traceLog.size() - 1) != state) {
            traceLog.remove(traceLog.size() - 1);
        }
        currentState.set(state);
    }
}