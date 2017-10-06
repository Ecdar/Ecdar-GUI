package SW9.controllers;

import SW9.Ecdar;
import SW9.abstractions.Component;
import SW9.abstractions.Edge;
import SW9.abstractions.Jork;
import SW9.abstractions.SubComponent;
import SW9.code_analysis.CodeAnalysis;
import SW9.code_analysis.Nearable;
import SW9.presentations.CanvasPresentation;
import SW9.presentations.ComponentPresentation;
import SW9.presentations.DropDownMenu;
import SW9.presentations.LocationPresentation;
import SW9.utility.UndoRedoStack;
import SW9.utility.colors.Color;
import SW9.utility.helpers.ItemDragHelper;
import SW9.utility.helpers.SelectHelper;
import SW9.utility.keyboard.Keybind;
import SW9.utility.keyboard.KeyboardTracker;
import SW9.utility.keyboard.NudgeDirection;
import SW9.utility.keyboard.Nudgeable;
import com.jfoenix.controls.JFXPopup;
import com.jfoenix.controls.JFXTextField;
import javafx.beans.InvalidationListener;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableDoubleValue;
import javafx.collections.ListChangeListener;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;

import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.function.Consumer;

public class SubComponentController implements Initializable, SelectHelper.ItemSelectable, Nudgeable {

    private final ObjectProperty<SubComponent> subComponent = new SimpleObjectProperty<>(null);
    private final ObjectProperty<Component> parentComponent = new SimpleObjectProperty<>(null);

    public HBox toolbar;
    public Rectangle background;
    public BorderPane frame;
    public JFXTextField identifier;
    public Label originalComponentLabel;
    public StackPane root;
    public Line line1;
    public Line line2;
    public Pane defaultLocationsContainer;
    public Label description;
    private DropDownMenu dropDownMenu;
    private boolean dropDownMenuInitialized = false;

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        makeDraggable();

