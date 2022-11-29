package ecdar.controllers;

import com.jfoenix.controls.JFXCheckBox;
import com.jfoenix.controls.JFXRippler;
import com.jfoenix.controls.JFXTextField;
import ecdar.abstractions.BackendInstance;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

public class BackendInstanceController implements Initializable {
    private BackendInstance backendInstance = new BackendInstance();

    /* Design elements */
    public Label backendNameIssue;
    public JFXRippler removeBackendRippler;
    public FontIcon removeBackendIcon;
    public FontIcon expansionIcon;
    public StackPane content;
    public JFXCheckBox isLocal;
    public HBox addressSection;
    public HBox pathToBackendSection;
    public JFXRippler pickPathToBackend;
    public FontIcon pickPathToBackendIcon;
    public StackPane moveBackendInstanceUpRippler;
    public StackPane moveBackendInstanceDownRippler;

    // Labels for showing potential issues
    public Label locationIssue;
    public Label portRangeStartIssue;
    public Label portRangeEndIssue;
    public Label portRangeIssue;

    /* Input fields */
    public JFXTextField backendName;
    public JFXTextField address;
    public JFXTextField pathToBackend;
    public JFXTextField portRangeStart;
    public JFXTextField portRangeEnd;
    public RadioButton defaultBackendRadioButton;
    public RadioButton threadSafeBackendRadioButton;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        Platform.runLater(() -> {
            this.handleLocalPropertyChanged();
            moveBackendInstanceUpRippler.setCursor(Cursor.HAND);
            moveBackendInstanceDownRippler.setCursor(Cursor.HAND);
            setHGrow();

            colorIconAsDisabledBasedOnProperty(removeBackendIcon, defaultBackendRadioButton.selectedProperty());
            colorIconAsDisabledBasedOnProperty(removeBackendIcon, threadSafeBackendRadioButton.selectedProperty());
            colorIconAsDisabledBasedOnProperty(pickPathToBackendIcon, backendInstance.getLockedProperty());
        });
    }

    /**
     * Binds the color of the given icon to the value of BooleanProperty.
     * @param icon The icon to change the color of
     * @param property The property to bind to
     */
    private void colorIconAsDisabledBasedOnProperty(FontIcon icon, BooleanProperty property) {
        // Disallow the user to pick new backend file location for locked backends
        property.addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                icon.setFill(Color.GREY);
            } else {
                icon.setFill(Color.BLACK);
            }
        });
        if (property.getValue()) icon.setFill(Color.GREY);
    }

    /***
     * Sets the backend instance and overrides the current values of the input fields in the GUI.
     * @param instance the new BackendInstance
     */
    public void setBackendInstance(BackendInstance instance) {
        this.backendInstance = instance;

        this.backendName.setText(instance.getName());
        this.isLocal.setSelected(instance.isLocal());
        this.defaultBackendRadioButton.setSelected(instance.isDefault());
        this.threadSafeBackendRadioButton.setSelected(instance.isThreadSafe());

        // Check if the path or the address should be used
        if (isLocal.isSelected()) {
            this.pathToBackend.setText(instance.getBackendLocation());
        } else {
            this.address.setText(instance.getBackendLocation());
        }

        this.portRangeStart.setText(String.valueOf(instance.getPortStart()));
        this.portRangeEnd.setText(String.valueOf(instance.getPortEnd()));
    }

    /**
     * Updates the values of the backend instance to the values from the input fields.
     * @return The updated backend instance
     */
    public BackendInstance updateBackendInstance() {
        backendInstance.setName(backendName.getText());
        backendInstance.setLocal(isLocal.isSelected());
        backendInstance.setDefault(defaultBackendRadioButton.isSelected());
        backendInstance.setIsThreadSafe(threadSafeBackendRadioButton.isSelected());
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
            addressSection.setVisible(false);
            addressSection.setManaged(false);
            pathToBackendSection.setVisible(true);
            pathToBackendSection.setManaged(true);
        } else {
            address.setDisable(false);
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
