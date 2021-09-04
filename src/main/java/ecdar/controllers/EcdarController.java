package ecdar.controllers;

import ecdar.Debug;
import ecdar.Ecdar;
import ecdar.abstractions.*;
import ecdar.backend.BackendDriverManager;
import ecdar.backend.BackendException;
import ecdar.backend.BackendHelper;
import ecdar.code_analysis.CodeAnalysis;
import ecdar.mutation.models.MutationTestPlan;
import ecdar.presentations.*;
import ecdar.utility.UndoRedoStack;
import ecdar.utility.colors.Color;
import ecdar.utility.colors.EnabledColor;
import ecdar.utility.helpers.SelectHelper;
import ecdar.utility.keyboard.Keybind;
import ecdar.utility.keyboard.KeyboardTracker;
import ecdar.utility.keyboard.NudgeDirection;
import ecdar.utility.keyboard.Nudgeable;
import com.jfoenix.controls.*;
import javafx.animation.Interpolator;
import javafx.animation.Transition;
import javafx.application.Platform;
import javafx.beans.binding.When;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import javafx.util.Pair;
import org.kordamp.ikonli.javafx.FontIcon;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class EcdarController implements Initializable, Presentable {

    // Reachability analysis
    public static boolean reachabilityServiceEnabled = false;
    private static long reachabilityTime = Long.MAX_VALUE;
    private static ExecutorService reachabilityService;

    private static final ObjectProperty<EdgeStatus> globalEdgeStatus = new SimpleObjectProperty<>(EdgeStatus.INPUT);

    // View stuff
    public StackPane root;
    public RightSimPanePresentation queryPane;
    public ProjectPanePresentation filePane;
    public StackPane toolbar;
    public Label queryPaneFillerElement;
    public Label filePaneFillerElement;
    public Rectangle bottomFillerElement;
    public StackPane modalBar;
    public JFXRippler colorSelected;
    public JFXRippler deleteSelected;
    public JFXRippler undo;
    public JFXRippler redo;
    public StackPane tabPaneContainer;

    public JFXButton switchToInputButton;
    public JFXButton switchToOutputButton;
    public JFXToggleButton switchEdgeStatusButton;

    public StackPane canvasPane;

    private double expandHeight = 300;

//    public final Transition expandMessagesContainer = new Transition() {
//        {
//            setInterpolator(Interpolator.SPLINE(0.645, 0.045, 0.355, 1));
//            setCycleDuration(Duration.millis(200));
//        }
//
//        @Override
//        protected void interpolate(final double frac) {
//            setMaxHeight(35 + frac * (expandHeight - 35));
//        }
//    };

    public static void runReachabilityAnalysis() {
        if (!reachabilityServiceEnabled) return;

        reachabilityTime = System.currentTimeMillis() + 500;
    }

    public static EdgeStatus getGlobalEdgeStatus() {
        return globalEdgeStatus.get();
    }

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        initializeEdgeStatusHandling();
        initializeKeybindings();
        initializeReachabilityAnalysisThread();

        canvasPane.getChildren().clear();

        CanvasShellPresentation canvasShellPresentation = new CanvasShellPresentation();
        HighLevelModelObject model = MainController.getActiveCanvasPresentation().getController().getActiveModel();
        if(model != null) {
            canvasShellPresentation.getController().canvasPresentation.getController().setActiveModel(MainController.getActiveCanvasPresentation().getController().getActiveModel());
        } else {
            canvasShellPresentation.getController().canvasPresentation.getController().setActiveModel(Ecdar.getProject().getComponents().get(0));
        }

        canvasShellPresentation.getController().toolbar.setTranslateY(48);
        canvasPane.getChildren().add(canvasShellPresentation);

        MainController.setActiveCanvasPresentation(canvasShellPresentation.getController().canvasPresentation);

//        filePane.getController().updateColorsOnFilePresentations();
    }

    /**
     * Initializes the keybinding for:
     *  - New component
     *  - Nudging with arrow keys
     *  - Nudging with WASD
     *  - Deletion
     *  - Colors
     */
    private void initializeKeybindings() {
        //Press ctrl+N or cmd+N to create a new component. The canvas changes to this new component
        KeyCodeCombination combination = new KeyCodeCombination(KeyCode.N, KeyCombination.SHORTCUT_DOWN);
        Keybind binding = new Keybind(combination, (event) -> {
            final Component newComponent = new Component(true);
            UndoRedoStack.pushAndPerform(() -> { // Perform

                Ecdar.getProject().getComponents().add(newComponent);
            }, () -> { // Undo
                Ecdar.getProject().getComponents().remove(newComponent);
            }, "Created new component: " + newComponent.getName(), "add-circle");

            // NULL POINTER vvvvv
            MainController.getActiveCanvasPresentation().getController().setActiveModel(newComponent);
        });
        KeyboardTracker.registerKeybind(KeyboardTracker.CREATE_COMPONENT, binding);

        // Keybind for nudging the selected elements
        KeyboardTracker.registerKeybind(KeyboardTracker.NUDGE_UP, new Keybind(new KeyCodeCombination(KeyCode.UP), (event) -> {
            event.consume();
            nudgeSelected(NudgeDirection.UP);
        }));

        KeyboardTracker.registerKeybind(KeyboardTracker.NUDGE_DOWN, new Keybind(new KeyCodeCombination(KeyCode.DOWN), (event) -> {
            event.consume();
            nudgeSelected(NudgeDirection.DOWN);
        }));

        KeyboardTracker.registerKeybind(KeyboardTracker.NUDGE_LEFT, new Keybind(new KeyCodeCombination(KeyCode.LEFT), (event) -> {
            event.consume();
            nudgeSelected(NudgeDirection.LEFT);
        }));

        KeyboardTracker.registerKeybind(KeyboardTracker.NUDGE_RIGHT, new Keybind(new KeyCodeCombination(KeyCode.RIGHT), (event) -> {
            event.consume();
            nudgeSelected(NudgeDirection.RIGHT);
        }));

        KeyboardTracker.registerKeybind(KeyboardTracker.DESELECT, new Keybind(new KeyCodeCombination(KeyCode.ESCAPE), (event) -> {
            SelectHelper.clearSelectedElements();
        }));

        KeyboardTracker.registerKeybind(KeyboardTracker.NUDGE_W, new Keybind(new KeyCodeCombination(KeyCode.W), () -> nudgeSelected(NudgeDirection.UP)));
        KeyboardTracker.registerKeybind(KeyboardTracker.NUDGE_A, new Keybind(new KeyCodeCombination(KeyCode.A), () -> nudgeSelected(NudgeDirection.LEFT)));
        KeyboardTracker.registerKeybind(KeyboardTracker.NUDGE_S, new Keybind(new KeyCodeCombination(KeyCode.S), () -> nudgeSelected(NudgeDirection.DOWN)));
        KeyboardTracker.registerKeybind(KeyboardTracker.NUDGE_D, new Keybind(new KeyCodeCombination(KeyCode.D), () -> nudgeSelected(NudgeDirection.RIGHT)));

        // Keybind for deleting the selected elements
        KeyboardTracker.registerKeybind(KeyboardTracker.DELETE_SELECTED, new Keybind(new KeyCodeCombination(KeyCode.DELETE), this::deleteSelectedClicked));

        // Keybinds for coloring the selected elements
        EnabledColor.enabledColors.forEach(enabledColor -> {
            KeyboardTracker.registerKeybind(KeyboardTracker.COLOR_SELECTED + "_" + enabledColor.keyCode.getName(), new Keybind(new KeyCodeCombination(enabledColor.keyCode), () -> {

                final List<Pair<SelectHelper.ItemSelectable, EnabledColor>> previousColor = new ArrayList<>();

                SelectHelper.getSelectedElements().forEach(selectable -> {
                    previousColor.add(new Pair<>(selectable, new EnabledColor(selectable.getColor(), selectable.getColorIntensity())));
                });
                changeColorOnSelectedElements(enabledColor, previousColor);
                SelectHelper.clearSelectedElements();
            }));
        });
    }

    /**
     * Handles the change of color on selected objects
     * @param enabledColor The new color for the selected objects
     * @param previousColor The color old color of the selected objects
     */
    public void changeColorOnSelectedElements(final EnabledColor enabledColor,
                                              final List<Pair<SelectHelper.ItemSelectable, EnabledColor>> previousColor)
    {
        UndoRedoStack.pushAndPerform(() -> { // Perform
            SelectHelper.getSelectedElements()
                    .forEach(selectable -> selectable.color(enabledColor.color, enabledColor.intensity));
        }, () -> { // Undo
            previousColor.forEach(selectableEnabledColorPair -> selectableEnabledColorPair.getKey().color(selectableEnabledColorPair.getValue().color, selectableEnabledColorPair.getValue().intensity));
        }, String.format("Changed the color of %d elements to %s", previousColor.size(), enabledColor.color.name()), "color-lens");
    }

    /**
     * Initializes edge status.
     * Input is the default status.
     * This method sets buttons for edge status whenever the status changes.
     */
    private void initializeEdgeStatusHandling() {
        globalEdgeStatus.set(EdgeStatus.INPUT);

        Tooltip.install(switchToInputButton, new Tooltip("Switch to input mode"));
        Tooltip.install(switchToOutputButton, new Tooltip("Switch to output mode"));

        globalEdgeStatus.addListener(((observable, oldValue, newValue) -> {
            if (newValue.equals(EdgeStatus.INPUT)) {
                switchToInputButton.setTextFill(javafx.scene.paint.Color.WHITE);
                switchToOutputButton.setTextFill(javafx.scene.paint.Color.GREY);
                switchEdgeStatusButton.setSelected(false);
            } else {
                switchToInputButton.setTextFill(javafx.scene.paint.Color.GREY);
                switchToOutputButton.setTextFill(javafx.scene.paint.Color.WHITE);
                switchEdgeStatusButton.setSelected(true);
            }
        }));
    }

    private void initializeReachabilityAnalysisThread() {
        new Thread(() -> {
            while (true) {

                // Wait for the reachability (the last time we changed the model) becomes smaller than the current time
                while (reachabilityTime > System.currentTimeMillis()) {
                    try {
                        Thread.sleep(2000);
                        Debug.backgroundThreads.removeIf(thread -> !thread.isAlive());
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                // We are now performing the analysis. Do not do another analysis before another change is introduced
                reachabilityTime = Long.MAX_VALUE;

                // Cancel any ongoing analysis
                if (reachabilityService != null) {
                    reachabilityService.shutdownNow();
                }

                // Start new analysis
                reachabilityService = Executors.newFixedThreadPool(10);

                while (Debug.backgroundThreads.size() > 0) {
                    final Thread thread = Debug.backgroundThreads.get(0);
                    thread.interrupt();
                    Debug.removeThread(thread);
                }

                try {
                    // Make sure that the model is generated
                    BackendHelper.buildEcdarDocument();
                } catch (final BackendException e) {
                    // Something went wrong with creating the document
                    Ecdar.showToast("Could not build XML model. I got the error: " + e.getMessage());
                    e.printStackTrace();
                    return;
                }

                Ecdar.getProject().getQueries().forEach(query -> {
                    if (query.isPeriodic()) query.run();
                });

                // List of threads to start
                List<Thread> threads = new ArrayList<>();

                // Submit all background reachability queries
                Ecdar.getProject().getComponents().forEach(component -> {
                    // Check if we should consider this component
                    if (!component.isIncludeInPeriodicCheck()) {
                        component.getLocations().forEach(location -> location.setReachability(Location.Reachability.EXCLUDED));
                    } else {
                        component.getLocations().forEach(location -> {
                            final String locationReachableQuery = BackendDriverManager.getInstance().getLocationReachableQuery(location, component);
                            final Thread verifyThread = BackendDriverManager.getInstance().getBackendThreadForQuery(
                                    locationReachableQuery,
                                    (result -> {
                                        if (result) {
                                            location.setReachability(Location.Reachability.REACHABLE);
                                        } else {
                                            location.setReachability(Location.Reachability.UNREACHABLE);
                                        }
                                        Debug.removeThread(Thread.currentThread());
                                    }),
                                    (e) -> {
                                        location.setReachability(Location.Reachability.UNKNOWN);
                                        Debug.removeThread(Thread.currentThread());
                                    },
                                    2000
                            );

                            if(verifyThread != null){
                                verifyThread.setName(locationReachableQuery + " (" + verifyThread.getName() + ")");
                                Debug.addThread(verifyThread);
                                threads.add(verifyThread);
                            }
                        });
                    }
                });

                threads.forEach((verifyThread) -> reachabilityService.submit(verifyThread::start));
            }
        }).start();
    }

//    public void expandMessagesIfNotExpanded() {
//        if (tabPaneContainer.getMaxHeight() <= 35) {
//            expandMessagesContainer.play();
//        }
//    }

//    public void collapseMessagesIfNotCollapsed() {
//        final Transition collapse = new Transition() {
//            double height = tabPaneContainer.getMaxHeight();
//
//            {
//                setInterpolator(Interpolator.SPLINE(0.645, 0.045, 0.355, 1));
//                setCycleDuration(Duration.millis(200));
//            }
//
//            @Override
//            protected void interpolate(final double frac) {
//                setMaxHeight(((height - 35) * (1 - frac)) + 35);
//            }
//        };
//
//        if (tabPaneContainer.getMaxHeight() > 35) {
//            expandHeight = tabPaneContainer.getHeight();
//            collapse.play();
//        }
//    }

//    @FXML
//    public void collapseMessagesClicked() {
//        final Transition collapse = new Transition() {
//            double height = tabPaneContainer.getMaxHeight();
//
//            {
//                setInterpolator(Interpolator.SPLINE(0.645, 0.045, 0.355, 1));
//                setCycleDuration(Duration.millis(200));
//            }
//
//            @Override
//            protected void interpolate(final double frac) {
//                setMaxHeight(((height - 35) * (1 - frac)) + 35);
//            }
//        };
//
//        if (tabPaneContainer.getMaxHeight() > 35) {
//            expandHeight = tabPaneContainer.getHeight();
//            collapse.play();
//        } else {
//            expandMessagesContainer.play();
//        }
//    }

//    /**
//     * This method is used as a central place to decide whether the tabPane is opened or closed
//     * @param height the value used to set the height of the tabPane
//     */
//    public void setMaxHeight(double height) {
//        tabPaneContainer.setMaxHeight(height);
//        if(height > 35) { //The tabpane is opened
//            filePane.showBottomInset(false);
//            queryPane.showBottomInset(false);
//            CanvasPresentation.showBottomInset(false);
//        } else {
//            // When closed we push up the scrollviews in the filePane and queryPane as the tabPane
//            // would otherwise cover some items in these views
//            filePane.showBottomInset(true);
//            queryPane.showBottomInset(true);
//            CanvasPresentation.showBottomInset(true);
//        }
//    }

    private void nudgeSelected(final NudgeDirection direction) {
        final List<SelectHelper.ItemSelectable> selectedElements = SelectHelper.getSelectedElements();

        final List<Nudgeable> nudgedElements = new ArrayList<>();

        UndoRedoStack.pushAndPerform(() -> { // Perform

                    final boolean[] foundUnNudgableElement = {false};
                    selectedElements.forEach(selectable -> {
                        if (selectable instanceof Nudgeable) {
                            final Nudgeable nudgeable = (Nudgeable) selectable;
                            if (nudgeable.nudge(direction)) {
                                nudgedElements.add(nudgeable);
                            } else {
                                foundUnNudgableElement[0] = true;
                            }
                        }
                    });

                    // If some one was not able to nudge disallow the current nudge and remove from the undo stack
                    if(foundUnNudgableElement[0]){
                        nudgedElements.forEach(nudgedElement -> nudgedElement.nudge(direction.reverse()));
                        UndoRedoStack.forgetLast();
                    }

                }, () -> { // Undo
                    nudgedElements.forEach(nudgedElement -> nudgedElement.nudge(direction.reverse()));
                },
                "Nudge " + selectedElements + " in direction: " + direction,
                "open-with");
    }

    @FXML
    private void deleteSelectedClicked() {
        if (SelectHelper.getSelectedElements().size() == 0) return;

        // Run through the selected elements and look for something that we can delete
        SelectHelper.getSelectedElements().forEach(selectable -> {
            if (selectable instanceof LocationController) {
                ((LocationController) selectable).tryDelete();
            } else if (selectable instanceof EdgeController) {
                final Component component = ((EdgeController) selectable).getComponent();
                final DisplayableEdge edge = ((EdgeController) selectable).getEdge();

                // Dont delete edge if it is locked
                if(edge.getIsLockedProperty().getValue()){return;}

                UndoRedoStack.pushAndPerform(() -> { // Perform
                    // Remove the edge
                    component.removeEdge(edge);
                }, () -> { // Undo
                    // Re-all the edge
                    component.addEdge(edge);
                }, String.format("Deleted %s", selectable.toString()), "delete");
            } else if (selectable instanceof NailController) {
                ((NailController) selectable).tryDelete();
            }
        });

        SelectHelper.clearSelectedElements();
    }

    @FXML
    private void undoClicked() {
        UndoRedoStack.undo();
    }

    @FXML
    private void redoClicked() {
        UndoRedoStack.redo();
    }

    /**
     * Switch to input edge mode
     */
    @FXML
    private void switchToInputClicked() {
        setGlobalEdgeStatus(EdgeStatus.INPUT);
    }

    /**
     * Switch to output edge mode
     */
    @FXML
    private void switchToOutputClicked() {
        setGlobalEdgeStatus(EdgeStatus.OUTPUT);
    }

    /**
     * Switch edge status.
     */
    @FXML
    private void switchEdgeStatusClicked() {
        if (getGlobalEdgeStatus().equals(EdgeStatus.INPUT)) {
            setGlobalEdgeStatus(EdgeStatus.OUTPUT);
        } else {
            setGlobalEdgeStatus(EdgeStatus.INPUT);
        }
    }

    /**
     * Sets the global edge status.
     * @param status the status
     */
    private void setGlobalEdgeStatus(EdgeStatus status) {
        globalEdgeStatus.set(status);
    }

    /**
     * Called when the view is about to be shown.
     * Initializes the keybindings for the view and resets the placement of the current active model
     * as it could have been changed doing the simulation
     *
     * @see #initializeKeybindings()
     * @see CanvasController#resetCurrentActiveModelPlacement()
     */
    public void willShow() {
        initializeKeybindings();
        MainController.getActiveCanvasPresentation().getController().resetCurrentActiveModelPlacement();
    }

    /**
     * Called when the view is about to be hidden.
     * Disables the keybindings for the view
     */
    public void willHide() {
        this.unregisterKeybindings();
    }

    /**
     * Unregisters all keybindings for the view
     */
    private void unregisterKeybindings() {
        KeyboardTracker.unregisterKeybind("CREATE_COMPONENT");
        KeyboardTracker.unregisterKeybind("NUDGE_UP");
        KeyboardTracker.unregisterKeybind("NUDGE_DOWN");
        KeyboardTracker.unregisterKeybind("NUDGE_LEFT");
        KeyboardTracker.unregisterKeybind("NUDGE_RIGHT");
        KeyboardTracker.unregisterKeybind("DESELECT");
        KeyboardTracker.unregisterKeybind("NUDGE_W");
        KeyboardTracker.unregisterKeybind("NUDGE_A");
        KeyboardTracker.unregisterKeybind("NUDGE_S");
        KeyboardTracker.unregisterKeybind("NUDGE_D");
        KeyboardTracker.unregisterKeybind("DELETE_SELECTED");
        KeyboardTracker.unregisterKeybind("COLOR_SELECTED");
    }

    public void setCanvasSplit(boolean shouldSplit) {
        if (shouldSplit) {
            setCanvasModeToSplit();
        } else {
            setCanvasModeToSingular();
        }
    }

    /**
     * Initialize a new CanvasShellPresentation and return it
     * @return new CanvasShellPresentation
     */
    private CanvasShellPresentation initializeNewCanvasShellPresentation() {
        CanvasShellPresentation canvasShellPresentation = new CanvasShellPresentation();
        canvasShellPresentation.setBorder(new Border(new BorderStroke(Color.GREY.getColor(Color.Intensity.I500), BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderStroke.THIN)));
        return canvasShellPresentation;
    }

    /**
     * Initialize a new CanvasShellPresentation and set its active component to the next component encountered from the startIndex and return it
     * @param components the list of components for assigning active component of the CanvasPresentation
     * @param startIndex the index to start at when trying to find the component to set as active
     * @return new CanvasShellPresentation
     */
    private CanvasShellPresentation initializeNewCanvasShellPresentationWithActiveComponent(ObservableList<Component> components, int startIndex) {
        CanvasShellPresentation canvasShellPresentation = initializeNewCanvasShellPresentation();

        int numComponents = components.size();
        canvasShellPresentation.getController().canvasPresentation.getController().setActiveModel(null);
        for(int currentCompNum = startIndex; currentCompNum < numComponents; currentCompNum++){
            if(MainController.getActiveCanvasPresentation().getController().getActiveModel() != components.get(currentCompNum)) {
                canvasShellPresentation.getController().canvasPresentation.getController().setActiveModel(components.get(currentCompNum));
                break;
            }
        }

        return canvasShellPresentation;
    }

    /**
     * Removes the canvases and adds a new one, with the active component of the active canvasPresentation.
     */
    private void setCanvasModeToSingular() {
        canvasPane.getChildren().clear();

        CanvasShellPresentation canvasShellPresentation = new CanvasShellPresentation();
        HighLevelModelObject model = MainController.getActiveCanvasPresentation().getController().getActiveModel();
        if(model != null) {
            canvasShellPresentation.getController().canvasPresentation.getController().setActiveModel(MainController.getActiveCanvasPresentation().getController().getActiveModel());
        } else {
            canvasShellPresentation.getController().canvasPresentation.getController().setActiveModel(Ecdar.getProject().getComponents().get(0));
        }

        canvasShellPresentation.getController().toolbar.setTranslateY(48);
        canvasPane.getChildren().add(canvasShellPresentation);

        MainController.setActiveCanvasPresentation(canvasShellPresentation.getController().canvasPresentation);

        filePane.getController().updateColorsOnFilePresentations();
    }

    /**
     * Removes the canvas and adds a GridPane with four new canvases, with different active components,
     * the first being the one previously displayed on the single canvas.
     */
    private void setCanvasModeToSplit() {
        canvasPane.getChildren().clear();

        GridPane canvasGrid = new GridPane();

        canvasGrid.addColumn(0);
        canvasGrid.addColumn(1);
        canvasGrid.addRow(0);
        canvasGrid.addRow(1);

        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPercentWidth(50);

        RowConstraints row1 = new RowConstraints();
        row1.setPercentHeight(50);

        canvasGrid.getColumnConstraints().add(col1);
        canvasGrid.getColumnConstraints().add(col1);
        canvasGrid.getRowConstraints().add(row1);
        canvasGrid.getRowConstraints().add(row1);

        ObservableList<Component> components = Ecdar.getProject().getComponents();
        int currentCompNum = 0, numComponents = components.size();

        // Add the canvasShellPresentation at the top-left
        CanvasShellPresentation canvasShellPresentation = initializeNewCanvasShellPresentation();
        canvasShellPresentation.getController().canvasPresentation.getController().setActiveModel(MainController.getActiveCanvasPresentation().getController().getActiveModel());
        canvasShellPresentation.getController().toolbar.setTranslateY(48);
        canvasGrid.add(canvasShellPresentation, 0, 0);
        MainController.setActiveCanvasPresentation(canvasShellPresentation.getController().canvasPresentation);

        // Add the canvasShellPresentation at the top-right
        canvasShellPresentation = initializeNewCanvasShellPresentationWithActiveComponent(components, currentCompNum);
        canvasShellPresentation.getController().toolbar.setTranslateY(48);
        canvasShellPresentation.setOpacity(0.75);
        canvasGrid.add(canvasShellPresentation, 1, 0);

        // Update the startIndex for the next canvasShellPresentation
        for (int i = 0; i < numComponents; i++) {
            if (canvasShellPresentation.getController().canvasPresentation.getController().getActiveModel() != null && canvasShellPresentation.getController().canvasPresentation.getController().getActiveModel().equals(components.get(i))) {
                currentCompNum = i + 1;
            }
        }

        // Add the canvasShellPresentation at the bottom-left
        canvasShellPresentation = initializeNewCanvasShellPresentationWithActiveComponent(components, currentCompNum);
        canvasShellPresentation.setOpacity(0.75);
        canvasGrid.add(canvasShellPresentation, 0, 1);

        // Update the startIndex for the next canvasShellPresentation
        for (int i = 0; i < numComponents; i++)
            if (canvasShellPresentation.getController().canvasPresentation.getController().getActiveModel() != null && canvasShellPresentation.getController().canvasPresentation.getController().getActiveModel().equals(components.get(i))) {
                currentCompNum = i + 1;
            }

        // Add the canvasShellPresentation at the bottom-right
        canvasShellPresentation = initializeNewCanvasShellPresentationWithActiveComponent(components, currentCompNum);
        canvasShellPresentation.setOpacity(0.75);
        canvasGrid.add(canvasShellPresentation, 1, 1);

        canvasPane.getChildren().add(canvasGrid);

        filePane.getController().updateColorsOnFilePresentations();
    }
}
