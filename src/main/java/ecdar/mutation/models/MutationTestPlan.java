package ecdar.mutation.models;

import com.google.gson.JsonPrimitive;
import ecdar.Ecdar;
import ecdar.abstractions.HighLevelModelObject;
import com.google.gson.JsonObject;
import javafx.beans.property.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A test plan for conducting model-based mutation testing on a component.
 */
public class MutationTestPlan extends HighLevelModelObject {
    private static final String PLAN_NAME_PREFIX = "Test ";

    private static final String TEST_MODEL_ID = "testModelId";
    private static final String MUTANTS_TEXT = "mutantsText";
    private static final String TEST_CASES_TEXT = "testCasesText";
    private static final String ACTION = "action";
    private static final String SUT_PATH = "sutPath";
    private static final String FORMAT = "exportFormat";
    private static final String DEMONIC = "useDemonic";
    private static final String ANGELIC_EXPORT = "useAngelic";


    private final StringProperty testModelId = new SimpleStringProperty("");
    private final StringProperty action = new SimpleStringProperty("");
    private final StringProperty sutPath = new SimpleStringProperty("");
    private final StringProperty format = new SimpleStringProperty("");
    private final BooleanProperty demonic = new SimpleBooleanProperty(false);
    private final BooleanProperty angelicWhenExport = new SimpleBooleanProperty(false);

    private final StringProperty mutantsText = new SimpleStringProperty("");
    private final StringProperty testCasesText = new SimpleStringProperty("");

    public List<MutationOperator> getOperators() {
        return operators;
    }

    private final List<MutationOperator> operators = new ArrayList<>();

    public enum Status {IDLE, WORKING, STOPPING}

    private final ObjectProperty<Status> status = new SimpleObjectProperty<>(Status.IDLE);


    /* Constructors */

    public MutationTestPlan() {
        generateName();
        operators.addAll(MutationOperator.getAllOperators());
    }

    public MutationTestPlan(final JsonObject json) {
        deserialize(json);
    }


    /* Properties */

    public String getTestModelId() {
        return testModelId.get();
    }

    public StringProperty testModelIdProperty() {
        return testModelId;
    }

    public void setTestModelId(final String testModelId) {
        this.testModelId.setValue(testModelId);
    }

    public String getMutantsText() {
        return mutantsText.get();
    }

    public StringProperty mutantsTextProperty() {
        return mutantsText;
    }

    public void setMutantsText(final String value) {
        mutantsText.set(value);
    }

    public String getTestCasesText() {
        return testCasesText.get();
    }

    public StringProperty testCasesTextProperty() {
        return testCasesText;
    }

    public void setTestCasesText(final String value) {
        testCasesText.set(value);
    }

    public String getAction() {
        return action.get();
    }

    public StringProperty actionProperty() {
        return action;
    }

    public void setAction(final String value) {
        action.set(value);
    }

    public String getSutPath() {
        return sutPath.get();
    }

    public StringProperty sutPathProperty() {
        return sutPath;
    }

    public void setSutPath(final String value) {
        sutPath.set(value);
    }

    public String getFormat() {
        return format.get();
    }

    public StringProperty formatProperty() {
        return format;
    }

    public void setFormat(final String value) {
        format.set(value);
    }

    public boolean isDemonic() {
        return demonic.get();
    }

    public BooleanProperty demonicProperty() {
        return demonic;
    }

    public void setDemonic(final boolean value) {
        demonic.set(value);
    }

    public boolean isAngelicWhenExport() {
        return angelicWhenExport.get();
    }

    public BooleanProperty angelicWhenExportProperty() {
        return angelicWhenExport;
    }

    public void setAngelicWhenExport(final boolean value) {
        angelicWhenExport.set(value);
    }

    public Status getStatus() {
        return status.get();
    }

    public ObjectProperty<Status> statusProperty() {
        return status;
    }

    public void setStatus(final Status value) {
        status.set(value);
    }


    /* Other methods */

    @Override
    public JsonObject serialize() {
        final JsonObject result = super.serialize();

        result.addProperty(TEST_MODEL_ID, getTestModelId());
        result.addProperty(ACTION, getAction());
        result.addProperty(SUT_PATH, getSutPath());
        result.addProperty(FORMAT, getFormat());
        result.addProperty(DEMONIC, isDemonic());
        result.addProperty(ANGELIC_EXPORT, isAngelicWhenExport());

        result.addProperty(MUTANTS_TEXT, getMutantsText());
        result.addProperty(TEST_CASES_TEXT, getTestCasesText());

        operators.forEach(operator -> result.addProperty(operator.getJsonName(), operator.isSelected()));

        return result;
    }

    @Override
    public void deserialize(final JsonObject json) {
        super.deserialize(json);

        setTestModelId(json.getAsJsonPrimitive(TEST_MODEL_ID).getAsString());
        setAction(json.getAsJsonPrimitive(ACTION).getAsString());
        setSutPath(json.getAsJsonPrimitive(SUT_PATH).getAsString());
        setFormat(json.getAsJsonPrimitive(FORMAT).getAsString());
        setDemonic(json.getAsJsonPrimitive(DEMONIC).getAsBoolean());
        setAngelicWhenExport(json.getAsJsonPrimitive(ANGELIC_EXPORT).getAsBoolean());

        setMutantsText(json.getAsJsonPrimitive(MUTANTS_TEXT).getAsString());
        setTestCasesText(json.getAsJsonPrimitive(TEST_CASES_TEXT).getAsString());

        operators.addAll(MutationOperator.getAllOperators());
        operators.forEach(operator -> {
            final JsonPrimitive primitive = json.getAsJsonPrimitive(operator.getJsonName());
            if (primitive != null) operator.setSelected(primitive.getAsBoolean());
        });
    }


    /**
     * Generate and sets a unique id for this system
     */
    private void generateName() {
        final HashSet<String> names = new HashSet<>();

        for (final MutationTestPlan plan : Ecdar.getProject().getTestPlans()){
            names.add(plan.getName());
        }

        for (int counter = 1; ; counter++) {
            if (!names.contains(PLAN_NAME_PREFIX + counter)){
                setName((PLAN_NAME_PREFIX + counter));
                return;
            }
        }
    }

    public List<MutationOperator> getSelectedMutationOperators() {
        return getOperators().stream().filter(MutationOperator::isSelected).collect(Collectors.toList());
    }

    public void clearResults() {
        setMutantsText("");
        setTestCasesText("");
    }
}
