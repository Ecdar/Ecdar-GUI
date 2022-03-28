package ecdar.controllers;

import ecdar.abstractions.Component;
import ecdar.abstractions.Declarations;
import ecdar.abstractions.HighLevelModelObject;
import ecdar.abstractions.EcdarSystem;
import ecdar.mutation.models.MutationTestPlan;
import ecdar.mutation.MutationTestPlanPresentation;
import ecdar.presentations.*;
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

    private final ObjectProperty<HighLevelModelObject> activeModel = new SimpleObjectProperty<>(null);
    private final HashMap<HighLevelModelObject, Pair<Double, Double>> ModelObjectTranslateMap = new HashMap<>();

    private DoubleProperty width, height;
    private BooleanProperty insetShouldShow;

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
        Platform.runLater(EcdarController.getActiveCanvasShellPresentation().getCanvasController()::leaveTextAreas);
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
        if (oldObject instanceof MutationTestPlan && !(newObject instanceof  MutationTestPlan)) ((CanvasShellPresentation) this.root.getParent()).getController().allowGrid();

        // We should not add the new object if it is null (e.g. when clearing the view)
        if (newObject == null) return;

        // Remove old object from view
        root.getChildren().removeIf(node -> node instanceof HighLevelModelPresentation);

        if (newObject instanceof Component) {
            setTranslateOfBox(newObject);
            activeComponentPresentation = new ComponentPresentation((Component) newObject);
            root.getChildren().add(activeComponentPresentation);

            // To avoid NullPointerException on initial model
            if (oldObject != null) ((CanvasShellPresentation) this.root.getParent()).getController().zoomHelper.resetZoom();
            
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

            ((CanvasShellPresentation) this.root.getParent()).getController().disallowGrid();

            activeComponentPresentation = null;
            root.getChildren().add(new MutationTestPlanPresentation((MutationTestPlan) newObject));
        } else {
            throw new IllegalStateException("Type of object is not supported.");
        }

        root.requestFocus();
    }

    private void setTranslateOfBox(final HighLevelModelObject newObject) {
        Platform.runLater(() -> {
            if (ModelObjectTranslateMap.containsKey(newObject)) {
                final Pair<Double, Double> restoreCoordinates = ModelObjectTranslateMap.get(newObject);
                root.setTranslateX(restoreCoordinates.getKey());
                root.setTranslateY(restoreCoordinates.getValue());
            } else {
                root.getChildren().get(0).setTranslateX(root.getWidth() / 2);
                root.getChildren().get(0).setTranslateY(root.getHeight() / 2);
            }
        });
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
}
