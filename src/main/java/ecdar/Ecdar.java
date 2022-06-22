package ecdar;

import ecdar.abstractions.Component;
import ecdar.abstractions.Project;
import ecdar.backend.BackendDriver;
import ecdar.backend.BackendHelper;
import ecdar.code_analysis.CodeAnalysis;
import ecdar.controllers.EcdarController;
import ecdar.presentations.BackgroundThreadPresentation;
import ecdar.presentations.EcdarPresentation;
import ecdar.presentations.UndoRedoHistoryPresentation;
import ecdar.utility.keyboard.Keybind;
import ecdar.utility.keyboard.KeyboardTracker;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ListChangeListener;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.text.Font;
import javafx.stage.Screen;
import javafx.stage.Stage;
import jiconfont.icons.google_material_design_icons.GoogleMaterialDesignIcons;
import jiconfont.javafx.IconFontFX;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.prefs.Preferences;

import java.net.URISyntaxException;
import java.security.CodeSource;

public class Ecdar extends Application {
    public static Preferences preferences = Preferences.userRoot().node("ECDAR");
    public static BooleanProperty autoScalingEnabled = new SimpleBooleanProperty(false);
    public static final String VERSION = "2.3.0-beta";
    public static boolean serializationDone = false;
    public static SimpleStringProperty projectDirectory = new SimpleStringProperty();

    private static double dpi;
    private static Project project;
    private static EcdarPresentation presentation;
    private static BooleanProperty isUICached = new SimpleBooleanProperty();
    private static final BooleanProperty isSplit = new SimpleBooleanProperty(true); //Set to true to ensure correct behaviour at first toggle.
    private static final BackendDriver backendDriver = new BackendDriver();
    private Stage debugStage;

    /**
     * Gets the absolute path to the server folder
     * @return
     */
    public static String getServerPath() {
        try {
            return getRootDirectory() + File.separator + "servers";
        } catch (final URISyntaxException e) {
            showToast("Could not fetch root directory.");
            e.printStackTrace();
            throw new RuntimeException("Could not create project directory", e);
        }
    }

    /**
     * Gets the path to the root directory.
     * @return the path to the root directory
     * @throws URISyntaxException if the source code location could not be converted to an URI
     */
    public static String getRootDirectory() throws URISyntaxException {
        final CodeSource codeSource = Ecdar.class.getProtectionDomain().getCodeSource();
        final File jarFile = new File(codeSource.getLocation().toURI().getPath());
        return jarFile.getParentFile().getPath();
    }

    /**
     * Suppresses warnings of illegal reflective access.
     * jFoenix performs illegal reflection inorder to access private methods/fields in the JavaFX code.
     * This comes with some performance benefits and is a technique used, allegedly, in many libraries.
     * The warnings are unlikely to be solved or enforced in such a way that the system breaks, see:
     * <a href="https://github.com/sshahine/JFoenix/issues/1170">https://github.com/sshahine/JFoenix/issues/1170</a>
     * The solution is taken from this answer: <a href="https://stackoverflow.com/a/46551505">https://stackoverflow.com/a/46551505</a>
     * All credit for this method goes to: Rafael Winterhalter
     */
    @SuppressWarnings("unchecked")
    public static void disableAccessWarnings() {
        try {
            Class unsafeClass = Class.forName("sun.misc.Unsafe");
            Field field = unsafeClass.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            Object unsafe = field.get(null);

            Method putObjectVolatile = unsafeClass.getDeclaredMethod("putObjectVolatile", Object.class, long.class, Object.class);
            Method staticFieldOffset = unsafeClass.getDeclaredMethod("staticFieldOffset", Field.class);

            Class loggerClass = Class.forName("jdk.internal.module.IllegalAccessLogger");
            Field loggerField = loggerClass.getDeclaredField("logger");
            Long offset = (Long) staticFieldOffset.invoke(unsafe, loggerField);
            putObjectVolatile.invoke(unsafe, loggerClass, offset, null);
        } catch (Exception ignored) {
        }
    }

