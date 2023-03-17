package ecdar.abstractions;

import EcdarProtoBuf.QueryProtos;
import com.google.gson.JsonParser;
import ecdar.Ecdar;
import ecdar.backend.*;
import ecdar.controllers.EcdarController;
import ecdar.utility.UndoRedoStack;
import ecdar.utility.helpers.StringValidator;
import ecdar.utility.serialize.Serializable;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.ObservableList;

import java.util.NoSuchElementException;
import java.util.function.Consumer;

public class Query implements Serializable {
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

    public Query(final String query, final String comment, final QueryState queryState, final Engine engine) {
        this.query.set(query);
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
        return query.get();
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

    public StringProperty errors() { return errors; }

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

        getEngine().enqueueQuery(this, this::handleQueryResponse, this::handleQueryBackendError);
    }

    private void handleQueryResponse(QueryProtos.QueryResponse value) {
        // If the query has been cancelled, ignore the result
        if (getQueryState() == QueryState.UNKNOWN) return;

        if (value.hasRefinement() && value.getRefinement().getSuccess()) {
            setQueryState(QueryState.SUCCESSFUL);
            getSuccessConsumer().accept(true);
        } else if (value.hasConsistency() && value.getConsistency().getSuccess()) {
            setQueryState(QueryState.SUCCESSFUL);
            getSuccessConsumer().accept(true);
        } else if (value.hasDeterminism() && value.getDeterminism().getSuccess()) {
            setQueryState(QueryState.SUCCESSFUL);
            getSuccessConsumer().accept(true);
        } else if (value.hasComponent()) {
            setQueryState(QueryState.SUCCESSFUL);
            getSuccessConsumer().accept(true);
            JsonObject returnedComponent = (JsonObject) JsonParser.parseString(value.getComponent().getComponent().getJson());
            addGeneratedComponent(new Component(returnedComponent));
        } else {
            setQueryState(QueryState.ERROR);
            getSuccessConsumer().accept(false);
        }
    }

    private void handleQueryBackendError(Throwable t) {
        // If the query has been cancelled, ignore the error
        if (getQueryState() == QueryState.UNKNOWN) return;

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

            ObservableList<Component> listOfGeneratedComponents = Ecdar.getProject().getTempComponents(); // ToDo NIELS: Refactor
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

        if(query.contains(":")) {
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

        if(json.has(ENGINE)) {
            setEngine(BackendHelper.getEngineByName(json.getAsJsonPrimitive(ENGINE).getAsString()));
        } else {
            setEngine(BackendHelper.getDefaultEngine());
        }
    }

    public void setForcedCancel(Boolean forcedCancel) {
        this.forcedCancel = forcedCancel;
    }

    public void addError(String e) {
        errors.set(errors.getValue() + e + "\n");
    }

    public String getCurrentErrors() {
        return errors.getValue();
    }
}
