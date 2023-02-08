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
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.ImageView;
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
    private final ObservableList<FilePresentation> activeFilePresentations = FXCollections.observableArrayList();

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        // Bind global declarations and add mouse event
        final FilePresentation globalDclPresentation = new FilePresentation(project.getGlobalDeclarations(), activeFilePresentations);
        globalDclPresentation.setOnMousePressed(event -> {
            Ecdar.getPresentation().getController().setActiveModelPresentationForActiveCanvas(new DeclarationsPresentation(Ecdar.getProject().getGlobalDeclarations()));
        });

        filesList.getChildren().add(globalDclPresentation);

        initializeComponentHandling();
        initializeSystemHandling();
        initializeMutationTestPlanHandling();

        project.reset();

        Platform.runLater(() -> {
            final var initializedModelPresentation = modelPresentationMap.keySet().stream().findAny().orElse(null);
            Ecdar.getPresentation().getController().setActiveModelPresentationForActiveCanvas(initializedModelPresentation);
        });
    }

    private void initializeSystemHandling() {
        project.getSystemsProperty().addListener((ListChangeListener<EcdarSystem>) change -> {
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
        final ArrayList<HighLevelModelPresentation> sortedComponentList = new ArrayList<>(modelPresentationMap.keySet());
        sortedComponentList.sort(Comparator.comparing(o -> o.getController().getModel().getName()));
        sortedComponentList.forEach(component -> modelPresentationMap.get(component).toFront());
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
                    if(project.getComponents().stream().noneMatch(component -> component.getName().equals(model.getName()))) {
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
                        for (int i = 2; i < 100; i++) {
                            final String newName = originalModelName + " #" + i;
                            if(project.getComponents().stream().noneMatch(component -> component.getName().equals(newName))) {
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
                    project.getSystemsProperty().remove(model);
                }, () -> { // Undo
                    project.getSystemsProperty().add((EcdarSystem) model);
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

    private void handleAddedModelPresentation(final HighLevelModelPresentation modelPresentation) {
        final FilePresentation filePresentation = new FilePresentation(modelPresentation.getController().getModel(), activeFilePresentations);
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
     * Method for creating a new component
     */
    @FXML
    private void createComponentClicked() {
        final Component newComponent = new Component(true, project.getUniqueComponentName());

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
        final EcdarSystem newSystem = new EcdarSystem();

        UndoRedoStack.pushAndPerform(() -> { // Perform
            project.getSystemsProperty().add(newSystem);
        }, () -> { // Undo
            project.getSystemsProperty().remove(newSystem);
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

    public void setActiveModelPresentations(HighLevelModelPresentation ... currentlyActiveModelPresentations) {
        activeFilePresentations.clear();
        for (HighLevelModelPresentation modelPresentation : currentlyActiveModelPresentations) {
            activeFilePresentations.add(modelPresentationMap.get(modelPresentation));
        }
    }

    public void changeOneActiveModelPresentationForAnother(final HighLevelModelPresentation oldActive, final HighLevelModelPresentation newActive) {
        activeFilePresentations.remove(modelPresentationMap.get(oldActive));
        activeFilePresentations.add(modelPresentationMap.get(newActive));
    }
}