    public static void main(final String[] args) {
        disableAccessWarnings();
        launch(Ecdar.class, args);
    }

    /**
     * Gets the project.
     * @return the project
     */
    public static Project getProject() {
        return project;
    }

    public static EcdarPresentation getPresentation() {
        return presentation;
    }

    public static void showToast(final String message) {
        Platform.runLater(() -> {
            presentation.showSnackbarMessage(message);
        });
    }

    public static void showHelp() {
        presentation.showHelp();
    }

    public static BooleanProperty toggleFilePane() {
        return presentation.toggleFilePane();
    }

    /**
     * Toggles whether to cache UI.
     * Caching reduces CPU usage on some devices.
     * It also increases GPU 3D engine usage on some devices.
     * @return the property specifying whether to cache
     */
    public static BooleanProperty toggleUICache() {
        isUICached.set(!isUICached.get());

        return isUICached;
    }

    public static BooleanProperty toggleQueryPane() {
        return presentation.toggleQueryPane();
    }

    /**
     * Calls {@link EcdarPresentation#toggleGrid()}.
     * @return A Boolean Property that is true if the grid has been turned on and false if it is off
     */
    public static BooleanProperty toggleGrid() {
        return presentation.toggleGrid();
    }

    /**
     * Toggles whether to canvas is split or single.
     * @return the property specifying whether the canvas is split
     */
    public static BooleanProperty toggleCanvasSplit() {
        isSplit.set(!isSplit.get());

        return isSplit;
    }

    /**
     * Returns the backend driver used to execute queries and handle simulation
     * @return BackendDriver
     */
    public static BackendDriver getBackendDriver() {
        return backendDriver;
    }

    public static double getDpiScale() {
        if (!autoScalingEnabled.getValue())
            return 1;
        return Math.floor(dpi / 96);
    }

    private void forceCreateFolder(final String directoryPath) throws IOException {
        final File directory = new File(directoryPath);
        FileUtils.forceMkdir(directory);
    }

