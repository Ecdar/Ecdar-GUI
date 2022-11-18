package ecdar.abstractions;

import EcdarProtoBuf.ObjectProtos;
import ecdar.Ecdar;
import ecdar.backend.*;
import ecdar.controllers.EcdarController;
import ecdar.utility.helpers.StringHelper;
import ecdar.utility.serialize.Serializable;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.beans.property.*;

import java.util.function.Consumer;

public class Query implements Serializable {
    private static final String QUERY = "query";
    private static final String COMMENT = "comment";
    private static final String IS_PERIODIC = "isPeriodic";
    private static final String BACKEND = "backend";

    private final StringProperty query = new SimpleStringProperty("");
    private final StringProperty comment = new SimpleStringProperty("");
    private final StringProperty errors = new SimpleStringProperty("");
    private final SimpleBooleanProperty isPeriodic = new SimpleBooleanProperty(false);
    private final ObjectProperty<QueryState> queryState = new SimpleObjectProperty<>(QueryState.UNKNOWN);
    private final ObjectProperty<QueryType> type = new SimpleObjectProperty<>();
    private BackendInstance backend;


    private final Consumer<Boolean> successConsumer = (aBoolean) -> {
        if (aBoolean) {
            for (Component c : Ecdar.getProject().getComponents()) {
                c.removeFailingLocations();
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

    private final Consumer<ObjectProtos.State> stateConsumer = (state) -> {
        for (Component c : Ecdar.getProject().getComponents()) {
            c.removeFailingLocations();
            if (query.getValue().contains(c.getName())) {
                for (ObjectProtos.Location location : state.getLocationTuple().getLocationsList()) {
                    c.addFailingLocation(location.getId());
                }
            }
        }
    };

    public Query(final String query, final String comment, final QueryState queryState) {
        this.setQuery(query);
        this.comment.set(comment);
        this.queryState.set(queryState);
        setBackend(BackendHelper.getDefaultBackendInstance());
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

    public BackendInstance getBackend() {
        return backend;
    }

    public void setBackend(BackendInstance backend) {
        this.backend = backend;
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
     * Getter for the state consumer.
     * @return The <a href="#stateConsumer">State Consumer</a>
     */
    public Consumer<ObjectProtos.State> getStateConsumer() {
        return stateConsumer;
    }

    @Override
    public JsonObject serialize() {
        final JsonObject result = new JsonObject();

        result.addProperty(QUERY, getType().getQueryName() + ": " + getQuery());
        result.addProperty(COMMENT, getComment());
        result.addProperty(IS_PERIODIC, isPeriodic());
        result.addProperty(BACKEND, backend.getName());

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

        if(json.has(BACKEND)) {
            setBackend(BackendHelper.getBackendInstanceByName(json.getAsJsonPrimitive(BACKEND).getAsString()));
        } else {
            setBackend(BackendHelper.getDefaultBackendInstance());
        }
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
}
