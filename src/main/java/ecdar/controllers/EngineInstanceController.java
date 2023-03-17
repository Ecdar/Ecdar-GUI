package ecdar.controllers;

import com.jfoenix.controls.JFXCheckBox;
import com.jfoenix.controls.JFXRippler;
import com.jfoenix.controls.JFXTextField;
import ecdar.backend.Engine;
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

public class EngineInstanceController implements Initializable {
    private Engine engine = new Engine();

    /* Design elements */
    public Label engineNameIssue;
    public JFXRippler removeEngineRippler;
    public FontIcon removeEngineIcon;
    public FontIcon expansionIcon;
    public StackPane content;
    public JFXCheckBox isLocal;
    public HBox addressSection;
    public HBox pathToEngineSection;
    public JFXRippler pickPathToEngine;
    public FontIcon pickPathToEngineIcon;
    public StackPane moveEngineInstanceUpRippler;
    public StackPane moveEngineInstanceDownRippler;

    // Labels for showing potential issues
    public Label locationIssue;
    public Label portRangeStartIssue;
    public Label portRangeEndIssue;
    public Label portRangeIssue;

    /* Input fields */
    public JFXTextField engineName;
    public JFXTextField address;
    public JFXTextField pathToEngine;
    public JFXTextField portRangeStart;
    public JFXTextField portRangeEnd;
    public RadioButton defaultEngineRadioButton;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        Platform.runLater(() -> {
            this.handleLocalPropertyChanged();
            moveEngineInstanceUpRippler.setCursor(Cursor.HAND);
            moveEngineInstanceDownRippler.setCursor(Cursor.HAND);
            setHGrow();

            colorIconAsDisabledBasedOnProperty(removeEngineIcon, defaultEngineRadioButton.selectedProperty());
            colorIconAsDisabledBasedOnProperty(pickPathToEngineIcon, engine.getLockedProperty());
        });
    }

    /**
     * Binds the color of the given icon to the value of BooleanProperty.
     * @param icon The icon to change the color of
     * @param property The property to bind to
     */
    private void colorIconAsDisabledBasedOnProperty(FontIcon icon, BooleanProperty property) {
        // Disallow the user to pick new engine file location for locked engines
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
     * Sets the engine instance and overrides the current values of the input fields in the GUI.
     * @param instance the new Engine
     */
    public void setEngine(Engine instance) {
        this.engine = instance;

        this.engineName.setText(instance.getName());
        this.isLocal.setSelected(instance.isLocal());
        this.defaultEngineRadioButton.setSelected(instance.isDefault());

        // Check if the path or the address should be used
        if (isLocal.isSelected()) {
            this.pathToEngine.setText(instance.getEngineLocation());
        } else {
            this.address.setText(instance.getIpAddress());
        }

        this.portRangeStart.setText(String.valueOf(instance.getPortStart()));
        this.portRangeEnd.setText(String.valueOf(instance.getPortEnd()));
    }

    /**
     * Updates the values of the engine instance to the values from the input fields.
     * @return The updated engine instance
     */
    public Engine updateEngineInstance() {
        engine.setName(engineName.getText());
        engine.setLocal(isLocal.isSelected());
        engine.setDefault(defaultEngineRadioButton.isSelected());
        engine.setEngineLocation(isLocal.isSelected() ? pathToEngine.getText() : address.getText());
        engine.setPortStart(Integer.parseInt(portRangeStart.getText()));
        engine.setPortEnd(Integer.parseInt(portRangeEnd.getText()));

        return engine;
    }

    private void setHGrow() {
        HBox.setHgrow(engineName.getParent().getParent().getParent(), Priority.ALWAYS);
        HBox.setHgrow(engineName.getParent(), Priority.ALWAYS);
        HBox.setHgrow(engineName, Priority.ALWAYS);
        HBox.setHgrow(content, Priority.ALWAYS);
        HBox.setHgrow(addressSection, Priority.ALWAYS);
        HBox.setHgrow(address, Priority.ALWAYS);
        HBox.setHgrow(pathToEngineSection, Priority.ALWAYS);
        HBox.setHgrow(pathToEngine, Priority.ALWAYS);
        HBox.setHgrow(portRangeStart, Priority.ALWAYS);
        HBox.setHgrow(portRangeEnd, Priority.ALWAYS);
    }

    private void handleLocalPropertyChanged() {
        if (isLocal.isSelected()) {
            address.setDisable(true);
            addressSection.setVisible(false);
            addressSection.setManaged(false);
            pathToEngineSection.setVisible(true);
            pathToEngineSection.setManaged(true);
        } else {
            address.setDisable(false);
            addressSection.setVisible(true);
            addressSection.setManaged(true);
            pathToEngineSection.setVisible(false);
            pathToEngineSection.setManaged(false);
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
    private void openPathToEngineDialog() {
        // Dialog title
        final FileChooser enginePicker = new FileChooser();
        enginePicker.setTitle("Choose Engine");

        // The initial location for the file choosing dialog
        final File jarDir = new File(pathToEngine.getText()).getAbsoluteFile().getParentFile();

        // If the file does not exist, we must be running it from a development environment, use a default location
        if(jarDir.exists()) {
            enginePicker.setInitialDirectory(jarDir);
        }

        // Prompt the user to find a file (will halt the UI thread)
        final File file = enginePicker.showOpenDialog(null);
        if(file != null) {
            pathToEngine.setText(file.getAbsolutePath());
        }
    }
}
