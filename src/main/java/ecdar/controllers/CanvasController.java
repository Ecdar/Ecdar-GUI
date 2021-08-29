package ecdar.controllers;

import ecdar.abstractions.Component;
import ecdar.abstractions.Declarations;
import ecdar.abstractions.HighLevelModelObject;
import ecdar.abstractions.EcdarSystem;
import ecdar.mutation.models.MutationTestPlan;
import ecdar.mutation.MutationTestPlanPresentation;
import ecdar.presentations.*;
import ecdar.utility.helpers.ZoomHelper;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.event.EventHandler;
import javafx.fxml.Initializable;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.util.Pair;

import java.net.URL;
import java.util.HashMap;
import java.util.ResourceBundle;
import java.util.function.Consumer;

import static ecdar.presentations.Grid.GRID_SIZE;

public class CanvasController implements Initializable {
    public final double DECLARATION_Y_MARGIN = GRID_SIZE * 5.5;
    public ComponentPresentation activeComponentPresentation;

    public Pane root;

    public final ZoomHelper zoomHelper = new ZoomHelper();

    private final ObjectProperty<HighLevelModelObject> activeModel = new SimpleObjectProperty<>(null);
    private final HashMap<HighLevelModelObject, Pair<Double, Double>> ModelObjectTranslateMap = new HashMap<>();

    private DoubleProperty width, height;
    private BooleanProperty insetShouldShow;

    // This is whether to allow the user to turn on/off the grid.
    // While this is false, the grid is always hidden, no matter the user option.
    private BooleanProperty allowGrid = new SimpleBooleanProperty(true);

    public DoubleProperty getWidthProperty() {
        return width;
    }

    public DoubleProperty getHeightProperty() {
        return height;
    }

    public BooleanProperty getInsetShouldShow() {
        return insetShouldShow;
    }

    public HighLevelModelObject getActiveModel() {
        return activeModel.get();
    }

    /**
     * Sets the given model as the one to be active, e.g. to be shown on the screen.
     * @param model the given model
     */
    public void setActiveModel(final HighLevelModelObject model) {
        activeModel.set(model);
        Platform.runLater(MainController.getActiveCanvasPresentation().getController()::leaveTextAreas);
    }

    public ObjectProperty<HighLevelModelObject> activeComponentProperty() {
        return activeModel;
    }

    public void leaveTextAreas() {
        leaveTextAreas.run();
    }

    public EventHandler<KeyEvent> getLeaveTextAreaKeyHandler() {
        return getLeaveTextAreaKeyHandler(keyEvent -> {});
    }

    public EventHandler<KeyEvent> getLeaveTextAreaKeyHandler(final Consumer<KeyEvent> afterEnter) {
        return (keyEvent) -> {
            leaveOnEnterPressed.accept(keyEvent);
            afterEnter.accept(keyEvent);
        };
    }

    private Consumer<KeyEvent> leaveOnEnterPressed;
    private Runnable leaveTextAreas;

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        width = new SimpleDoubleProperty();
        height = new SimpleDoubleProperty();
        insetShouldShow = new SimpleBooleanProperty();
        insetShouldShow.set(true);

        root.widthProperty().addListener((observable, oldValue, newValue) -> width.setValue(newValue));
        root.heightProperty().addListener((observable, oldValue, newValue) -> height.setValue(newValue));

        activeModel.addListener((obs, oldModel, newModel) -> onActiveModelChanged(oldModel, newModel));

        leaveTextAreas = () -> root.requestFocus();

