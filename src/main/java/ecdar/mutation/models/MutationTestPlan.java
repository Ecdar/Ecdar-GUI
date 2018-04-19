package ecdar.mutation.models;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import ecdar.Ecdar;
import ecdar.abstractions.Component;
import ecdar.abstractions.HighLevelModelObject;
import ecdar.mutation.operators.MutationOperator;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;

import java.util.*;
import java.util.stream.Collectors;

/**
 * A test plan for conducting model-based mutation testing on a component.
 */
public class MutationTestPlan extends HighLevelModelObject {
    /**
     * The status of the test plan.
     * STOPPING: Stopped by the user
     * ERROR: An error has just occurred, and we are waiting for the execution to stop because of it
     */
    public enum Status {IDLE, WORKING, STOPPING, ERROR}

    private static final String PLAN_NAME_PREFIX = "Test ";

    // JSON constants
    private static final String TEST_MODEL_ID = "testModelId";
    private static final String ACTION = "action";
    private static final String SUT_PATH = "sutPath";
    private static final String FORMAT = "exportFormat";
    private static final String DEMONIC = "useDemonic";
    private static final String ANGELIC_EXPORT = "useAngelic";
    private static final String MAX_GENERATION_THREADS = "maxGenerationThreads";
    private static final String MAX_SUT_INSTANCES = "maxSutInstances";
    private static final String MAX_OUTPUT_WAIT_TIME = "maxOutputWaitTime";
    private static final String VERIFYTGA_TRIES = "verifytgaTries";
    private static final String TIME_UNIT = "timeUnit";
    private static final String STEP_BOUNDS = "stepBounds";
    private static final String SIMULATE_TIME = "simulateTime";
    private static final String VERDICT_PREFIX = "verdict";

    // General fields
    private final ObjectProperty<Component> testModel = new SimpleObjectProperty<>(null);
    private final StringProperty action = new SimpleStringProperty("");
    private final List<MutationOperator> operators = new ArrayList<>();
    private final ObjectProperty<Status> status = new SimpleObjectProperty<>(Status.IDLE);

    // For testing
    private final StringProperty sutPath = new SimpleStringProperty("");
    private final BooleanProperty demonic = new SimpleBooleanProperty(true);
    private final IntegerProperty concurrentGenerationThreads = new SimpleIntegerProperty(10);
    private final IntegerProperty concurrentSutInstances = new SimpleIntegerProperty(1);
    private final IntegerProperty maxOutputWaitTime = new SimpleIntegerProperty(5);
    private final IntegerProperty verifytgaTries = new SimpleIntegerProperty(3);
    private final IntegerProperty timeUnit = new SimpleIntegerProperty(1000);
    private final IntegerProperty stepBounds = new SimpleIntegerProperty(100);
    private final BooleanProperty simulateTime = new SimpleBooleanProperty(false);

    // Temporary values for displaying resultViews of testing
    private final ObservableList<Text> progressTexts = FXCollections.observableArrayList();
    private final StringProperty mutantsText = new SimpleStringProperty("");
    private final StringProperty testCasesText = new SimpleStringProperty("");
    private final StringProperty testTimeText = new SimpleStringProperty("");

    private final ListProperty<TestResult> results = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final Map<TestResult.Verdict, BooleanProperty> shouldShowMap = new HashMap<>();


    // For exporting
    private final BooleanProperty angelicWhenExport = new SimpleBooleanProperty(false);
    private final StringProperty format = new SimpleStringProperty("");


    /* Constructors */

    /**
     * Constructs a plan for scratch.
     */
    public MutationTestPlan() {
        generateName();
        operators.addAll(MutationOperator.getAllOperators());

        for (final TestResult.Verdict verdict : TestResult.Verdict.values()) {
            shouldShowMap.put(verdict, new SimpleBooleanProperty(false));
        }

        // Show failed test-cases as default
        shouldShowMap.get(TestResult.Verdict.FAIL_NORMAL).set(true);
        shouldShowMap.get(TestResult.Verdict.FAIL_PRIMARY).set(true);
    }

