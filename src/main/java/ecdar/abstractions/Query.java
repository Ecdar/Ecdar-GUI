package ecdar.abstractions;

import EcdarProtoBuf.QueryProtos;
import com.google.gson.JsonParser;
import ecdar.Ecdar;
import ecdar.backend.*;
import ecdar.controllers.EcdarController;
import ecdar.controllers.SimulationController;
import ecdar.utility.UndoRedoStack;
import ecdar.utility.helpers.StringValidator;
import EcdarProtoBuf.ObjectProtos;
import ecdar.utility.helpers.StringHelper;
import ecdar.utility.serialize.Serializable;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.ObservableList;

import java.util.ArrayList;
import java.util.NoSuchElementException;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class Query implements RequestSource<QueryProtos.QueryResponse>, Serializable {
    private static final String QUERY = "query";
    private static final String COMMENT = "comment";
    private static final String IS_PERIODIC = "isPeriodic";
    private static final String ENGINE = "engine";

    private final StringProperty query = new SimpleStringProperty("");
    private final StringProperty comment = new SimpleStringProperty("");
    private final StringProperty errors = new SimpleStringProperty("");
    private final SimpleBooleanProperty isPeriodic = new SimpleBooleanProperty(false);
    private final ObjectProperty<QueryState> queryState = new SimpleObjectProperty<>(QueryState.UNKNOWN);
    private final ObjectProperty<QueryType> type = new SimpleObjectProperty<>();
    private Engine engine;


    private final Consumer<Boolean> successConsumer = (aBoolean) -> {
        if (aBoolean) {
            for (Component c : Ecdar.getProject().getComponents()) {
                c.removeFailingLocations();
                c.removeFailingEdges();
                c.setIsFailing(false);
            }
            setQueryState(QueryState.SUCCESSFUL);
        } else {
            setQueryState(QueryState.ERROR);
        }
    };

    private Boolean forcedCancel = false;
    private final Consumer<Exception> failureConsumer = (e) -> {
        if (forcedCancel) {
            setQueryState(QueryState.UNKNOWN);
        } else {
            setQueryState(QueryState.SYNTAX_ERROR);
            if (e instanceof BackendException.MissingFileQueryException) {
                Ecdar.showToast("Please save the project before trying to run queries");
            }

            this.addError(e.getMessage());
            final Throwable cause = e.getCause();
            if (cause != null) {
                // We had trouble generating the model if we get a NullPointerException
                if (cause instanceof NullPointerException) {
                    setQueryState(QueryState.UNKNOWN);
                } else {
                    Platform.runLater(() -> EcdarController.openQueryDialog(this, cause.toString()));
                }
            }
        }
    };

    private final BiConsumer<ObjectProtos.State, List<String>> stateActionConsumer = (state, actions) -> {
        // ToDo: Color all IO strings red
        for (Component c : Ecdar.getProject().getComponents()) {
            c.removeFailingLocations();
            c.removeFailingEdges();
        }

//        for (ObjectProtos.LeafLocation location : ) {
//            Component c = Ecdar.getProject().findComponent(location.getSpecificComponent().getComponentName());
//
//            if (c == null) {
//                throw new NullPointerException("Could not find the specific component: " + location.getSpecificComponent().getComponentName());
//            }
//
//            if (location.getId().isEmpty()) {
//                if (c.getName().equals(location.getSpecificComponent().getComponentName())) {
//                    c.setFailingIOStrings(actions);
//                    c.setIsFailing(true);
//                }
//            } else {
//                Location l = c.findLocation(location.getId());
//                if (l == null) {
//                    throw new NullPointerException("Could not find location: " + location.getId());
//                }
//
//                c.addFailingLocation(l.getId());
//                for (Edge edge : c.getEdges()) {
//                    if (actions.get(0).equals(edge.getSync()) && edge.getSourceLocation() == l) {
//                        c.addFailingEdge(edge);
//                    }
//                }
//            }
//        }
    };

    public Query(final String query, final String comment, final QueryState queryState, final Engine engine) {
        this.setQuery(query);
        this.comment.set(comment);
        this.queryState.set(queryState);
        setEngine(engine);
    }

    public Query(final String query, final String comment, final QueryState queryState) {
        this(query, comment, queryState, BackendHelper.getDefaultEngine());
    }

    public Query(final JsonObject jsonElement) {
        deserialize(jsonElement);
    }

    public QueryState getQueryState() {
        return queryState.get();
    }

    public void setQueryState(final QueryState queryState) {
        this.queryState.set(queryState);
    }

    public ObjectProperty<QueryState> queryStateProperty() {
        return queryState;
    }

    public String getQuery() {
        return StringHelper.ConvertUnicodeToSymbols(this.query.get());
    }

    public void setQuery(final String query) {
        this.query.set(query);
    }

    public StringProperty queryProperty() {
        return query;
    }

    public String getComment() {
        return comment.get();
    }

    public void setComment(final String comment) {
        this.comment.set(comment);
    }

    public StringProperty commentProperty() {
        return comment;
    }

    public StringProperty errors() {
        return errors;
    }

    public boolean isPeriodic() {
        return isPeriodic.get();
    }

    public SimpleBooleanProperty isPeriodicProperty() {
        return isPeriodic;
    }

    public void setIsPeriodic(final boolean isPeriodic) {
        this.isPeriodic.set(isPeriodic);
    }

    public Engine getEngine() {
        return engine;
    }

    public void setEngine(Engine engine) {
        this.engine = engine;
    }

    public void setType(QueryType type) {
        this.type.set(type);
    }

    public QueryType getType() {
        return this.type.get();
    }

    public ObjectProperty<QueryType> getTypeProperty() {
        return this.type;
    }

    public Consumer<Boolean> getSuccessConsumer() {
        return successConsumer;
    }

    public Consumer<Exception> getFailureConsumer() {
        return failureConsumer;
    }

    /**
     * Executes the query
     */
    public void execute() throws NoSuchElementException {
        if (getQueryState().equals(QueryState.RUNNING) || !StringValidator.validateQuery(getQuery()))
            return;

        if (getQuery().isEmpty()) {
            setQueryState(QueryState.SYNTAX_ERROR);
            addError("Query is empty");
            return;
        }

        setQueryState(QueryState.RUNNING);
        errors().set("");

        getEngine().enqueueRequest(this, this::handleQueryResponse, this::handleQueryBackendError);
    }

    private void handleQueryResponse(QueryProtos.QueryResponse value) {
        if (getQueryState() == QueryState.UNKNOWN) return;

        if (value.hasSuccess()) {
            setQueryState(QueryState.SUCCESSFUL);
            getSuccessConsumer().accept(true);

            switch (value.getResultCase()) {
                case COMPONENT:
                    JsonObject returnedComponent = (JsonObject) JsonParser.parseString(value.getComponent().getJson());
                    addGeneratedComponent(new Component(returnedComponent));
                    break;
                case REACHABILITY:
                    // Highlight edges in path
                    ArrayList<String> edgeIds = new ArrayList<>();
                    for (var decision : value.getReachabilityPath().getPath().getDecisionsList()) {
                        for (var edge : decision.getEdgesList()) {
                            edgeIds.add(edge.getId());
                        }
                    }

//                    highlightReachabilityEdges(edgeIds); // ToDo NIELS: Refactor
                    break;
            }
        } else {
            setQueryState(QueryState.ERROR);
            getFailureConsumer().accept(new BackendException.QueryErrorException(value.getError().getError()));
            getSuccessConsumer().accept(false);

            switch (value.getResultCase()) {
                case REFINEMENT:
                    getStateActionConsumer().accept(value.getRefinement().getRefinementState().getState().getState(),
                            new ArrayList<>());
                    break;

                case CONSISTENCY:
                    getStateActionConsumer().accept(value.getConsistency().getFailureState(),
                            new ArrayList<>());
                    break;

                case DETERMINISM:
                    getStateActionConsumer().accept(value.getDeterminism().getFailureState().getState(),
                            new ArrayList<>());
                    break;

                case IMPLEMENTATION:
                    getStateActionConsumer().accept(value.getImplementation().getFailureState().getState(),
                            new ArrayList<>());
                    break;

                case REACHABILITY:
                    // ToDo: Reachability failure state not implemented
                    getStateActionConsumer().accept(value.getConsistency().getFailureState(),
                            new ArrayList<>());
                    break;
            }
        }
    }

    private void handleQueryBackendError(Throwable t) {
        // If the query has been cancelled, ignore the error
        if (getQueryState() == QueryState.UNKNOWN) return;

        // Due to limit information provided by the engines, we can only show the following for unreachable locations
        if (getType() == QueryType.REACHABILITY) {
            Ecdar.showToast("Timeout (no response from backend): The reachability query failed. This might be due to the fact that the location is not reachable.");
        }

        // Each error starts with a capitalized description of the error equal to the gRPC error type encountered
        String errorType = t.getMessage().split(":\\s+", 2)[0];

        if ("DEADLINE_EXCEEDED".equals(errorType)) {
            setQueryState(QueryState.ERROR);
            getFailureConsumer().accept(new BackendException.QueryErrorException("The engine did not answer the request in time"));
        } else {
            try {
                setQueryState(QueryState.ERROR);
                getFailureConsumer().accept(new BackendException.QueryErrorException("The execution of this query failed with message:" + System.lineSeparator() + t.getLocalizedMessage()));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void addGeneratedComponent(Component newComponent) {
        Platform.runLater(() -> {
            newComponent.setTemporary(true);

            ObservableList<Component> listOfGeneratedComponents = Ecdar.getProject().getTempComponents();
            Component matchedComponent = null;

            for (Component currentGeneratedComponent : listOfGeneratedComponents) {
                int comparisonOfNames = currentGeneratedComponent.getName().compareTo(newComponent.getName());

                if (comparisonOfNames == 0) {
                    matchedComponent = currentGeneratedComponent;
                    break;
                } else if (comparisonOfNames < 0) {
                    break;
                }
            }

            if (matchedComponent == null) {
                UndoRedoStack.pushAndPerform(() -> { // Perform
                    Ecdar.getProject().getTempComponents().add(newComponent);
                }, () -> { // Undo
                    Ecdar.getProject().getTempComponents().remove(newComponent);
                }, "Created new component: " + newComponent.getName(), "add-circle");
            } else {
                // Remove current component with name and add the newly generated one
                Component finalMatchedComponent = matchedComponent;
                UndoRedoStack.pushAndPerform(() -> { // Perform
                    Ecdar.getProject().getTempComponents().remove(finalMatchedComponent);
                    Ecdar.getProject().getTempComponents().add(newComponent);
                }, () -> { // Undo
                    Ecdar.getProject().getTempComponents().remove(newComponent);
                    Ecdar.getProject().getTempComponents().add(finalMatchedComponent);
                }, "Created new component: " + newComponent.getName(), "add-circle");
            }

            Ecdar.getProject().addComponent(newComponent);
        });
    }

    /**
     * Getter for the state action consumer.
     *
     * @return The <a href="#stateConsumer">State Consumer</a>
     */
    public BiConsumer<ObjectProtos.State, List<String>> getStateActionConsumer() {
        return stateActionConsumer;
    }

    public void cancel() {
        if (getQueryState().equals(QueryState.RUNNING)) {
            forcedCancel = true;
            setQueryState(QueryState.UNKNOWN);
        }
    }

    public void addError(String e) {
        errors.set(errors.getValue() + e + "\n");
    }

    public String getCurrentErrors() {
        return errors.getValue();
    }

    @Override
    public GrpcRequest accept(GrpcRequestFactory requestFactory, Consumer<QueryProtos.QueryResponse> successConsumer, Consumer<Throwable> errorConsumer) {
        return requestFactory.create(this, this::handleQueryResponse, this::handleQueryBackendError);
    }

    @Override
    public JsonObject serialize() {
        final JsonObject result = new JsonObject();

        result.addProperty(QUERY, getType().getQueryName() + ": " + getQuery());
        result.addProperty(COMMENT, getComment());
        result.addProperty(IS_PERIODIC, isPeriodic());
        result.addProperty(ENGINE, engine.getName());

        return result;
    }

    @Override
    public void deserialize(final JsonObject json) {
        String query = json.getAsJsonPrimitive(QUERY).getAsString();

        if (query.contains(":")) {
            String[] queryFieldFromJSON = json.getAsJsonPrimitive(QUERY).getAsString().split(": ");
            setType(QueryType.fromString(queryFieldFromJSON[0]));
            setQuery(queryFieldFromJSON[1]);
        } else {
            setQuery(query);
        }

        setComment(json.getAsJsonPrimitive(COMMENT).getAsString());

        if (json.has(IS_PERIODIC)) {
            setIsPeriodic(json.getAsJsonPrimitive(IS_PERIODIC).getAsBoolean());
        }

        if (json.has(ENGINE)) {
            setEngine(BackendHelper.getEngineByName(json.getAsJsonPrimitive(ENGINE).getAsString()));
        } else {
            setEngine(BackendHelper.getDefaultEngine());
        }
    }
}
