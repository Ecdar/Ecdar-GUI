package ecdar.controllers;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
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

import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.ResourceBundle;

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
        // ToDo NIELS: Read saved options and override current
    }

    private void initializeBackendInstanceList() {
        String defaultBackendInstanceList = "[{'name': 'Reveaal', 'isLocal': 'true', 'isDefault': 'true', 'location': 'src/Reveaal', 'portRangeStart': '5032', 'portRangeEnd': '5040'},{'name': 'jECDAR', 'isLocal': 'True', 'isDefault': 'False', 'location': 'src/libs/j-Ecdar.jar', 'portRangeStart': '5042', 'portRangeEnd': '5050'}]";
        final JsonArray backends = JsonParser.parseString(Ecdar.preferences.get("backend_instances", defaultBackendInstanceList)).getAsJsonArray();

        ArrayList<BackendInstance> backendInstances = new ArrayList<>();

        backends.forEach((backend) -> {
            BackendInstance newBackendInstance = new BackendInstance(backend.getAsJsonObject());
            BackendInstancePresentation newBackendInstancePresentation = new BackendInstancePresentation(newBackendInstance);
            addBackendInstancePresentationToList(newBackendInstancePresentation);
            backendInstances.add(newBackendInstance);
        });

        BackendHelper.setBackendInstances(backendInstances);

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

                error = backendNameIsErrorFree(backendInstanceController);
                error = portRangeIsErrorFree(backendInstanceController) && error;
            }
        }

        return error;
    }

    private boolean backendNameIsErrorFree(BackendInstanceController backendInstanceController) {
        String backendName = backendInstanceController.backendName.getText();

        if (backendName.isBlank()) {
            backendInstanceController.backendNameIssue.setText("The backend name cannot be empty");
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
            backendInstanceController.portRangeStartIssue.setText("Value must be integer");
            errorFree = false;
        }

        try {
            portRangeEnd = Integer.parseInt(backendInstanceController.portRangeEnd.getText());
        } catch (NumberFormatException numberFormatException) {
            backendInstanceController.portRangeEndIssue.setText("Value must be integer");
            errorFree = false;
        }

        Range<Integer> portRange = Range.between(0, 65535);

        if (!portRange.contains(portRangeStart)) {
            if (backendInstanceController.portRangeStartIssue.getText().isBlank()) {
                backendInstanceController.portRangeStartIssue.setText("Port must be within range 0 - 65535");
            } else {
                backendInstanceController.portRangeStartIssue.setText(" and within range 0 - 65535");
            }
            errorFree = false;
        }
        if (!portRange.contains(portRangeEnd)){
            if (backendInstanceController.portRangeEndIssue.getText().isBlank()) {
                backendInstanceController.portRangeEndIssue.setText("Port must be within range 0 - 65535");
            } else {
                backendInstanceController.portRangeEndIssue.setText(" and within range 0 - 65535");
            }
            errorFree = false;
        }

        if(portRangeEnd - portRangeStart < 0) {
            backendInstanceController.portRangeIssue.setText("Start of port range must be greater than end");
            errorFree = false;
        }

        backendInstanceController.portRangeStartIssue.setVisible(!errorFree);
        backendInstanceController.portRangeEndIssue.setVisible(!errorFree);
        backendInstanceController.portRangeIssue.setVisible(!errorFree);

        return errorFree;
    }

    private boolean backendInstanceLocationIsErrorFree(BackendInstanceController backendInstanceController) {
        if (backendInstanceController.isLocal.isSelected()) {
            Path localBackendPath = Paths.get(backendInstanceController.pathToBackend.getText());
            if (!Files.isExecutable(localBackendPath)) {
                // ToDo NIELS: The path either does not exist or it is read/execute protected
                return false;
            }

            if (!localBackendPath.endsWith(".jar") && !localBackendPath.endsWith(".exe") && !localBackendPath.getFileName().toString().contains(".")) {
                // ToDo NIELS: The path is not an accepted file type
                return false;
            }
        } else {
            try {
                InetAddress address = InetAddress.getByName(backendInstanceController.address.getText());
                boolean reachable = address.isReachable(200);

                if (!reachable) {
                    // ToDo NIELS: Address is unreachable
                    return false;
                }

            } catch (UnknownHostException unknownHostException) {
                // ToDo NIELS: The address is not an acceptable hostname
                return false;
            } catch (IOException ioException) {
                // ToDo NIELS: IOException while trying to reach host
                return false;
            }
        }

        return true;
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

            BackendHelper.setBackendInstances(backendInstances);

            JsonArray jsonArray = new JsonArray();
            for (BackendInstance bi : backendInstances) {
                jsonArray.add(bi.serialize());
            }

            Ecdar.preferences.put("backend_instances", jsonArray.toString());

            // The is always a default backend set, so isPresent check is unnecessary
            String defaultBackendName = (backendInstances.stream().filter(BackendInstance::isDefault).findFirst().get().getName());
            Ecdar.preferences.put("default_backend", defaultBackendName);

            return true;
        } else {
            return false;
        }
    }
}
