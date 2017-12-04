package SW9.controllers;

import SW9.abstractions.Component;
import SW9.abstractions.Declarations;
import SW9.abstractions.HighLevelModelObject;
import SW9.abstractions.SystemModel;
import SW9.presentations.*;
import SW9.utility.helpers.SelectHelper;
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

import static SW9.presentations.Grid.GRID_SIZE;

public class CanvasController implements Initializable {
    public final static double DECLARATION_X_MARGIN = GRID_SIZE * 5.5;

    public Pane root;

    private final static ObjectProperty<HighLevelModelObject> activeModel = new SimpleObjectProperty<>(null);
    private final static HashMap<HighLevelModelObject, Pair<Double, Double>> ModelObjectTranslateMap = new HashMap<>();

    private static DoubleProperty width, height;
    private static BooleanProperty insetShouldShow;
    private ComponentPresentation activeComponentPresentation;

    public static DoubleProperty getWidthProperty() {
        return width;
    }

    public static DoubleProperty getHeightProperty() {
        return height;
    }

    public static BooleanProperty getInsetShouldShow() {
        return insetShouldShow;
    }

    public static HighLevelModelObject getActiveModel() {
        return activeModel.get();
    }

    /**
     * Sets the given model as the one to be active, e.g. to be shown on the screen.
     * @param model the given model
     */
    public static void setActiveModel(final HighLevelModelObject model) {
        CanvasController.activeModel.set(model);
        Platform.runLater(CanvasController::leaveTextAreas);
    }

    public static ObjectProperty<HighLevelModelObject> activeComponentProperty() {
        return activeModel;
    }

    public static void leaveTextAreas() {
        leaveTextAreas.run();
    }

    public static EventHandler<KeyEvent> getLeaveTextAreaKeyHandler() {
        return getLeaveTextAreaKeyHandler(keyEvent -> {});
    }

    public static EventHandler<KeyEvent> getLeaveTextAreaKeyHandler(final Consumer<KeyEvent> afterEnter) {
        return (keyEvent) -> {
            leaveOnEnterPressed.accept(keyEvent);
            afterEnter.accept(keyEvent);
        };
    }

    private static Consumer<KeyEvent> leaveOnEnterPressed;
    private static Runnable leaveTextAreas;

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        width = new SimpleDoubleProperty();
        height = new SimpleDoubleProperty();
        insetShouldShow = new SimpleBooleanProperty();
        insetShouldShow.set(true);

        root.widthProperty().addListener((observable, oldValue, newValue) -> width.setValue(newValue));
        root.heightProperty().addListener((observable, oldValue, newValue) -> height.setValue(newValue));

        CanvasPresentation.mouseTracker.registerOnMousePressedEventHandler(event -> {
            // Deselect all elements
            SelectHelper.clearSelectedElements();
        });

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
        if (oldObject != null && (oldObject instanceof Component || oldObject instanceof SystemModel)) {
            ModelObjectTranslateMap.put(oldObject, new Pair<>(root.getTranslateX(), root.getTranslateY()));
        }

        // We should not add the new object if it is null (e.g. when clearing the view)
        if (newObject == null) return;

        // Remove old object from view
        root.getChildren().removeIf(node -> node instanceof HighLevelModelPresentation);

        if (newObject instanceof Component) {
            setTranslateOfBox(newObject);
            activeComponentPresentation = new ComponentPresentation((Component) newObject);
            root.getChildren().add(activeComponentPresentation);
        } else if (newObject instanceof Declarations) {
            root.setTranslateX(0);
            root.setTranslateY(DECLARATION_X_MARGIN);

            activeComponentPresentation = null;
            root.getChildren().add(new DeclarationPresentation((Declarations) newObject));
        } else if (newObject instanceof SystemModel) {
            setTranslateOfBox(newObject);
            activeComponentPresentation = null;
            root.getChildren().add(new SystemPresentation((SystemModel) newObject));
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
            root.setTranslateX(GRID_SIZE * 3);
            root.setTranslateY(GRID_SIZE * 8);
        }
    }

    /**
     * Updates if height of views should have offsets at the bottom.
     * Whether views should have an offset is based on the configuration of the error view.
     * @param shouldHave true iff views should have an offset
     */
    public static void updateOffset(final Boolean shouldHave) {
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
