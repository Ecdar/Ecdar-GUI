package SW9.controllers;

import SW9.Debug;
import SW9.Ecdar;
import SW9.abstractions.*;
import SW9.backend.BackendException;
import SW9.backend.UPPAALDriver;
import SW9.code_analysis.CodeAnalysis;
import SW9.presentations.*;
import SW9.utility.UndoRedoStack;
import SW9.utility.colors.Color;
import SW9.utility.colors.EnabledColor;
import SW9.utility.helpers.SelectHelper;
import SW9.utility.keyboard.Keybind;
import SW9.utility.keyboard.KeyboardTracker;
import SW9.utility.keyboard.NudgeDirection;
import SW9.utility.keyboard.Nudgeable;
import com.jfoenix.controls.*;
import javafx.animation.Interpolator;
import javafx.animation.Transition;
import javafx.application.Platform;
import javafx.beans.binding.When;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ListChangeListener;
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
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class HUPPAALController implements Initializable {

    // Reachability analysis
    public static boolean reachabilityServiceEnabled = false;
    private static long reachabilityTime = Long.MAX_VALUE;
    private static ExecutorService reachabilityService;

    private static EdgeStatus globalEdgeStatus;

    // View stuff
    public StackPane root;
    public QueryPanePresentation queryPane;
    public ProjectPanePresentation filePane;
    public StackPane toolbar;
    public Label queryPaneFillerElement;
    public Label filePaneFillerElement;
    public CanvasPresentation canvas;
    public StackPane dialogContainer;
    public JFXDialog dialog;
    public StackPane modalBar;
    public JFXTextField queryTextField;
    public JFXTextField commentTextField;
    public JFXRippler generateUppaalModel;
    public JFXRippler colorSelected;
    public JFXRippler deleteSelected;
    public JFXRippler undo;
    public JFXRippler redo;
    public JFXTabPane tabPane;
    public Tab errorsTab;
    public Tab warningsTab;
    public Rectangle tabPaneResizeElement;
    public StackPane tabPaneContainer;
    public JFXRippler switchToInput;
    public JFXRippler switchToOutput;

    public StackPane inputModePane;
    public StackPane outputModePane;
    public ImageView inputModeImage;
    public ImageView outputModeImage;

    private double expandHeight = 300;

    public final Transition expandMessagesContainer = new Transition() {
        {
            setInterpolator(Interpolator.SPLINE(0.645, 0.045, 0.355, 1));
            setCycleDuration(Duration.millis(200));
        }

        @Override
        protected void interpolate(final double frac) {
            setMaxHeight(35 + frac * (expandHeight - 35));
        }
    };
    public Rectangle bottomFillerElement;
    public JFXRippler collapseMessages;
    public FontIcon collapseMessagesIcon;
    public ScrollPane errorsScrollPane;
    public VBox errorsList;
    public ScrollPane warningsScrollPane;
    public VBox warningsList;
    public Tab backendErrorsTab;
    public ScrollPane backendErrorsScrollPane;
    public VBox backendErrorsList;

    // The program top menu
    public MenuBar menuBar;
    public MenuItem menuBarViewFilePanel;
    public MenuItem menuBarViewQueryPanel;
    public MenuItem menuBarViewGrid;
    public MenuItem menuBarFileSave;
    public MenuItem menuBarFileSaveAs;
    public MenuItem menuBarFileOpenProject;
    public MenuItem menuBarFileExportAsPng;
    public MenuItem menuBarHelpHelp;
    public MenuItem menuBarEditBalance;

    public JFXSnackbar snackbar;
    public HBox statusBar;
    public Label statusLabel;
    public Label queryLabel;
    public HBox queryStatusContainer;

    public StackPane queryDialogContainer;
    public JFXDialog queryDialog;
    public Text queryTextResult;
    public Text queryTextQuery;

    private static JFXDialog _queryDialog;
    private static Text _queryTextResult;
    private static Text _queryTextQuery;

    private double tabPanePreviousY = 0;
    public boolean shouldISkipOpeningTheMessagesContainer = true;

    public static void runReachabilityAnalysis() {
        if (!reachabilityServiceEnabled) return;

        reachabilityTime = System.currentTimeMillis() + 500;
    }

    public static EdgeStatus getGlobalEdgeStatus() {
        return globalEdgeStatus;
    }

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {

        dialog.setDialogContainer(dialogContainer);
        dialogContainer.opacityProperty().bind(dialog.getChildren().get(0).scaleXProperty());
        dialog.setOnDialogClosed(event -> dialogContainer.setVisible(false));

        _queryDialog = queryDialog;
        _queryTextResult = queryTextResult;
        _queryTextQuery = queryTextQuery;
        queryDialog.setDialogContainer(queryDialogContainer);
        queryDialogContainer.opacityProperty().bind(queryDialog.getChildren().get(0).scaleXProperty());
        queryDialog.setOnDialogClosed(event -> {
            queryDialogContainer.setVisible(false);
            queryDialogContainer.setMouseTransparent(true);
        });
        queryDialog.setOnDialogOpened(event -> {
            queryDialogContainer.setVisible(true);
            queryDialogContainer.setMouseTransparent(false);
        });

        globalEdgeStatus = EdgeStatus.INPUT;

        Tooltip.install(switchToInput, new Tooltip("Switch to input mode"));
        Tooltip.install(switchToOutput, new Tooltip("Switch to output mode"));

        //Press ctrl+N or cmd+N to create a new component. The canvas changes to this new component
        KeyCodeCombination combination = new KeyCodeCombination(KeyCode.N, KeyCombination.SHORTCUT_DOWN);
        Keybind binding = new Keybind(combination, (event) -> {
            final Component newComponent = new Component(true);
            UndoRedoStack.push(() -> { // Perform
                Ecdar.getProject().getComponents().add(newComponent);
            }, () -> { // Undo
                Ecdar.getProject().getComponents().remove(newComponent);
            }, "Created new component: " + newComponent.getName(), "add-circle");

            CanvasController.setActiveComponent(newComponent);
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

                UndoRedoStack.push(() -> { // Perform
                    SelectHelper.getSelectedElements().forEach(selectable -> {
                        selectable.color(enabledColor.color, enabledColor.intensity);
                    });
                }, () -> { // Undo
                    previousColor.forEach(selectableEnabledColorPair -> {
                        selectableEnabledColorPair.getKey().color(selectableEnabledColorPair.getValue().color, selectableEnabledColorPair.getValue().intensity);
                    });
                }, String.format("Changed the color of %d elements to %s", previousColor.size(), enabledColor.color.name()), "color-lens");

                SelectHelper.clearSelectedElements();
            }));
        });

        final BooleanProperty hasChanged = new SimpleBooleanProperty(false);



        Ecdar.getProject().getComponents().addListener(new ListChangeListener<Component>() {
            @Override
            public void onChanged(final Change<? extends Component> c) {
                if (!hasChanged.get()) {
                    CanvasController.setActiveComponent(Ecdar.getProject().getComponents().get(0));
                    hasChanged.set(true);
                }

                if(Ecdar.serializationDone && Ecdar.getProject().getComponents().size() - 1 == 0 && Ecdar.getProject().getMainComponent() == null) {
                    c.next();
                    c.getAddedSubList().get(0).setIsMain(true);
                }

            }
        });

        initializeTabPane();
        initializeStatusBar();
        initializeMessages();
        initializeMenuBar();
        initializeNoMainComponentError();

        initializeReachabilityAnalysisThread();

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
                    UPPAALDriver.buildHUPPAALDocument();

                    Ecdar.getProject().getQueries().forEach(query -> {
                        if (query.isPeriodic()) query.run();
                    });

                    // List of threads to start
                    List<Thread> threads = new ArrayList<>();

                    // Submit all background reachability queries
                    Ecdar.getProject().getComponents().forEach(component -> {
                        // Check if we should consider this component
                        if (!component.isIncludeInPeriodicCheck()) {
                            component.getLocationsWithInitialAndFinal().forEach(location -> location.setReachability(Location.Reachability.EXCLUDED));
                        } else {
                            component.getLocationsWithInitialAndFinal().forEach(location -> {
                                final String locationReachableQuery = UPPAALDriver.getLocationReachableQuery(location, component);
                                final Thread verifyThread = UPPAALDriver.runQuery(
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

                                verifyThread.setName(locationReachableQuery + " (" + verifyThread.getName() + ")");
                                Debug.addThread(verifyThread);
                                threads.add(verifyThread);
                            });
                        }
                    });

                    threads.forEach((verifyThread) -> reachabilityService.submit(verifyThread::start));

                } catch (final BackendException e) {
                    // Something went wrong with creating the document
                    e.printStackTrace();
                } catch (final Exception ignored) {
                    // The main component is null. Ignore.
                }
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
                        if(Debug.backgroundThreads.size() == 0) {
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

    private void initializeNoMainComponentError() {
        final CodeAnalysis.Message noMainComponentErrorMessage = new CodeAnalysis.Message("No main component specified", CodeAnalysis.MessageType.ERROR);

        Ecdar.getProject().mainComponentProperty().addListener((obs, oldMain, newMain) -> {
            if(newMain == null) {
                CodeAnalysis.addMessage(null, noMainComponentErrorMessage);
            } else {
                HUPPAALController.runReachabilityAnalysis();
                CodeAnalysis.removeMessage(null, noMainComponentErrorMessage);
            }
        });


    }

    private void initializeMenuBar() {
        menuBar.setUseSystemMenuBar(true);

        menuBarFileSave.setAccelerator(new KeyCodeCombination(KeyCode.S, KeyCombination.SHORTCUT_DOWN));
        menuBarFileSave.setOnAction(event -> {
            Ecdar.save();
        });

        menuBarFileSaveAs.setAccelerator(new KeyCodeCombination(KeyCode.S, KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN));
        menuBarFileSaveAs.setOnAction(event -> {
            FileChooser filePicker = new FileChooser();
            filePicker.setTitle("Save project");
            filePicker.setInitialFileName("New Ecdar Project");
            filePicker.setInitialDirectory(new File(Ecdar.projectDirectory.get()));

            File file = filePicker.showSaveDialog(root.getScene().getWindow());
            if (file != null){
                Ecdar.saveAs(file.getPath());
            } else {
                Ecdar.showToast("The project was not saved");
            }

        });

        menuBarFileOpenProject.setAccelerator(new KeyCodeCombination(KeyCode.O, KeyCombination.SHORTCUT_DOWN));
        menuBarFileOpenProject.setOnAction(event -> {
            // Dialog title
            final DirectoryChooser projectPicker = new DirectoryChooser();
            projectPicker.setTitle("Open project");

            // The initial location for the file choosing dialog
            final File jarDir = new File(System.getProperty("java.class.path")).getAbsoluteFile().getParentFile();

            // If the file does not exist, we must be running it from a development environment, use an default location
            if(jarDir.exists()) {
                projectPicker.setInitialDirectory(jarDir);
            }

            // Prompt the user to find a file (will halt the UI thread)
            final File file = projectPicker.showDialog(root.getScene().getWindow());
            if(file != null) {
                try {
                    Ecdar.projectDirectory.set(file.getAbsolutePath());
                    Ecdar.initializeProjectFolder();
                } catch (final IOException e) {
                    e.printStackTrace();
                }
            }
        });

        menuBarFileExportAsPng.setAccelerator(new KeyCodeCombination(KeyCode.L, KeyCombination.SHORTCUT_DOWN));
        menuBarFileExportAsPng.setOnAction(event -> {
            //If there is no active component
            if(CanvasController.getActiveComponent() == null){
                Ecdar.showToast("No component to export");
                return;
            }
            //Save as png in picked directory
            final WritableImage image;
            String name = CanvasController.getActiveComponent().getName();
            if(canvas.isGridOn()){
                canvas.toggleGrid();
                image = canvas.snapshot(new SnapshotParameters(), null);
                canvas.toggleGrid();
            } else {
                image = canvas.snapshot(new SnapshotParameters(), null);
            }

            FileChooser filePicker = new FileChooser();
            filePicker.setTitle("Export png");
            filePicker.setInitialFileName(name);
            filePicker.setInitialDirectory(new File(Ecdar.projectDirectory.get()));
            FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("PNG File", "*.png");
            filePicker.getExtensionFilters().add(extFilter);

            File file = filePicker.showSaveDialog(root.getScene().getWindow());
            if (file != null){
                try {
                    ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", file);
                    Ecdar.showToast("Export succeeded");
                } catch (IOException e){
                    Ecdar.showToast("Export failed "+ e.getMessage());
                }
            } else {
                Ecdar.showToast("Export was cancelled");
            }
        });


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

        menuBarHelpHelp.setOnAction(event -> Ecdar.showHelp());

        menuBarEditBalance.setAccelerator(new KeyCodeCombination(KeyCode.B, KeyCombination.SHORTCUT_DOWN));
        menuBarEditBalance.setOnAction(event -> {
            // Map to store the previous identifiers (to undo/redo)
            final Map<Location, String> previousIdentifiers = new HashMap<>();

            UndoRedoStack.push(() -> { // Perform
                // Set the counter used to generate the identifiers
                Location.resetHiddenID();

                // A list of components we have not ordered yet
                final List<Component> missingComponents = new ArrayList<>();
                Ecdar.getProject().getComponents().forEach(missingComponents::add);

                // List to iterate through the components
                final List<SubComponent> subComponentsToCheck = new ArrayList<>();

                // Consumer to reset the location identifier
                final Consumer<Location> resetLocation = (location -> {
                    previousIdentifiers.put(location, location.getId());
                    location.resetId();
                });

                // Consumer to reset the location identifiers in a given component
                final Consumer<Component> resetLocationsInComponent = (component) -> {
                    // Check if we already balanced this component
                    if(!missingComponents.contains(component)) return;

                    // Set the identifier for the initial location
                    resetLocation.accept(component.getInitialLocation());

                    // Set the identifiers for the rest of the locations
                    component.getLocations().forEach(resetLocation);

                    // Set the identifier for the final location
                    resetLocation.accept(component.getFinalLocation());

                    // We are now finished with this component, remove it from the list and add subcomponents to the checking list
                    missingComponents.remove(component);
                    component.getSubComponents().forEach(subComponentsToCheck::add);
                };

                // Balance the identifiers in the main component
                resetLocationsInComponent.accept(Ecdar.getProject().getMainComponent());

                // While we are missing subcomponents, balance them!
                while(!subComponentsToCheck.isEmpty()) {
                    // Pick the 0th element which we will now check
                    final SubComponent subComponent = subComponentsToCheck.get(0);

                    // Reset the location identifiers in the given subcomponent's component
                    resetLocationsInComponent.accept(subComponent.getComponent());

                    // Remove the subcomponent from the list
                    subComponentsToCheck.remove(0);
                }

                // If we still need to balance some component (they might not be used) then do it now
                while(!missingComponents.isEmpty()) {
                    resetLocationsInComponent.accept(missingComponents.get(0));
                }
            }, () -> { // Undo
                previousIdentifiers.forEach(Location::setId);
            }, "Balanced location identifiers", "shuffle");
        });
    }

    private void initializeMessages() {
        final Map<Component, MessageCollectionPresentation> componentMessageCollectionPresentationMapForErrors = new HashMap<>();
        final Map<Component, MessageCollectionPresentation> componentMessageCollectionPresentationMapForWarnings = new HashMap<>();

        final Consumer<Component> addComponent = (component) -> {
            final MessageCollectionPresentation messageCollectionPresentationErrors = new MessageCollectionPresentation(component, CodeAnalysis.getErrors(component));
            componentMessageCollectionPresentationMapForErrors.put(component, messageCollectionPresentationErrors);
            errorsList.getChildren().add(messageCollectionPresentationErrors);

            final Runnable addIfErrors = () -> {
                if (CodeAnalysis.getErrors(component).size() == 0) {
                    errorsList.getChildren().remove(messageCollectionPresentationErrors);
                } else if (!errorsList.getChildren().contains(messageCollectionPresentationErrors)) {
                    errorsList.getChildren().add(messageCollectionPresentationErrors);
                }
            };

            addIfErrors.run();
            CodeAnalysis.getErrors(component).addListener(new ListChangeListener<CodeAnalysis.Message>() {
                @Override
                public void onChanged(final Change<? extends CodeAnalysis.Message> c) {
                    while (c.next()) {
                        addIfErrors.run();
                    }
                }
            });

            final MessageCollectionPresentation messageCollectionPresentationWarnings = new MessageCollectionPresentation(component, CodeAnalysis.getWarnings(component));
            componentMessageCollectionPresentationMapForWarnings.put(component, messageCollectionPresentationWarnings);
            warningsList.getChildren().add(messageCollectionPresentationWarnings);

            final Runnable addIfWarnings = () -> {
                if (CodeAnalysis.getWarnings(component).size() == 0) {
                    warningsList.getChildren().remove(messageCollectionPresentationWarnings);
                } else if (!warningsList.getChildren().contains(messageCollectionPresentationWarnings)) {
                    warningsList.getChildren().add(messageCollectionPresentationWarnings);
                }
            };

            addIfWarnings.run();
            CodeAnalysis.getWarnings(component).addListener(new ListChangeListener<CodeAnalysis.Message>() {
                @Override
                public void onChanged(final Change<? extends CodeAnalysis.Message> c) {
                    while (c.next()) {
                        addIfWarnings.run();
                    }
                }
            });
        };

        // Add error that is project wide but not a backend error
        addComponent.accept(null);

        Ecdar.getProject().getComponents().forEach(addComponent);
        Ecdar.getProject().getComponents().addListener(new ListChangeListener<Component>() {
            @Override
            public void onChanged(final Change<? extends Component> c) {
                while (c.next()) {
                    c.getAddedSubList().forEach(addComponent::accept);

                    c.getRemoved().forEach(component -> {
                        errorsList.getChildren().remove(componentMessageCollectionPresentationMapForErrors.get(component));
                        componentMessageCollectionPresentationMapForErrors.remove(component);

                        warningsList.getChildren().remove(componentMessageCollectionPresentationMapForWarnings.get(component));
                        componentMessageCollectionPresentationMapForWarnings.remove(component);
                    });
                }
            }
        });

        final Map<CodeAnalysis.Message, MessagePresentation> messageMessagePresentationHashMap = new HashMap<>();

        CodeAnalysis.getBackendErrors().addListener(new ListChangeListener<CodeAnalysis.Message>() {
            @Override
            public void onChanged(final Change<? extends CodeAnalysis.Message> c) {
                while (c.next()) {
                    c.getAddedSubList().forEach(addedMessage -> {
                        final MessagePresentation messagePresentation = new MessagePresentation(addedMessage);
                        backendErrorsList.getChildren().add(messagePresentation);
                        messageMessagePresentationHashMap.put(addedMessage, messagePresentation);
                    });

                    c.getRemoved().forEach(removedMessage -> {
                        backendErrorsList.getChildren().remove(messageMessagePresentationHashMap.get(removedMessage));
                        messageMessagePresentationHashMap.remove(removedMessage);
                    });
                }
            }
        });
    }

    private void initializeTabPane() {
        bottomFillerElement.heightProperty().bind(tabPaneContainer.maxHeightProperty());

        tabPane.getSelectionModel().selectedIndexProperty().addListener((obs, oldSelected, newSelected) -> {
            if (newSelected.intValue() < 0 || tabPaneContainer.getMaxHeight() > 35) return;

            if (shouldISkipOpeningTheMessagesContainer) {
                tabPane.getSelectionModel().clearSelection();
                shouldISkipOpeningTheMessagesContainer = false;
            } else {
                expandMessagesIfNotExpanded();
            }
        });

        tabPane.getSelectionModel().clearSelection();

        tabPane.setTabMinHeight(35);
        tabPane.setTabMaxHeight(35);
    }

    @FXML
    private void tabPaneResizeElementPressed(final MouseEvent event) {
        tabPanePreviousY = event.getScreenY();
    }

    @FXML
    private void tabPaneResizeElementDragged(final MouseEvent event) {
        final double mouseY = event.getScreenY();
        double newHeight = tabPaneContainer.getMaxHeight() - (mouseY - tabPanePreviousY);
        newHeight = Math.max(35, newHeight);

        setMaxHeight(newHeight);
        tabPanePreviousY = mouseY;

    }

    public void expandMessagesIfNotExpanded() {
        if (tabPaneContainer.getMaxHeight() <= 35) {
            expandMessagesContainer.play();
        }
    }

    public void collapseMessagesIfNotCollapsed() {
        final Transition collapse = new Transition() {
            double height = tabPaneContainer.getMaxHeight();

            {
                setInterpolator(Interpolator.SPLINE(0.645, 0.045, 0.355, 1));
                setCycleDuration(Duration.millis(200));
            }

            @Override
            protected void interpolate(final double frac) {
                setMaxHeight(((height - 35) * (1 - frac)) + 35);
            }
        };

        if (tabPaneContainer.getMaxHeight() > 35) {
            expandHeight = tabPaneContainer.getHeight();
            collapse.play();
        }
    }

    @FXML
    public void collapseMessagesClicked() {
        final Transition collapse = new Transition() {
            double height = tabPaneContainer.getMaxHeight();

            {
                setInterpolator(Interpolator.SPLINE(0.645, 0.045, 0.355, 1));
                setCycleDuration(Duration.millis(200));
            }

            @Override
            protected void interpolate(final double frac) {
                setMaxHeight(((height - 35) * (1 - frac)) + 35);
            }
        };

        if (tabPaneContainer.getMaxHeight() > 35) {
            expandHeight = tabPaneContainer.getHeight();
            collapse.play();
        } else {
            expandMessagesContainer.play();
        }
    }

    /**
     * This method is used as a central place to decide whether the tabPane is opened or closed
     * @param height the value used to set the height of the tabPane
     */
    public void setMaxHeight(double height)
    {
        tabPaneContainer.setMaxHeight(height);
        if(height > 35) { //The tabpane is opened
            filePane.showBottomInset(false);
            queryPane.showBottomInset(false);
        } else {
            // When closed we push up the scrollviews in the filePane and queryPane as the tabPane
            // would otherwise cover some items in these views
            filePane.showBottomInset(true);
            queryPane.showBottomInset(true);
        }
    }

    @FXML
    private void generateUppaalModelClicked() {
        final Component mainComponent = Ecdar.getProject().getMainComponent();

        if (mainComponent == null) {
            System.out.println("No main component");
            return; // We cannot generate a UPPAAL file without a main component
        }

        try {
            UPPAALDriver.generateDebugUPPAALModel();
            Ecdar.showToast("UPPAAL model stored");
        } catch (final Exception e) {
            Ecdar.showToast("Could not store UPPAAL debug model due to an error");
            e.printStackTrace();
        }
    }

    private void nudgeSelected(final NudgeDirection direction) {
        final List<SelectHelper.ItemSelectable> selectedElements = SelectHelper.getSelectedElements();

        final List<Nudgeable> nudgedElements = new ArrayList<>();

        UndoRedoStack.push(() -> { // Perform

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
                final Component component = ((LocationController) selectable).getComponent();
                final Location location = ((LocationController) selectable).getLocation();

                final Location initialLocation = component.getInitialLocation();
                final Location finalLocation = component.getFinalLocation();

                if (location.getId().equals(initialLocation.getId()) || location.getId().equals(finalLocation.getId())) {
                    ((LocationPresentation) ((LocationController) selectable).root).shake();
                    return; // Do not delete initial or final locations
                }

                final List<Edge> relatedEdges = component.getRelatedEdges(location);

                UndoRedoStack.push(() -> { // Perform
                    // Remove the location
                    component.getLocations().remove(location);
                    relatedEdges.forEach(component::removeEdge);
                }, () -> { // Undo
                    // Re-all the location
                    component.getLocations().add(location);
                    relatedEdges.forEach(component::addEdge);

                }, String.format("Deleted %s", selectable.toString()), "delete");
            } else if (selectable instanceof EdgeController) {
                final Component component = ((EdgeController) selectable).getComponent();
                final Edge edge = ((EdgeController) selectable).getEdge();

                UndoRedoStack.push(() -> { // Perform
                    // Remove the edge
                    component.removeEdge(edge);
                }, () -> { // Undo
                    // Re-all the edge
                    component.addEdge(edge);
                }, String.format("Deleted %s", selectable.toString()), "delete");
            } else if (selectable instanceof JorkController) {
                final Component component = CanvasController.getActiveComponent();
                final Jork jork = ((JorkController) selectable).getJork();

                final List<Edge> relatedEdges = component.getRelatedEdges(jork);

                UndoRedoStack.push(() -> { // Perform
                    // Remove the jork
                    component.getJorks().remove(jork);
                    relatedEdges.forEach(component::removeEdge);
                }, () -> { // Undo
                    // Re-all the jork
                    component.getJorks().add(jork);
                    relatedEdges.forEach(component::addEdge);
                }, String.format("Deleted %s", selectable.toString()), "delete");
            } else if (selectable instanceof SubComponentController) {
                final Component component = CanvasController.getActiveComponent();
                final SubComponent subComponent = ((SubComponentController) selectable).getSubComponent();


                final List<Edge> relatedEdges = component.getRelatedEdges(subComponent);

                UndoRedoStack.push(() -> { // Perform
                    // Remove the subComponent
                    component.getSubComponents().remove(subComponent);
                    relatedEdges.forEach(component::removeEdge);
                }, () -> { // Undo
                    // Re-all the subComponent
                    component.getSubComponents().add(subComponent);
                    relatedEdges.forEach(component::addEdge);
                }, String.format("Deleted %s", selectable.toString()), "delete");
            } else if (selectable instanceof NailController) {
                final NailController nailController = (NailController) selectable;
                final Edge edge = nailController.getEdge();
                final Component component = nailController.getComponent();
                final Nail nail = nailController.getNail();
                final int index = edge.getNails().indexOf(nail);

                final String restoreProperty = edge.getProperty(nail.getPropertyType());

                // If the last nail on a self loop for a location or join/fork delete the edge also
                final boolean shouldDeleteEdgeAlso = edge.isSelfLoop() && edge.getNails().size() == 1 && edge.getSourceSubComponent() == null;

                // Create an undo redo description based, add extra comment if edge is also deleted
                String message =  String.format("Deleted %s", selectable.toString());
                if(shouldDeleteEdgeAlso) {
                    message += String.format("(Was last Nail on self loop edge --> %s also deleted)", edge.toString());
                }

                UndoRedoStack.push(
                        () -> {
                            edge.removeNail(nail);
                            edge.setProperty(nail.getPropertyType(), "");
                            if(shouldDeleteEdgeAlso) {
                                component.removeEdge(edge);
                            }
                        },
                        () -> {
                            if(shouldDeleteEdgeAlso) {
                                component.addEdge(edge);
                            }
                            edge.setProperty(nail.getPropertyType(), restoreProperty);
                            edge.insertNailAt(nail, index);
                        },
                        message,
                        "delete"
                );
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

        switchToInput.setManaged(false);
        switchToInput.setVisible(false);
        switchToOutput.setManaged(true);
        switchToOutput.setVisible(true);
    }

    /**
     * Switch to output edge mode
     */
    @FXML
    private void switchToOutputClicked() {
        setGlobalEdgeStatus(EdgeStatus.OUTPUT);

        switchToInput.setManaged(true);
        switchToInput.setVisible(true);
        switchToOutput.setManaged(false);
        switchToOutput.setVisible(false);
    }

    /**
     * Sets the global edge status.
     * @param status the status
     */
    private void setGlobalEdgeStatus(EdgeStatus status) {
        globalEdgeStatus = status;
    }

    @FXML
    private void closeDialog() {
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
