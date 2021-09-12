package ecdar.controllers;

import com.jfoenix.controls.*;
import ecdar.Debug;
import ecdar.Ecdar;
import ecdar.abstractions.Component;
import ecdar.abstractions.EcdarSystem;
import ecdar.abstractions.HighLevelModelObject;
import ecdar.abstractions.Query;
import ecdar.code_analysis.CodeAnalysis;
import ecdar.mutation.models.MutationTestPlan;
import ecdar.presentations.*;
import ecdar.simulation.EcdarSimulationController;
import ecdar.simulation.EcdarSimulatorOverviewController;
import ecdar.utility.UndoRedoStack;
import ecdar.utility.colors.Color;
import ecdar.utility.keyboard.Keybind;
import ecdar.utility.keyboard.KeyboardTracker;
import javafx.animation.Interpolator;
import javafx.animation.Transition;
import javafx.application.Platform;
import javafx.beans.binding.When;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ListChangeListener;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.Consumer;

/**
 * This is the controller for the view that contains the views of the top menu bar, the bottom messages tabpane,
 * the help dialog, the snackbar, the bottom statusbar and the side navigation.
 * In the center of this view is an EcdarPresentation.
 */
public class MainController implements Initializable {

    // View stuff
    public StackPane root;
    public EcdarPresentation ecdarPresentation = new EcdarPresentation();
    public SimulatorPresentation simulatorPresentation = new SimulatorPresentation();
    public StackPane toolbar;
    public StackPane dialogContainer;
    public JFXDialog dialog;
    public StackPane modalBar;
    public JFXTextField queryTextField;
    public JFXTextField commentTextField;
    public JFXRippler undo;
    public JFXRippler redo;
    public JFXTabPane tabPane;
    public Tab errorsTab;
    public Tab warningsTab;
    public Rectangle tabPaneResizeElement;
    public StackPane tabPaneContainer;

    public ImageView helpInitialImage;
    public StackPane helpInitialPane;
    public ImageView helpUrgentImage;
    public StackPane helpUrgentPane;
    public ImageView helpInputImage;
    public StackPane helpInputPane;
    public ImageView helpOutputImage;
    public StackPane helpOutputPane;

    // The program top menu
    public MenuBar menuBar;
    public MenuItem menuBarViewFilePanel;
    public MenuItem menuBarViewQueryPanel;
    public MenuItem menuBarViewGrid;
    public MenuItem menuBarViewCanvasSplit;
    public MenuItem menuBarViewSimLeftPanel;
    public MenuItem menuBarViewSimRightPanel;
    public MenuItem menuBarFileSave;
    public MenuItem menuBarFileSaveAs;
    public MenuItem menuBarFileCreateNewProject;
    public MenuItem menuBarFileOpenProject;
    public MenuItem menuBarFileExportAsPng;
    public MenuItem menuBarFileExportAsPngNoBorder;
    public MenuItem menuBarZoomInSimulator;
    public MenuItem menuBarZoomOutSimulator;
    public MenuItem menuBarZoomResetSimulator;
    public MenuItem menuBarOptionsCache;
    public MenuItem menuBarHelpHelp;

    public StackPane queryDialogContainer;
    public JFXDialog queryDialog;
    public Text queryTextResult;
    public Text queryTextQuery;
    private static JFXDialog _queryDialog;
    private static Text _queryTextResult;
    private static Text _queryTextQuery;

    public StackPane reloadDialogContainer;
    public JFXDialog reloadSimulatorDialog;
    private static JFXDialog _reloadSimulatorDialog;

    public JFXRippler collapseMessages;
    public FontIcon collapseMessagesIcon;
    public ScrollPane errorsScrollPane;
    public VBox errorsList;
    public ScrollPane warningsScrollPane;
    public VBox warningsList;
    public Tab backendErrorsTab;
    public ScrollPane backendErrorsScrollPane;
    public VBox backendErrorsList;

    public JFXSnackbar snackbar;

    public HBox statusBar;
    public Label statusLabel;
    public Label queryLabel;
    public HBox queryStatusContainer;

    public BorderPane borderPane;

    public HBox centerContainer;
    public MenuItem menuBarViewHome;
    public MenuItem menuBarViewSim;
    public MenuItem menuBarFileNewMutationTestObject;
    public MenuItem menuEditMoveLeft;
    public MenuItem menuEditMoveUp;
    public MenuItem menuEditMoveRight;
    public MenuItem menuEditMoveDown;
    public MenuItem menuBarHelpTest;
    public MenuItem menuBarHelpAbout;
    public StackPane aboutContainer;
    public JFXDialog aboutDialog;
    public JFXButton aboutAcceptButton;
    public JFXButton testHelpAcceptButton;
    public JFXDialog testHelpDialog;
    public StackPane testHelpContainer;