    @Override
    public void start(final Stage stage) {
        // Load or create new project
        project = new Project();

        // Set the title for the application
        stage.setTitle("Ecdar " + VERSION);

        // Load the fonts required for the project
        IconFontFX.register(GoogleMaterialDesignIcons.getIconFont());
        loadFonts();

        // Remove the classic decoration
        // kyrke - 2020-04-17: Disabled due to bug https://bugs.openjdk.java.net/browse/JDK-8154847
        //stage.initStyle(StageStyle.UNIFIED);

        // Make the view used for the application
        presentation = new EcdarPresentation();

        // Bind presentation to cached property
        isUICached.addListener(((observable, oldValue, newValue) -> presentation.setCache(newValue)));
        isUICached.set(true); // Set to true as default

        // Make the scene that we will use, and set its size to 80% of the primary screen
        final Screen screen = Screen.getPrimary();
        final Scene scene = new Scene(presentation, screen.getVisualBounds().getWidth() * 0.8, screen.getVisualBounds().getHeight() * 0.8);
        stage.setScene(scene);
        dpi = screen.getDpi();

        // Load all .css files used todo: these should be loaded in the view classes (?)
        scene.getStylesheets().add("ecdar/main.css");
        scene.getStylesheets().add("ecdar/colors.css");
        scene.getStylesheets().add("ecdar/model_canvas.css");
        scene.getStylesheets().add("ecdar/query_pane.css");
        scene.getStylesheets().add("ecdar/scroll_pane.css");

        // Handle a mouse click as a deselection of all elements
        scene.setOnMousePressed(event -> {
            if (scene.getFocusOwner() == null || scene.getFocusOwner().getParent() == null) return;
            scene.getFocusOwner().getParent().requestFocus();
        });

        // Let our keyboard tracker handle all key presses
        scene.setOnKeyPressed(KeyboardTracker.handleKeyPress);

        // Set the icon for the application
        try {
            stage.getIcons().add(new Image(Objects.requireNonNull(getClass().getResource("ic_launcher/Ecdar_logo.png")).toExternalForm()));
        } catch (NullPointerException e) {
            e.printStackTrace();
            Ecdar.showToast("The application icon could not be loaded");
        }

        // We're now ready! Let the curtains fall!
        stage.show();

        project.reset();

        // Set active model
        Platform.runLater(() -> EcdarController.setActiveModelForActiveCanvas(Ecdar.getProject().getComponents().get(0)));

        EcdarController.reachabilityServiceEnabled = true;

        // Register a key-bind for showing debug-information
        KeyboardTracker.registerKeybind("DEBUG", new Keybind(new KeyCodeCombination(KeyCode.F12), () -> {
            // Toggle the debug mode for the debug class (will update misc. debug variables which presentations bind to)
            Debug.debugModeEnabled.set(!Debug.debugModeEnabled.get());

            if (debugStage != null) {
                debugStage.close();
                debugStage = null;
                return;
            }

            final UndoRedoHistoryPresentation undoRedoHistoryPresentation = new UndoRedoHistoryPresentation();
            undoRedoHistoryPresentation.setMinWidth(100);

            final BackgroundThreadPresentation backgroundThreadPresentation = new BackgroundThreadPresentation();
            backgroundThreadPresentation.setMinWidth(100);

            final HBox root = new HBox(undoRedoHistoryPresentation, backgroundThreadPresentation);
            root.setStyle("-fx-background-color: brown;");
            HBox.setHgrow(undoRedoHistoryPresentation, Priority.ALWAYS);
            HBox.setHgrow(backgroundThreadPresentation, Priority.ALWAYS);


            debugStage = new Stage();
            debugStage.setScene(new Scene(root));

            debugStage.getScene().getStylesheets().add("ecdar/main.css");
            debugStage.getScene().getStylesheets().add("ecdar/colors.css");

            debugStage.setWidth(screen.getVisualBounds().getWidth() * 0.2);
            debugStage.setHeight(screen.getVisualBounds().getWidth() * 0.3);

            debugStage.show();
            //stage.requestFocus();
        }));

        stage.setOnCloseRequest(event -> {
            BackendHelper.stopQueries();

            try {
                backendDriver.closeAllBackendConnections();
            } catch (IOException e) {
                e.printStackTrace();
            }

            Platform.exit();
            System.exit(0);
        });
    }

    /**
     * Initializes and resets the project.
     * This can be used as a test setup.
     */
    public static void setUpForTest() {
        project = new Project();
        project.reset();

        // This implicitly starts the fx-application thread
        // It prevents java.lang.RuntimeException: Internal graphics not initialized yet
        // https://stackoverflow.com/questions/27839441/internal-graphics-not-initialized-yet-javafx
        // new JFXPanel();
    }

    public static void initializeProjectFolder() throws IOException {
        // Make sure that the project directory exists
        final File directory = new File(projectDirectory.get());
        FileUtils.forceMkdir(directory);

        CodeAnalysis.getErrors().addListener(new ListChangeListener<CodeAnalysis.Message>() {
            @Override
            public void onChanged(Change<? extends CodeAnalysis.Message> c) {
                CodeAnalysis.getErrors().forEach(message -> {
                    System.out.println(message.getMessage());
                });
            }
        });
        CodeAnalysis.clearErrorsAndWarnings();
        CodeAnalysis.disable();
        getProject().clean();

        // Deserialize the project
        Ecdar.getProject().deserialize(directory);
        CodeAnalysis.enable();

        // Generate all component presentations by making them the active component in the view one by one
        Component initialShownComponent = null;
        for (final Component component : Ecdar.getProject().getComponents()) {
            // The first component should be shown
            if (initialShownComponent == null) {
                initialShownComponent = component;
            }
            EcdarController.setActiveModelForActiveCanvas(component);
        }

        // If we found a component set that as active
        if (initialShownComponent != null) {
            EcdarController.setActiveModelForActiveCanvas(initialShownComponent);
        }
        serializationDone = true;
    }

