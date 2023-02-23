package ecdar.controllers;

import com.jfoenix.controls.JFXTextField;
import ecdar.Ecdar;
import ecdar.abstractions.Component;
import ecdar.abstractions.EcdarSystem;
import ecdar.abstractions.HighLevelModel;
import ecdar.abstractions.Project;
import ecdar.mutation.MutationTestPlanPresentation;
import ecdar.mutation.models.MutationTestPlan;
import ecdar.presentations.*;
import ecdar.utility.UndoRedoStack;
import com.jfoenix.controls.JFXPopup;
import com.jfoenix.controls.JFXRippler;
import com.jfoenix.controls.JFXTextArea;
import ecdar.utility.colors.EnabledColor;
import ecdar.utility.keyboard.Keybind;
import ecdar.utility.keyboard.KeyboardTracker;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.material.Material;

import java.net.URL;
import java.util.*;

public class ProjectPaneController implements Initializable {
    public StackPane root;
    public HBox toolbar;
    public Label toolbarTitle;
    public HBox generatedComponentsDivider;
    public Label generatedComponentsDividerText;
    public ScrollPane scrollPane;
    public VBox filesList;
    public VBox tempFilesList;
    public JFXRippler createComponent;
    public JFXRippler createSystem;
    public JFXRippler generatedComponentsVisibilityButton;
    public FontIcon generatedComponentsVisibilityButtonIcon;

    public ImageView createComponentImage;
    public StackPane createComponentPane;
    public ImageView createSystemImage;
    public StackPane createSystemPane;


    public final Project project = new Project();
    private final HashMap<HighLevelModelPresentation, FilePresentation> modelPresentationMap = new HashMap<>();
    private final ObservableList<ComponentPresentation> componentPresentations = FXCollections.observableArrayList();

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        Platform.runLater(() -> {
            // Bind global declarations and add mouse event
            final DeclarationsPresentation globalDeclarationsPresentation = new DeclarationsPresentation(project.getGlobalDeclarations());
            final FilePresentation globalDclPresentation = new FilePresentation(project.getGlobalDeclarations());
            modelPresentationMap.put(globalDeclarationsPresentation, globalDclPresentation);
            globalDclPresentation.setOnMousePressed(event -> {
                Ecdar.getPresentation().getController().setActiveModelPresentationForActiveCanvas(globalDeclarationsPresentation);
            });

            filesList.getChildren().add(globalDclPresentation);
        });

        initializeComponentHandling();
        initializeSystemHandling();
        initializeMutationTestPlanHandling();
        initializeCreateComponentKeybinding();
        resetProject();

