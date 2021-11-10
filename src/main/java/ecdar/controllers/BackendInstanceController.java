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
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.File;

public class BackendInstanceController {
    public JFXTextField address;
    public RadioButton defaultBackendRadioButton;
    public JFXTextField portRangeStart;
    public JFXTextField portRangeEnd;
    public JFXCheckBox localAddress;
    public JFXTextField pathToBackend;
    public HBox addressSection;
    public JFXRippler pickPathToBackend;
    public HBox pathToBackendSection;
    public JFXRippler removeBackendRippler;
    public StackPane moveBackendInstanceUpRippler;
    public StackPane moveBackendInstanceDownRippler;
    public StackPane content;
    public FontIcon expansionIcon;

    public BackendInstanceController() {
        Platform.runLater(() -> {
            this.handleLocalPropertyChanged();
            moveBackendInstanceUpRippler.setCursor(Cursor.HAND);
            moveBackendInstanceDownRippler.setCursor(Cursor.HAND);
        });
    }

    private void handleLocalPropertyChanged() {
        if (localAddress.isSelected()) {
            address.setDisable(true);
            address.setText("127.0.0.1");
            addressSection.setVisible(false);
            addressSection.setManaged(false);
            pathToBackendSection.setVisible(true);
            pathToBackendSection.setManaged(true);
        } else {
            address.setDisable(false);
            address.setText("");
            addressSection.setVisible(true);
            addressSection.setManaged(true);
            pathToBackendSection.setVisible(false);
            pathToBackendSection.setManaged(false);
        }
    }

    @FXML
    private void addressLocalClicked(){
        handleLocalPropertyChanged();
    }

    @FXML
    private void expansionClicked() {
        if (expansionIcon.getIconLiteral().equals("gmi-expand-less")) {
            expansionIcon.setIconLiteral("gmi-expand-more");
            content.setVisible(false);
            content.setManaged(false);
        } else {
            expansionIcon.setIconLiteral("gmi-expand-less");
            content.setVisible(true);
            content.setManaged(true);
        }
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
}