        initializeSelectListener();
    }

    private void initializeDropDownMenu() {
        if (dropDownMenuInitialized) return;
        dropDownMenuInitialized = true;

        dropDownMenu = new DropDownMenu(((Pane) root.getParent().getParent().getParent()), root, 230, true);

        dropDownMenu.addClickableListElement("Open in canvas", event -> {
            CanvasController.setActiveVerificationObject(getSubComponent().getComponent());
        });


        dropDownMenu.addClickableListElement("Draw edge",
                (event) -> {
                    final Edge newEdge = new Edge(getSubComponent(), EcdarController.getGlobalEdgeStatus());

                    KeyboardTracker.registerKeybind(KeyboardTracker.ABANDON_EDGE, new Keybind(new KeyCodeCombination(KeyCode.ESCAPE), () -> {
                        getParentComponent().removeEdge(newEdge);
                        UndoRedoStack.forgetLast();
                    }));

                    UndoRedoStack.pushAndPerform(() -> { // Perform
                        getParentComponent().addEdge(newEdge);
                    }, () -> { // Undo
                        getParentComponent().removeEdge(newEdge);
                    }, "Created edge starting from " + getSubComponent(), "add-circle");

                    dropDownMenu.close();
                }
        );

        dropDownMenu.addSpacerElement();

        final DropDownMenu subMenu = new DropDownMenu(((Pane) root.getParent().getParent().getParent()), root, 150, false);

        dropDownMenu.addSubMenu("Update Subcomponent", subMenu, 2 * 35 + 15 + 1);

        dropDownMenu.addSpacerElement();

        dropDownMenu.addClickableListElement("Delete", mouseEvent -> {
            dropDownMenu.close();

            final SubComponent subcomponent = getSubComponent();
            final Component parentComponent = getParentComponent();

            final List<Edge> relatedEdges = parentComponent.getRelatedEdges(subcomponent);

            UndoRedoStack.pushAndPerform(() -> { // Perform
                parentComponent.removeSubComponent(subcomponent);
                relatedEdges.forEach(parentComponent::removeEdge);
            }, () -> { // Undo
                parentComponent.addSubComponent(subcomponent);
                relatedEdges.forEach(parentComponent::addEdge);
            }, "Deleted subcomponent " + subcomponent, "delete");
        });
    }

    private void initializeSelectListener() {
        SelectHelper.elementsToBeSelected.addListener(new ListChangeListener<Nearable>() {
            @Override
            public void onChanged(final Change<? extends Nearable> c) {
                while (c.next()) {
                    if (c.getAddedSize() == 0) return;

                    for (final Nearable nearable : SelectHelper.elementsToBeSelected) {
                        if (nearable instanceof SubComponent) {
                            if (nearable.equals(getSubComponent())) {
                                SelectHelper.addToSelection(SubComponentController.this);
                                break;
                            }
                        }
                    }
                }
            }
        });
    }

    private void makeDraggable() {

        root.addEventHandler(MouseEvent.MOUSE_PRESSED, event -> {
            event.consume();

            final Edge unfinishedEdge = getParentComponent().getUnfinishedEdge();

            if ((event.isPrimaryButtonDown() || event.isMiddleButtonDown()) && unfinishedEdge != null) {
                unfinishedEdge.setTargetSubComponent(getSubComponent());

            } else if ((event.isShiftDown() && event.isPrimaryButtonDown()) || event.isMiddleButtonDown()) {

                final Edge newEdge = new Edge(getSubComponent(), EcdarController.getGlobalEdgeStatus());

                KeyboardTracker.registerKeybind(KeyboardTracker.ABANDON_EDGE, new Keybind(new KeyCodeCombination(KeyCode.ESCAPE), () -> {
                    getParentComponent().removeEdge(newEdge);
                    UndoRedoStack.forgetLast();
                }));

                UndoRedoStack.pushAndPerform(() -> { // Perform
                    getParentComponent().addEdge(newEdge);
                }, () -> { // Undo
                    getParentComponent().removeEdge(newEdge);
                }, "Created edge starting from subcomponent " + getSubComponent().getIdentifier(), "add-circle");
            } else if (event.isSecondaryButtonDown() && unfinishedEdge == null) {
                initializeDropDownMenu();
                dropDownMenu.show(JFXPopup.PopupVPosition.TOP, JFXPopup.PopupHPosition.LEFT, event.getX() - 5, event.getY() - 5);
            } else if(event.isPrimaryButtonDown()) {
                // If the sub component is pressed twice open its corresponding component in the canvas
                if(event.getClickCount() > 1) {
                    CanvasController.setActiveVerificationObject(getSubComponent().getComponent());
                } else {
                    if (event.isShortcutDown()) {
                        SelectHelper.addToSelection(this);
                    } else {
                        SelectHelper.select(this);
                    }
                }
            }
        });

        ItemDragHelper.makeDraggable(root, this::getDragBounds);
    }

    public SubComponent getSubComponent() {
        return subComponent.get();
    }

    public void setSubComponent(final SubComponent subComponent) {
        this.subComponent.set(subComponent);
    }

    public ObjectProperty<SubComponent> subComponentProperty() {
        return subComponent;
    }

    public Component getParentComponent() {
        return parentComponent.get();
    }

    public void setParentComponent(final Component parentComponent) {
        this.parentComponent.set(parentComponent);
    }

    public ObjectProperty<Component> parentComponentProperty() {
        return parentComponent;
    }

    @Override
    public void color(final Color color, final Color.Intensity intensity) {
        // Cannot be colored
    }

    @Override
    public Color getColor() {
        return null;
    }

    @Override
    public Color.Intensity getColorIntensity() {
        return null;
    }

    @Override
    public ItemDragHelper.DragBounds getDragBounds() {
        final ObservableDoubleValue minX = new SimpleDoubleProperty(CanvasPresentation.GRID_SIZE);
        final ObservableDoubleValue maxX = getParentComponent().widthProperty().subtract(getSubComponent().widthProperty().add(CanvasPresentation.GRID_SIZE));
        final ObservableDoubleValue minY = new SimpleDoubleProperty(ComponentPresentation.TOOL_BAR_HEIGHT + CanvasPresentation.GRID_SIZE);
        final ObservableDoubleValue maxY = getParentComponent().heightProperty().subtract(getSubComponent().heightProperty().add(CanvasPresentation.GRID_SIZE));
        return new ItemDragHelper.DragBounds(minX, maxX, minY, maxY);
    }

    @Override
    public void select() {
        ((SelectHelper.Selectable) root).select();

        defaultLocationsContainer.getChildren().forEach(node -> ((LocationPresentation) node).select());
    }

    @Override
    public void deselect() {
        ((SelectHelper.Selectable) root).deselect();

        defaultLocationsContainer.getChildren().forEach(node -> ((LocationPresentation) node).deselect());
    }

    @Override
    public DoubleProperty xProperty() {
        return root.layoutXProperty();
    }

    @Override
    public DoubleProperty yProperty() {
        return root.layoutYProperty();
    }

    @Override
    public double getX() {
        return xProperty().get();
    }

    @Override
    public double getY() {
        return yProperty().get();
    }

    @Override
    public boolean nudge(final NudgeDirection direction) {

        final double oldX = root.getLayoutX();
        final double newX = getDragBounds().trimX(root.getLayoutX() + direction.getXOffset());
        root.layoutXProperty().set(newX);

        final double oldY = root.getLayoutY();
        final double newY = getDragBounds().trimY(root.getLayoutY() + direction.getYOffset());
        root.layoutYProperty().set(newY);

        return oldX != newX || oldY != newY;
    }
}
