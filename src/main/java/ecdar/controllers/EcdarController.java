package ecdar.controllers;

import ecdar.Debug;
import ecdar.Ecdar;
import ecdar.abstractions.*;
import ecdar.backend.BackendException;
import ecdar.backend.BackendHelper;
import ecdar.backend.QueryListener;
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
import javafx.application.Platform;
import javafx.beans.binding.When;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
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
import javafx.util.Pair;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EcdarController implements Initializable {

    // Reachability analysis
    public static boolean reachabilityServiceEnabled = false;
    private static long reachabilityTime = Long.MAX_VALUE;
    private static ExecutorService reachabilityService;

    private static final ObjectProperty<EdgeStatus> globalEdgeStatus = new SimpleObjectProperty<>(EdgeStatus.INPUT);

    // View stuff
    public StackPane root;
    public BorderPane borderPane;
    public StackPane canvasPane;
    public StackPane leftPane;
    public StackPane rightPane;
    public Rectangle bottomFillerElement;
    public QueryPanePresentation queryPane;
    public ProjectPanePresentation filePane;
    public HBox toolbar;
    public MessageTabPanePresentation messageTabPane;
    public StackPane dialogContainer;
    public JFXDialog dialog;
    public StackPane modalBar;
    public JFXTextField queryTextField;
    public JFXTextField commentTextField;
    public JFXRippler colorSelected;
    public JFXRippler deleteSelected;
    public JFXRippler undo;
    public JFXRippler redo;

    public ImageView helpInitialImage;
    public StackPane helpInitialPane;
    public ImageView helpUrgentImage;
    public StackPane helpUrgentPane;
    public ImageView helpInputImage;
    public StackPane helpInputPane;
    public ImageView helpOutputImage;
    public StackPane helpOutputPane;

    public JFXButton switchToInputButton;
    public JFXButton switchToOutputButton;
    public JFXToggleButton switchEdgeStatusButton;

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
    public MenuItem menuBarViewFilePanel;
    public MenuItem menuBarViewQueryPanel;
    public MenuItem menuBarViewGrid;
    public MenuItem menuBarAutoscaling;
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
    public MenuItem menuBarFileCreateNewProject;
    public MenuItem menuBarFileOpenProject;
    public MenuItem menuBarFileSave;
    public MenuItem menuBarFileSaveAs;
    public MenuItem menuBarFileNewMutationTestObject;
    public MenuItem menuBarFileExportAsPng;
    public MenuItem menuBarFileExportAsPngNoBorder;
    public MenuItem menuBarOptionsCache;
    public MenuItem menuBarOptionsBackendOptions;
    public MenuItem menuBarHelpHelp;
    public MenuItem menuBarHelpAbout;
    public MenuItem menuBarHelpTest;

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

    private static JFXDialog _queryDialog;
    private static Text _queryTextResult;
    private static Text _queryTextQuery;

    private static final ObjectProperty<CanvasPresentation> activeCanvasPresentation = new SimpleObjectProperty<>(new CanvasPresentation());

    public static void runReachabilityAnalysis() {
        if (!reachabilityServiceEnabled) return;

        reachabilityTime = System.currentTimeMillis() + 500;
    }

    public static EdgeStatus getGlobalEdgeStatus() {
        return globalEdgeStatus.get();
    }

    /**
     * Finds and scales all icon nodes below the given node in accordance with the current scaling, using -fx-icon-size
     *
     * @param node The "root" to start the search from
     */
    public void scaleIcons(Node node) {
        Platform.runLater(() -> {
            scaleIcons(node, getCalculatedNewScale());
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
    }

    private double getCalculatedNewScale() {
        return (Double.parseDouble(scaling.getSelectedToggle().getProperties().get("scale").toString()) * Ecdar.getDpiScale()) * 13.0;
    }

    private void scaleEdgeStatusToggle(double size) {
        switchEdgeStatusButton.setScaleX(size / 13.0);
        switchEdgeStatusButton.setScaleY(size / 13.0);
    }

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        initilizeDialogs();
        initializeCanvasPane();
        initializeEdgeStatusHandling();
        initializeKeybindings();
        initializeStatusBar();
        initializeMenuBar();
        initializeReachabilityAnalysisThread();

        bottomFillerElement.heightProperty().bind(messageTabPane.maxHeightProperty());
        messageTabPane.getController().setRunnableForOpeningAndClosingMessageTabPane(this::changeInsetsOfFileAndQueryPanes);
    }

    private void initilizeDialogs() {
        dialog.setDialogContainer(dialogContainer);
        dialogContainer.opacityProperty().bind(dialog.getChildren().get(0).scaleXProperty());
        dialog.setOnDialogClosed(event -> dialogContainer.setVisible(false));

        _queryDialog = queryDialog;
        _queryTextResult = queryTextResult;
        _queryTextQuery = queryTextQuery;

        initializeDialog(queryDialog, queryDialogContainer);
        initializeDialog(backendOptionsDialog, backendOptionsDialogContainer);

        backendOptionsDialog.getController().resetBackendsButton.setOnMouseClicked(event -> {
            backendOptionsDialog.getController().resetBackendsToDefault();
        });

        backendOptionsDialog.getController().closeButton.setOnMouseClicked(event -> {
            backendOptionsDialog.getController().cancelBackendOptionsChanges();
            dialog.close();
            backendOptionsDialog.close();
        });

        backendOptionsDialog.getController().saveButton.setOnMouseClicked(event -> {
            if (backendOptionsDialog.getController().saveChangesToBackendOptions()) {
                dialog.close();
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

        filePane.getStyleClass().add("responsive-pane-sizing");
        queryPane.getStyleClass().add("responsive-pane-sizing");

        initializeEdgeStatusHandling();
        initializeKeybindings();
        initializeStatusBar();
        initializeMenuBar();
        initializeReachabilityAnalysisThread();
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
     *
     * @param enabledColor  The new color for the selected objects
     * @param previousColor The color old color of the selected objects
     */
    public void changeColorOnSelectedElements(final EnabledColor enabledColor,
                                              final List<Pair<SelectHelper.ItemSelectable, EnabledColor>> previousColor) {
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
        switchToInputButton.setDisableVisualFocus(true); // Hiding input button rippler on start-up

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

        // Ensure that the rippler is centered when scale is changed
        Platform.runLater(() -> ((JFXRippler) switchEdgeStatusButton.lookup(".jfx-rippler")).setRipplerRecenter(true));
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
                            final String locationReachableQuery = BackendHelper.getLocationReachableQuery(location, component);

                            Query reachabilityQuery = new Query(locationReachableQuery, "", QueryState.UNKNOWN);
                            reachabilityQuery.setType(QueryType.REACHABILITY);

                            Ecdar.getBackendDriver().addQueryToExecutionQueue(locationReachableQuery,
                                    BackendHelper.getDefaultBackendInstance(),
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
                                    new QueryListener(reachabilityQuery));

                            final Thread verifyThread = new Thread(() -> Ecdar.getBackendDriver().addQueryToExecutionQueue(locationReachableQuery,
                                    BackendHelper.getDefaultBackendInstance(),
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
                                    new QueryListener(reachabilityQuery)));

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

    public static CanvasPresentation getActiveCanvasPresentation() {
        return activeCanvasPresentation.get();
    }

    public static DoubleProperty getActiveCanvasZoomFactor() {
        return getActiveCanvasPresentation().getController().zoomHelper.currentZoomFactor;
    }

    public static void setActiveCanvasPresentation(CanvasPresentation newActiveCanvasPresentation) {
        activeCanvasPresentation.get().setOpacity(0.75);
        newActiveCanvasPresentation.setOpacity(1);
        activeCanvasPresentation.set(newActiveCanvasPresentation);
    }

    public static void setActiveModelForActiveCanvas(HighLevelModelObject newActiveModel) {
        EcdarController.getActiveCanvasPresentation().getController().setActiveModel(newActiveModel);
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
        menuBarViewFilePanel.getGraphic().setOpacity(1);
        menuBarViewFilePanel.setAccelerator(new KeyCodeCombination(KeyCode.P, KeyCodeCombination.SHORTCUT_DOWN));
        menuBarViewFilePanel.setOnAction(event -> {
            final BooleanProperty isOpen = Ecdar.toggleFilePane();
            menuBarViewFilePanel.getGraphic().opacityProperty().bind(new When(isOpen).then(1).otherwise(0));
        });

        menuBarViewQueryPanel.getGraphic().setOpacity(0);
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

        menuBarAutoscaling.getGraphic().setOpacity(Ecdar.autoScalingEnabled.getValue() ? 1 : 0);
        menuBarAutoscaling.setOnAction(event -> {
            Ecdar.autoScalingEnabled.setValue(!Ecdar.autoScalingEnabled.getValue());
            updateScaling(getCalculatedNewScale() / 13);
            Ecdar.preferences.put("autoscaling", String.valueOf(Ecdar.autoScalingEnabled.getValue()));
        });
        Ecdar.autoScalingEnabled.addListener((observable, oldValue, newValue) -> {
            menuBarAutoscaling.getGraphic().opacityProperty().setValue(newValue ? 1 : 0);
        });

        scaling.selectedToggleProperty().addListener((observable, oldValue, newValue) -> updateScaling(Double.parseDouble(newValue.getProperties().get("scale").toString())));

        menuBarViewCanvasSplit.getGraphic().setOpacity(1);
        menuBarViewCanvasSplit.setOnAction(event -> {
            final BooleanProperty isSplit = Ecdar.toggleCanvasSplit();
            if (isSplit.get()) {
                Platform.runLater(this::setCanvasModeToSingular);
                menuBarViewCanvasSplit.setText("Split canvas");
            } else {
                Platform.runLater(this::setCanvasModeToSplit);
                menuBarViewCanvasSplit.setText("Merge canvases");
            }
        });

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
        double calculatedNewScale = getCalculatedNewScale();
        Ecdar.getPresentation().setStyle("-fx-font-size: " + calculatedNewScale + "px;");

        // Text do not scale on the canvas to avoid ugly elements,
        // this zooms in on the component in order to get the "same font size"
        EcdarController.getActiveCanvasPresentation().getController().zoomHelper.setZoomLevel(calculatedNewScale / 13);
        Ecdar.preferences.put("scale", String.valueOf(newScale));

        scaleIcons(root, calculatedNewScale);
        scaleEdgeStatusToggle(calculatedNewScale);
        messageTabPane.getController().updateScale(newScale);
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
                } catch (final IOException e) {
                    e.printStackTrace();
                }
            }
        });
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
        } else {
            Ecdar.showToast("The project was not saved.");
        }
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

            CanvasPresentation canvasShell = getActiveCanvasPresentation();
            CanvasPresentation canvas = getActiveCanvasPresentation();

            // If there was an active component, hide button for toggling declarations
            final ComponentPresentation presentation = canvas.getController().getActiveComponentPresentation();
            if (presentation != null) {
                presentation.getController().toggleDeclarationButton.setVisible(false);
            }

            final WritableImage image = takeSnapshot(canvasShell);

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

    private void initializeCanvasPane() {
        Platform.runLater(this::setCanvasModeToSingular);
    }

    /**
     * Removes the canvases and adds a new one, with the active component of the active canvasPresentation.
     */
    private void setCanvasModeToSingular() {
        canvasPane.getChildren().clear();
        CanvasPresentation canvasPresentation = new CanvasPresentation();
        HighLevelModelObject model = activeCanvasPresentation.get().getController().getActiveModel();
        if (model != null) {
            canvasPresentation.getController().setActiveModel(activeCanvasPresentation.get().getController().getActiveModel());
        } else {
            // If no components where found, the project has not been initialized. The active model will be updated when the project is initialized
            canvasPresentation.getController().setActiveModel(Ecdar.getProject().getComponents().stream().findFirst().orElse(null));
        }

        canvasPane.getChildren().add(canvasPresentation);
        activeCanvasPresentation.set(canvasPresentation);
        filePane.getController().updateColorsOnFilePresentations();

        Rectangle clip = new Rectangle();
        clip.setArcWidth(1);
        clip.setArcHeight(1);
        clip.widthProperty().bind(canvasPane.widthProperty());
        clip.heightProperty().bind(canvasPane.heightProperty());
        canvasPresentation.getController().zoomablePane.setClip(clip);

        canvasPresentation.getController().zoomablePane.minWidthProperty().bind(canvasPane.widthProperty());
        canvasPresentation.getController().zoomablePane.maxWidthProperty().bind(canvasPane.widthProperty());
        canvasPresentation.getController().zoomablePane.minHeightProperty().bind(canvasPane.heightProperty());
        canvasPresentation.getController().zoomablePane.maxHeightProperty().bind(canvasPane.heightProperty());
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

        // Add the canvasPresentation at the top-left
        CanvasPresentation canvasPresentation = initializeNewCanvasPresentation();
        canvasPresentation.getController().setActiveModel(getActiveCanvasPresentation().getController().getActiveModel());
        canvasGrid.add(canvasPresentation, 0, 0);
        setActiveCanvasPresentation(canvasPresentation);

        // Add the canvasPresentation at the top-right
        canvasPresentation = initializeNewCanvasPresentationWithActiveComponent(components, currentCompNum);
        canvasPresentation.setOpacity(0.75);
        canvasGrid.add(canvasPresentation, 1, 0);

        // Update the startIndex for the next canvasPresentation
        for (int i = 0; i < numComponents; i++) {
            if (canvasPresentation.getController().getActiveModel() != null && canvasPresentation.getController().getActiveModel().equals(components.get(i))) {
                currentCompNum = i + 1;
            }
        }

        // Add the canvasPresentation at the bottom-left
        canvasPresentation = initializeNewCanvasPresentationWithActiveComponent(components, currentCompNum);
        canvasPresentation.setOpacity(0.75);
        canvasGrid.add(canvasPresentation, 0, 1);

        // Update the startIndex for the next canvasPresentation
        for (int i = 0; i < numComponents; i++)
            if (canvasPresentation.getController().getActiveModel() != null && canvasPresentation.getController().getActiveModel().equals(components.get(i))) {
                currentCompNum = i + 1;
            }

        // Add the canvasPresentation at the bottom-right
        canvasPresentation = initializeNewCanvasPresentationWithActiveComponent(components, currentCompNum);
        canvasPresentation.setOpacity(0.75);
        canvasGrid.add(canvasPresentation, 1, 1);

        canvasPane.getChildren().add(canvasGrid);
        filePane.getController().updateColorsOnFilePresentations();
    }

    /**
     * Initialize a new CanvasShellPresentation and set its active component to the next component encountered from the startIndex and return it
     *
     * @param components the list of components for assigning active component of the CanvasPresentation
     * @param startIndex the index to start at when trying to find the component to set as active
     * @return new CanvasShellPresentation
     */
    private CanvasPresentation initializeNewCanvasPresentationWithActiveComponent(ObservableList<Component> components, int startIndex) {
        CanvasPresentation canvasPresentation = initializeNewCanvasPresentation();

        int numComponents = components.size();
        canvasPresentation.getController().setActiveModel(null);
        for (int currentCompNum = startIndex; currentCompNum < numComponents; currentCompNum++) {
            if (getActiveCanvasPresentation().getController().getActiveModel() != components.get(currentCompNum)) {
                canvasPresentation.getController().setActiveModel(components.get(currentCompNum));
                break;
            }
        }

        return canvasPresentation;
    }

    /**
     * Initialize a new CanvasShellPresentation and return it
     *
     * @return new CanvasShellPresentation
     */
    private CanvasPresentation initializeNewCanvasPresentation() {
        CanvasPresentation canvasShellPresentation = new CanvasPresentation();
        canvasShellPresentation.setBorder(new Border(new BorderStroke(Color.GREY.getColor(Color.Intensity.I500), BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderStroke.THIN)));
        return canvasShellPresentation;
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
     * This method is used to push the contents of the file and query panes when the tab pane is opened
     */
    public void changeInsetsOfFileAndQueryPanes() {
        if (messageTabPane.getController().isOpen()) {
            filePane.showBottomInset(false);
            queryPane.showBottomInset(false);
            CanvasPresentation.showBottomInset(false);
        } else {
            filePane.showBottomInset(true);
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
                if (edge.getIsLockedProperty().getValue()) {
                    return;
                }

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
     *
     * @param status the status
     */
    private void setGlobalEdgeStatus(EdgeStatus status) {
        globalEdgeStatus.set(status);
    }

    @FXML
    private void closeQueryDialog() {
        dialog.close();
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