    /**
     * Constructs a plan from JSON.
     * @param json the JSON
     */
    public MutationTestPlan(final JsonObject json) {
        deserialize(json);
    }


    /* Properties */

    public Component getTestModel() {
        return testModel.get();
    }
    public ObjectProperty<Component> getTestModelProperty() {
        return testModel;
    }
    public void setTestModel(final Component testModel) {
        this.testModel.setValue(testModel);
    }

    public String getMutantsText() {
        return mutantsText.get();
    }
    public StringProperty getMutantsTextProperty() {
        return mutantsText;
    }
    public void setMutantsText(final String value) {
        mutantsText.set(value);
    }

    public String getTestCasesText() {
        return testCasesText.get();
    }
    public StringProperty getTestCasesTextProperty() {
        return testCasesText;
    }
    public void setTestCasesText(final String value) {
        testCasesText.set(value);
    }

    public String getAction() {
        return action.get();
    }
    public StringProperty getActionProperty() {
        return action;
    }
    public void setAction(final String value) {
        action.set(value);
    }

    public String getSutPath() {
        return sutPath.get();
    }
    public StringProperty getSutPathProperty() {
        return sutPath;
    }
    public void setSutPath(final String value) {
        sutPath.set(value);
    }

    public String getFormat() {
        return format.get();
    }
    public StringProperty getFormatProperty() {
        return format;
    }
    public void setFormat(final String value) {
        format.set(value);
    }

    public boolean isDemonic() {
        return demonic.get();
    }
    public BooleanProperty getDemonicProperty() {
        return demonic;
    }
    public void setDemonic(final boolean value) {
        demonic.set(value);
    }

    public boolean isAngelicWhenExport() {
        return angelicWhenExport.get();
    }
    public BooleanProperty getAngelicWhenExportProperty() {
        return angelicWhenExport;
    }
    public void setAngelicWhenExport(final boolean value) {
        angelicWhenExport.set(value);
    }

    public Status getStatus() {
        return status.get();
    }
    public ObjectProperty<Status> getStatusProperty() {
        return status;
    }
    public void setStatus(final Status value) {
        status.set(value);
    }

    public List<MutationOperator> getOperators() {
        return operators;
    }

    public int getConcurrentGenerationThreads() {
        return concurrentGenerationThreads.get();
    }
    public IntegerProperty getConcurrentGenerationsThreadsProperty() {
        return concurrentGenerationThreads;
    }
    public void setConcurrentGenerationThreads(final int concurrentGenerationThreads) {
        this.concurrentGenerationThreads.set(concurrentGenerationThreads);
    }

    public int getConcurrentSutInstances() {
        return concurrentSutInstances.get();
    }
    public IntegerProperty getConcurrentSutInstancesProperty() {
        return concurrentSutInstances;
    }
    public void setConcurrentSutInstances(final int concurrentSutInstances) {
        this.concurrentSutInstances.set(concurrentSutInstances);
    }

    /**
     * Gets the maximum allowed time (in time units) to delay for the execution of a single rule,
     * before giving the inclusive verdict.
     * @return the maximum allowed time
     */
    public int getOutputWaitTime() {
        return maxOutputWaitTime.get();
    }
    public IntegerProperty getOutputWaitTimeProperty() {
        return maxOutputWaitTime;
    }
    public void setOutputWaitTime(final int outputWaitTime){
        this.maxOutputWaitTime.set(outputWaitTime);
    }

    public ObservableList<TestResult> getResults() {
        return results.get();
    }

    public int getVerifytgaTries() {
        return verifytgaTries.get();
    }
    public IntegerProperty getVerifytgaTriesProperty() {
        return verifytgaTries;
    }
    public void setVerifytgaTries(final int verifytgaTries) {
        this.verifytgaTries.set(verifytgaTries);
    }

