package ecdar.abstractions;

import EcdarProtoBuf.ObjectProtos;
import ecdar.Ecdar;
import ecdar.presentations.DecisionPresentation;
import ecdar.presentations.StatePresentation;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Simulation {
    public final String composition;
    public final ObservableList<Component> simulatedComponents = FXCollections.observableArrayList();
    public final ObjectProperty<State> initialState = new SimpleObjectProperty<>();
    public final ObservableMap<String, BigDecimal> variables = FXCollections.observableHashMap();
    public final ObservableMap<String, BigDecimal> clocks = FXCollections.observableHashMap();

    public final ObjectProperty<State> currentState = new SimpleObjectProperty<>();

    public final ObservableList<DecisionPresentation> availableDecisions = FXCollections.observableArrayList();
    public final ObservableList<StatePresentation> traceLog = FXCollections.observableArrayList();

    public Simulation(String composition, State initialState) {
        this.composition = composition;
        setSimulatedComponents(composition);

        this.initialState.set(initialState);
        this.currentState.set(initialState);
    }

    private void setSimulatedComponents(String composition) {
        // Match all referenced components by ignoring operators.
        Pattern pattern = Pattern.compile("([\\w]*)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(composition);

        // Get all components referenced in the composition
        List<String> componentsToSimulateByName = matcher.results().map(MatchResult::group).collect(Collectors.toList());
        List<Component> componentsToSimulate = Ecdar.getProject().getComponents().stream()
                .filter(component -> componentsToSimulateByName.contains(component.getName()))
                .collect(Collectors.toList());


        simulatedComponents.addAll(componentsToSimulate);
    }

    public void addDecisionsFromProto(List<ObjectProtos.Decision> protoDecisions) {
        Platform.runLater(() -> {
            protoDecisions.forEach(protoDecision -> {
                availableDecisions.add(new DecisionPresentation(new Decision(composition, protoDecision)));
            });
        });
    }

    public void addStateToTraceLog(State state, Consumer<StatePresentation> onTraceLogStatePressed) {
        StatePresentation statePresentation = new StatePresentation(state);

        statePresentation.setOnMouseReleased(event -> {
            event.consume();
            onTraceLogStatePressed.accept(statePresentation);
        });

        traceLog.add(statePresentation);
    }
}
