package ecdar.controllers;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXRippler;
import ecdar.Ecdar;
import ecdar.abstractions.BackendInstance;
import ecdar.backend.BackendHelper;
import ecdar.presentations.BackendInstancePresentation;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.apache.commons.lang3.Range;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class BackendOptionsDialogController implements Initializable {
    public VBox backendInstanceList;
    public JFXRippler addBackendButton;
    public JFXButton closeButton;
    public ToggleGroup defaultBackendToggleGroup = new ToggleGroup();
    public JFXButton saveButton;
    public JFXButton resetBackendsButton;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initializeBackendInstanceList();
    }

    /**
     * Reverts any changes made to the backend options by reloading the options specified in the preference file,
     * or to the default, if no backends are present in the preferences file
     */
    public void cancelBackendOptionsChanges() {
        initializeBackendInstanceList();
    }

    /**
     * Saves the changes made to the backend options to the preferences file and returns true
     * if no errors where found in the backend instance definitions, otherwise false
     *
     * @return whether the changes could be saved,
     * meaning that no errors where found in the changes made to the backend options
     */
    public boolean saveChangesToBackendOptions() {
        if (this.backendInstanceListIsErrorFree()) {
            ArrayList<BackendInstance> backendInstances = new ArrayList<>();
            for (Node backendInstance : backendInstanceList.getChildren()) {
                if (backendInstance instanceof BackendInstancePresentation) {
                    backendInstances.add(((BackendInstancePresentation) backendInstance).getController().updateBackendInstance());
                }
            }

            BackendHelper.updateBackendInstances(backendInstances);

            JsonArray jsonArray = new JsonArray();
            for (BackendInstance bi : backendInstances) {
                jsonArray.add(bi.serialize());
            }

            Ecdar.preferences.put("backend_instances", jsonArray.toString());

            BackendInstance defaultBackend = backendInstances.stream().filter(BackendInstance::isDefault).findFirst().orElse(backendInstances.get(0));
            BackendHelper.setDefaultBackendInstance(defaultBackend);

            String defaultBackendName = (defaultBackend.getName());
            Ecdar.preferences.put("default_backend", defaultBackendName);

            return true;
        } else {
            return false;
        }
    }

    /**
     * Resets the backends to the default backends present in the 'default_backends.json' file
     */
    public void resetBackendsToDefault() {
        updateBackendsInGUI(getDefaultBackends());
    }

    private void initializeBackendInstanceList() {
        ArrayList<BackendInstance> backends;

        // Load backends from preferences or get default
        var savedBackends = Ecdar.preferences.get("backend_instances", null);
        if (savedBackends != null) {
            backends = getBackendsFromJsonArray(
                    JsonParser.parseString(savedBackends).getAsJsonArray());
        } else {
            backends = getDefaultBackends();
        }

        // Style add backend button and handle click event
        HBox.setHgrow(addBackendButton, Priority.ALWAYS);
        addBackendButton.setMaxWidth(Double.MAX_VALUE);
        addBackendButton.setOnMouseClicked((event) -> {
            BackendInstancePresentation newBackendInstancePresentation = new BackendInstancePresentation();
            addBackendInstancePresentationToList(newBackendInstancePresentation);
        });

        updateBackendsInGUI(backends);
    }

    private void updateBackendsInGUI(ArrayList<BackendInstance> backends) {
        backendInstanceList.getChildren().clear();

        backends.forEach((bi) -> {
            BackendInstancePresentation newBackendInstancePresentation = new BackendInstancePresentation(bi);
            newBackendInstancePresentation.getController().backendName.disableProperty().bind(bi.getLockedProperty());
            newBackendInstancePresentation.getController().pathToBackend.disableProperty().bind(bi.getLockedProperty());
            addBackendInstancePresentationToList(newBackendInstancePresentation);
        });

        BackendHelper.updateBackendInstances(backends);
    }

    private ArrayList<BackendInstance> getBackendsFromJsonArray(JsonArray backends) {
        ArrayList<BackendInstance> backendInstances = new ArrayList<>();
        backendInstanceList.getChildren().clear();
        backends.forEach((backend) -> {
            BackendInstance newBackendInstance = new BackendInstance(backend.getAsJsonObject());
            backendInstances.add(newBackendInstance);
        });

        return backendInstances;
    }

    private ArrayList<BackendInstance> getDefaultBackends() {
        ArrayList<BackendInstance> defaultBackends = new ArrayList<>();

        // Add Reveaal engine
        var reveaal = new BackendInstance();
        reveaal.setName("Reveaal");
        reveaal.setLocal(true);
        reveaal.setDefault(true);
        reveaal.setPortStart(5032);
        reveaal.setPortEnd(5040);
        reveaal.lockInstance();

        List<File> searchPathForReveaal = List.of(
                new File("lib/Reveaal.exe"), new File("lib/Reveaal")
        );
        getBackendPathIfFileExists(reveaal, searchPathForReveaal);
        defaultBackends.add(reveaal);

        // Add jECDAR engine
        var jEcdar = new BackendInstance();
        jEcdar.setName("jECDAR");
        jEcdar.setLocal(true);
        jEcdar.setDefault(false);
        jEcdar.setPortStart(5042);
        jEcdar.setPortEnd(5050);
        jEcdar.lockInstance();

        List<File> searchPathForJEcdar = List.of(
                new File("lib/j-Ecdar.exe"), new File("lib/j-Ecdar.bat")
        );
        getBackendPathIfFileExists(jEcdar, searchPathForJEcdar);
        defaultBackends.add(jEcdar);

        return defaultBackends;
    }

    private void getBackendPathIfFileExists(BackendInstance engine, List<File> searchPathForFile) {
        engine.setBackendLocation("");

        for (var f : searchPathForFile) {
            if (f.exists()) {
                engine.setBackendLocation(f.getAbsolutePath());
                break;
            }
        }

        if (engine.getBackendLocation().equals("")) {
            throw new RuntimeException("Could not locate file for default engine, checked: " + searchPathForFile.stream().map(File::getPath).collect(Collectors.joining(", ")));
        }
    }

    private void addBackendInstancePresentationToList(BackendInstancePresentation newBackendInstancePresentation) {
        backendInstanceList.getChildren().add(newBackendInstancePresentation);
        newBackendInstancePresentation.getController().moveBackendInstanceUpRippler.setOnMouseClicked((mouseEvent) -> moveBackendInstance(newBackendInstancePresentation, -1));
        newBackendInstancePresentation.getController().moveBackendInstanceDownRippler.setOnMouseClicked((mouseEvent) -> moveBackendInstance(newBackendInstancePresentation, +1));
        newBackendInstancePresentation.getController().removeBackendRippler.setOnMouseClicked((mouseEvent) -> {
            if (!newBackendInstancePresentation.getController().defaultBackendRadioButton.isSelected()) {
                backendInstanceList.getChildren().remove(newBackendInstancePresentation);
            }
        });
        newBackendInstancePresentation.getController().defaultBackendRadioButton.setToggleGroup(defaultBackendToggleGroup);
    }

    private void moveBackendInstance(BackendInstancePresentation newBackendInstance, int i) {
        int currentIndex = backendInstanceList.getChildren().indexOf(newBackendInstance);
        int newIndex = (currentIndex + i) % backendInstanceList.getChildren().size();
        if (newIndex < 0) {
            newIndex = backendInstanceList.getChildren().size() - 1;
        }

        backendInstanceList.getChildren().remove(newBackendInstance);
        backendInstanceList.getChildren().add(newIndex, newBackendInstance);
    }

    /**
     * Marks input fields in the backendInstanceList that contains errors and returns whether any errors were found
     *
     * @return whether any errors were found
     */
    private boolean backendInstanceListIsErrorFree() {
        boolean error = true;

        for (Node child : backendInstanceList.getChildren()) {
            if (child instanceof BackendInstancePresentation) {
                BackendInstanceController backendInstanceController = ((BackendInstancePresentation) child).getController();
                error = backendNameIsErrorFree(backendInstanceController) && error;
                error = portRangeIsErrorFree(backendInstanceController) && error;
                error = backendInstanceLocationIsErrorFree(backendInstanceController) && error;
            }
        }

        return error;
    }

    private boolean backendNameIsErrorFree(BackendInstanceController backendInstanceController) {
        String backendName = backendInstanceController.backendName.getText();

        if (backendName.isBlank()) {
            backendInstanceController.backendNameIssue.setText(ValidationErrorMessages.BACKEND_NAME_EMPTY.toString());
            backendInstanceController.backendNameIssue.setVisible(true);
            return false;
        }

        backendInstanceController.backendNameIssue.setVisible(false);
        return true;
    }

    private boolean portRangeIsErrorFree(BackendInstanceController backendInstanceController) {
        boolean errorFree = true;
        int portRangeStart = 0, portRangeEnd = 0;
        backendInstanceController.portRangeStartIssue.setText("");
        backendInstanceController.portRangeStartIssue.setVisible(false);
        backendInstanceController.portRangeEndIssue.setText("");
        backendInstanceController.portRangeEndIssue.setVisible(false);
        backendInstanceController.portRangeIssue.setVisible(false);

        try {
            portRangeStart = Integer.parseInt(backendInstanceController.portRangeStart.getText());
        } catch (NumberFormatException numberFormatException) {
            backendInstanceController.portRangeStartIssue.setText(ValidationErrorMessages.VALUE_NOT_INTEGER.toString());
            errorFree = false;
        }

        try {
            portRangeEnd = Integer.parseInt(backendInstanceController.portRangeEnd.getText());
        } catch (NumberFormatException numberFormatException) {
            backendInstanceController.portRangeEndIssue.setText(ValidationErrorMessages.VALUE_NOT_INTEGER.toString());
            errorFree = false;
        }

        Range<Integer> portRange = Range.between(0, 65535);

        if (!portRange.contains(portRangeStart)) {
            if (backendInstanceController.portRangeStartIssue.getText().isBlank()) {
                backendInstanceController.portRangeStartIssue.setText(ValidationErrorMessages.PORT_VALUE_NOT_WITHIN_ACCEPTABLE_RANGE.toString());
            } else {
                backendInstanceController.portRangeStartIssue.setText(ValidationErrorMessages.PORT_VALUE_NOT_WITHIN_ACCEPTABLE_RANGE_CONCATINATION.toString());
            }
            errorFree = false;
        }
        if (!portRange.contains(portRangeEnd)) {
            if (backendInstanceController.portRangeEndIssue.getText().isBlank()) {
                backendInstanceController.portRangeEndIssue.setText(ValidationErrorMessages.PORT_VALUE_NOT_WITHIN_ACCEPTABLE_RANGE.toString());
            } else {
                backendInstanceController.portRangeEndIssue.setText(ValidationErrorMessages.PORT_VALUE_NOT_WITHIN_ACCEPTABLE_RANGE_CONCATINATION.toString());
            }
            errorFree = false;
        }

        if (portRangeEnd - portRangeStart < 0) {
            backendInstanceController.portRangeIssue.setText(ValidationErrorMessages.PORT_RANGE_MUST_BE_INCREMENTAL.toString());
            errorFree = false;
        }

        backendInstanceController.portRangeStartIssue.setVisible(!errorFree);
        backendInstanceController.portRangeEndIssue.setVisible(!errorFree);
        backendInstanceController.portRangeIssue.setVisible(!errorFree);

        return errorFree;
    }

    private boolean backendInstanceLocationIsErrorFree(BackendInstanceController backendInstanceController) {
        boolean errorFree = true;

        if (backendInstanceController.isLocal.isSelected()) {
            if (backendInstanceController.pathToBackend.getText().isBlank()) {
                backendInstanceController.locationIssue.setText(ValidationErrorMessages.FILE_LOCATION_IS_BLANK.toString());
                errorFree = false;
            } else {
                Path localBackendPath = Paths.get(backendInstanceController.pathToBackend.getText());

                if (!Files.isExecutable(localBackendPath)) {
                    backendInstanceController.locationIssue.setText(ValidationErrorMessages.FILE_DOES_NOT_EXIST_OR_NOT_EXECUTABLE.toString());
                    errorFree = false;
                }
            }
        } else {
            if (backendInstanceController.address.getText().isBlank()) {
                backendInstanceController.locationIssue.setText(ValidationErrorMessages.HOST_ADDRESS_IS_BLANK.toString());
                errorFree = false;
            } else {
                try {
                    InetAddress address = InetAddress.getByName(backendInstanceController.address.getText());
                    boolean reachable = address.isReachable(200);

                    if (!reachable) {
                        backendInstanceController.locationIssue.setText(ValidationErrorMessages.HOST_NOT_REACHABLE.toString());
                        errorFree = false;
                    }

                } catch (UnknownHostException unknownHostException) {
                    backendInstanceController.locationIssue.setText(ValidationErrorMessages.UNACCEPTABLE_HOST_NAME.toString());
                    errorFree = false;
                } catch (IOException ioException) {
                    backendInstanceController.locationIssue.setText(ValidationErrorMessages.IO_EXCEPTION_WITH_HOST.toString());
                    errorFree = false;
                }
            }
        }

        backendInstanceController.locationIssue.setVisible(!errorFree);

        return errorFree;
    }

    private enum ValidationErrorMessages {
        BACKEND_NAME_EMPTY {
            @Override
            public String toString() {
                return "The backend name cannot be empty";
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
                return "Please specify a file for this backend";
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
