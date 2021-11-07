package ecdar.controllers;

import com.jfoenix.controls.JFXCheckBox;
import com.jfoenix.controls.JFXRippler;
import com.jfoenix.controls.JFXTextField;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Cursor;
import javafx.scene.control.RadioButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.stage.DirectoryChooser;

import java.io.File;

public class BackendInstanceController {
    public JFXTextField address;
    public RadioButton defaultBackend;
    public JFXTextField portRangeStart;
    public JFXTextField portRangeEnd;
    public JFXCheckBox localAddress;
    public JFXTextField pathToBackend;
    public JFXRippler pickPathToBackend;
    public HBox pathToBackendSection;
    public HBox portSection;
    public JFXRippler removeBackend;
    public StackPane moveBackendInstance;

    public BackendInstanceController() {
        Platform.runLater(() -> {
            this.handleLocalPropertyChanged();
            moveBackendInstance.setCursor(Cursor.OPEN_HAND);
        });
    }

    private void handleLocalPropertyChanged() {
        if (localAddress.isSelected()) {
            address.setDisable(true);
            address.setText("127.0.0.1");
            portSection.setVisible(false);
            portSection.setManaged(false);
            pathToBackendSection.setVisible(true);
            pathToBackendSection.setManaged(true);
        } else {
            address.setDisable(false);
            address.setText("");
            portSection.setVisible(true);
            portSection.setManaged(true);
            pathToBackendSection.setVisible(false);
            pathToBackendSection.setManaged(false);
        }
    }

    @FXML
    private void addressLocalClicked(){
        handleLocalPropertyChanged();
    }

    @FXML
    private void openPathToBackendDialog() {
        // Dialog title
        final DirectoryChooser backendPicker = new DirectoryChooser();
        backendPicker.setTitle("Choose backend");

        // The initial location for the file choosing dialog
        final File jarDir = new File(pathToBackend.getText()).getAbsoluteFile().getParentFile();

        // If the file does not exist, we must be running it from a development environment, use an default location
        if(jarDir.exists()) {
            backendPicker.setInitialDirectory(jarDir);
        }

        // Prompt the user to find a file (will halt the UI thread)
        final File file = backendPicker.showDialog(null);
        if(file != null) {
            pathToBackend.setText(file.getAbsolutePath());
        }
    }

    @FXML
    private void removeBackendClicked() {
        System.out.println("Backend removed");
        // ToDo NIELS: Handle queries using the given backend
        // ToDo NIELS: Handle prompt user to confirm deletion
    }
}