    private double tabPanePreviousY = 0;
    public boolean shouldISkipOpeningTheMessagesContainer = true;
    private static final ObjectProperty<CanvasPresentation> activeCanvasPresentation = new SimpleObjectProperty<>(new CanvasPresentation());

    private double expandHeight = 300;

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

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initializeMode();
        initializeMenuBar();
        initializeTabPane();
        initializeMessages();
        initializeStatusBar();
        initializeQueryDialog();
        initializeReloadDialog();

        simulatorPresentation.getController().root.prefWidthProperty().bind(centerContainer.widthProperty());

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
    }

    /**
     * Sets the view for the initial mode of the application.
     * Also initializes keyboardbindings for changing mode
     */
    private void initializeMode() {
        currentMode.addListener((obs, oldMode, newMode) -> {
            if(newMode == Mode.Editor && oldMode != newMode) {
                enterEditorMode();
            } else if(newMode == Mode.Simulator && oldMode != newMode) {
                enterSimulatorMode();
            }
        });

        if(currentMode.get() == Mode.Editor) {
            enterEditorMode();
        } else if(currentMode.get() == Mode.Simulator) {
            enterSimulatorMode();
        }
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

        initializeUICacheMenuElement();

        initializeHelpMenu();

        //TODO change to match on macOS
        menuBarZoomInSimulator.setAccelerator(new KeyCodeCombination(KeyCode.PLUS, KeyCombination.SHORTCUT_DOWN));
        menuBarZoomInSimulator.setOnAction(event -> simulatorPresentation.getController().overviewPresentation.getController().zoomIn());

        //TODO change to match on macOS
        menuBarZoomOutSimulator.setAccelerator(new KeyCodeCombination(KeyCode.MINUS, KeyCombination.SHORTCUT_DOWN));
        menuBarZoomOutSimulator.setOnAction(event -> simulatorPresentation.getController().overviewPresentation.getController().zoomOut());

        menuBarZoomResetSimulator.setAccelerator(new KeyCodeCombination(KeyCode.DIGIT0, KeyCombination.SHORTCUT_DOWN));
        menuBarZoomResetSimulator.setOnAction(event -> simulatorPresentation.getController().overviewPresentation.getController().resetZoom());
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
    private void initializeUICacheMenuElement() {
        menuBarOptionsCache.setOnAction(event -> {
            final BooleanProperty isCached = Ecdar.toggleUICache();
            menuBarOptionsCache.getGraphic().opacityProperty().bind(new When(isCached).then(1).otherwise(0));
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

        menuBarViewHome.setAccelerator(new KeyCodeCombination(KeyCode.DIGIT1, KeyCombination.SHORTCUT_DOWN));
        menuBarViewHome.setOnAction(event -> editorRipplerClicked());

        menuBarViewSim.setAccelerator(new KeyCodeCombination(KeyCode.DIGIT2, KeyCombination.SHORTCUT_DOWN));
        menuBarViewSim.setOnAction(event -> simulatorRipplerClicked());

        menuBarViewSimLeftPanel.getGraphic().setOpacity(1);
        menuBarViewSimLeftPanel.setAccelerator(new KeyCodeCombination(KeyCode.L, KeyCodeCombination.SHORTCUT_DOWN));
        menuBarViewSimLeftPanel.setOnAction(event -> {
            final BooleanProperty isOpen = Ecdar.toggleLeftSimPane();
            menuBarViewSimLeftPanel.getGraphic().opacityProperty().bind(new When(isOpen).then(1).otherwise(0));
        });

        menuBarViewSimRightPanel.getGraphic().setOpacity(1);
        menuBarViewSimRightPanel.setAccelerator(new KeyCodeCombination(KeyCode.R, KeyCodeCombination.SHORTCUT_DOWN));
        menuBarViewSimRightPanel.setOnAction(event -> {
            final BooleanProperty isOpen = Ecdar.toggleRightSimPane();
            menuBarViewSimRightPanel.getGraphic().opacityProperty().bind(new When(isOpen).then(1).otherwise(0));
        });

        menuBarViewCanvasSplit.getGraphic().setOpacity(1);
        menuBarViewCanvasSplit.setOnAction(event -> {
            final BooleanProperty isSplit = Ecdar.toggleCanvasSplit();
            if(isSplit.get()) {
                ecdarPresentation.getController().setCanvasSplit(false);
                menuBarViewCanvasSplit.setText("Split canvas");
            } else {
                ecdarPresentation.getController().setCanvasSplit(true);
                menuBarViewCanvasSplit.setText("Merge canvases");
            }
        });
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
                    UndoRedoStack.clear();
                } catch (final IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * Initializes the query dialog that pops up to show the result of executing a query
     */
    private void initializeQueryDialog() {
        dialog.setDialogContainer(dialogContainer);
        dialogContainer.opacityProperty().bind(dialog.getChildren().get(0).scaleXProperty());
        dialog.setOnDialogClosed(event -> dialogContainer.setVisible(false));

        _queryDialog = queryDialog;
        _queryTextResult = queryTextResult;
        _queryTextQuery = queryTextQuery;

        setDialogAndContainer(queryDialog, queryDialogContainer);
    }

    /**
     * Initializes the dialog that is shown when the user is about to reload a system in the simulator
     */
    private void initializeReloadDialog() {
        // Assign the dialog to the static _reloadSimulatorDialog property
        // so it can be referenced in the static openReloadSimulationDialog() method
        _reloadSimulatorDialog = reloadSimulatorDialog;

        setDialogAndContainer(reloadSimulatorDialog, reloadDialogContainer);
    }

    /**
     * Helper method to set the container on a {@link JFXDialog} and set visibility when shown or hidden
     * @param dialog The dialog to show
     * @param container The container of the dialog
     */
    private void setDialogAndContainer(JFXDialog dialog, StackPane container) {
        dialog.setDialogContainer(container);

        container.opacityProperty().bind(dialog.getChildren().get(0).scaleXProperty());

        dialog.setOnDialogClosed(event -> {
            container.setVisible(false);
            container.setMouseTransparent(true);
        });

        dialog.setOnDialogOpened(event -> {
            container.setVisible(true);
            container.setMouseTransparent(false);
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
     * Save project at a given directory.
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

            cropAndExportImage(image);
        });

        menuBarFileExportAsPngNoBorder.setOnAction(event -> {
            CanvasPresentation canvas = getActiveCanvasPresentation();

            final ComponentPresentation presentation = canvas.getController().getActiveComponentPresentation();

            //If there is no active component
            if (presentation == null){
                Ecdar.showToast("No component to export.");
                return;
            }

            presentation.getController().hideBorderAndBackground();
            final WritableImage image = takeSnapshot(canvas);
            presentation.getController().showBorderAndBackground();

            cropAndExportImage(image);
        });
    }

    /**
     * Take a snapshot with the grid hidden.
     * The grid is put into its original state afterwards.
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
     * @param image the image
     */
    private void cropAndExportImage(final WritableImage image) {
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
        if (file != null){
            try {
                ImageIO.write(finalImage, "png", file);
                Ecdar.showToast("Export succeeded.");
            } catch (final IOException e) {
                Ecdar.showToast("Export failed. "+ e.getMessage());
            }
        } else {
            Ecdar.showToast("Export was cancelled.");
        }
    }

    /**
     * Crops an image so that the all-white borders are removed.
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

    @FXML
    private void closeDialog() {
        dialog.close();
    }

    public static CanvasPresentation getActiveCanvasPresentation() {
        return activeCanvasPresentation.get();
    }

    public static void setActiveCanvasPresentation(CanvasPresentation newActiveCanvasPresentation) {
        activeCanvasPresentation.set(newActiveCanvasPresentation);
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
                    c.getAddedSubList().forEach(addComponent);

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

        EcdarController ecdarController = ecdarPresentation.getController();
        if(ecdarController != null) {
            ecdarController.bottomFillerElement.heightProperty().bind(tabPaneContainer.maxHeightProperty());
        }
        EcdarSimulationController simController = simulatorPresentation.getController();
        if(simController != null) {
            simController.bottomFillerElement.heightProperty().bind(tabPaneContainer.maxHeightProperty());
        }
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

    /**
     * Changes the view and mode to the editor
     * Only enter if the mode is not already Editor
     */
    private void enterEditorMode() {
        if(centerContainer.getChildren().contains(ecdarPresentation))
            return;

        ecdarPresentation.getController().willShow();
        simulatorPresentation.getController().willHide();

        centerContainer.getChildren().remove(simulatorPresentation);
        centerContainer.getChildren().add(ecdarPresentation);

        // Enable or disable the menu items that can be used when in the simulator
        updateMenuItems();
    }

    /**
     * Changes the view and mode to the simulator
     * Only enter if the mode is not already Simulator
     */
    private void enterSimulatorMode() {
        if(centerContainer.getChildren().contains(simulatorPresentation))
            return;

        currentMode.setValue(Mode.Simulator);

        ecdarPresentation.getController().willHide();
        simulatorPresentation.getController().willShow();

        centerContainer.getChildren().remove(ecdarPresentation);
        centerContainer.getChildren().add(simulatorPresentation);

        // Enable or disable the menu items that can be used when in the simulator
        updateMenuItems();
    }

    /**
     * Method for click on the Editor rippler. Changes mode to the editor
     */
    private void editorRipplerClicked() {
        if(currentMode.get() != Mode.Editor) {
            currentMode.setValue(Mode.Editor);
        }
    }

    /**
     * Method for click on the Simulator rippler. Changes mode to the simulator
     */
    private void simulatorRipplerClicked() {
        if(currentMode.get() != Mode.Simulator) {
            currentMode.setValue(Mode.Simulator);
        }
    }

    /**
     * Update the menu items to match the current mode
     */
    private void updateMenuItems() {
        switch (currentMode.get()) {
            case Editor:
                menuBarViewGrid.setDisable(false);
                menuBarViewFilePanel.setDisable(false);
                menuBarViewQueryPanel.setDisable(false);
                menuBarViewSimLeftPanel.setDisable(true);
                menuBarViewSimRightPanel.setDisable(true);
                menuBarFileExportAsPngNoBorder.setDisable(false);
                menuBarFileExportAsPng.setDisable(false);
                menuBarZoomInSimulator.setDisable(true);
                menuBarZoomOutSimulator.setDisable(true);
                menuBarZoomResetSimulator.setDisable(true);
                KeyboardTracker.unregisterKeybind(KeyboardTracker.ZOOM_IN);
                KeyboardTracker.unregisterKeybind(KeyboardTracker.ZOOM_OUT);
                KeyboardTracker.unregisterKeybind(KeyboardTracker.RESET_ZOOM);
                break;
            case Simulator:
                menuBarViewGrid.setDisable(true);
                menuBarViewFilePanel.setDisable(true);
                menuBarViewSimLeftPanel.setDisable(false);
                menuBarViewSimRightPanel.setDisable(false);
                menuBarFileExportAsPngNoBorder.setDisable(true);
                menuBarFileExportAsPng.setDisable(true);
                menuBarViewQueryPanel.setDisable(true);
                menuBarZoomInSimulator.setDisable(false);
                menuBarZoomOutSimulator.setDisable(false);
                menuBarZoomResetSimulator.setDisable(false);
                setExtraZoomKeybindings();
                break;
        }

    }

    /**
     * Sets keybindings for zooming in the {@link EcdarSimulatorOverviewController} using the numpad
     */
    private void setExtraZoomKeybindings() {
        // Numpad plus +
        final KeyCodeCombination zoomInCombination2 = new KeyCodeCombination(KeyCode.ADD, KeyCombination.SHORTCUT_DOWN);
        KeyboardTracker.registerKeybind(KeyboardTracker.ZOOM_IN,new Keybind(zoomInCombination2, (keyEvent) -> {
            keyEvent.consume();
            simulatorPresentation.getController().overviewPresentation.getController().zoomIn();
        }));

        // numpad minus
        final KeyCodeCombination zoomOutCombination2 = new KeyCodeCombination(KeyCode.SUBTRACT, KeyCombination.SHORTCUT_DOWN);
        KeyboardTracker.registerKeybind(KeyboardTracker.ZOOM_OUT, new Keybind(zoomOutCombination2, (keyEvent) -> {
            keyEvent.consume();
            simulatorPresentation.getController().overviewPresentation.getController().zoomOut();
        }));

        //numpad 0
        final KeyCodeCombination zoomResetCombination = new KeyCodeCombination(KeyCode.NUMPAD0, KeyCombination.SHORTCUT_DOWN);
        KeyboardTracker.registerKeybind(KeyboardTracker.RESET_ZOOM, new Keybind(zoomResetCombination, (keyEvent) -> {
            keyEvent.consume();
            simulatorPresentation.getController().overviewPresentation.getController().resetZoom();
        }));
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
    public void setMaxHeight(double height) {
        tabPaneContainer.setMaxHeight(height);
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

    @FXML
    private void closeQueryDialog() {
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

    /**
     * Used by the cancel button in the reload dialog.
     * Closes the dialog when called.
     */
    @FXML
    private void cancelReloadSimulatorDialog() {
        reloadSimulatorDialog.close();
    }

    /**
     * Used by the OK button in the reload dialog.
     * Reloads the system of the simulator.
     */
    @FXML
    private void okReloadSimulatorDialog() {
        simulatorPresentation.getController().resetCurrentSimulation();
        reloadSimulatorDialog.close();
    }

    public static void openReloadSimulationDialog() {
        _reloadSimulatorDialog.show();
    }

    /**
     * Changes the mode to simulator, causing the view to change to simulator mode
     */
    public static void showSimulator() {
        currentMode.setValue(Mode.Simulator);
    }
}
