package SW9.controllers;

import SW9.abstractions.ComponentInstance;
import javafx.scene.layout.BorderPane;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;

/**
 * Controller for a component instance.
 */
public class ComponentInstanceController {
    public BorderPane frame;
    public Line line1;
    public Line line2;
    public Rectangle background;
    private ComponentInstance instance;

    public void setInstance(final ComponentInstance instance) {
        this.instance = instance;
    }

    public ComponentInstance getInstance() {
        return instance;
    }
}
