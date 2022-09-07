package ecdar.abstractions;

import ecdar.Ecdar;
import ecdar.backend.*;
import ecdar.controllers.EcdarController;
import ecdar.utility.helpers.StringValidator;
import ecdar.utility.serialize.Serializable;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.beans.property.*;

import java.util.HashMap;
import java.util.Map;

public class Query implements Serializable {
    private static final String QUERY = "query";
    private static final String COMMENT = "comment";
    private static final String IS_PERIODIC = "isPeriodic";
    private static final String IGNORED_INPUTS = "ignoredInputs";
    private static final String IGNORED_OUTPUTS = "ignoredOutputs";
    private static final String BACKEND = "backend";

    public final HashMap<String, Boolean> ignoredInputs = new HashMap<>();
    public final HashMap<String, Boolean> ignoredOutputs = new HashMap<>();

    private final ObjectProperty<QueryState> queryState = new SimpleObjectProperty<>(QueryState.UNKNOWN);
    private final StringProperty query = new SimpleStringProperty("");
    private final StringProperty comment = new SimpleStringProperty("");
    private final SimpleBooleanProperty isPeriodic = new SimpleBooleanProperty(false);
    private final StringProperty errors = new SimpleStringProperty("");
    private final ObjectProperty<QueryType> type = new SimpleObjectProperty<>();
    private BackendInstance backend;
    private Runnable runQuery;

    public Query(final String query, final String comment, final QueryState queryState) {
        this.query.set(query);
        this.comment.set(comment);
        this.queryState.set(queryState);
        setBackend(BackendHelper.getDefaultBackendInstance());

        initializeRunQuery();
    }

    public Query(final JsonObject jsonElement) {
        deserialize(jsonElement);

        initializeRunQuery();
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

    private Boolean forcedCancel = false;

    private void initializeRunQuery() {
        runQuery = () -> {
            setQueryState(QueryState.RUNNING);
            forcedCancel = false;
            errors.set("");
            
            if (getQuery().isEmpty()) {
                setQueryState(QueryState.SYNTAX_ERROR);
                this.addError("Query is empty");
                return;
            }

            Ecdar.getBackendDriver().addQueryToExecutionQueue(getType().getQueryName() + ": " + getQuery() + " " + getIgnoredInputOutputsOnQuery(),
                    getBackend(),
                    aBoolean -> {
                        if (aBoolean) {
                            setQueryState(QueryState.SUCCESSFUL);
                        } else {
                            setQueryState(QueryState.ERROR);
                        }
                    },
                    e -> {
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
                    },
                    new QueryListener(this)
            );
        };
    }

    @Override
    public JsonObject serialize() {
        final JsonObject result = new JsonObject();

        result.addProperty(QUERY, getType().getQueryName() + ": " + getQuery());
        result.addProperty(COMMENT, getComment());
        result.addProperty(IS_PERIODIC, isPeriodic());

        result.add(IGNORED_INPUTS, getHashMapAsJsonObject(ignoredInputs));
        result.add(IGNORED_OUTPUTS, getHashMapAsJsonObject(ignoredOutputs));

        result.addProperty(BACKEND, backend.getName());

        return result;
    }

    private JsonObject getHashMapAsJsonObject(HashMap<String, Boolean> ignoredOutputs) {
        JsonObject resultingJsonObject = new JsonObject();
        for (Map.Entry<String, Boolean> currentPair : ignoredOutputs.entrySet()) {
            resultingJsonObject.addProperty(currentPair.getKey(), currentPair.getValue());
        }
        return resultingJsonObject;
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

        if (json.has(IGNORED_INPUTS)) {
            deserializeJsonObjectToMap(json.getAsJsonObject(IGNORED_INPUTS), ignoredInputs);
        }

        if (json.has(IGNORED_OUTPUTS)) {
            deserializeJsonObjectToMap(json.getAsJsonObject(IGNORED_OUTPUTS), ignoredOutputs);
        }

        if(json.has(BACKEND)) {
            setBackend(BackendHelper.getBackendInstanceByName(json.getAsJsonPrimitive(BACKEND).getAsString()));
        } else {
            setBackend(BackendHelper.getDefaultBackendInstance());
        }
    }

    private void deserializeJsonObjectToMap(JsonObject jsonObject, HashMap<String, Boolean> associatedMap) {
        jsonObject.entrySet().forEach((entry) -> associatedMap.put(entry.getKey(), entry.getValue().getAsBoolean()));
    }

    public void run() {
        if (StringValidator.validateQuery(query.get())) runQuery.run();
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

    private String getIgnoredInputOutputsOnQuery() {
        if (!BackendHelper.backendSupportsInputOutputs(this.backend) || (!ignoredInputs.containsValue(true) && !ignoredOutputs.containsValue(true))) {
            return "";
        }

        // Create StringBuilder starting with a quotation mark to signal start of extra outputs
        StringBuilder ignoredInputOutputsStringBuilder = new StringBuilder("--ignored_outputs=\"");

        // Append outputs, comma separated
        appendMapItemsWithValueTrue(ignoredInputOutputsStringBuilder, ignoredOutputs);

        // Append quotation marks to signal end of outputs and start of inputs
        ignoredInputOutputsStringBuilder.append("\" --ignored_inputs=\"");

        // Append inputs, comma separated
        appendMapItemsWithValueTrue(ignoredInputOutputsStringBuilder, ignoredInputs);

        // Append quotation mark to signal end of extra inputs
        ignoredInputOutputsStringBuilder.append("\"");

        return ignoredInputOutputsStringBuilder.toString();
    }

    private void appendMapItemsWithValueTrue(StringBuilder stringBuilder, Map<String, Boolean> map) {
        map.forEach((key, value) -> {
            if (value) {
                stringBuilder.append(key);
                stringBuilder.append(",");
            }
        });

        if (stringBuilder.lastIndexOf(",") + 1 == stringBuilder.length()) {
            stringBuilder.setLength(stringBuilder.length() - 1);
        }
    }
}
