package ecdar.controllers;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXDialog;
import com.jfoenix.controls.JFXRippler;
import ecdar.Ecdar;
import ecdar.abstractions.BackendInstance;
import ecdar.backend.BackendHelper;
import ecdar.presentations.BackendInstancePresentation;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.apache.commons.lang3.Range;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
        String defaultBackendInstanceList = "{'backends': [{'name': 'Reveaal', 'isLocal': 'true', 'isDefault': 'true', 'location': 'src/Reveaal', 'portRangeStart': '5032', 'portRangeEnd': '5040'},{'name': 'jECDAR', 'isLocal': 'True', 'isDefault': 'False', 'location': 'src/libs/j-Ecdar.jar', 'portRangeStart': '5042', 'portRangeEnd': '5050'}]}";
        final JsonObject jsonObject = JsonParser.parseString(Ecdar.preferences.get("backends", defaultBackendInstanceList)).getAsJsonObject();
        final JsonArray backends = jsonObject.getAsJsonArray("backends");

        backends.forEach((backend) -> {
            BackendInstance newBackendInstance = new BackendInstance(backend.getAsJsonObject());
            BackendInstancePresentation newBackendInstancePresentation = new BackendInstancePresentation(newBackendInstance);
            addBackendInstancePresentationToList(newBackendInstancePresentation);
        });

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
        newBackendInstancePresentation.getController().removeBackendRippler.setOnMouseClicked((mouseEvent) -> backendInstanceList.getChildren().remove(newBackendInstancePresentation));
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
        for (Node child : backendInstanceList.getChildren()) {
            if (child instanceof BackendInstancePresentation) {
                BackendInstanceController backendInstanceController = ((BackendInstancePresentation) child).getController();

                return portRangeIsErrorFree(backendInstanceController);
            }
        }

        return true;
    }

    private boolean portRangeIsErrorFree(BackendInstanceController backendInstanceController) {
        int portRangeStart;
        int portRangeEnd;

        try {
            portRangeStart = Integer.parseInt(backendInstanceController.portRangeStart.getText());
        } catch (NumberFormatException numberFormatException) {
            // ToDO NIELS: The value is not an integer
            return false;
        }

        try {
            portRangeEnd = Integer.parseInt(backendInstanceController.portRangeEnd.getText());
        } catch (NumberFormatException numberFormatException) {
            // ToDO NIELS: The value is not an integer
            return false;
        }

        Range<Integer> portRange = Range.between(0, 65535);

        if (!portRange.contains(portRangeStart)
                || !portRange.contains(portRangeEnd)
                || portRangeEnd - portRangeStart < 0) {
            // ToDo NIELS: The port range is not acceptable
            return false;
        }

        return true;
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
            Ecdar.preferences.put("default_backend", BackendHelper.getDefaultBackend().getName());
            return true;
        } else {
            return false;
        }
    }
}
