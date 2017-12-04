package SW9.controllers;

import SW9.Ecdar;
import SW9.abstractions.Component;
import SW9.abstractions.HighLevelModelObject;
import SW9.presentations.DropDownMenu;
import SW9.presentations.FilePresentation;
import SW9.utility.UndoRedoStack;
import com.jfoenix.controls.JFXPopup;
import com.jfoenix.controls.JFXRippler;
import com.jfoenix.controls.JFXTextArea;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.ResourceBundle;

public class ProjectPaneController implements Initializable {
    public StackPane root;
    public AnchorPane toolbar;
    public Label toolbarTitle;
    public ScrollPane scrollPane;
    public VBox filesList;
    public JFXRippler createComponent;

    private final HashMap<Component, FilePresentation> componentPresentationMap = new HashMap<>();

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        // Bind global declarations and add mouse event
        final FilePresentation globalDclPresentation = new FilePresentation(Ecdar.getProject().getGlobalDeclarations());
        globalDclPresentation.setOnMousePressed(event -> {
            event.consume();
            CanvasController.setActiveModel(Ecdar.getProject().getGlobalDeclarations());
        });
        filesList.getChildren().add(globalDclPresentation);

        // Bind system declarations and add mouse event
        final FilePresentation systemDclPresentation = new FilePresentation(Ecdar.getProject().getSystemDeclarations());
        systemDclPresentation.setOnMousePressed(event -> {
            event.consume();
            CanvasController.setActiveModel(Ecdar.getProject().getSystemDeclarations());
        });
        filesList.getChildren().add(systemDclPresentation);

        Ecdar.getProject().getComponents().addListener(new ListChangeListener<Component>() {
            @Override
            public void onChanged(final Change<? extends Component> c) {
                while (c.next()) {
                    c.getAddedSubList().forEach(o -> handleAddedComponent(o));
                    c.getRemoved().forEach(o -> handleRemovedComponent(o));

                    // We should make a new component active
                    if (c.getRemoved().size() > 0) {
                        if (Ecdar.getProject().getComponents().size() > 0) {
                            // Find the first available component and show it instead of the removed one
                            final Component component = Ecdar.getProject().getComponents().get(0);
                            CanvasController.setActiveModel(component);
                        } else {
                            // Show no components (since there are none in the project)
                            CanvasController.setActiveModel(null);
                        }
                    }
                    // Sort the children alphabetically
                    sortPresentations();
                }
            }
        });

        Ecdar.getProject().getComponents().forEach(this::handleAddedComponent);
    }

    private void sortPresentations() {
        final ArrayList<Component> sortedComponentList = new ArrayList<>();
        componentPresentationMap.keySet().forEach(sortedComponentList::add);
        sortedComponentList.sort((o1, o2) -> o1.getName().compareTo(o2.getName()));
        sortedComponentList.forEach(component -> componentPresentationMap.get(component).toFront());
    }

    private void initializeColorSelector(final FilePresentation filePresentation) {
        final JFXRippler moreInformation = (JFXRippler) filePresentation.lookup("#moreInformation");
        final int listWidth = 230;
        final DropDownMenu moreInformationDropDown = new DropDownMenu(root, moreInformation, listWidth, true);
        final HighLevelModelObject model = filePresentation.getModel();

        moreInformationDropDown.addListElement("Configuration");

        initializeTogglePeriodicCheck(moreInformationDropDown, (Component) model);

        moreInformationDropDown.addSpacerElement();

        moreInformationDropDown.addListElement("Description");

        final JFXTextArea textArea = new JFXTextArea();
        textArea.setMinHeight(30);

        if (model instanceof Component) {
            ((Component) model).descriptionProperty().bindBidirectional(textArea.textProperty());
        }

        textArea.textProperty().addListener((obs, oldText, newText) -> {
            int i = 0;
            for (final char c : newText.toCharArray()) {
                if (c == '\n') {
                    i++;
                }
            }

            textArea.setMinHeight(i * 17 + 30);
        });

        moreInformationDropDown.addCustomChild(textArea);

        moreInformationDropDown.addSpacerElement();

        // Color picker button
        if (model instanceof Component) {
            moreInformationDropDown.addColorPicker((Component) filePresentation.getModel(),
                    ((Component) filePresentation.getModel())::dye);

            moreInformationDropDown.addSpacerElement();

            // Delete button
            moreInformationDropDown.addClickableListElement("Delete", event -> {
                UndoRedoStack.pushAndPerform(() -> { // Perform
                    Ecdar.getProject().getComponents().remove(model);
                }, () -> { // Undo
                    Ecdar.getProject().getComponents().add((Component) model);
                }, "Deleted component " + model.getName(), "delete");

                moreInformationDropDown.close();
            });
        }

        moreInformation.setOnMousePressed((e) -> {
            e.consume();
            moreInformationDropDown.show(JFXPopup.PopupVPosition.TOP, JFXPopup.PopupHPosition.LEFT, 10, 10);
        });
    }

    private void initializeTogglePeriodicCheck(DropDownMenu moreInformationDropDown, final Component component) {
        moreInformationDropDown.addTogglableListElement("Include in periodic check", component.includeInPeriodicCheckProperty(), event -> {
            final boolean didIncludeInPeriodicCheck = component.includeInPeriodicCheckProperty().get();

            UndoRedoStack.pushAndPerform(() -> { // Perform
                component.includeInPeriodicCheckProperty().set(!didIncludeInPeriodicCheck);
            }, () -> { // Undo
                component.includeInPeriodicCheckProperty().set(didIncludeInPeriodicCheck);
            }, "Component " + component.getName() + " is included in periodic check: " + !didIncludeInPeriodicCheck, "search");
        });
    }

    private void handleAddedComponent(final Component component) {
        final FilePresentation filePresentation = new FilePresentation(component);
        initializeColorSelector(filePresentation);
        filesList.getChildren().add(filePresentation);
        componentPresentationMap.put(component, filePresentation);

        // Open the component if the presentation is pressed
        filePresentation.setOnMousePressed(event -> {
            event.consume();
            CanvasController.setActiveModel(component);
        });
        component.nameProperty().addListener(obs -> sortPresentations());
    }

    private void handleRemovedComponent(final Component component) {
        filesList.getChildren().remove(componentPresentationMap.get(component));
        componentPresentationMap.remove(component);
    }

    @FXML
    private void createComponentClicked() {
        final Component newComponent = new Component(true);

        UndoRedoStack.pushAndPerform(() -> { // Perform
            Ecdar.getProject().getComponents().add(newComponent);
        }, () -> { // Undo
            Ecdar.getProject().getComponents().remove(newComponent);
        }, "Created new component: " + newComponent.getName(), "add-circle");

        CanvasController.setActiveModel(newComponent);
    }

}