    /**
     * Gets the time (in ms) that corresponds to a time unit.
     * @return the time that corresponds to a time unit
     */
    public int getTimeUnit() {
        return timeUnit.get();
    }
    public IntegerProperty getTimeUnitProperty() {
        return timeUnit;
    }
    public void setTimeUnit(final int timeUnit) {
        this.timeUnit.set(timeUnit);
    }

    public ObservableList<Text> getProgressTexts() {
        return progressTexts;
    }

    public String getTestTimeText() {
        return testTimeText.get();
    }
    public StringProperty getTestTimeTextProperty() {
        return testTimeText;
    }
    public void setTestTimeText(final String testTimeText) {
        this.testTimeText.set(testTimeText);
    }

    public int getStepBounds() {
        return stepBounds.get();
    }
    public IntegerProperty getStepBoundsProperty() {
        return stepBounds;
    }
    public void setStepBounds(final int stepBounds) {
        this.stepBounds.set(stepBounds);
    }

    public boolean isSimulateTime() {
        return simulateTime.get();
    }
    public BooleanProperty getSimulateTimeProperty() {
        return simulateTime;
    }
    public void setSimulateTime(final boolean simulateTime) {
        this.simulateTime.set(simulateTime);
    }

    /* Other methods */

    @Override
    public JsonObject serialize() {
        final JsonObject result = super.serialize();

        if (getTestModel() != null) result.addProperty(TEST_MODEL_ID, getTestModel().getName());
        result.addProperty(ACTION, getAction());
        result.addProperty(SUT_PATH, getSutPath());
        result.addProperty(FORMAT, getFormat());
        result.addProperty(DEMONIC, isDemonic());
        result.addProperty(ANGELIC_EXPORT, isAngelicWhenExport());

        operators.forEach(operator -> result.addProperty(operator.getCodeName(), operator.isSelected()));

        result.addProperty(MAX_GENERATION_THREADS, getConcurrentGenerationThreads());
        result.addProperty(MAX_SUT_INSTANCES, getConcurrentSutInstances());
        result.addProperty(MAX_OUTPUT_WAIT_TIME, getOutputWaitTime());
        result.addProperty(VERIFYTGA_TRIES, getVerifytgaTries());
        result.addProperty(TIME_UNIT, getTimeUnit());
        result.addProperty(STEP_BOUNDS, getStepBounds());
        result.addProperty(SIMULATE_TIME, isSimulateTime());

        for (final TestResult.Verdict verdict : shouldShowMap.keySet())
            result.addProperty(VERDICT_PREFIX + verdict.toString(), shouldShow(verdict));

        return result;
    }

    @Override
    public void deserialize(final JsonObject json) {
        super.deserialize(json);

        JsonPrimitive primitive = json.getAsJsonPrimitive(TEST_MODEL_ID);
        if (primitive != null) setTestModel(Ecdar.getProject().findComponent(primitive.getAsString()));

        setAction(json.getAsJsonPrimitive(ACTION).getAsString());
        setSutPath(json.getAsJsonPrimitive(SUT_PATH).getAsString());
        setFormat(json.getAsJsonPrimitive(FORMAT).getAsString());
        setDemonic(json.getAsJsonPrimitive(DEMONIC).getAsBoolean());
        setAngelicWhenExport(json.getAsJsonPrimitive(ANGELIC_EXPORT).getAsBoolean());

        operators.addAll(MutationOperator.getAllOperators());
        operators.forEach(operator -> {
            final JsonPrimitive opPrimitive = json.getAsJsonPrimitive(operator.getCodeName());
            if (opPrimitive != null) operator.setSelected(opPrimitive.getAsBoolean());
        });

        primitive = json.getAsJsonPrimitive(MAX_GENERATION_THREADS);
        if (primitive != null) setConcurrentGenerationThreads(primitive.getAsInt());

        primitive = json.getAsJsonPrimitive(MAX_SUT_INSTANCES);
        if (primitive != null) setConcurrentSutInstances(primitive.getAsInt());

        primitive = json.getAsJsonPrimitive(MAX_OUTPUT_WAIT_TIME);
        if (primitive != null) setOutputWaitTime(primitive.getAsInt());

        primitive = json.getAsJsonPrimitive(VERIFYTGA_TRIES);
        if (primitive != null) setVerifytgaTries(primitive.getAsInt());

        primitive = json.getAsJsonPrimitive(TIME_UNIT);
        if (primitive != null) setTimeUnit(primitive.getAsInt());

        primitive = json.getAsJsonPrimitive(STEP_BOUNDS);
        if (primitive != null) setStepBounds(primitive.getAsInt());

        primitive = json.getAsJsonPrimitive(SIMULATE_TIME);
        if (primitive != null) setSimulateTime(primitive.getAsBoolean());

        for (final TestResult.Verdict verdict : TestResult.Verdict.values()) {
            primitive = json.getAsJsonPrimitive(VERDICT_PREFIX + verdict.toString());
            shouldShowMap.put(verdict, new SimpleBooleanProperty(primitive != null && primitive.getAsBoolean()));
        }
    }