        Platform.runLater(() -> {
            final var initializedModelPresentation = modelPresentationMap.keySet().stream().filter(mp -> mp instanceof ComponentPresentation).findFirst().orElse(null);
            Ecdar.getPresentation().getController().setActiveModelPresentationForActiveCanvas(initializedModelPresentation);
        });
    }

    private void initializeSystemHandling() {
        project.getSystems().addListener((ListChangeListener<EcdarSystem>) change -> {
            while (change.next()) {
                change.getAddedSubList().forEach(o -> handleAddedModelPresentation(new SystemPresentation(o)));
                change.getRemoved().forEach(o -> handleRemovedModelPresentation(modelPresentationMap.keySet().stream().filter(modelPresentation -> modelPresentation.getController().getModel().equals(o)).findFirst().orElse(null)));

                // Sort the children alphabetically
                sortPresentations();
            }
        });
    }

    private void initializeComponentHandling() {
        project.getComponents().addListener(getComponentListChangeListener());
        project.getTempComponents().addListener(getComponentListChangeListener());
    }

    private ListChangeListener<Component> getComponentListChangeListener() {
        return c -> {
            while (c.next()) {
                c.getAddedSubList().forEach(o -> handleAddedModelPresentation(new ComponentPresentation(o)));
                c.getRemoved().forEach(o -> handleRemovedModelPresentation(modelPresentationMap.keySet().stream().filter(modelPresentation -> modelPresentation.getController().getModel().equals(o)).findFirst().orElse(null)));

                // Sort the children alphabetically
                sortPresentations();
            }
        };
    }

    private void initializeMutationTestPlanHandling() {
        project.getTestPlans().addListener((ListChangeListener<MutationTestPlan>) change -> {
            while (change.next()) {
                change.getAddedSubList().forEach(o -> handleAddedModelPresentation(new MutationTestPlanPresentation(o)));
                change.getRemoved().forEach(o -> handleRemovedModelPresentation(new MutationTestPlanPresentation(o)));

                // Sort the children alphabetically
                sortPresentations();
            }
        });
    }

    private void sortPresentations() {
        Platform.runLater(() -> {
            final ArrayList<HighLevelModelPresentation> sortedComponentList = new ArrayList<>(modelPresentationMap.keySet());
            sortedComponentList.sort(Comparator.comparing(o -> o.getController().getModel().getName()));
            sortedComponentList.forEach(component -> modelPresentationMap.get(component).toFront());

            var globalDec = modelPresentationMap.keySet().stream().filter(hp -> hp instanceof DeclarationsPresentation).findFirst().orElse(null);
            modelPresentationMap.get(globalDec).toBack();
        });
    }

    private void initializeMoreInformationDropDown(final FilePresentation filePresentation) {
        final JFXRippler moreInformation = (JFXRippler) filePresentation.lookup("#moreInformation");
        final DropDownMenu moreInformationDropDown = new DropDownMenu(moreInformation);
        final HighLevelModel model = filePresentation.getController().getModel();

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
                    model,
                    ((Component) model)::dye
            );
        } else if (model instanceof EcdarSystem) {
            moreInformationDropDown.addColorPicker(
                    model,
                    ((EcdarSystem) model)::dye
            );
        }

        // Delete button for components
        if (model instanceof Component) {
            moreInformationDropDown.addSpacerElement();

            if (!filePresentation.getController().getModel().isTemporary()) {
                moreInformationDropDown.addClickableListElement("Delete", event -> {
                    UndoRedoStack.pushAndPerform(() -> { // Perform
                        project.getComponents().remove(model);
                    }, () -> { // Undo
                        project.addComponent((Component) model);
                    }, "Deleted component " + model.getName(), "delete");
                    moreInformationDropDown.hide();
                });
            } else {
                moreInformationDropDown.addClickableListElement("Delete", event -> {
                    UndoRedoStack.pushAndPerform(() -> { // Perform
                        project.getTempComponents().remove(model);
                    }, () -> { // Undo
                        project.getTempComponents().add((Component) model);
                    }, "Deleted component " + model.getName(), "delete");
                    moreInformationDropDown.hide();
                });

                moreInformationDropDown.addClickableListElement("Add as component", event -> {
                    if (project.getComponents().stream().noneMatch(component -> component.getName().equals(model.getName()))) {
                        UndoRedoStack.pushAndPerform(() -> { // Perform
                            project.getTempComponents().remove(model);
                            model.setTemporary(false);
                            project.addComponent((Component) model);
                        }, () -> { // Undo
                            project.getComponents().remove(model);
                            model.setTemporary(true);
                            project.getTempComponents().add((Component) model);
                        }, "Add component " + model.getName(), "add");
                        moreInformationDropDown.hide();
                    } else {
                        String originalModelName = model.getName();
                        // Get new model number starting from 2 to symbolize second version
                        for (int i = 2; i < 100; i++) {
                            final String newName = originalModelName + " #" + i;
                            if (project.getComponents().stream().noneMatch(component -> component.getName().equals(newName))) {
                                UndoRedoStack.pushAndPerform(() -> { // Perform
                                    project.getTempComponents().remove(model);
                                    model.setTemporary(false);
                                    project.addComponent((Component) model);
                                    model.setName(newName);
                                }, () -> { // Undo
                                    project.getComponents().remove(model);
                                    model.setTemporary(true);
                                    project.getTempComponents().add((Component) model);
                                    model.setName(originalModelName);
                                }, "Add component " + model.getName(), "add");
                                moreInformationDropDown.hide();
                                break;
                            }
                        }
                    }
                });
            }
        }

        // Delete button for systems
        if (model instanceof EcdarSystem) {
            moreInformationDropDown.addSpacerElement();
            moreInformationDropDown.addClickableListElement("Delete", event -> {
                UndoRedoStack.pushAndPerform(() -> { // Perform
                    project.getSystems().remove(model);
                }, () -> { // Undo
                    project.getSystems().add((EcdarSystem) model);
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
                    project.getTestPlans().remove(model);
                }, () -> { // Undo
                    project.getTestPlans().add((MutationTestPlan) model);
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

    private void initializeCreateComponentKeybinding() {
        //Press ctrl+N or cmd+N to create a new component. The canvas changes to this new component
        KeyCodeCombination combination = new KeyCodeCombination(KeyCode.N, KeyCombination.SHORTCUT_DOWN);
        Keybind binding = new Keybind(combination, (event) -> {
            final Component newComponent = new Component(getAvailableColor(), getUniqueComponentName());
            UndoRedoStack.pushAndPerform(() -> { // Perform
                project.addComponent(newComponent);
            }, () -> { // Undo
                project.getComponents().remove(newComponent);
            }, "Created new component: " + newComponent.getName(), "add-circle");

            EcdarController.getActiveCanvasPresentation().getController().setActiveModelPresentation(getComponentPresentations().stream().filter(componentPresentation -> componentPresentation.getController().getComponent().equals(newComponent)).findFirst().orElse(null));
        });
        KeyboardTracker.registerKeybind(KeyboardTracker.CREATE_COMPONENT, binding);
    }

    private void handleAddedModelPresentation(final HighLevelModelPresentation modelPresentation) {
        final FilePresentation filePresentation = new FilePresentation(modelPresentation.getController().getModel());
        initializeMoreInformationDropDown(filePresentation);

        // Add the file presentation related to the modelPresentation to the project pane
        if (modelPresentation.getController().getModel().isTemporary()) {
            tempFilesList.getChildren().add(filePresentation);
        } else {
            filesList.getChildren().add(filePresentation);
        }

        modelPresentationMap.put(modelPresentation, filePresentation);// ToDo NIELS: Bind these two
        if (modelPresentation instanceof ComponentPresentation) {
            componentPresentations.add((ComponentPresentation) modelPresentation);
        }

        // Open the component if the file is pressed
        filePresentation.setOnMousePressed(event -> {
            final var previouslyActiveFile = modelPresentationMap.get(EcdarController.getActiveCanvasPresentation()
                    .getController()
                    .getActiveModelPresentation());
            if (previouslyActiveFile != null) previouslyActiveFile.getController().setIsActive(false);

            Ecdar.getPresentation().getController().setActiveModelPresentationForActiveCanvas(modelPresentation);
            Platform.runLater(() -> {
                filePresentation.getController().setIsActive(true);
            });
        });

        modelPresentation.getController().getModel().nameProperty().addListener(obs -> sortPresentations());
        filePresentation.getController().setIsActive(true);
        Platform.runLater(() -> Ecdar.getPresentation().getController().setActiveModelPresentationForActiveCanvas(modelPresentation));
    }

    private void handleRemovedModelPresentation(final HighLevelModelPresentation modelPresentation) {
        // If we remove the modelPresentation active on the canvas
        if (EcdarController.getActiveCanvasPresentation().getController().getActiveModelPresentation() == modelPresentation) {
            if (project.getComponents().size() > 0) {
                // Find the first available component and show it instead of the removed one
                final HighLevelModelPresentation newActiveModelPresentation = modelPresentationMap.keySet().iterator().next();
                Ecdar.getPresentation().getController().setActiveModelPresentationForActiveCanvas(newActiveModelPresentation);
                modelPresentationMap.get(newActiveModelPresentation).getController().setIsActive(true);
            } else {
                // Show no components (since there are none in the project)
                Ecdar.getPresentation().getController().setActiveModelPresentationForActiveCanvas(null);
            }
        }

        // Remove the file presentation related to the model from the project pane
        if (modelPresentation.getController().getModel().isTemporary()) {
            tempFilesList.getChildren().removeIf(n -> n == modelPresentationMap.get(modelPresentation));
        } else {
            filesList.getChildren().removeIf(n -> n == modelPresentationMap.get(modelPresentation));
        }

        modelPresentationMap.remove(modelPresentation);
    }

    /**
     * Resets components.
     * After this, there is only one component.
     * Be sure to disable code analysis before call and enable after call.
     */
    public void resetProject() {
        project.clean();
        project.addComponent(new Component(getAvailableColor(), getUniqueComponentName()));
    }

    public EnabledColor getAvailableColor() {
        ArrayList<EnabledColor> availableColors = new ArrayList<>(EnabledColor.enabledColors);
        for (Component comp : project.getComponents()) {
            availableColors.removeIf(c -> comp.getColor().equals(c));
        }

        if (availableColors.isEmpty()) {
            return EnabledColor.enabledColors.get(new Random().nextInt(EnabledColor.enabledColors.size()));
        }

        return availableColors.get(0);
    }

    /**
     * Gets the name of all components in the project and inserts it into a set
     *
     * @return the set of all component names
     */
    private HashSet<String> getComponentNames() {
        final HashSet<String> names = new HashSet<>();

        for (final Component component : project.getComponents()) {
            names.add(component.getName());
        }

        return names;
    }

    /**
     * Generate a unique name for the component
     *
     * @return A project unique name
     */
    public String getUniqueComponentName() {
        for (int counter = 1; ; counter++) {
            final String name = Project.COMPONENT + counter;
            if (!getComponentNames().contains(name)) {
                return name;
            }
        }
    }

    public String getUniqueSystemName() {
        for (int counter = 1; ; counter++) {
            final String name = Project.SYSTEM + counter;
            if (!getSystemNames().contains(name)) {
                return name;
            }
        }
    }

    private HashSet<String> getSystemNames() {
        final HashSet<String> names = new HashSet<>();

        for (final EcdarSystem component : project.getSystems()) {
            names.add(component.getName());
        }

        return names;
    }

    /**
     * Method for creating a new component
     */
    @FXML
    private void createComponentClicked() {
        final Component newComponent = new Component(getAvailableColor(), getUniqueComponentName());

        UndoRedoStack.pushAndPerform(() -> { // Perform
            project.addComponent(newComponent);
        }, () -> { // Undo
            project.getComponents().remove(newComponent);
        }, "Created new component: " + newComponent.getName(), "add-circle");
    }

    /**
     * Method for creating a new system
     */
    @FXML
    private void createSystemClicked() {
        final EcdarSystem newSystem = new EcdarSystem(getAvailableColor(), getUniqueSystemName());

        UndoRedoStack.pushAndPerform(() -> { // Perform
            project.getSystems().add(newSystem);
        }, () -> { // Undo
            project.getSystems().remove(newSystem);
        }, "Created new system: " + newSystem.getName(), "add-circle");
    }

    /**
     * Method for hiding/showing generated components
     */
    @FXML
    private void setGeneratedComponentsVisibilityButtonClicked() {
        if (generatedComponentsVisibilityButtonIcon.getIconCode() == Material.EXPAND_MORE) {
            generatedComponentsVisibilityButtonIcon.setIconCode(Material.EXPAND_LESS);
            this.tempFilesList.setVisible(true);
            this.tempFilesList.setManaged(true);
        } else {
            generatedComponentsVisibilityButtonIcon.setIconCode(Material.EXPAND_MORE);
            this.tempFilesList.setVisible(false);
            this.tempFilesList.setManaged(false);
        }
    }

    public ObservableList<ComponentPresentation> getComponentPresentations() {
        return componentPresentations;
    }

    public void setActiveModelPresentations(HighLevelModelPresentation activeModelPresentation) {
        Platform.runLater(() -> {
            modelPresentationMap.values().forEach(fp -> fp.getController().setIsActive(false));
            modelPresentationMap.get(activeModelPresentation).getController().setIsActive(true);
        });
    }

    public void setActiveModelPresentations(List<HighLevelModelPresentation> currentlyActiveModelPresentations) {
        Platform.runLater(() -> {
            modelPresentationMap.values().forEach(fp -> fp.getController().setIsActive(false));

            for (HighLevelModelPresentation modelPresentation : currentlyActiveModelPresentations) {
                modelPresentationMap.get(modelPresentation).getController().setIsActive(true);
            }
        });
    }

    public void changeOneActiveModelPresentationForAnother(final HighLevelModelPresentation oldActive, final HighLevelModelPresentation newActive) {
        if (modelPresentationMap.get(oldActive) != null) modelPresentationMap.get(oldActive)
                .getController()
                .setIsActive(false);
        modelPresentationMap.get(newActive).getController().setIsActive(true);
    }
}
