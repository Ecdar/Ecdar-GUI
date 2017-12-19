package SW9.controllers;

import SW9.abstractions.*;
import SW9.presentations.NailPresentation;
import SW9.presentations.SignatureArrow;
import SW9.presentations.TagPresentation;
import SW9.utility.colors.Color;
import com.jfoenix.controls.JFXTextField;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ListChangeListener;
import javafx.fxml.Initializable;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.function.Consumer;

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
    public VBox inputSignature;
    public VBox outputSignature;

    public Circle inputNailCircle;
    public Label inputNailLabel;
    public Circle outputNailCircle;
    public Label outputNailLabel;
    public Line separatorLine;

    private final ObjectProperty<ComponentInstance> instance = new SimpleObjectProperty<>(null);
    private final ObjectProperty<SystemModel> system = new SimpleObjectProperty<>(null);

    public void initialize(final URL location, final ResourceBundle resources) {
        instance.addListener(((observable, oldInstance, newInstance) -> {
            initializeSignature(newInstance.getComponent());
            nailingIt();
        }));
        nailingIt();
    }

    private void nailingIt() {
        inputNailLabel.setText("?");
        outputNailLabel.setText("!");

        final Component component = instance.get().getComponent();
        final Runnable updateNailColor = () ->
        {
            final Color color = component.getColor();
            final Color.Intensity colorIntensity = component.getColorIntensity();

            inputNailCircle.setFill(color.getColor(colorIntensity));
            inputNailCircle.setStroke(color.getColor(colorIntensity.next(2)));
            outputNailCircle.setFill(color.getColor(colorIntensity));
            outputNailCircle.setStroke(color.getColor(colorIntensity.next(2)));
        };

        inputNailLabel.setTranslateX(-3);
        inputNailLabel.setTranslateY(-8);
        outputNailLabel.setTranslateX(-3);
        outputNailLabel.setTranslateY(-8);

        // When the color of the component updates, update the nail indicator as well
        component.colorProperty().addListener((observable) -> updateNailColor.run());

        // When the color intensity of the component updates, update the nail indicator
        component.colorIntensityProperty().addListener((observable) -> updateNailColor.run());

        // Initialize the color of the nail
        updateNailColor.run();
    }

    /***
     * Inserts the initial edges of the component to the input/output signature
     * @param newComponent The component that should be presented with its signature
     */
    private void initializeSignature(final Component newComponent) {
        newComponent.getOutputStrings().forEach((channel) -> insertSignatureArrow(channel, EdgeStatus.OUTPUT));
        newComponent.getInputStrings().forEach((channel) -> insertSignatureArrow(channel, EdgeStatus.INPUT));
    }

    /***
     * Initialize the listeners, that listen for changes in the input and output edges of the presented component.
     * The view is updated whenever an insert (deletions are also a type of insert) is reported
     * @param newComponent The component that should be presented with its signature
     */
    private void initializeSignatureListeners(final Component newComponent) {
        newComponent.getOutputStrings().addListener((ListChangeListener<String>) c -> {
            // By clearing the container we don't have to fiddle with which elements are removed and added
            outputSignature.getChildren().clear();
            while (c.next()){
                c.getAddedSubList().forEach((channel) -> insertSignatureArrow(channel, EdgeStatus.OUTPUT));
            }
        });

        newComponent.getInputStrings().addListener((ListChangeListener<String>) c -> {
            inputSignature.getChildren().clear();
            while (c.next()){
                c.getAddedSubList().forEach((channel) -> insertSignatureArrow(channel, EdgeStatus.INPUT));
            }
        });
    }

    /***
     * Inserts a new {@link SW9.presentations.SignatureArrow} in the containers for either input or output signature
     * @param channel A String with the channel name that should be shown with the arrow
     * @param status An EdgeStatus for the type of arrow to insert
     */
    private void insertSignatureArrow(final String channel, final EdgeStatus status) {
        //SignatureArrow newArrow = new SignatureArrow(channel, status);
        Label newLabel = new Label(channel);
        newLabel.getStyleClass().add("caption");
        newLabel.setMaxWidth(90); // Limit the length of text on the arrow
        newLabel.setEllipsisString("…");// Inserts … when there's no more room for letters

        if(status == EdgeStatus.INPUT) {
            inputSignature.getChildren().add(newLabel);
        } else {
            outputSignature.getChildren().add(newLabel);
        }
    }

    public ComponentInstance getInstance() {
        return instance.getValue();
    }

    public void setInstance(final ComponentInstance instance) { this.instance.setValue(instance); }

    public void setSystem(final SystemModel system) { this.system.setValue(system); }

    public SystemModel getSystem() {
        return system.getValue();
    }
}
