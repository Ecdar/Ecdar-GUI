package ecdar.controllers;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import ecdar.Debug;
import ecdar.Ecdar;
import ecdar.abstractions.*;
import ecdar.backend.BackendHelper;
import ecdar.code_analysis.CodeAnalysis;
import ecdar.mutation.models.MutationTestPlan;
import ecdar.presentations.*;
import ecdar.utility.UndoRedoStack;
import ecdar.utility.colors.Color;
import ecdar.utility.helpers.SelectHelper;
import ecdar.utility.keyboard.Keybind;
import ecdar.utility.keyboard.KeyboardTracker;
import ecdar.utility.keyboard.NudgeDirection;
import ecdar.utility.keyboard.Nudgeable;
import com.jfoenix.controls.*;
import javafx.application.Platform;
import javafx.beans.binding.When;
import javafx.beans.property.*;
import javafx.collections.ListChangeListener;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Node;
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

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class EcdarController implements Initializable {
    // Reachability analysis
    public static boolean reachabilityServiceEnabled = false;
    private static long reachabilityTime = Long.MAX_VALUE;
    private static ExecutorService reachabilityService;

    // View stuff
    public StackPane root;
    public BorderPane borderPane;
    public StackPane topPane;
    public StackPane leftPane;
    public StackPane rightPane;
    public Rectangle bottomFillerElement;
    public MessageTabPanePresentation messageTabPane;
    public StackPane modellingHelpDialogContainer;
    public JFXDialog modellingHelpDialog;
    public StackPane modalBar;
    public JFXTextField queryTextField;
    public JFXTextField commentTextField;

    public ImageView helpInitialImage;
    public StackPane helpInitialPane;
    public ImageView helpUrgentImage;
    public StackPane helpUrgentPane;
    public ImageView helpInputImage;
    public StackPane helpInputPane;
    public ImageView helpOutputImage;
    public StackPane helpOutputPane;

    public MenuItem menuEditMoveLeft;
    public MenuItem menuEditMoveUp;
    public MenuItem menuEditMoveRight;
    public MenuItem menuEditMoveDown;
    public JFXButton testHelpAcceptButton;
    public JFXDialog testHelpDialog;
    public StackPane testHelpContainer;
    public StackPane aboutContainer;
    public JFXDialog aboutDialog;
    public JFXButton aboutAcceptButton;

    // The program top menu
    public MenuBar menuBar;
    public MenuItem menuBarViewProjectPanel;
    public MenuItem menuBarViewQueryPanel;
    public MenuItem menuBarViewGrid;
    public MenuItem menuBarViewAutoscaling;
    public Menu menuViewMenuScaling;
    public ToggleGroup scaling;
    public RadioMenuItem scaleXS;
    public RadioMenuItem scaleS;
    public RadioMenuItem scaleM;
    public RadioMenuItem scaleL;
    public RadioMenuItem scaleXL;
    public RadioMenuItem scaleXXL;
    public RadioMenuItem scaleXXXL;
    public MenuItem menuBarViewCanvasSplit;
    public MenuItem menuBarViewEditor;
    public MenuItem menuBarViewSimulator;
    public MenuItem menuBarFileCreateNewProject;
    public MenuItem menuBarFileOpenProject;
    public Menu menuBarFileRecentProjects;
    public MenuItem menuBarFileSave;
    public MenuItem menuBarFileSaveAs;
    public MenuItem menuBarFileNewMutationTestObject;
    public MenuItem menuBarFileExportAsPng;
    public MenuItem menuBarFileExportAsPngNoBorder;
    public MenuItem menuBarOptionsCache;
    public MenuItem menuBarOptionsBackgroundQueries;
    public MenuItem menuBarOptionsBackendOptions;
    public MenuItem menuBarHelpHelp;
    public MenuItem menuBarHelpAbout;
    public MenuItem menuBarHelpTest;

    public JFXToggleButton switchGuiView;
    public Snackbar snackbar;
    public HBox statusBar;
    public Label statusLabel;
    public Label queryLabel;
    public HBox queryStatusContainer;

    public StackPane queryDialogContainer;
    public JFXDialog queryDialog;
    public Text queryTextResult;
    public Text queryTextQuery;

    public StackPane backendOptionsDialogContainer;
    public BackendOptionsDialogPresentation backendOptionsDialog;

    public StackPane simulationInitializationDialogContainer;
    public SimulationInitializationDialogPresentation simulationInitializationDialog;

    public final DoubleProperty scalingProperty = new SimpleDoubleProperty();

    private static JFXDialog _queryDialog;
    private static Text _queryTextResult;
    private static Text _queryTextQuery;
    private static final Text temporaryComponentWatermark = new Text("Temporary component");

    public static void runReachabilityAnalysis() {
        if (!reachabilityServiceEnabled) return;

        reachabilityTime = System.currentTimeMillis() + 500;
    }

    /**
     * Enumeration to keep track of which mode the application is in
     */
    private enum Mode {
        Editor, Simulator
    }

    /**
     * currentMode is a property that keeps track of which mode the application is in.
     * The initial mode is Mode.Editor
     */
    private static final ObjectProperty<Mode> currentMode = new SimpleObjectProperty<>(Mode.Editor);

    private static final EditorPresentation editorPresentation = new EditorPresentation();
    public final ProjectPanePresentation projectPane = new ProjectPanePresentation();
    public final QueryPanePresentation queryPane = new QueryPanePresentation();

    private static final SimulatorPresentation simulatorPresentation = new SimulatorPresentation();
    public final LeftSimPanePresentation leftSimPane = new LeftSimPanePresentation();
    public final RightSimPanePresentation rightSimPane = new RightSimPanePresentation();

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        initializeDialogs();
        initializeKeybindings();
        initializeStatusBar();
        initializeMenuBar();
        startBackgroundQueriesThread(); // Will terminate immediately if background queries are turned off

        bottomFillerElement.heightProperty().bind(messageTabPane.maxHeightProperty());
        messageTabPane.getController().setRunnableForOpeningAndClosingMessageTabPane(this::changeInsetsOfProjectAndQueryPanes);

        // Update file coloring when active model changes
        editorPresentation.getController().getActiveCanvasPresentation().getController().activeComponentProperty().addListener(observable -> projectPane.getController().updateColorsOnFilePresentations());
        editorPresentation.getController().activeCanvasPresentationProperty().addListener(observable -> projectPane.getController().updateColorsOnFilePresentations());

        leftSimPane.minWidthProperty().bind(projectPane.minWidthProperty());
        leftSimPane.maxWidthProperty().bind(projectPane.maxWidthProperty());
        rightSimPane.minWidthProperty().bind(queryPane.minWidthProperty());
        rightSimPane.maxWidthProperty().bind(queryPane.maxWidthProperty());

        enterEditorMode();
    }

    public StackPane getCenter() {
        if (currentMode.get().equals(Mode.Editor)) {
            return editorPresentation.getController().canvasPane;
        } else {
            return simulatorPresentation;
        }
    }

    public static EdgeStatus getGlobalEdgeStatus() {
        return editorPresentation.getController().getGlobalEdgeStatus();
    }

    public EditorPresentation getEditorPresentation() {
        return editorPresentation;
    }

    public SimulatorPresentation getSimulatorPresentation() {
        return simulatorPresentation;
    }

    public static CanvasPresentation getActiveCanvasPresentation() {
        return editorPresentation.getController().getActiveCanvasPresentation();
    }

    public static DoubleProperty getActiveCanvasZoomFactor() {
        return getActiveCanvasPresentation().getController().zoomHelper.currentZoomFactor;
    }

    public static void setActiveCanvasPresentation(CanvasPresentation newActiveCanvasPresentation) {
        getActiveCanvasPresentation().setOpacity(0.75);
        newActiveCanvasPresentation.setOpacity(1);
        editorPresentation.getController().setActiveCanvasPresentation(newActiveCanvasPresentation);
    }

    public static void setActiveModelForActiveCanvas(HighLevelModelObject newActiveModel) {
        getActiveCanvasPresentation().getController().setActiveModel(newActiveModel);

        // Change zoom level to fit new active model
        Platform.runLater(() -> getActiveCanvasPresentation().getController().zoomHelper.zoomToFit());
    }

    public static void setTemporaryComponentWatermarkVisibility(boolean visibility) {
        temporaryComponentWatermark.setVisible(visibility);
    }

    /**
     * Finds and scales all icon nodes below the given node in accordance with the current scaling, using -fx-icon-size
     *
     * @param node The "root" to start the search from
     */
    public void scaleIcons(Node node) {
        Platform.runLater(() -> {
            scaleIcons(node, getNewCalculatedScale());
        });
    }

    private void scaleIcons(Node node, double size) {
        // Scale icons
        Set<Node> mediumIcons = node.lookupAll(".icon-size-medium");
        for (Node icon : mediumIcons)
            icon.setStyle("-fx-icon-size: " + Math.floor(size / 13.0 * 24) + "px;");

        Set<Node> smallIcons = node.lookupAll(".icon-size-small");
        for (Node icon : smallIcons)
            icon.setStyle("-fx-icon-size: " + Math.floor(size / 13.0 * 20) + "px;");

        Set<Node> xSmallIcons = node.lookupAll(".icon-size-x-small");
        for (Node icon : xSmallIcons)
            icon.setStyle("-fx-icon-size: " + Math.floor(size / 13.0 * 18) + "px;");

        switchGuiView.setSize(Math.floor(size / 13.0 * 24));
    }

    private double getNewCalculatedScale() {
        return (Double.parseDouble(scaling.getSelectedToggle().getProperties().get("scale").toString()) * Ecdar.getDpiScale()) * 13.0;
    }

    private void initializeDialogs() {
        modellingHelpDialog.setDialogContainer(modellingHelpDialogContainer);
        modellingHelpDialogContainer.opacityProperty().bind(modellingHelpDialog.getChildren().get(0).scaleXProperty());
        modellingHelpDialog.setOnDialogClosed(event -> modellingHelpDialogContainer.setVisible(false));

        _queryDialog = queryDialog;
        _queryTextResult = queryTextResult;
        _queryTextQuery = queryTextQuery;

        initializeDialog(queryDialog, queryDialogContainer);
        initializeBackendOptionsDialog();

        initializeDialog(simulationInitializationDialog, simulationInitializationDialogContainer);

        simulationInitializationDialog.getController().cancelButton.setOnMouseClicked(event -> {
            switchGuiView.setSelected(false);
            simulationInitializationDialog.close();
        });

        simulationInitializationDialog.getController().startButton.setOnMouseClicked(event -> {

            // ToDo NIELS: Start simulation of selected query
            Ecdar.getSimulationHandler().setComposition(simulationInitializationDialog.getController().simulationComboBox.getSelectionModel().getSelectedItem());
            currentMode.setValue(Mode.Simulator);
            simulationInitializationDialog.close();
        });
    }

    private void initializeBackendOptionsDialog() {
        initializeDialog(backendOptionsDialog, backendOptionsDialogContainer);
        backendOptionsDialog.getController().resetBackendsButton.setOnMouseClicked(event -> {
            backendOptionsDialog.getController().resetBackendsToDefault();
        });

        backendOptionsDialog.getController().closeButton.setOnMouseClicked(event -> {
            backendOptionsDialog.getController().cancelBackendOptionsChanges();
            backendOptionsDialog.close();
        });

        backendOptionsDialog.getController().saveButton.setOnMouseClicked(event -> {
            if (backendOptionsDialog.getController().saveChangesToBackendOptions()) {
                backendOptionsDialog.close();
            }
        });

        if (BackendHelper.getBackendInstances().size() < 1) {
            Ecdar.showToast("No engines were found. Download j-Ecdar or Reveaal, or add another engine to fix this. No queries can be executed without engines.");
        } else {
            BackendInstance defaultBackend = BackendHelper.getBackendInstances().stream().filter(BackendInstance::isDefault).findFirst().orElse(BackendHelper.getBackendInstances().get(0));
            BackendHelper.setDefaultBackendInstance(defaultBackend);
        }
    }

    private void initializeDialog(JFXDialog dialog, StackPane dialogContainer) {
        dialog.setDialogContainer(dialogContainer);
        dialogContainer.opacityProperty().bind(dialog.getChildren().get(0).scaleXProperty());
        dialogContainer.opacityProperty().bind(dialog.getChildren().get(0).scaleXProperty());
        dialog.setOnDialogClosed(event -> {
            dialogContainer.setVisible(false);
            dialogContainer.setMouseTransparent(true);
        });
        dialog.setOnDialogOpened(event -> {
            dialogContainer.setVisible(true);
            dialogContainer.setMouseTransparent(false);
        });

        projectPane.getStyleClass().add("responsive-pane-sizing");
        queryPane.getStyleClass().add("responsive-pane-sizing");

        initializeKeybindings();
        initializeStatusBar();
    }

    /**
     * Initializes the watermark for temporary/generated components
     */
    private void intitializeTemporaryComponentWatermark() {
        temporaryComponentWatermark.getStyleClass().add("display4");
        temporaryComponentWatermark.setOpacity(0.1);
        temporaryComponentWatermark.setRotate(-45);
        temporaryComponentWatermark.setDisable(true);
        temporaryComponentWatermark.setVisible(false);
        root.getChildren().add(temporaryComponentWatermark);
    }

    /**
     * Initializes the keybinding for:
     * - New component
     * - Nudging with arrow keys
     * - Nudging with WASD
     * - Deletion
     * - Colors
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

            getActiveCanvasPresentation().getController().setActiveModel(newComponent);
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
    }

    private void startBackgroundQueriesThread() {
        new Thread(() -> {
            while (Ecdar.shouldRunBackgroundQueries.get()) {
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

                // Stop thread if background queries have been toggled off
                if (!Ecdar.shouldRunBackgroundQueries.get()) return;

                Ecdar.getProject().getQueries().forEach(query -> {
                    if (query.isPeriodic()) Ecdar.getQueryExecutor().executeQuery(query);
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
                            final String locationReachableQuery = BackendHelper.getLocationReachableQuery(location, component, SimulatorController.getSimulationQuery());

                            Query reachabilityQuery = new Query(locationReachableQuery, "", QueryState.UNKNOWN);
                            reachabilityQuery.setType(QueryType.REACHABILITY);

                            Ecdar.getQueryExecutor().executeQuery(reachabilityQuery);

                            final Thread verifyThread = new Thread(() -> Ecdar.getQueryExecutor().executeQuery(reachabilityQuery));

                            verifyThread.setName(locationReachableQuery + " (" + verifyThread.getName() + ")");
                            Debug.addThread(verifyThread);
                            threads.add(verifyThread);
                        });
                    }
                });

                threads.forEach((verifyThread) -> reachabilityService.submit(verifyThread::start));
            }
        }).start();
    }

    private void initializeStatusBar() {
        statusBar.setBackground(new Background(new BackgroundFill(
                Color.GREY_BLUE.getColor(Color.Intensity.I800),
                CornerRadii.EMPTY,
                Insets.EMPTY
        )));

        statusLabel.setTextFill(Color.GREY_BLUE.getColor(Color.Intensity.I50));
        statusLabel.textProperty().bind(Ecdar.projectDirectory);
        statusLabel.setOpacity(0.5);

        queryLabel.setTextFill(Color.GREY_BLUE.getColor(Color.Intensity.I50));
        queryLabel.setOpacity(0.5);

        Debug.backgroundThreads.addListener(new ListChangeListener<Thread>() {
            @Override
            public void onChanged(final Change<? extends Thread> c) {
                while (c.next()) {
                    Platform.runLater(() -> {
                        if (Debug.backgroundThreads.size() == 0) {
                            queryStatusContainer.setOpacity(0);
                        } else {
                            queryStatusContainer.setOpacity(1);
                            queryLabel.setText(Debug.backgroundThreads.size() + " background queries running");
                        }
                    });
                }
            }
        });
    }

    private void initializeMenuBar() {
        menuBar.setUseSystemMenuBar(true);

        initializeCreateNewProjectMenuItem();
        initializeOpenProjectMenuItem();
        initializeRecentProjectsMenu();

        menuBarFileSave.setAccelerator(new KeyCodeCombination(KeyCode.S, KeyCombination.SHORTCUT_DOWN));
        menuBarFileSave.setOnAction(event -> save());

        menuBarFileSaveAs.setAccelerator(new KeyCodeCombination(KeyCode.S, KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN));
        menuBarFileSaveAs.setOnAction(event -> saveAs());

        initializeNewMutationTestObjectMenuItem();
        initializeFileExportAsPng();
        initializeEditMenu();
        initializeViewMenu();
        initializeOptionsMenu();
        initializeHelpMenu();
    }

    private void initializeHelpMenu() {
        menuBarHelpHelp.setOnAction(event -> Ecdar.showHelp());

        menuBarHelpTest.setOnAction(event -> {
            testHelpContainer.setVisible(true);
            testHelpDialog.show(testHelpContainer);
        });
        testHelpAcceptButton.setOnAction(event -> testHelpDialog.close());
        testHelpDialog.setOnDialogClosed(event -> testHelpContainer.setVisible(false)); // hide container when dialog is fully closed

        menuBarHelpAbout.setOnAction(event -> {
            aboutContainer.setVisible(true);
            aboutDialog.show(aboutContainer);
        });
        aboutAcceptButton.setOnAction(event -> aboutDialog.close());
        aboutDialog.setOnDialogClosed(event -> aboutContainer.setVisible(false)); // hide container when dialog is fully closed
    }

    /**
     * Initializes the UI Cache menu element.
     */
    private void initializeOptionsMenu() {
        menuBarOptionsCache.setOnAction(event -> {
            final BooleanProperty isCached = Ecdar.toggleUICache();
            menuBarOptionsCache.getGraphic().opacityProperty().bind(new When(isCached).then(1).otherwise(0));
        });

        menuBarOptionsBackendOptions.setOnAction(event -> {
            backendOptionsDialogContainer.setVisible(true);
            backendOptionsDialog.show(backendOptionsDialogContainer);
            backendOptionsDialog.setMouseTransparent(false);
        });

        menuBarOptionsBackgroundQueries.setOnAction(event -> {
            final BooleanProperty shouldRunBackgroundQueries = Ecdar.toggleRunBackgroundQueries();
            Ecdar.preferences.putBoolean("run_background_queries", shouldRunBackgroundQueries.get());
            if (shouldRunBackgroundQueries.get()) {
                // If background queries have been turned back on, start a new thread
                startBackgroundQueriesThread();
            }
        });

        Ecdar.shouldRunBackgroundQueries.setValue(Ecdar.preferences.getBoolean("run_background_queries", true));
        menuBarOptionsBackgroundQueries.getGraphic().opacityProperty().bind(new When(Ecdar.shouldRunBackgroundQueries).then(1).otherwise(0));
    }

    private void initializeEditMenu() {
        menuEditMoveLeft.setAccelerator(new KeyCodeCombination(KeyCode.LEFT, KeyCombination.CONTROL_DOWN));
        menuEditMoveLeft.setOnAction(event -> {
            final HighLevelModelObject activeModel = getActiveCanvasPresentation().getController().getActiveModel();

            if (activeModel instanceof Component) ((Component) activeModel).moveAllNodesLeft();
            else Ecdar.showToast("This can only be performed on components.");
        });

        menuEditMoveRight.setAccelerator(new KeyCodeCombination(KeyCode.RIGHT, KeyCombination.CONTROL_DOWN));
        menuEditMoveRight.setOnAction(event -> {
            final HighLevelModelObject activeModel = getActiveCanvasPresentation().getController().getActiveModel();

            if (activeModel instanceof Component) ((Component) activeModel).moveAllNodesRight();
            else Ecdar.showToast("This can only be performed on components.");
        });

        menuEditMoveUp.setAccelerator(new KeyCodeCombination(KeyCode.UP, KeyCombination.CONTROL_DOWN));
        menuEditMoveUp.setOnAction(event -> {
            final HighLevelModelObject activeModel = getActiveCanvasPresentation().getController().getActiveModel();

            if (activeModel instanceof Component) ((Component) activeModel).moveAllNodesUp();
            else Ecdar.showToast("This can only be performed on components.");
        });

        menuEditMoveDown.setAccelerator(new KeyCodeCombination(KeyCode.DOWN, KeyCombination.CONTROL_DOWN));
        menuEditMoveDown.setOnAction(event -> {
            final HighLevelModelObject activeModel = getActiveCanvasPresentation().getController().getActiveModel();

            if (activeModel instanceof Component) ((Component) activeModel).moveAllNodesDown();
            else Ecdar.showToast("This can only be performed on components.");
        });
    }

    /**
     * Initialize the View menu.
     */
    private void initializeViewMenu() {
        menuBarViewProjectPanel.getGraphic().setOpacity(1);
        menuBarViewProjectPanel.setAccelerator(new KeyCodeCombination(KeyCode.P, KeyCodeCombination.SHORTCUT_DOWN));
        menuBarViewProjectPanel.setOnAction(event -> {
            final BooleanProperty isOpen = Ecdar.toggleLeftPane();
            menuBarViewProjectPanel.getGraphic().opacityProperty().bind(new When(isOpen).then(1).otherwise(0));
        });

        menuBarViewQueryPanel.getGraphic().setOpacity(1);
        menuBarViewQueryPanel.setAccelerator(new KeyCodeCombination(KeyCode.G, KeyCodeCombination.SHORTCUT_DOWN));
        menuBarViewQueryPanel.setOnAction(event -> {
            final BooleanProperty isOpen = Ecdar.toggleQueryPane();
            menuBarViewQueryPanel.getGraphic().opacityProperty().bind(new When(isOpen).then(1).otherwise(0));
        });

        menuBarViewGrid.getGraphic().setOpacity(1);
        menuBarViewGrid.setAccelerator(new KeyCodeCombination(KeyCode.K, KeyCodeCombination.SHORTCUT_DOWN));
        menuBarViewGrid.setOnAction(event -> {
            final BooleanProperty isOn = Ecdar.toggleGrid();
            menuBarViewGrid.getGraphic().opacityProperty().bind(new When(isOn).then(1).otherwise(0));
        });

        menuBarViewAutoscaling.getGraphic().setOpacity(Ecdar.autoScalingEnabled.getValue() ? 1 : 0);
        menuBarViewAutoscaling.setOnAction(event -> {
            Ecdar.autoScalingEnabled.setValue(!Ecdar.autoScalingEnabled.getValue());
            updateScaling(getNewCalculatedScale() / 13);
            Ecdar.preferences.put("autoscaling", String.valueOf(Ecdar.autoScalingEnabled.getValue()));
        });
        Ecdar.autoScalingEnabled.addListener((observable, oldValue, newValue) -> {
            menuBarViewAutoscaling.getGraphic().opacityProperty().setValue(newValue ? 1 : 0);
        });

        scaling.selectedToggleProperty().addListener((observable, oldValue, newValue) -> updateScaling(Double.parseDouble(newValue.getProperties().get("scale").toString())));

        menuBarViewCanvasSplit.getGraphic().setOpacity(1);
        menuBarViewCanvasSplit.setOnAction(event -> {
            final BooleanProperty isSplit = Ecdar.toggleCanvasSplit();
            if (isSplit.get()) {
                Platform.runLater(() -> editorPresentation.getController().setCanvasModeToSingular());
                menuBarViewCanvasSplit.setText("Split canvas");
            } else {
                Platform.runLater(() -> editorPresentation.getController().setCanvasModeToSplit());
                menuBarViewCanvasSplit.setText("Merge canvases");
            }
        });

        switchGuiView.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                if (Ecdar.getProject().getQueries().isEmpty()) {
                    Ecdar.showToast("Please add a query to simulate before entering the simulator");
                    switchGuiView.setSelected(false);
                    return;
                }

                if (!Ecdar.getSimulationHandler().isSimulationRunning()) {
                    ArrayList<String> queryOptions = Ecdar.getProject().getQueries().stream().map(Query::getQuery).collect(Collectors.toCollection(ArrayList::new));
                    if (!simulationInitializationDialog.getController().simulationComboBox.getItems().equals(queryOptions)) {
                        simulationInitializationDialog.getController().simulationComboBox.getItems().setAll(queryOptions);
                    }

                    simulationInitializationDialogContainer.setVisible(true);
                    simulationInitializationDialog.show(simulationInitializationDialogContainer);
                    simulationInitializationDialog.setMouseTransparent(false);
                } else {
                    currentMode.setValue(Mode.Simulator);
                }
            } else {
                currentMode.setValue(Mode.Editor);
            }
        });

        currentMode.addListener((obs, oldMode, newMode) -> {
            if (newMode == Mode.Editor && oldMode != newMode) {
                enterEditorMode();
            } else if (newMode == Mode.Simulator && oldMode != newMode) {
                enterSimulatorMode();
            }
        });

        menuBarViewEditor.setAccelerator(new KeyCodeCombination(KeyCode.DIGIT1, KeyCombination.SHORTCUT_DOWN));
        menuBarViewEditor.setOnAction(event -> switchGuiView.setSelected(false));

        menuBarViewSimulator.setAccelerator(new KeyCodeCombination(KeyCode.DIGIT2, KeyCombination.SHORTCUT_DOWN));
        menuBarViewSimulator.setOnAction(event -> switchGuiView.setSelected(true));


        // On startup, set the scaling to the values saved in preferences
        Platform.runLater(() -> {
            Ecdar.autoScalingEnabled.setValue(Ecdar.preferences.getBoolean("autoscaling", true));

            Object matchingToggle = scaleM;
            for (Object i : scaling.getToggles()) {
                if (Float.parseFloat(((RadioMenuItem) i).getProperties().get("scale").toString())
                        == Ecdar.preferences.getFloat("scale", 1.0F)) {
                    matchingToggle = i;
                    break;
                }
            }
            scaling.selectToggle(scaleM); // Necessary to avoid project pane appearing off-screen
            scaling.selectToggle((RadioMenuItem) matchingToggle);
        });

    }

    private void updateScaling(double newScale) {
        double newCalculatedScale = getNewCalculatedScale();
        Ecdar.getPresentation().setStyle("-fx-font-size: " + newCalculatedScale + "px;");

        // Text do not scale on the canvas to avoid ugly elements,
        // this zooms in on the component in order to get the "same font size"
        EcdarController.getActiveCanvasPresentation().getController().zoomHelper.setZoomLevel(newCalculatedScale / 13);
        Ecdar.preferences.put("scale", String.valueOf(newScale));

        scaleIcons(root, newCalculatedScale);
        editorPresentation.getController().scaleEdgeStatusToggle(newCalculatedScale);
        messageTabPane.getController().updateScale(newScale);

        // Update listeners of UI scale
        scalingProperty.set(newScale);
    }

    /**
     * Initializes the open project menu item.
     */
    private void initializeOpenProjectMenuItem() {
        menuBarFileOpenProject.setAccelerator(new KeyCodeCombination(KeyCode.O, KeyCombination.SHORTCUT_DOWN));
        menuBarFileOpenProject.setOnAction(event -> {
            // Dialog title
            final DirectoryChooser projectPicker = new DirectoryChooser();
            projectPicker.setTitle("Open project");

            // The initial location for the file choosing dialog
            final File jarDir = new File(System.getProperty("java.class.path")).getAbsoluteFile().getParentFile();

            // If the file does not exist, we must be running it from a development environment, use default location
            if (jarDir.exists()) {
                projectPicker.setInitialDirectory(jarDir);
            }

            // Prompt the user to find a file (will halt the UI thread)
            final File file = projectPicker.showDialog(root.getScene().getWindow());
            if (file != null) {
                try {
                    Ecdar.projectDirectory.set(file.getAbsolutePath());
                    Ecdar.initializeProjectFolder();
                    UndoRedoStack.clear();
                    addProjectToRecentProjects(file.getAbsolutePath());
                } catch (final IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * Initializes the "Recent projects" menu item.
     */
    private void initializeRecentProjectsMenu() {
        ArrayList<String> recentProjects = loadRecentProjects();
        recentProjects.forEach((path) -> {
            MenuItem item = new MenuItem(path);

            item.setOnAction(event -> {
                try {
                    Ecdar.projectDirectory.set(path);
                    Ecdar.initializeProjectFolder();
                } catch (IOException ex) {
                    Ecdar.showToast("Unable to load project: \"" + path + "\"");
                }
            });

            menuBarFileRecentProjects.getItems().add(item);
        });

        MenuItem item;
        if (!recentProjects.isEmpty()) {
            item = new MenuItem("Clear recent projects");

            item.setOnAction(event -> {
                Ecdar.preferences.put("recent_project", "[]");
                menuBarFileRecentProjects.getItems().clear();
                initializeRecentProjectsMenu();
            });
        } else {
            item = new MenuItem("- No recent projects -");
            item.setDisable(true);
        }

        menuBarFileRecentProjects.getItems().add(item);
    }

    /**
     * Saves the project to the {@link Ecdar#projectDirectory} path.
     * This include making directories, converting project files (components and queries)
     * into Json formatted files.
     */
    public void save() {
        if (Ecdar.projectDirectory.isNull().get()) {
            saveAs();
        } else {
            save(new File(Ecdar.projectDirectory.get()));
        }
    }

    /**
     * Save project as.
     */
    private void saveAs() {
        final FileChooser filePicker = new FileChooser();
        filePicker.setTitle("Save project");
        filePicker.setInitialFileName("New Ecdar Project");
        filePicker.setInitialDirectory(new File(System.getProperty("user.home")));

        final File file = filePicker.showSaveDialog(root.getScene().getWindow());
        if (file != null) {
            Ecdar.projectDirectory.setValue(file.getPath());
            save(file);
            addProjectToRecentProjects(file.getPath());
        } else {
            Ecdar.showToast("The project was not saved.");
        }
    }

    private void addProjectToRecentProjects(String projectPath) {
        ArrayList<String> recentProjectPaths = loadRecentProjects();

        // Remove if already present to update order
        recentProjectPaths.remove(projectPath);

        if (recentProjectPaths.size() > 4) {
            recentProjectPaths.remove(4);
        }

        recentProjectPaths.add(projectPath);
        Ecdar.preferences.put("recent_project", new Gson().toJson(recentProjectPaths));

        // Update current recent projects list
        menuBarFileRecentProjects.getItems().clear();
        initializeRecentProjectsMenu();
    }

    private ArrayList<String> loadRecentProjects() {
        String recentProjectsJson = Ecdar.preferences.get("recent_project", "[]");
        ArrayList<String> recentProjectPaths = new ArrayList<>();

        Gson gson = new Gson();
        JsonArray recentProjects = gson.fromJson(recentProjectsJson, JsonArray.class);
        recentProjects.forEach((e) -> recentProjectPaths.add(e.getAsString()));

        Collections.reverse(recentProjectPaths);
        return recentProjectPaths;
    }

    /**
     * Save project at a given directory.
     *
     * @param directory directory to save at
     */
    private static void save(final File directory) {
        try {
            Ecdar.getProject().serialize(directory);
        } catch (final IOException e) {
            Ecdar.showToast("Could not save project: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Initializes menu item for creating a new project.
     */
    private void initializeCreateNewProjectMenuItem() {
        menuBarFileCreateNewProject.setAccelerator(new KeyCodeCombination(KeyCode.N, KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN));
        menuBarFileCreateNewProject.setOnAction(event -> {
            final ButtonType yesButton = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
            final ButtonType noButton = new ButtonType("Don't save", ButtonBar.ButtonData.NO);
            final ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
            final Alert alert = new Alert(Alert.AlertType.NONE,
                    "Do you want to save the existing project?",
                    yesButton,
                    noButton,
                    cancelButton);

            alert.setTitle("Create new project");
            final Optional<ButtonType> result = alert.showAndWait();

            if (result.isPresent()) {
                if (result.get() == yesButton) {
                    save();
                    createNewProject();
                } else if (result.get() == noButton) {
                    createNewProject();
                }
            }
        });
    }

    /**
     * Initializes menu item for creating a new mutation test object.
     */
    private void initializeNewMutationTestObjectMenuItem() {
        menuBarFileNewMutationTestObject.setAccelerator(new KeyCodeCombination(KeyCode.T, KeyCombination.SHORTCUT_DOWN));
        menuBarFileNewMutationTestObject.setOnAction(event -> {
            final MutationTestPlan newPlan = new MutationTestPlan();

            UndoRedoStack.pushAndPerform(() -> { // Perform
                Ecdar.getProject().getTestPlans().add(newPlan);
                getActiveCanvasPresentation().getController().setActiveModel(newPlan);
            }, () -> { // Undo
                Ecdar.getProject().getTestPlans().remove(newPlan);
            }, "Created new mutation test plan", "");
        });
    }

    /**
     * Creates a new project.
     */
    private static void createNewProject() {
        CodeAnalysis.disable();

        CodeAnalysis.clearErrorsAndWarnings();

        Ecdar.projectDirectory.set(null);

        Ecdar.getProject().reset();
        getActiveCanvasPresentation().getController().setActiveModel(Ecdar.getProject().getComponents().get(0));

        UndoRedoStack.clear();

        CodeAnalysis.enable();
    }

    /**
     * Initializes button for exporting as png.
     */
    private void initializeFileExportAsPng() {
        menuBarFileExportAsPng.setAccelerator(new KeyCodeCombination(KeyCode.L, KeyCombination.SHORTCUT_DOWN));
        menuBarFileExportAsPng.setOnAction(event -> {
            // If there is no active component or system
            if (!(getActiveCanvasPresentation().getController().getActiveModel() instanceof Component || getActiveCanvasPresentation().getController().getActiveModel() instanceof EcdarSystem)) {
                Ecdar.showToast("No component or system to export.");
                return;
            }

            CanvasPresentation canvas = getActiveCanvasPresentation();

            // If there was an active component, hide button for toggling declarations
            final ComponentPresentation presentation = canvas.getController().getActiveComponentPresentation();
            if (presentation != null) {
                presentation.getController().toggleDeclarationButton.setVisible(false);
            }

            final WritableImage image = takeSnapshot(canvas);

            // If there was an active component, show the button again
            if (presentation != null) {
                presentation.getController().toggleDeclarationButton.setVisible(true);
            }

            CropAndExportImage(image);
        });

        menuBarFileExportAsPngNoBorder.setOnAction(event -> {
            CanvasPresentation canvas = getActiveCanvasPresentation();

            final ComponentPresentation presentation = canvas.getController().getActiveComponentPresentation();

            //If there is no active component
            if (presentation == null) {
                Ecdar.showToast("No component to export.");
                return;
            }

            presentation.getController().hideBorderAndBackground();
            final WritableImage image = takeSnapshot(canvas);
            presentation.getController().showBorderAndBackground();

            CropAndExportImage(image);
        });
    }

    /**
     * Changes the view and mode to the editor
     * Only enter if the mode is not already Editor
     */
    private void enterEditorMode() {
//        ToDo NIELS: Consider implementing willShow and willHide to handle general elements that should only be available for one of the modes
//        editorPresentation.getController().willShow();
//        simulatorPresentation.getController().willHide();

        borderPane.setCenter(editorPresentation);
        leftPane.getChildren().clear();
        leftPane.getChildren().add(projectPane);
        rightPane.getChildren().clear();
        rightPane.getChildren().add(queryPane);

        // Enable or disable the menu items that can be used when in the simulator
//        updateMenuItems();
    }

    /**
     * Changes the view and mode to the simulator
     * Only enter if the mode is not already Simulator
     */
    private void enterSimulatorMode() {
//        ToDo NIELS: Consider implementing willShow and willHide to handle general elements that should only be available for one of the modes
//        ecdarPresentation.getController().willHide();
        simulatorPresentation.getController().willShow();

        borderPane.setCenter(simulatorPresentation);
        leftPane.getChildren().clear();
        leftPane.getChildren().add(leftSimPane);
        rightPane.getChildren().clear();
        rightPane.getChildren().add(rightSimPane);

        // Enable or disable the menu items that can be used when in the simulator
//        updateMenuItems();
    }

    /**
     * Take a snapshot with the grid hidden.
     * The grid is put into its original state afterwards.
     *
     * @return the snapshot
     */
    private WritableImage takeSnapshot(CanvasPresentation canvas) {
        final WritableImage image;

        canvas.getController().disallowGrid();
        image = scaleAndTakeSnapshot(canvas);
        canvas.getController().allowGrid();

        return image;
    }

    /**
     * Zooms in times 4 to get a higher resolution.
     * Then take snapshot and zoom to times 1 again.
     *
     * @return the snapshot
     */
    private WritableImage scaleAndTakeSnapshot(CanvasPresentation canvas) {
        final WritableImage image;
        Double zoomLevel = canvas.getController().zoomHelper.getZoomLevel();
        canvas.getController().zoomHelper.zoomToFit();

        image = canvas.snapshot(new SnapshotParameters(), null);

        canvas.getController().zoomHelper.setZoomLevel(zoomLevel);
        return image;
    }

    /**
     * Crops and exports an image.
     *
     * @param image the image
     */
    private void CropAndExportImage(final WritableImage image) {
        final String name = getActiveCanvasPresentation().getController().getActiveModel().getName();

        final FileChooser filePicker = new FileChooser();
        filePicker.setTitle("Export png");
        filePicker.setInitialFileName(name + ".png");

        // Set initial directory to project directory (if saved) or user.home (otherwise)
        String directory = Ecdar.projectDirectory.get();
        if (directory == null) directory = System.getProperty("user.home");

        filePicker.setInitialDirectory(new File(directory));
        filePicker.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG File", "*.png"));

        final BufferedImage finalImage;
        try {
            finalImage = autoCropImage(SwingFXUtils.fromFXImage(image, null));
        } catch (final IllegalArgumentException e) {
            Ecdar.showToast("Export failed. " + e.getMessage());
            return;
        }

        final File file = filePicker.showSaveDialog(root.getScene().getWindow());
        if (file != null) {
            try {
                ImageIO.write(finalImage, "png", file);
                Ecdar.showToast("Export succeeded.");
            } catch (final IOException e) {
                Ecdar.showToast("Export failed. " + e.getMessage());
            }
        } else {
            Ecdar.showToast("Export was cancelled.");
        }
    }

    /**
     * Crops an image so that the all-white borders are removed.
     *
     * @param image the original image
     * @return the cropped image
     */
    private static BufferedImage autoCropImage(final BufferedImage image) {
        final int topY = getAutoCropTopY(image);
        final int leftX = getAutoCropLeftX(image);
        final int rightX = getAutoCropRightX(image);
        final int bottomY = getAutoCropBottomY(image);


        return image.getSubimage(leftX, topY, rightX - leftX, bottomY - topY);
    }

    /**
     * Gets the top y coordinate of an auto cropped image.
     *
     * @param image the original image
     * @return the y coordinate
     */
    private static int getAutoCropTopY(final BufferedImage image) {
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if (image.getRGB(x, y) != java.awt.Color.WHITE.getRGB()) {
                    return y;
                }
            }
        }

        throw new IllegalArgumentException("Image is all white");
    }

    /**
     * Gets the left x coordinate of an auto cropped image.
     *
     * @param image the original image
     * @return the x coordinate
     */
    private static int getAutoCropLeftX(final BufferedImage image) {
        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                if (image.getRGB(x, y) != java.awt.Color.WHITE.getRGB()) {
                    return x;
                }
            }
        }

        throw new IllegalArgumentException("Image is all white");
    }

    /**
     * Gets the bottom y coordinate of an auto cropped image.
     *
     * @param image the original image
     * @return the y coordinate
     */
    private static int getAutoCropBottomY(final BufferedImage image) {
        for (int y = image.getHeight() - 1; y >= 0; y--) {
            for (int x = 0; x < image.getWidth(); x++) {
                if (image.getRGB(x, y) != java.awt.Color.WHITE.getRGB()) {
                    return y;
                }
            }
        }

        throw new IllegalArgumentException("Image is all white");
    }

    /**
     * Gets the right x coordinate of an auto cropped image.
     *
     * @param image the original image
     * @return the x coordinate
     */
    private static int getAutoCropRightX(final BufferedImage image) {
        for (int x = image.getWidth() - 1; x >= 0; x--) {
            for (int y = 0; y < image.getHeight(); y++) {
                if (image.getRGB(x, y) != java.awt.Color.WHITE.getRGB()) {
                    return x;
                }
            }
        }

        throw new IllegalArgumentException("Image is all white");
    }

    /**
     * This method is used to push the contents of the project and query panes when the tab pane is opened
     */
    private void changeInsetsOfProjectAndQueryPanes() {
        if (messageTabPane.getController().isOpen()) {
            projectPane.showBottomInset(false);
            queryPane.showBottomInset(false);
            CanvasPresentation.showBottomInset(false);
        } else {
            projectPane.showBottomInset(true);
            queryPane.showBottomInset(true);
            CanvasPresentation.showBottomInset(true);
        }
    }

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
                    if (foundUnNudgableElement[0]) {
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
    private void closeQueryDialog() {
        modellingHelpDialog.close();
        queryDialog.close();
    }

    public static void openQueryDialog(final Query query, final String text) {
        if (text != null) {
            _queryTextResult.setText(text);
        }
        if (query != null) {
            _queryTextQuery.setText(query.getQuery());
        }
        _queryDialog.show();
    }
}
