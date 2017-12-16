package SW9.controllers;

import SW9.abstractions.ComponentInstance;
import SW9.abstractions.SystemModel;
import com.jfoenix.controls.JFXTextField;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;

/**
 * Controller for a component instance.
 */
public class ComponentInstanceController {
    public BorderPane frame;
    public Line line1;
    public Rectangle background;
    public Label originalComponentLabel;
    public JFXTextField identifier;
    public StackPane root;
    public HBox toolbar;

    private ComponentInstance instance;
    private SystemModel system;

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
