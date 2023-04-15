package ecdar.controllers;

import com.jfoenix.controls.JFXRippler;
import ecdar.Ecdar;
import ecdar.mutation.MutationTestPlanPresentation;
import ecdar.presentations.*;
import ecdar.utility.helpers.ZoomHelper;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.util.Pair;

import java.net.URL;
import java.util.HashMap;
import java.util.ResourceBundle;
import java.util.function.Consumer;

public class CanvasController implements Initializable {
    public StackPane root;
    public StackPane zoomablePane;
    public StackPane modelPane;
    public HBox toolbar;

    public JFXRippler zoomIn;
    public JFXRippler zoomOut;
    public JFXRippler zoomToFit;
    public JFXRippler resetZoom;
    public final ZoomHelper zoomHelper = new ZoomHelper();
    public final double DECLARATION_Y_MARGIN = Ecdar.CANVAS_PADDING * 5.5;
    public ComponentPresentation activeComponentPresentation;

    private final ObjectProperty<HighLevelModelPresentation> activeModelPresentation = new SimpleObjectProperty<>(null);
    private final HashMap<HighLevelModelPresentation, Pair<Double, Double>> ModelObjectTranslateMap = new HashMap<>();
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

    public HighLevelModelPresentation getActiveModelPresentation() {
        return activeModelPresentation.get();
    }

    /**
     * Sets the given model as the one to be active, e.g. to be shown on the screen.
     * @param model the given model
     */
    public void setActiveModelPresentation(final HighLevelModelPresentation model) {
        activeModelPresentation.set(model);
        Platform.runLater(Ecdar.getPresentation().getController().getEditorPresentation().getController().getActiveCanvasPresentation().getController()::leaveTextAreas);
        Platform.runLater(zoomHelper::zoomToFit);
    }

    public ObjectProperty<HighLevelModelPresentation> activeModelProperty() {
        return activeModelPresentation;
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

        activeModelPresentation.addListener((obs, oldModel, newModel) -> {
            onActiveModelChanged(oldModel, newModel);
        });

        Platform.runLater(() -> leaveTextAreas = () -> root.requestFocus());
        leaveOnEnterPressed = (keyEvent) -> {
            if (keyEvent.getCode().equals(KeyCode.ENTER) || keyEvent.getCode().equals(KeyCode.ESCAPE)) {
                leaveTextAreas();
            }
        };

        // Enable zoom
        modelPane.scaleXProperty().bind(zoomHelper.currentZoomFactor);
        modelPane.scaleYProperty().bind(zoomHelper.currentZoomFactor);
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
    private void onActiveModelChanged(final HighLevelModelPresentation oldObject, final HighLevelModelPresentation newObject) {
        // If old object is a component or system, add to map in order to remember coordinate
        if (oldObject instanceof ComponentPresentation || oldObject instanceof SystemPresentation) {
            ModelObjectTranslateMap.put(oldObject, new Pair<>(modelPane.getTranslateX(), modelPane.getTranslateY()));
        }

        // We should not add the new object if it is null (e.g. when clearing the view)
        if (newObject == null) return;

        // Remove old object from view
        modelPane.getChildren().removeIf(node -> node instanceof HighLevelModelPresentation);

        if (newObject instanceof ComponentPresentation) {
            activeComponentPresentation = (ComponentPresentation) newObject;
            modelPane.getChildren().add(activeComponentPresentation);

            // To avoid NullPointerException on initial model
            Platform.runLater(zoomHelper::resetZoom);

        } else if (newObject instanceof DeclarationsPresentation) {
            activeComponentPresentation = null;
            modelPane.getChildren().add(newObject);

            // Bind size of Declaration to size of the model pane to ensure alignment and avoid drag
            DeclarationsController declarationsController = (DeclarationsController) newObject.getController();
            declarationsController.root.minWidthProperty().bind(modelPane.minWidthProperty());
            declarationsController.root.maxWidthProperty().bind(modelPane.maxWidthProperty());
            declarationsController.root.minHeightProperty().bind(modelPane.minHeightProperty());
            declarationsController.root.maxHeightProperty().bind(modelPane.maxHeightProperty());
        } else if (newObject instanceof SystemPresentation) {
            activeComponentPresentation = null;
            modelPane.getChildren().add(newObject);
        } else if (newObject instanceof MutationTestPlanPresentation) {
            activeComponentPresentation = null;
            modelPane.getChildren().add(newObject);
        } else {
            throw new IllegalStateException("Type of object is not supported.");
        }

        boolean shouldZoomBeActive = newObject instanceof ComponentPresentation || newObject instanceof SystemPresentation;
        setZoomAvailable(shouldZoomBeActive);

        root.requestFocus();
    }

    private void setZoomAvailable(boolean shouldZoomBeActive) {
        toolbar.setVisible(shouldZoomBeActive);
        toolbar.getParent().setMouseTransparent(!shouldZoomBeActive); // Avoid mouse being intercepted on declaration
        zoomHelper.setActive(shouldZoomBeActive);
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
