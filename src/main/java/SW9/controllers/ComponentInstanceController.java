package SW9.controllers;

import SW9.abstractions.*;
import com.jfoenix.controls.JFXTextField;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import SW9.Ecdar;
import SW9.presentations.DropDownMenu;
import SW9.presentations.MenuElement;
import com.jfoenix.controls.JFXPopup;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Controller for a component instance.
 */
public class ComponentInstanceController implements Initializable {
    final int MAX_LABELS = 4; // The max number of labels to show in the signature containers

    public BorderPane frame;
    public Line line1;
    public Rectangle background;
    public Label originalComponentLabel;
    public JFXTextField identifier;
    public StackPane root;
    public HBox toolbar;

    public VBox inputSignature;
    public VBox outputSignature;
    public Label inputOverflowLabel;
    public Label outputOverflowLabel;
    public VBox outputContainer;
    public VBox inputContainer;
    public Line separatorLine;

    public Circle inputNailCircle;
    public Label inputNailLabel;
    public Group inputNailGroup;
    public Circle outputNailCircle;
    public Label outputNailLabel;
    public Group outputNailGroup;

    private final ObjectProperty<ComponentInstance> instance = new SimpleObjectProperty<>(null);
    private final ObjectProperty<EcdarSystem> system = new SimpleObjectProperty<>(null);

    private final BooleanProperty hasEdge = new SimpleBooleanProperty(false);
    private DropDownMenu dropDownMenu;

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        system.addListener(((observable, oldValue, newValue) -> {
            initializeDropDownMenu(newValue);
        }));

        instance.addListener(
                ((observable, oldInstance, newInstance) -> initializeSignature(newInstance.getComponent())));
    }

    private void initializeDropDownMenu(final EcdarSystem system) {
        dropDownMenu = new DropDownMenu(frame);

        dropDownMenu.addMenuElement(new MenuElement("Draw Edge")
                .setClickable(() -> {
                    createNewSystemEdge();
                    dropDownMenu.hide();
                })
                .setDisableable(hasEdge));

        dropDownMenu.addSpacerElement();

        dropDownMenu.addClickableListElement("Delete", event -> {
            getSystem().removeComponentInstance(getInstance());
            dropDownMenu.hide();
        });
    }

    /**
     * Listens to an edge to update whether the root has an edge.
     * @param edge the edge to update with
     */
    private void handleHasEdge(final EcdarSystemEdge edge) {
        edge.getTempNodeProperty().addListener((observable -> updateHasEdge(edge)));
        edge.getChildProperty().addListener((observable -> updateHasEdge(edge)));
        edge.getParentProperty().addListener((observable -> updateHasEdge(edge)));
    }

    /**
     * Update has edge property to whether the instance is in a given edge.
     * @param edge the given edge
     */
    private void updateHasEdge(final EcdarSystemEdge edge) {
        hasEdge.set(edge.isInEdge(getInstance()));
    }

    @FXML
    private void onMouseClicked(final MouseEvent event) {
        event.consume();

        final EcdarSystemEdge unfinishedEdge = getSystem().getUnfinishedEdge();

        if ((event.isShiftDown() && event.getButton().equals(MouseButton.PRIMARY)) || event.getButton().equals(MouseButton.MIDDLE)) {
            // If shift click or middle click a component instance, create a new edge

            // Component instance must not already have an edge and there cannot be any other unfinished edges in the system
            if(!hasEdge.get() && unfinishedEdge == null) {
                createNewSystemEdge();
            }
        }

        // if primary clicked and there is an unfinished edge, finish it with the component instance as target
        if (unfinishedEdge != null && event.getButton().equals(MouseButton.PRIMARY)) {
            // If already has edge, give error
            if (hasEdge.get()) {
                Ecdar.showToast("This component instance already has an edge.");
                return;
            }

            final boolean succeeded = unfinishedEdge.tryFinishWithComponentInstance(getInstance());
            if (succeeded) {
                hasEdge.set(true);
                handleHasEdge(unfinishedEdge);
            }
            return;
        }

        if (event.getButton().equals(MouseButton.SECONDARY)) {
            dropDownMenu.show(JFXPopup.PopupVPosition.TOP, JFXPopup.PopupHPosition.LEFT, 20, 20);
        }
    }

    /***
     * Helper method to create a new EcdarSystemEdge and add it to the current system and component instance
     * @return The newly created EcdarSystemEdge
     */
    private EcdarSystemEdge createNewSystemEdge() {
        final EcdarSystemEdge edge = new EcdarSystemEdge(getInstance());
        getSystem().addEdge(edge);
        hasEdge.set(true);
  
        // Update state when edge child and parent changes
        handleHasEdge(edge);
        return edge;
    }

    /***
     * Inserts the signature labels for input and output
     * @param newComponent The component that should be presented with its signature
     */
    private void initializeSignature(final Component newComponent) {
        insertSignature(newComponent.getInputStrings(), EdgeStatus.INPUT);
        insertSignature(newComponent.getOutputStrings(), EdgeStatus.OUTPUT);
    }

    /***
     * Helper method that iterates over a list of channels and updates overflow label
     * @param channels List of Strings with channels names to insert
     * @param status The EdgeStatus indicating which container (input/output) should be updated
     */
    private void insertSignature(List<String> channels, EdgeStatus status) {
        // Choose overflow label depending on edge status
        final Label overflowLabel = status == EdgeStatus.INPUT ? inputOverflowLabel : outputOverflowLabel;

        // Update the overflow label if needed, otherwise hide it
        if(channels.size() > MAX_LABELS) {
            int currentOverflow = channels.size() - MAX_LABELS;
            overflowLabel.setText(overflowText(currentOverflow));
            overflowLabel.setOpacity(1);
        } else {
            overflowLabel.setOpacity(0);
        }

        // Only add MAX_LABELS to the signature unless there are fewer channels than MAX_LABELS
        int limit = Math.min(MAX_LABELS, channels.size());
        for (int i = 0; i < limit; i++) {
            insertSignatureLabel(channels.get(i), status);
        }
    }

    /***
     * Create a String for the overflow labels, showing how much is hidden
     * @param count An integer with the count to show in the label
     * @return A String with text for overflow labels
     */
    private String overflowText(int count) {
        return "︙ (" + "+" + count + ")";
    }

    /***
     * Inserts a new Label in the containers for either input or output signature
     * @param channel A String with the channel name that should be shown
     * @param status An EdgeStatus for the type of arrow to insert
     */
    private void insertSignatureLabel(final String channel, final EdgeStatus status) {
        Label newLabel = new Label(channel);
        newLabel.getStyleClass().add("caption");
        newLabel.setAlignment(Pos.CENTER);
        newLabel.setMaxWidth(100); // Limit the length of text
        newLabel.setEllipsisString("…");// Inserts … when there's no more room for letters

        if(status == EdgeStatus.INPUT) {
            inputSignature.getChildren().add(newLabel);
        } else {
            outputSignature.getChildren().add(newLabel);
        }
    }

    public ComponentInstance getInstance() { return instance.getValue(); }

    public void setInstance(final ComponentInstance instance) { this.instance.setValue(instance); }

    public void setSystem(final EcdarSystem system) { this.system.set(system); }

    public EcdarSystem getSystem() { return system.get(); }
}
