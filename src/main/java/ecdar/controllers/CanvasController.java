package ecdar.controllers;

import com.jfoenix.controls.JFXRippler;
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
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.util.Pair;

import java.net.URL;
import java.util.HashMap;
import java.util.ResourceBundle;
import java.util.Stack;
import java.util.function.Consumer;

import static ecdar.presentations.Grid.GRID_SIZE;

public class CanvasController implements Initializable {
    public final double DECLARATION_Y_MARGIN = GRID_SIZE * 5.5;
    public ComponentPresentation activeComponentPresentation;

    public StackPane root;
    public Grid grid;
    public StackPane modelPane;
    public HBox toolbar;

    public JFXRippler zoomIn;
    public JFXRippler zoomOut;
    public JFXRippler zoomToFit;
    public JFXRippler resetZoom;

    public final ZoomHelper zoomHelper = new ZoomHelper();
    // This is whether to allow the user to turn on/off the grid.
    // While this is false, the grid is always hidden, no matter the user option.
    private final BooleanProperty allowGrid = new SimpleBooleanProperty(true);

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
        Platform.runLater(EcdarController.getActiveCanvasPresentation().getController()::leaveTextAreas);
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

        root.setPadding(new Insets(toolbar.getHeight()));

        leaveTextAreas = () -> root.requestFocus();
        leaveOnEnterPressed = (keyEvent) -> {
            if (keyEvent.getCode().equals(KeyCode.ENTER) || keyEvent.getCode().equals(KeyCode.ESCAPE)) {
                leaveTextAreas();
            }
        };
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

    /**
     * Updates if height of views should have offsets at the bottom.
     * Whether views should have an offset is based on the configuration of the error view.
     * @param shouldHave true iff views should have an offset
     */
    public void updateOffset(final Boolean shouldHave) {
        insetShouldShow.set(shouldHave);
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
            ModelObjectTranslateMap.put(oldObject, new Pair<>(modelPane.getTranslateX(), modelPane.getTranslateY()));
        }

        // if old object is a mutation test plan, and new object is not, allow grid to show
        if (oldObject instanceof MutationTestPlan && !(newObject instanceof  MutationTestPlan)) allowGrid();

        // We should not add the new object if it is null (e.g. when clearing the view)
        if (newObject == null) return;

        // Remove old object from view
        modelPane.getChildren().removeIf(node -> node instanceof HighLevelModelPresentation);

        if (newObject instanceof Component) {
            setTranslateOfBox(newObject);
            activeComponentPresentation = new ComponentPresentation((Component) newObject);
            modelPane.getChildren().add(activeComponentPresentation);

            // To avoid NullPointerException on initial model
            if (oldObject != null) zoomHelper.resetZoom();

        } else if (newObject instanceof Declarations) {
            activeComponentPresentation = null;
            modelPane.getChildren().add(new DeclarationPresentation((Declarations) newObject));
        } else if (newObject instanceof EcdarSystem) {
            setTranslateOfBox(newObject);
            activeComponentPresentation = null;
            modelPane.getChildren().add(new SystemPresentation((EcdarSystem) newObject));
        } else if (newObject instanceof MutationTestPlan) {
            this.disallowGrid();

            activeComponentPresentation = null;
            modelPane.getChildren().add(new MutationTestPlanPresentation((MutationTestPlan) newObject));
        } else {
            throw new IllegalStateException("Type of object is not supported.");
        }

        boolean shouldZoomBeActive = newObject instanceof Component || newObject instanceof EcdarSystem;
        toolbar.setVisible(shouldZoomBeActive);
        zoomHelper.setActive(shouldZoomBeActive);

        root.requestFocus();
    }

    private void setTranslateOfBox(final HighLevelModelObject newObject) {
        Platform.runLater(() -> {
            if (ModelObjectTranslateMap.containsKey(newObject)) {
                final Pair<Double, Double> restoreCoordinates = ModelObjectTranslateMap.get(newObject);
                modelPane.setTranslateX(restoreCoordinates.getKey());
                modelPane.setTranslateY(restoreCoordinates.getValue());
            } else {
                modelPane.setTranslateX(root.getWidth() / 2);
                modelPane.setTranslateY(root.getHeight() / 2);
            }
        });
    }

    /**
     * Gets the active component presentation.
     * This is null, if the declarations presentation is shown instead.
     * @return the active component presentation
     */
    public ComponentPresentation getActiveComponentPresentation() {
        return activeComponentPresentation;
    }

    @FXML
    private void zoomInClicked() {
        zoomHelper.zoomIn();
    }

    @FXML
    private void zoomOutClicked() {
        zoomHelper.zoomOut();
    }

    @FXML
    private void zoomToFitClicked() {
        zoomHelper.zoomToFit();
    }

    @FXML
    private void resetZoomClicked() {
        zoomHelper.resetZoom();
    }
}
