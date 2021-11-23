package ecdar.controllers;

import com.jfoenix.controls.JFXCheckBox;
import com.jfoenix.controls.JFXRippler;
import com.jfoenix.controls.JFXTextField;
import ecdar.abstractions.BackendInstance;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

public class BackendInstanceController implements Initializable {
    private BackendInstance backendInstance = new BackendInstance();

    public JFXTextField backendName;
    public Label backendNameIssue;
    public FontIcon expansionIcon;
    public JFXRippler removeBackendRippler;
    public FontIcon removeBackendIcon;
    public StackPane content;
    public JFXCheckBox isLocal;
    public HBox addressSection;
    public JFXTextField address;
    public HBox pathToBackendSection;
    public JFXRippler pickPathToBackend;
    public JFXTextField pathToBackend;
    public Label locationIssue;
    public JFXTextField portRangeStart;
    public Label portRangeStartIssue;
    public JFXTextField portRangeEnd;
    public Label portRangeEndIssue;
    public Label portRangeIssue;
    public StackPane moveBackendInstanceUpRippler;
    public StackPane moveBackendInstanceDownRippler;
    public RadioButton defaultBackendRadioButton;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        Platform.runLater(() -> {
            this.handleLocalPropertyChanged();
            moveBackendInstanceUpRippler.setCursor(Cursor.HAND);
            moveBackendInstanceDownRippler.setCursor(Cursor.HAND);
            setHGrow();

            // Prevent deletion of default backend instance
            defaultBackendRadioButton.selectedProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue) {
                    removeBackendIcon.setFill(Color.GREY);
                } else {
                    removeBackendIcon.setFill(Color.BLACK);
                }
            });

            if (defaultBackendRadioButton.isSelected()) removeBackendIcon.setFill(Color.GREY);
        });
    }

    /***
     * Sets the BackendInstance object and overrides the current settings shown in the GUI
     * @param instance the new BackendInstance
     */
    public void setBackendInstance(BackendInstance instance) {
        this.backendInstance = instance;

        this.backendName.setText(instance.getName());
        this.isLocal.setSelected(instance.isLocal());
        this.defaultBackendRadioButton.setSelected(instance.isDefault());

        // Check if the path or the address should be used
        if (isLocal.isSelected()) {
            this.pathToBackend.setText(instance.getBackendLocation());
        } else {
            this.address.setText(instance.getBackendLocation());
        }

        this.portRangeStart.setText(String.valueOf(instance.getPortStart()));
        this.portRangeEnd.setText(String.valueOf(instance.getPortEnd()));
    }

    public BackendInstance updateBackendInstance() {
        backendInstance.setName(backendName.getText());
        backendInstance.setLocal(isLocal.isSelected());
        backendInstance.setDefault(defaultBackendRadioButton.isSelected());
        backendInstance.setBackendLocation(isLocal.isSelected() ? pathToBackend.getText() : address.getText());
        backendInstance.setPortStart(Integer.parseInt(portRangeStart.getText()));
        backendInstance.setPortEnd(Integer.parseInt(portRangeEnd.getText()));

        return backendInstance;
    }

    private void setHGrow() {
        HBox.setHgrow(backendName.getParent().getParent().getParent(), Priority.ALWAYS);
        HBox.setHgrow(backendName.getParent(), Priority.ALWAYS);
        HBox.setHgrow(backendName, Priority.ALWAYS);
        HBox.setHgrow(content, Priority.ALWAYS);
        HBox.setHgrow(addressSection, Priority.ALWAYS);
        HBox.setHgrow(address, Priority.ALWAYS);
        HBox.setHgrow(pathToBackendSection, Priority.ALWAYS);
        HBox.setHgrow(pathToBackend, Priority.ALWAYS);
        HBox.setHgrow(portRangeStart, Priority.ALWAYS);
        HBox.setHgrow(portRangeEnd, Priority.ALWAYS);
    }

    private void handleLocalPropertyChanged() {
        if (isLocal.isSelected()) {
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
        final FileChooser backendPicker = new FileChooser();
        backendPicker.setTitle("Choose backend");

        // The initial location for the file choosing dialog
        final File jarDir = new File(pathToBackend.getText()).getAbsoluteFile().getParentFile();

        // If the file does not exist, we must be running it from a development environment, use a default location
        if(jarDir.exists()) {
            backendPicker.setInitialDirectory(jarDir);
        }

        // Prompt the user to find a file (will halt the UI thread)
        final File file = backendPicker.showOpenDialog(null);
        if(file != null) {
            pathToBackend.setText(file.getAbsolutePath());
        }
    }
}
