package SW9.controllers;

import SW9.abstractions.Box;
import SW9.abstractions.HighLevelModelObject;
import SW9.utility.mouse.MouseTracker;
import com.jfoenix.controls.JFXTextField;
import javafx.beans.property.ObjectProperty;
import javafx.fxml.Initializable;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import org.fxmisc.richtext.LineNumberFactory;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static SW9.presentations.Grid.GRID_SIZE;

/**
 * Controller for a high level model, such as a component or a system.
 */
public abstract class ModelController {
    public StackPane root;
    public Rectangle background;
    public BorderPane frame;
    public Rectangle rightAnchor;
    public Rectangle bottomAnchor;
    public Line topLeftLine;
    public BorderPane toolbar;
    public JFXTextField name;

    public abstract HighLevelModelObject getModel();

    /**
     * Initializes this.
     * @param box the box of the model
     */
    void initialize(final Box box) {
    }

    /**
     * Hides the border and background.
     */
    void hideBorderAndBackground() {
        frame.setVisible(false);
        topLeftLine.setVisible(false);
        background.setVisible(false);
    }

    /**
     * Shows the border and background.
     */
    void showBorderAndBackground() {
        frame.setVisible(true);
        topLeftLine.setVisible(true);
        background.setVisible(true);
    }
}
