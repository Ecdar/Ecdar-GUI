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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.ResourceBundle;
import java.util.Scanner;

public class BackendOptionsDialogController implements Initializable {
    public VBox backendInstanceList;
    public JFXRippler addBackendButton;
    public JFXButton closeButton;
    public ToggleGroup defaultBackendToggleGroup = new ToggleGroup();
    public JFXButton saveButton;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initializeBackendInstanceList();
    }

    public void resetBackendOptions() {
        backendInstanceList.getChildren().clear();
        initializeBackendInstanceList();
    }

    private void initializeBackendInstanceList() {
        try {
            // ToDo NIELS: Maybe use load JSON from file function instead of reading from file like this
            File myObj = new File("src/main/resources/ecdar/default_backends.json");
            Scanner myReader = new Scanner(myObj);

            StringBuilder defaultBackendInstanceList = new StringBuilder();
            while (myReader.hasNextLine()) {
                defaultBackendInstanceList.append(myReader.nextLine());
            }
            myReader.close();

            final JsonArray backends = JsonParser.parseString(Ecdar.preferences.get("backend_instances", defaultBackendInstanceList.toString())).getAsJsonArray();

            ArrayList<BackendInstance> backendInstances = new ArrayList<>();

            backends.forEach((backend) -> {
                BackendInstance newBackendInstance = new BackendInstance(backend.getAsJsonObject());
                BackendInstancePresentation newBackendInstancePresentation = new BackendInstancePresentation(newBackendInstance);
                addBackendInstancePresentationToList(newBackendInstancePresentation);
                backendInstances.add(newBackendInstance);
            });

            BackendHelper.updateBackendInstances(backendInstances);
        } catch (FileNotFoundException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }

        HBox.setHgrow(addBackendButton, Priority.ALWAYS);
        addBackendButton.setMaxWidth(Double.MAX_VALUE);
        addBackendButton.setOnMouseClicked((event) -> {
            BackendInstancePresentation newBackendInstancePresentation = new BackendInstancePresentation();
            addBackendInstancePresentationToList(newBackendInstancePresentation);
        });
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

        // Math.max added to avoid index -1
        int newIndex = Math.max(0, (currentIndex + i) % backendInstanceList.getChildren().size());
        // ToDo NIELS: Prevent loop around for overflow or add for underflow

        backendInstanceList.getChildren().remove(newBackendInstance);
        backendInstanceList.getChildren().add(newIndex, newBackendInstance);
    }

    /**
     * Marks input fields in the backendInstanceList if any are present and returns whether any were found
     *
     * @return whether any errors were found
     */
    private boolean backendInstaceListIsErrorFree() {
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
                    // ToDo NIELS: Log the following: ioException.getMessage();
                    errorFree = false;
                }
            }
        }

        backendInstanceController.locationIssue.setVisible(!errorFree);

        return errorFree;
    }

    /**
     * Returns true if no errors where found in the backend instance definitions, otherwise false
     *
     * @return whether the changes could be saved
     */
    public boolean saveChangesToBackendOptions() {
        if (this.backendInstaceListIsErrorFree()) {
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

            // The is always a default backend set, so isPresent check is unnecessary
            BackendInstance defaultBackend = backendInstances.stream().filter(BackendInstance::isDefault).findFirst().get();
            BackendHelper.setDefaultBackendInstance(defaultBackend);

            String defaultBackendName = (defaultBackend.getName());
            Ecdar.preferences.put("default_backend", defaultBackendName);

            return true;
        } else {
            return false;
        }
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
