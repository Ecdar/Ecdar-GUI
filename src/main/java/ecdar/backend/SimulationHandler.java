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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
    private String simulationQuery;
    private ArrayList<Component> simulationComponents = new ArrayList<>();
    private final ObservableMap<String, BigDecimal> simulationVariables = FXCollections.observableHashMap();
    private final ObservableMap<String, BigDecimal> simulationClocks = FXCollections.observableHashMap();
    public ObservableList<SimulationState> traceLog = FXCollections.observableArrayList();

    private List<String> ComponentsInSimulation = new ArrayList<>();

    private EngineConnection con;

    /**
     * Empty constructor that should be used if the system or project has not be initialized yet
     */
    public SimulationHandler() {

    }

    public void clearComponentsInSimulation() {
        ComponentsInSimulation.clear();
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

        if (con == null) {
            EngineConnectionStarter ecs = new EngineConnectionStarter(BackendHelper.getDefaultEngine());
            con = ecs.tryStartNewConnection();
        }
    }

    /**
     * Reloads the whole simulation sets the initial transitions, states, etc
     */
    public void initialStep() {
        initializeSimulation();

        GrpcRequest request = new GrpcRequest(engineConnection -> {
            StreamObserver<SimulationStepResponse> responseObserver = new StreamObserver<>() {
                @Override
                public void onNext(QueryProtos.SimulationStepResponse value) {
                    // ToDo: This is temp solution to compile but should be fixed to handle ambiguity
                    currentState.set(new SimulationState(value.getNewDecisionPoints(0)));
                    Platform.runLater(() -> traceLog.add(currentState.get()));
                }
                
                @Override
                public void onError(Throwable t) {
                    Ecdar.showToast("Could not start simulation:\n" + t.getMessage());
                }

                @Override
                public void onCompleted() {
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
            engineConnection.getStub().withDeadlineAfter(20000, TimeUnit.MILLISECONDS)
                    .startSimulation(simStartRequest.build(), responseObserver);
        });

        request.execute(con);
        numberOfSteps++;
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
        // removes invalid states from the log when stepping forward after previewing a previous state
        removeStatesFromLog(currentState.get()); 
        
        GrpcRequest request = new GrpcRequest(engineConnection -> {
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
                  }
                
                @Override
                public void onCompleted() {
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

            engineConnection.getStub().withDeadlineAfter(20000, TimeUnit.MILLISECONDS)
                    .takeSimulationStep(simStepRequest.build(), responseObserver);
        });

        request.execute(con);
        numberOfSteps++;
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
            if (Ecdar.getProject().getComponents().get(i).getEdges().stream().anyMatch(p -> Objects.equals(p.getId(), edge.getId()))) {
                return i;
            }
        }

        throw new IllegalArgumentException("Edge does not belong to any component");
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
        return currentState.get().getEnabledEdges();
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

    public SimulationState getCurrentState() {
        return currentState.get();
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
     * Removes all states from the trace log after the given state
     */
    private void removeStatesFromLog(SimulationState state) {
        while (traceLog.get(traceLog.size() - 1) != state) {
            traceLog.remove(traceLog.size() - 1);
        }
    }

    public void setComponentsInSimulation(List<String> value) {
        ComponentsInSimulation = value;
    }

    public List<String> getComponentsInSimulation() {
        return ComponentsInSimulation;
    }

    public void setSimulationQuery(String query) {
        simulationQuery = query;
    }

    public String getSimulationQuery(){
        return simulationQuery;
    }

    /**
     * Set list of components used in the simulation
     */
    public void setSimulationComponents(ArrayList<Component> components){
        simulationComponents = components;
    }

    /**
     * Get list of components used in the simulation
     */
    public ArrayList<Component> getSimulationComponents(){
        return simulationComponents;
    }

    /**
     * Highlights the edges from the reachability response
     */
    public void highlightReachabilityEdges(ArrayList<String> ids){
        //unhighlight all edges
        for(var comp : simulationComponents){
            for(var edge : comp.getEdges()){
                edge.setIsHighlightedForReachability(false);
            }
        }
        //highlight the edges from the reachability response
        for(var comp : simulationComponents){
            for(var edge : comp.getEdges()){
                for(var id : ids){
                    if(edge.getId().equals(id)){
                        edge.setIsHighlightedForReachability(true);
                    }
                }
            }
        }
    }
}