    private void loadFonts() {
        Font.loadFont(getClass().getResourceAsStream("fonts/roboto/Roboto-Black.ttf"), 14);
        Font.loadFont(getClass().getResourceAsStream("fonts/roboto/Roboto-BlackItalic.ttf"), 14);
        Font.loadFont(getClass().getResourceAsStream("fonts/roboto/Roboto-Bold.ttf"), 14);
        Font.loadFont(getClass().getResourceAsStream("fonts/roboto/Roboto-BoldItalic.ttf"), 14);
        Font.loadFont(getClass().getResourceAsStream("fonts/roboto/RobotoCondensed-Bold.ttf"), 14);
        Font.loadFont(getClass().getResourceAsStream("fonts/roboto/RobotoCondensed-BoldItalic.ttf"), 14);
        Font.loadFont(getClass().getResourceAsStream("fonts/roboto/RobotoCondensed-Italic.ttf"), 14);
        Font.loadFont(getClass().getResourceAsStream("fonts/roboto/RobotoCondensed-Light.ttf"), 14);
        Font.loadFont(getClass().getResourceAsStream("fonts/roboto/RobotoCondensed-LightItalic.ttf"), 14);
        Font.loadFont(getClass().getResourceAsStream("fonts/roboto/RobotoCondensed-Regular.ttf"), 14);
        Font.loadFont(getClass().getResourceAsStream("fonts/roboto/Roboto-Italic.ttf"), 14);
        Font.loadFont(getClass().getResourceAsStream("fonts/roboto/Roboto-Light.ttf"), 14);
        Font.loadFont(getClass().getResourceAsStream("fonts/roboto/Roboto-LightItalic.ttf"), 14);
        Font.loadFont(getClass().getResourceAsStream("fonts/roboto/Roboto-Medium.ttf"), 14);
        Font.loadFont(getClass().getResourceAsStream("fonts/roboto/Roboto-MediumItalic.ttf"), 14);
        Font.loadFont(getClass().getResourceAsStream("fonts/roboto/Roboto-Regular.ttf"), 14);
        Font.loadFont(getClass().getResourceAsStream("fonts/roboto/Roboto-Thin.ttf"), 14);
        Font.loadFont(getClass().getResourceAsStream("fonts/roboto/Roboto-ThinItalic.ttf"), 14);

        Font.loadFont(getClass().getResourceAsStream("fonts/roboto_mono/RobotoMono-Bold.ttf"), 14);
        Font.loadFont(getClass().getResourceAsStream("fonts/roboto_mono/RobotoMono-BoldItalic.ttf"), 14);
        Font.loadFont(getClass().getResourceAsStream("fonts/roboto_mono/RobotoMono-Italic.ttf"), 14);
        Font.loadFont(getClass().getResourceAsStream("fonts/roboto_mono/RobotoMono-Light.ttf"), 14);
        Font.loadFont(getClass().getResourceAsStream("fonts/roboto_mono/RobotoMono-LightItalic.ttf"), 14);
        Font.loadFont(getClass().getResourceAsStream("fonts/roboto_mono/RobotoMono-Medium.ttf"), 14);
        Font.loadFont(getClass().getResourceAsStream("fonts/roboto_mono/RobotoMono-MediumItalic.ttf"), 14);
        Font.loadFont(getClass().getResourceAsStream("fonts/roboto_mono/RobotoMono-Regular.ttf"), 14);
        Font.loadFont(getClass().getResourceAsStream("fonts/roboto_mono/RobotoMono-Thin.ttf"), 14);
        Font.loadFont(getClass().getResourceAsStream("fonts/roboto_mono/RobotoMono-ThinItalic.ttf"), 14);
    }
}
