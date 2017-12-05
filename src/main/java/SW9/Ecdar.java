package SW9;

import SW9.abstractions.Component;
import SW9.abstractions.Project;
import SW9.backend.UPPAALDriver;
import SW9.code_analysis.CodeAnalysis;
import SW9.controllers.CanvasController;
import SW9.controllers.EcdarController;
import SW9.presentations.BackgroundThreadPresentation;
import SW9.presentations.EcdarPresentation;
import SW9.presentations.UndoRedoHistoryPresentation;
import SW9.utility.keyboard.Keybind;
import SW9.utility.keyboard.KeyboardTracker;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
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
import javafx.stage.StageStyle;
import jiconfont.icons.GoogleMaterialDesignIcons;
import jiconfont.javafx.IconFontFX;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.CodeSource;

public class Ecdar extends Application {

    public static String serverDirectory;
    public static String debugDirectory;
    public static boolean serializationDone = false;
    private static Project project;
    private static EcdarPresentation presentation;
    public static SimpleStringProperty projectDirectory = new SimpleStringProperty();
    private Stage debugStage;

    {
        try {
            final CodeSource codeSource = Ecdar.class.getProtectionDomain().getCodeSource();
            final File jarFile = new File(codeSource.getLocation().toURI().getPath());
            final String rootDirectory = jarFile.getParentFile().getPath() + File.separator;
            serverDirectory = rootDirectory + "servers";
            debugDirectory = rootDirectory + "uppaal-debug";
            forceCreateFolder(serverDirectory);
            forceCreateFolder(debugDirectory);
        } catch (final URISyntaxException e) {
            System.out.println("Could not create project directory!");
            System.exit(1);
        } catch (final IOException e) {
            System.out.println("Could not create project directory!");
            System.exit(2);
        }
    }
    
    public static void main(final String[] args) {
        launch(Ecdar.class, args);
    }

    /**
     * Gets the project.
     * @return the project
     */
    public static Project getProject() {
        return project;
    }

    public static void showToast(final String message) {
        presentation.showSnackbarMessage(message);
    }

    public static void showHelp() {
        presentation.showHelp();
    }

    public static BooleanProperty toggleFilePane() {
        return presentation.toggleFilePane();
    }

    public static BooleanProperty toggleQueryPane() {
        return presentation.toggleQueryPane();
    }

    /**
     * Calls
     * {@see CanvasPresentation#toggleGrid()}
     * @return A Boolean Property that is true if the grid has been turned on and false if it is off
     */
    public static BooleanProperty toggleGrid() {
        return presentation.toggleGrid();
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
        stage.setTitle("Ecdar");

        // Load the fonts required for the project
        IconFontFX.register(GoogleMaterialDesignIcons.getIconFont());
        loadFonts();

        // Remove the classic decoration
        stage.initStyle(StageStyle.UNIFIED);

        // Make the view used for the application
        presentation = new EcdarPresentation();

        // Make the scene that we will use, and set its size to 80% of the primary screen
        final Screen screen = Screen.getPrimary();
        final Scene scene = new Scene(presentation, screen.getVisualBounds().getWidth() * 0.8, screen.getVisualBounds().getHeight() * 0.8);
        stage.setScene(scene);

        // Load all .css files used todo: these should be loaded in the view classes (?)
        scene.getStylesheets().add("SW9/main.css");
        scene.getStylesheets().add("SW9/colors.css");
        scene.getStylesheets().add("SW9/model_canvas.css");

        // Handle a mouse click as a deselection of all elements
        scene.setOnMousePressed(event -> {
            if (scene.getFocusOwner() == null || scene.getFocusOwner().getParent() == null) return;
            scene.getFocusOwner().getParent().requestFocus();
        });

        // Let our keyboard tracker handle all key presses
        scene.setOnKeyPressed(KeyboardTracker.handleKeyPress);

        // Set the icon for the application
        stage.getIcons().addAll(
                new Image(getClass().getResource("ic_launcher/mipmap-hdpi/ic_launcher.png").toExternalForm()),
                new Image(getClass().getResource("ic_launcher/mipmap-mdpi/ic_launcher.png").toExternalForm()),
                new Image(getClass().getResource("ic_launcher/mipmap-xhdpi/ic_launcher.png").toExternalForm()),
                new Image(getClass().getResource("ic_launcher/mipmap-xxhdpi/ic_launcher.png").toExternalForm()),
                new Image(getClass().getResource("ic_launcher/mipmap-xxxhdpi/ic_launcher.png").toExternalForm())
        );

        // We're now ready! Let the curtains fall!
        stage.show();

        project.reset();
        CanvasController.setActiveModel(Ecdar.getProject().getComponents().get(0));

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

            debugStage.getScene().getStylesheets().add("SW9/main.css");
            debugStage.getScene().getStylesheets().add("SW9/colors.css");

            debugStage.setWidth(screen.getVisualBounds().getWidth() * 0.2);
            debugStage.setHeight(screen.getVisualBounds().getWidth() * 0.3);

            debugStage.show();
            //stage.requestFocus();
        }));

        stage.setOnCloseRequest(event -> {
            UPPAALDriver.stopEngines();

            Platform.exit();
            System.exit(0);
        });
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
            CanvasController.setActiveModel(component);
        }

        // If we found a component set that as active
        if (initialShownComponent != null) {
            CanvasController.setActiveModel(initialShownComponent);
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