    /**
     * Generate and sets a unique id for this system.
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

    /**
     * Gets the mutation operators selected by the user.
     * @return the selected operators
     */
    public List<MutationOperator> getSelectedMutationOperators() {
        return getOperators().stream().filter(MutationOperator::isSelected).collect(Collectors.toList());
    }

    /**
     * Clears the texts used to display resultViews.
     */
    public void clearResults() {
        setMutantsText("");
        setTestCasesText("");
        setTestTimeText("");
        getResults().clear();
    }

    /**
     * Writes progress to the user.
     * @param message the message to display
     */
    public void writeProgress(final String message) {
        final Text text = new Text(message);
        text.setFill(Color.web("#333333"));
        writeProgress(text);
    }

    /**
     * Writes progress to the user.
     * @param text the message to display
     */
    public void writeProgress(final Text text) {
        progressTexts.clear();
        progressTexts.add(text);
    }

    /**
     * Gets if we should stop working.
     * @return true iff we should stop
     */
    public boolean shouldStop() {
        return getStatus().equals(MutationTestPlan.Status.STOPPING) ||
                getStatus().equals(MutationTestPlan.Status.ERROR);
    }

    /**
     * Adds a result.
     * @param result the result to add
     */
    public void addResult(final TestResult result) {
        getResults().add(result);
    }

    /**
     * Gets the resultViews matching a specified verdict.
     * @param verdict the verdict to filter with
     * @return the matching resultViews
     */
    public List<TestResult> getResults(final TestResult.Verdict... verdict) {
        return getResults().filtered(result -> Arrays.asList(verdict).contains(result.getVerdict()));
    }

    /**
     * Gets the property for whether to show resultViews of some type of verdict.
     * @param verdict the type of verdict to search for
     * @return the property for whether to show the resultViews
     */
    public BooleanProperty getShouldShowProperty(final TestResult.Verdict verdict) {
        return shouldShowMap.get(verdict);
    }

    /**
     * Gets if we should show the results with a specified verdict.
     * @param verdict the specified verdict
     * @return true iff we should show the results
     */
    public boolean shouldShow(final TestResult.Verdict verdict) {
        return getShouldShowProperty(verdict).get();
    }

    /**
     * Gets the test results that should be shown to the user.
     * @return the results to show
     */
    public List<TestResult> getResultsToShow() {
        return getResults().filtered(result -> getVerdictsToShow().contains(result.getVerdict()));
    }

    /**
     * Gets the test verdicts for which test results of those verdicts should be shown to the user.
     * @return the verdicts to show
     */
    private List<TestResult.Verdict> getVerdictsToShow() {
        return shouldShowMap.keySet().stream().filter(verdict -> shouldShowMap.get(verdict).get()).collect(Collectors.toList());
    }

    /**
     * Removes a test result.
     * @param result the result to remove
     */
    public synchronized void removeResult(final TestResult result) {
        getResults().remove(result);
    }
}