        leaveOnEnterPressed = (keyEvent) -> {
            if (keyEvent.getCode().equals(KeyCode.ENTER) || keyEvent.getCode().equals(KeyCode.ESCAPE)) {
                leaveTextAreas();
            }
        };
    }

    /**
     * Updates the component translate map with the old object.
     * Then removes old object from view and shows the new one.
     * @param oldObject old object
     * @param newObject new object
     */
    private void onActiveModelChanged(final HighLevelModelObject oldObject, final HighLevelModelObject newObject) {
        // If old object is a component or system, add to map in order to remember coordinate
        if (oldObject != null && (oldObject instanceof Component || oldObject instanceof EcdarSystem)) {
            ModelObjectTranslateMap.put(oldObject, new Pair<>(root.getTranslateX(), root.getTranslateY()));
        }

        // if old object is a mutation test plan, and new object is not, allow grid to show
        if (oldObject instanceof MutationTestPlan && !(newObject instanceof  MutationTestPlan)) allowGrid();

        // We should not add the new object if it is null (e.g. when clearing the view)
        if (newObject == null) return;

        // Remove old object from view
        root.getChildren().removeIf(node -> node instanceof HighLevelModelPresentation);

        if (newObject instanceof Component) {
            setTranslateOfBox(newObject);
            activeComponentPresentation = new ComponentPresentation((Component) newObject);
            root.getChildren().add(activeComponentPresentation);

            // To avoid NullPointerException on initial model
            if (oldObject != null) zoomHelper.resetZoom();
            
        } else if (newObject instanceof Declarations) {
            root.setTranslateX(0);
            root.setTranslateY(DECLARATION_Y_MARGIN);

            activeComponentPresentation = null;
            root.getChildren().add(new DeclarationPresentation((Declarations) newObject));
        } else if (newObject instanceof EcdarSystem) {
            setTranslateOfBox(newObject);
            activeComponentPresentation = null;
            root.getChildren().add(new SystemPresentation((EcdarSystem) newObject));
        } else if (newObject instanceof MutationTestPlan) {
            root.setTranslateX(0);
            root.setTranslateY(DECLARATION_Y_MARGIN);

            disallowGrid();

            activeComponentPresentation = null;
            root.getChildren().add(new MutationTestPlanPresentation((MutationTestPlan) newObject));
        } else {
            throw new IllegalStateException("Type of object is not supported.");
        }

        root.requestFocus();
    }

    private void setTranslateOfBox(final HighLevelModelObject newObject) {
        if (ModelObjectTranslateMap.containsKey(newObject)) {
            final Pair<Double, Double> restoreCoordinates = ModelObjectTranslateMap.get(newObject);
            root.setTranslateX(restoreCoordinates.getKey());
            root.setTranslateY(restoreCoordinates.getValue());
        } else {
            root.setTranslateX(GRID_SIZE * 12);
            root.setTranslateY(GRID_SIZE * 8);
        }
    }

    /**
     * Updates if height of views should have offsets at the bottom.
     * Whether views should have an offset is based on the configuration of the error view.
     * @param shouldHave true iff views should have an offset
     */
    public void updateOffset(final Boolean shouldHave) {
        insetShouldShow.set(shouldHave);
    }

    /**
     * Gets the active component presentation.
     * This is null, if the declarations presentation is shown instead.
     * @return the active component presentation
     */
    ComponentPresentation getActiveComponentPresentation() {
        return activeComponentPresentation;
    }

    /**
     * Allows the user to turn the grid on/off.
     * If the user has currently chosen on, then this method also shows the grid.
     */
    public void allowGrid() {
        allowGrid.set(true);
    }

    /**
     * Disallows the user to turn the grid on/off.
     * Also hides the grid.
     */
    public void disallowGrid() {
        allowGrid.set(false);
    }

    public BooleanProperty allowGridProperty() {
        return allowGrid;
    }

    public boolean isGridAllowed() {
        return allowGrid.get();
    }

    public void resetCurrentActiveModelPlacement() {
        if (activeModel.get() instanceof Component) {
            this.resetComponentBoxPlacement((Component)activeModel.get());
        }
    }

    private void resetComponentBoxPlacement(Component component) {
        component.getBox().setX(5.0D);
        component.getBox().setY(5.0D);
    }
}
