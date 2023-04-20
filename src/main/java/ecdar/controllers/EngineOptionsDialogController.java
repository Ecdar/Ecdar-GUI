package ecdar.controllers;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXRippler;
import ecdar.Ecdar;
import ecdar.backend.Engine;
import ecdar.backend.BackendException;
import ecdar.backend.BackendHelper;
import ecdar.presentations.EnginePresentation;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.apache.commons.lang3.Range;
import org.apache.commons.lang3.SystemUtils;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class EngineOptionsDialogController implements Initializable {
    public VBox engineInstanceList;
    public JFXRippler addEngineButton;
    public JFXButton closeButton;
    public ToggleGroup defaultEngineToggleGroup = new ToggleGroup();
    public JFXButton saveButton;
    public JFXButton resetEnginesButton;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initializeEngineInstanceList();
    }

    /**
     * Reverts any changes made to the engine options by reloading the options specified in the preference file,
     * or to the default, if no engines are present in the preferences file.
     */
    public void cancelEngineOptionsChanges() {
        initializeEngineInstanceList();
    }

    /**
     * Saves the changes made to the engine options to the preferences file and returns true
     * if no errors where found in the engine instance definitions, otherwise false.
     *
     * @return whether the changes could be saved,
     * meaning that no errors where found in the changes made to the engine options
     */
    public boolean saveChangesToEngineOptions() {
        if (this.engineInstanceListIsErrorFree()) {
            ArrayList<Engine> engines = new ArrayList<>();
            for (Node engine : engineInstanceList.getChildren()) {
                if (engine instanceof EnginePresentation) {
                    engines.add(((EnginePresentation) engine).getController().updateEngineInstance());
                }
            }

            if (engines.size() < 1) {
                Ecdar.showToast("Please add an engine instance or press: \"" + resetEnginesButton.getText() + "\"");
                return false;
            }

            // Close all engine connections to avoid dangling engine connections when port range is changed
            try {
                BackendHelper.clearEngineConnections();
            } catch (BackendException e) {
                e.printStackTrace();
            }

            BackendHelper.updateEngineInstances(engines);

            JsonArray jsonArray = new JsonArray();
            for (Engine engine : engines) {
                jsonArray.add(engine.serialize());
            }

            Ecdar.preferences.put("engines", jsonArray.toString());

            Engine defaultEngine = engines.stream().filter(Engine::isDefault).findFirst().orElse(engines.get(0));
            BackendHelper.setDefaultEngine(defaultEngine);

            return true;
        } else {
            return false;
        }
    }

    /**
     * Resets the engines to those packaged with the system.
     */
    public void resetEnginesToDefault() {
        updateEnginesInGUI(getPackagedEngines());
    }

    private void initializeEngineInstanceList() {
        ArrayList<Engine> engines;

        // Load engines from preferences or get default
        var savedEngines = Ecdar.preferences.get("engines", null);
        if (savedEngines != null) {
            engines = getEnginesFromJsonArray(
                    JsonParser.parseString(savedEngines).getAsJsonArray());
        } else {
            engines = getPackagedEngines();
        }

        // Style add engine button and handle click event
        HBox.setHgrow(addEngineButton, Priority.ALWAYS);
        addEngineButton.setMaxWidth(Double.MAX_VALUE);
        addEngineButton.setOnMouseClicked((event) -> {
            EnginePresentation newEnginePresentation = new EnginePresentation();
            addEnginePresentationToList(newEnginePresentation);
        });

        updateEnginesInGUI(engines);
    }

    /**
     * Clear the engine list and add the newly defined engines to it.
     *
     * @param engines The new list of engines
     */
    private void updateEnginesInGUI(ArrayList<Engine> engines) {
        engineInstanceList.getChildren().clear();

        engines.forEach((engine) -> {
            EnginePresentation newEnginePresentation = new EnginePresentation(engine);

            // Bind input fields that should not be changed for packaged engines to the locked property of the engine instance
            newEnginePresentation.getController().engineName.disableProperty().bind(engine.getLockedProperty());
            newEnginePresentation.getController().pathToEngine.disableProperty().bind(engine.getLockedProperty());
            newEnginePresentation.getController().pickPathToEngine.disableProperty().bind(engine.getLockedProperty());
            newEnginePresentation.getController().isLocal.disableProperty().bind(engine.getLockedProperty());
            addEnginePresentationToList(newEnginePresentation);
        });

        BackendHelper.updateEngineInstances(engines);
    }

    /**
     * Instantiate enginesArray defined in the given JsonArray.
     *
     * @param enginesArray The JsonArray containing the enginesArray
     * @return An ArrayList of the instantiated enginesArray
     */
    private ArrayList<Engine> getEnginesFromJsonArray(JsonArray enginesArray) {
        ArrayList<Engine> engines = new ArrayList<>();
        engineInstanceList.getChildren().clear();
        enginesArray.forEach((engine) -> {
            Engine newEngine = new Engine(engine.getAsJsonObject());
            engines.add(newEngine);
        });

        return engines;
    }

    /**
     * Checks a set of paths to the packaged engines, j-Ecdar and Reveaal, and instantiates them
     * if one of the related files exists.
     *
     * @return The packaged engines
     */
    private ArrayList<Engine> getPackagedEngines() {
        ArrayList<Engine> defaultEngines = new ArrayList<>();

        // Add Reveaal engine
        var reveaal = new Engine();
        reveaal.setName("Reveaal");
        reveaal.setLocal(true);
        reveaal.setDefault(true);
        reveaal.setPortStart(5040);
        reveaal.setPortEnd(5042);
        reveaal.lockInstance();
        reveaal.setIsThreadSafe(true);

        // Load correct Reveaal executable based on OS
        List<String> potentialFilesForReveaal = new ArrayList<>();
        if (SystemUtils.IS_OS_WINDOWS) {
            potentialFilesForReveaal.add("Reveaal.exe");
        } else {
            potentialFilesForReveaal.add("Reveaal");
        }
        if (setEnginePathIfFileExists(reveaal, potentialFilesForReveaal)) defaultEngines.add(reveaal);

        // Add jECDAR engine
        var jEcdar = new Engine();
        jEcdar.setName("j-Ecdar");
        jEcdar.setLocal(true);
        jEcdar.setDefault(false);
        jEcdar.setPortStart(5042);
        jEcdar.setPortEnd(5050);
        jEcdar.lockInstance();
        jEcdar.setIsThreadSafe(false);

        // Load correct j-Ecdar executable based on OS
        List<String> potentialFiledForJEcdar = new ArrayList<>();
        if (SystemUtils.IS_OS_WINDOWS) {
            potentialFiledForJEcdar.add("j-Ecdar.bat");
        } else {
            potentialFiledForJEcdar.add("j-Ecdar");
        }

        if (setEnginePathIfFileExists(jEcdar, potentialFiledForJEcdar)) defaultEngines.add(jEcdar);

        return defaultEngines;
    }

    /**
     * Sets the path to the engine if one of the potential files exists
     *
     * @param engine         The engine to set the path for
     * @param potentialFiles List of potential files to use for the engine
     * @return True if one of the potentialFiles where found in path, false otherwise.
     * This value also signals whether the engine engineLocation is set
     */
    private boolean setEnginePathIfFileExists(Engine engine, List<String> potentialFiles) {
        engine.setEngineLocation("");

        try {
            // Get directory containing the bin and lib folders for the executing program
            String pathToEcdarDirectory = new File(getClass().getProtectionDomain().getCodeSource().getLocation().toURI()).getParentFile().getPath();

            List<File> files = List.of(Objects.requireNonNull(new File(pathToEcdarDirectory).listFiles()));
            for (File f : files) {
                if (potentialFiles.contains(f.getName())) {
                    engine.setEngineLocation(f.getAbsolutePath());
                    return true;
                }
            }
        } catch (URISyntaxException e) {
            e.printStackTrace();
            Ecdar.showToast("Unable to get URI of parent directory: \"" + getClass().getProtectionDomain().getCodeSource().getLocation() + "\" due to: " + e.getMessage());
        } catch (NullPointerException e) {
            e.printStackTrace();
            Ecdar.showToast("Encountered null reference when trying to get path of executing program");
        }

        return !engine.getEngineLocation().equals("");
    }

    /**
     * Add the new engine instance presentation to the engine options dialog
     * @param newEnginePresentation The presentation of the new engine instance
     */
    private void addEnginePresentationToList(EnginePresentation newEnginePresentation) {
        engineInstanceList.getChildren().add(newEnginePresentation);
        newEnginePresentation.getController().moveEngineInstanceUpRippler.setOnMouseClicked((mouseEvent) -> moveEngineInstance(newEnginePresentation, -1));
        newEnginePresentation.getController().moveEngineInstanceDownRippler.setOnMouseClicked((mouseEvent) -> moveEngineInstance(newEnginePresentation, +1));

        // Set remove engine action to only fire if the engine is not locked
        newEnginePresentation.getController().removeEngineRippler.setOnMouseClicked((mouseEvent) -> {
            if (!newEnginePresentation.getController().defaultEngineRadioButton.isSelected()) {
                engineInstanceList.getChildren().remove(newEnginePresentation);
            }
        });
        newEnginePresentation.getController().defaultEngineRadioButton.setToggleGroup(defaultEngineToggleGroup);
    }

    /**
     * Calculated the new position of the engine, 'i' places further down, in the engine list.
     * The engine presentation is removed and added to the new position.
     * Given a negative value, the instance is moved up. This function uses loop-around, meaning that:
     * - If the instance is moved down while already at the bottom of the list, it is placed at the top.
     * - If the instance is moved up while already at the top of the list, it is placed at the bottom.
     *
     * @param enginePresentation The engine presentation to move
     * @param i                           The number of steps to move the engine down
     */
    private void moveEngineInstance(EnginePresentation enginePresentation, int i) {
        int currentIndex = engineInstanceList.getChildren().indexOf(enginePresentation);
        int newIndex = (currentIndex + i) % engineInstanceList.getChildren().size();
        if (newIndex < 0) {
            newIndex = engineInstanceList.getChildren().size() - 1;
        }

        engineInstanceList.getChildren().remove(enginePresentation);
        engineInstanceList.getChildren().add(newIndex, enginePresentation);
    }

    /**
     * Marks input fields in the engineList that contains errors and returns whether any errors were found
     *
     * @return whether any errors were found
     */
    private boolean engineInstanceListIsErrorFree() {
        boolean error = true;

        for (Node child : engineInstanceList.getChildren()) {
            if (child instanceof EnginePresentation) {
                EngineInstanceController engineInstanceController = ((EnginePresentation) child).getController();
                error = engineNameIsErrorFree(engineInstanceController) && error;
                error = portRangeIsErrorFree(engineInstanceController) && error;
                error = engineInstanceLocationIsErrorFree(engineInstanceController) && error;
            }
        }

        return error;
    }

    private boolean engineNameIsErrorFree(EngineInstanceController engineInstanceController) {
        String engineName = engineInstanceController.engineName.getText();

        if (engineName.isBlank()) {
            engineInstanceController.engineNameIssue.setText(ValidationErrorMessages.ENGINE_NAME_EMPTY.toString());
            engineInstanceController.engineNameIssue.setVisible(true);
            return false;
        }

        engineInstanceController.engineNameIssue.setVisible(false);
        return true;
    }

    private boolean portRangeIsErrorFree(EngineInstanceController engineInstanceController) {
        boolean errorFree = true;
        int portRangeStart = 0, portRangeEnd = 0;
        engineInstanceController.portRangeStartIssue.setText("");
        engineInstanceController.portRangeStartIssue.setVisible(false);
        engineInstanceController.portRangeEndIssue.setText("");
        engineInstanceController.portRangeEndIssue.setVisible(false);
        engineInstanceController.portRangeIssue.setVisible(false);

        try {
            portRangeStart = Integer.parseInt(engineInstanceController.portRangeStart.getText());
        } catch (NumberFormatException numberFormatException) {
            engineInstanceController.portRangeStartIssue.setText(ValidationErrorMessages.VALUE_NOT_INTEGER.toString());
            errorFree = false;
        }

        try {
            portRangeEnd = Integer.parseInt(engineInstanceController.portRangeEnd.getText());
        } catch (NumberFormatException numberFormatException) {
            engineInstanceController.portRangeEndIssue.setText(ValidationErrorMessages.VALUE_NOT_INTEGER.toString());
            errorFree = false;
        }

        Range<Integer> portRange = Range.between(0, 65535);

        if (!portRange.contains(portRangeStart)) {
            if (engineInstanceController.portRangeStartIssue.getText().isBlank()) {
                engineInstanceController.portRangeStartIssue.setText(ValidationErrorMessages.PORT_VALUE_NOT_WITHIN_ACCEPTABLE_RANGE.toString());
            } else {
                engineInstanceController.portRangeStartIssue.setText(ValidationErrorMessages.PORT_VALUE_NOT_WITHIN_ACCEPTABLE_RANGE_CONCATINATION.toString());
            }
            errorFree = false;
        }
        if (!portRange.contains(portRangeEnd)) {
            if (engineInstanceController.portRangeEndIssue.getText().isBlank()) {
                engineInstanceController.portRangeEndIssue.setText(ValidationErrorMessages.PORT_VALUE_NOT_WITHIN_ACCEPTABLE_RANGE.toString());
            } else {
                engineInstanceController.portRangeEndIssue.setText(ValidationErrorMessages.PORT_VALUE_NOT_WITHIN_ACCEPTABLE_RANGE_CONCATINATION.toString());
            }
            errorFree = false;
        }

        if (portRangeEnd - portRangeStart < 0) {
            engineInstanceController.portRangeIssue.setText(ValidationErrorMessages.PORT_RANGE_MUST_BE_INCREMENTAL.toString());
            errorFree = false;
        }

        engineInstanceController.portRangeStartIssue.setVisible(!errorFree);
        engineInstanceController.portRangeEndIssue.setVisible(!errorFree);
        engineInstanceController.portRangeIssue.setVisible(!errorFree);

        return errorFree;
    }

    private boolean engineInstanceLocationIsErrorFree(EngineInstanceController engineInstanceController) {
        boolean errorFree = true;

        if (engineInstanceController.isLocal.isSelected()) {
            if (engineInstanceController.pathToEngine.getText().isBlank()) {
                engineInstanceController.locationIssue.setText(ValidationErrorMessages.FILE_LOCATION_IS_BLANK.toString());
                errorFree = false;
            } else {
                Path localEnginePath = Paths.get(engineInstanceController.pathToEngine.getText());

                if (!Files.isExecutable(localEnginePath)) {
                    engineInstanceController.locationIssue.setText(ValidationErrorMessages.FILE_DOES_NOT_EXIST_OR_NOT_EXECUTABLE.toString());
                    errorFree = false;
                }
            }
        } else {
            if (engineInstanceController.address.getText().isBlank()) {
                engineInstanceController.locationIssue.setText(ValidationErrorMessages.HOST_ADDRESS_IS_BLANK.toString());
                errorFree = false;
            } else {
                try {
                    InetAddress address = InetAddress.getByName(engineInstanceController.address.getText());
                    boolean reachable = address.isReachable(200);

                    if (!reachable) {
                        engineInstanceController.locationIssue.setText(ValidationErrorMessages.HOST_NOT_REACHABLE.toString());
                        errorFree = false;
                    }

                } catch (UnknownHostException unknownHostException) {
                    engineInstanceController.locationIssue.setText(ValidationErrorMessages.UNACCEPTABLE_HOST_NAME.toString());
                    errorFree = false;
                } catch (IOException ioException) {
                    engineInstanceController.locationIssue.setText(ValidationErrorMessages.IO_EXCEPTION_WITH_HOST.toString());
                    errorFree = false;
                }
            }
        }

        engineInstanceController.locationIssue.setVisible(!errorFree);

        return errorFree;
    }

    private enum ValidationErrorMessages {
        ENGINE_NAME_EMPTY {
            @Override
            public String toString() {
                return "The engine name cannot be empty";
            }
        },
        VALUE_NOT_INTEGER {
            @Override
            public String toString() {
                return "Value must be integer";
            }
        },
        PORT_RANGE_MUST_BE_INCREMENTAL {
            @Override
            public String toString() {
                return "Start of port range must be greater than end";
            }
        },
        PORT_VALUE_NOT_WITHIN_ACCEPTABLE_RANGE {
            @Override
            public String toString() {
                return "Value must be within range 0 - 65535";
            }
        },
        PORT_VALUE_NOT_WITHIN_ACCEPTABLE_RANGE_CONCATINATION {
            @Override
            public String toString() {
                return " and within range 0 - 65535";
            }
        },
        FILE_LOCATION_IS_BLANK {
            @Override
            public String toString() {
                return "Please specify a file for this engine";
            }
        },
        FILE_DOES_NOT_EXIST_OR_NOT_EXECUTABLE {
            @Override
            public String toString() {
                return "The above file does not exists or ECDAR does not have the privileges to execute it";
            }
        },
        HOST_ADDRESS_IS_BLANK {
            @Override
            public String toString() {
                return "Please specify an address for the external host";
            }
        },
        HOST_NOT_REACHABLE {
            @Override
            public String toString() {
                return "The above address is not reachable. Make sure that the host is correct";
            }
        },
        UNACCEPTABLE_HOST_NAME {
            @Override
            public String toString() {
                return "The above address is not an acceptable host name";
            }
        },
        IO_EXCEPTION_WITH_HOST {
            @Override
            public String toString() {
                return "An I/O exception was encountered while trying to reach the host";
            }
        }
    }
}
