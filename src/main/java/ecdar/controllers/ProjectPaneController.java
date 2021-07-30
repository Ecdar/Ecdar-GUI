package ecdar.controllers;

import com.jfoenix.controls.JFXTextField;
import ecdar.Ecdar;
import ecdar.abstractions.Component;
import ecdar.abstractions.EcdarSystem;
import ecdar.abstractions.HighLevelModelObject;
import ecdar.mutation.models.MutationTestPlan;
import ecdar.presentations.DropDownMenu;
import ecdar.presentations.FilePresentation;
import ecdar.utility.UndoRedoStack;
import com.jfoenix.controls.JFXPopup;
import com.jfoenix.controls.JFXRippler;
import com.jfoenix.controls.JFXTextArea;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
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
    public JFXRippler createSystem;

    public ImageView createComponentImage;
    public StackPane createComponentPane;
    public ImageView createSystemImage;
    public StackPane createSystemPane;

    private final HashMap<HighLevelModelObject, FilePresentation> modelPresentationMap = new HashMap<>();

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        Ecdar.getProject().getComponents().addListener(new ListChangeListener<Component>() {
            @Override
            public void onChanged(final Change<? extends Component> c) {
                while (c.next()) {
                    c.getAddedSubList().forEach(o -> handleAddedModel(o));
                    c.getRemoved().forEach(o -> handleRemovedModel(o));

                    // Sort the children alphabetically
                    sortPresentations();
                }
            }
        });

        Ecdar.getProject().getComponents().forEach(this::handleAddedModel);

        // Listen to added and removed systems
        Ecdar.getProject().getSystemsProperty().addListener((ListChangeListener<EcdarSystem>) change -> {
            while (change.next()) {
                change.getAddedSubList().forEach(this::handleAddedModel);
                change.getRemoved().forEach(this::handleRemovedModel);

                // Sort the children alphabetically
                sortPresentations();
            }
        });

        initializeMutationTestPlanHandling();
    }

    private void initializeMutationTestPlanHandling() {
        Ecdar.getProject().getTestPlans().addListener((ListChangeListener<MutationTestPlan>) change -> {
            while (change.next()) {
                change.getAddedSubList().forEach(this::handleAddedModel);
                change.getRemoved().forEach(this::handleRemovedModel);

                // Sort the children alphabetically
                sortPresentations();
            }
        });
    }

    private void sortPresentations() {
        final ArrayList<HighLevelModelObject> sortedComponentList = new ArrayList<>();
        modelPresentationMap.keySet().forEach(sortedComponentList::add);
        sortedComponentList.sort((o1, o2) -> o1.getName().compareTo(o2.getName()));
        sortedComponentList.forEach(component -> modelPresentationMap.get(component).toFront());
    }

    private void initializeMoreInformationDropDown(final FilePresentation filePresentation) {
        final JFXRippler moreInformation = (JFXRippler) filePresentation.lookup("#moreInformation");
        final DropDownMenu moreInformationDropDown = new DropDownMenu(moreInformation);
        final HighLevelModelObject model = filePresentation.getModel();

        // If component, added toggle for periodic check
        if (model instanceof Component) {
            moreInformationDropDown.addListElement("Configuration");

            initializeTogglePeriodicCheck(moreInformationDropDown, (Component) model);

            moreInformationDropDown.addSpacerElement();
        }

        if (model instanceof Component || model instanceof EcdarSystem) {
            moreInformationDropDown.addListElement("Description");

            final JFXTextArea textArea = new JFXTextArea();
            textArea.setMinHeight(30);

            if (model instanceof Component)
                textArea.textProperty().bindBidirectional(((Component) model).descriptionProperty());
            else
                textArea.textProperty().bindBidirectional(((EcdarSystem) model).getDescriptionProperty());

            textArea.textProperty().addListener((obs, oldText, newText) -> {
                int i = 0;
                for (final char c : newText.toCharArray()) {
                    if (c == '\n') {
                        i++;
                    }
                }
                textArea.setMinHeight(i * 17 + 30);
            });

            moreInformationDropDown.addCustomElement(textArea);
            moreInformationDropDown.addSpacerElement();
        }

        // Add color picker
        if (model instanceof Component) {
            moreInformationDropDown.addColorPicker(
                    filePresentation.getModel(),
                    ((Component) filePresentation.getModel())::dye
            );
        } else if (model instanceof EcdarSystem) {
            moreInformationDropDown.addColorPicker(
                    filePresentation.getModel(),
                    ((EcdarSystem) filePresentation.getModel())::dye
            );
        }

        // Delete button for components
        if (model instanceof Component) {
            moreInformationDropDown.addSpacerElement();
            moreInformationDropDown.addClickableListElement("Delete", event -> {
                UndoRedoStack.pushAndPerform(() -> { // Perform
                    Ecdar.getProject().getComponents().remove(model);
                }, () -> { // Undo
                    Ecdar.getProject().getComponents().add((Component) model);
                }, "Deleted component " + model.getName(), "delete");
                moreInformationDropDown.hide();
            });
        }

        // Delete button for systems
        if (model instanceof EcdarSystem) {
            moreInformationDropDown.addSpacerElement();
            moreInformationDropDown.addClickableListElement("Delete", event -> {
                UndoRedoStack.pushAndPerform(() -> { // Perform
                    Ecdar.getProject().getSystemsProperty().remove(model);
                }, () -> { // Undo
                    Ecdar.getProject().getSystemsProperty().add((EcdarSystem) model);
                }, "Deleted system " + model.getName(), "delete");
                moreInformationDropDown.hide();
            });
        }



        if (model instanceof MutationTestPlan) {
            moreInformationDropDown.addListElement("Name");

            final JFXTextField textField = new JFXTextField();
            textField.textProperty().bindBidirectional(model.nameProperty());
            textField.setPadding(new Insets(0, 10, 0, 10));
            moreInformationDropDown.addCustomElement(textField);

            moreInformationDropDown.addSpacerElement();

            // Delete button for test plan
            moreInformationDropDown.addClickableListElement("Delete", event -> {
                UndoRedoStack.pushAndPerform(() -> { // Perform
                    Ecdar.getProject().getTestPlans().remove(model);
                }, () -> { // Undo
                    Ecdar.getProject().getTestPlans().add((MutationTestPlan) model);
                }, "Deleted test plan " + model.getName(), "delete");
                moreInformationDropDown.hide();
            });
        }

        moreInformation.setOnMousePressed((e) -> {
            e.consume();
            moreInformationDropDown.show(JFXPopup.PopupVPosition.TOP, JFXPopup.PopupHPosition.LEFT, 20, 20);
        });
    }

    private void initializeTogglePeriodicCheck(DropDownMenu moreInformationDropDown, final Component component) {
        moreInformationDropDown.addToggleableListElement("Include in periodic check", component.includeInPeriodicCheckProperty(), event -> {
            final boolean didIncludeInPeriodicCheck = component.includeInPeriodicCheckProperty().get();

            UndoRedoStack.pushAndPerform(() -> { // Perform
                component.includeInPeriodicCheckProperty().set(!didIncludeInPeriodicCheck);
            }, () -> { // Undo
                component.includeInPeriodicCheckProperty().set(didIncludeInPeriodicCheck);
            }, "Component " + component.getName() + " is included in periodic check: " + !didIncludeInPeriodicCheck, "search");
            moreInformationDropDown.hide();
        });
    }

    private void handleAddedModel(final HighLevelModelObject model) {
        final FilePresentation filePresentation = new FilePresentation(model);

        initializeMoreInformationDropDown(filePresentation);

        filesList.getChildren().add(filePresentation);
        modelPresentationMap.put(model, filePresentation);

        // Open the component if the presentation is pressed
        filePresentation.setOnMousePressed(event -> {
            event.consume();
            EcdarController.getActiveCanvasPresentation().getController().setActiveModel(model);
            updateColorsOnFilePresentations();
        });
        model.nameProperty().addListener(obs -> sortPresentations());
    }

    private void handleRemovedModel(final HighLevelModelObject model) {
        filesList.getChildren().remove(modelPresentationMap.get(model));
        modelPresentationMap.remove(model);


        // If we remove the model active on the canvas
        if (EcdarController.getActiveCanvasPresentation().getController().getActiveModel() == model) {
            if (Ecdar.getProject().getComponents().size() > 0) {
                // Find the first available component and show it instead of the removed one
                final Component component = Ecdar.getProject().getComponents().get(0);
                EcdarController.getActiveCanvasPresentation().getController().setActiveModel(component);
                updateColorsOnFilePresentations();
            } else {
                // Show no components (since there are none in the project)
                EcdarController.getActiveCanvasPresentation().getController().setActiveModel(null);
            }
        }
    }

    /**
     * Update the color of all FilePresentations to display currently active components
     */
    public void updateColorsOnFilePresentations() {
        for (Node child : filesList.getChildren()) {
            if (child instanceof FilePresentation) {
                ((FilePresentation) child).updateColors();
            }
        }
    }

    /**
     * Method for creating a new component
     */
    @FXML
    private void createComponentClicked() {
        final Component newComponent = new Component(true);

        UndoRedoStack.pushAndPerform(() -> { // Perform
            Ecdar.getProject().getComponents().add(newComponent);
        }, () -> { // Undo
            Ecdar.getProject().getComponents().remove(newComponent);
        }, "Created new component: " + newComponent.getName(), "add-circle");

        EcdarController.getActiveCanvasPresentation().getController().setActiveModel(newComponent);
        updateColorsOnFilePresentations();
    }

    /**
     * Method for creating a new system
     */
    @FXML
    private void createSystemClicked() {
        final EcdarSystem newSystem = new EcdarSystem();

        UndoRedoStack.pushAndPerform(() -> { // Perform
            Ecdar.getProject().getSystemsProperty().add(newSystem);
        }, () -> { // Undo
            Ecdar.getProject().getSystemsProperty().remove(newSystem);
        }, "Created new system: " + newSystem.getName(), "add-circle");

        EcdarController.getActiveCanvasPresentation().getController().setActiveModel(newSystem);
    }
}
