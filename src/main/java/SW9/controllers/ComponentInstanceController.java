package SW9.controllers;

import SW9.abstractions.ComponentInstance;
import SW9.abstractions.SystemModel;
import com.jfoenix.controls.JFXTextField;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Controller for a component instance.
 */
public class ComponentInstanceController implements Initializable {
    public BorderPane frame;
    public Line line1;
    public Rectangle background;
    public Label originalComponentLabel;
    public JFXTextField identifier;
    public StackPane root;
    public HBox toolbar;

    private ComponentInstance instance;
    private SystemModel system;

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
    }

    public ComponentInstance getInstance() {
        return instance;
    }

    public void setInstance(final ComponentInstance instance) {
        this.instance = instance;
    }

    public void setSystem(final SystemModel system) {
        this.system = system;
    }

    public SystemModel getSystem() {
        return system;
    }
}
