package SW9.controllers;

import SW9.abstractions.Component;
import SW9.abstractions.Declarations;
import SW9.abstractions.VerificationObject;
import SW9.presentations.CanvasPresentation;
import SW9.presentations.ComponentPresentation;
import SW9.presentations.DeclarationPresentation;
import SW9.utility.helpers.SelectHelper;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
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

import static SW9.presentations.CanvasPresentation.GRID_SIZE;

public class CanvasController implements Initializable {

    private final static ObjectProperty<VerificationObject> activeVerificationObject = new SimpleObjectProperty<>(null);

    private final static HashMap<Component, Pair<Double, Double>> componentTranslateMap = new HashMap<>();

    public Pane root;

    public static VerificationObject getActiveVerificationObject() {
        return activeVerificationObject.get();
    }

    public static void setActiveVerificationObject(final Component component) {
        CanvasController.activeVerificationObject.set(component);
        Platform.runLater(CanvasController::leaveTextAreas);
    }

    public static ObjectProperty<VerificationObject> activeComponentProperty() {
        return activeVerificationObject;
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
        CanvasPresentation.mouseTracker.registerOnMousePressedEventHandler(event -> {
            // Deselect all elements
            SelectHelper.clearSelectedElements();
        });

        activeVerificationObject.addListener((obs, oldVeriObj, newVeriObj) ->
                onActiveVerificationObjectChanged(oldVeriObj, newVeriObj));

        leaveTextAreas = () -> root.requestFocus();

        leaveOnEnterPressed = (keyEvent) -> {
            if (keyEvent.getCode().equals(KeyCode.ENTER) || keyEvent.getCode().equals(KeyCode.ESCAPE)) {
                leaveTextAreas();
            }
        };

    }

    /**
     * Updates component translate map with old verification object.
     * Removes old verification object from view and shows new one.
     * @param oldVeriObj old verification object
     * @param newVeriObj new verification object
     */
    private void onActiveVerificationObjectChanged(final VerificationObject oldVeriObj, final VerificationObject newVeriObj) {
        // If old object is a component, add to map
        if (oldVeriObj != null && oldVeriObj instanceof Component) {
            componentTranslateMap.put((Component) oldVeriObj, new Pair<>(root.getTranslateX(), root.getTranslateY()));
        }

        if (newVeriObj == null) return; // We should not add the new component since it is null (clear the view)

        // Remove verification object from view
        root.getChildren().removeIf(node -> node instanceof ComponentPresentation);

        if (newVeriObj instanceof Component) {
            if (componentTranslateMap.containsKey(newVeriObj)) {
                final Pair<Double, Double> restoreCoordinates = componentTranslateMap.get(newVeriObj);
                root.setTranslateX(restoreCoordinates.getKey());
                root.setTranslateY(restoreCoordinates.getValue());
            } else {
                root.setTranslateX(GRID_SIZE * 3);
                root.setTranslateY(GRID_SIZE * 8);
            }

            final ComponentPresentation newComponentPresentation = new ComponentPresentation((Component) newVeriObj);
            root.getChildren().add(newComponentPresentation);
        } else if (newVeriObj instanceof Declarations) {
            root.getChildren().add(new DeclarationPresentation((Declarations) newVeriObj));
        }

        root.requestFocus();
    }

}
