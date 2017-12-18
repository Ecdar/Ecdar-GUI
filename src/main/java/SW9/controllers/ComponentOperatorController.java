package SW9.controllers;
import SW9.abstractions.ComponentOperator;
import SW9.abstractions.SystemModel;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Polygon;

/**
 * Controller for ComponentOperators
 */
public class ComponentOperatorController {
    private ComponentOperator operator;
    private SystemModel system;
    public Polygon frame;
    public StackPane root;
    public Label label;

    public ComponentOperator getOperator() { return operator; }

    public void setOperator(final ComponentOperator operator) {
        this.operator = operator;
    }

    public void setSystem(final SystemModel system) {
        this.system = system;
    }

    public SystemModel getSystem() {
        return system;
    }
}
