package ecdar.presentations;

import com.jfoenix.controls.JFXSnackbar;
import ecdar.Ecdar;
import ecdar.code_analysis.CodeAnalysis;
import ecdar.controllers.MainController;
import ecdar.utility.colors.Color;
import ecdar.utility.helpers.DropShadowHelper;
import javafx.animation.Timeline;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.text.Text;

/**
 * This presentation class contains the views of the top menu bar, the bottom messages tabpane,
 * the help dialog, the snackbar, the bottom statusbar and the side navigation.
 * In the center of this view is an EcdarPresentation
 */
public class MainPresentation extends StackPane {
    private final MainController controller;

    public MainPresentation() {
        controller = new EcdarFXMLLoader().loadAndGetController("MainPresentation.fxml", this);

        initializeTopBar();

        initializeTopBar();
        initializeMessageContainer();
        initializeSnackbar();
        initializeQueryDetailsDialog();
        initializeHelpImages();
    }

    private void initializeTopBar() {
        final Color color = Color.GREY_BLUE;
        final Color.Intensity intensity = Color.Intensity.I800;

        // Set the background for the top toolbar
        controller.menuBar.setBackground(
                new Background(new BackgroundFill(color.getColor(intensity),
                        CornerRadii.EMPTY,
                        Insets.EMPTY)
                ));

        // Set the bottom border
        controller.menuBar.setBorder(new Border(new BorderStroke(
                color.getColor(intensity.next()),
                BorderStrokeStyle.SOLID,
                CornerRadii.EMPTY,
                new BorderWidths(0, 0, 1, 0)
        )));
    }

    /**
     * Initialize help image views.
     */
    private void initializeHelpImages() {
        controller.helpInitialImage.setImage(new Image(Ecdar.class.getResource("ic_help_initial.png").toExternalForm()));
        fitSizeWhenAvailable(controller.helpInitialImage, controller.helpInitialPane);

        controller.helpUrgentImage.setImage(new Image(Ecdar.class.getResource("ic_help_urgent.png").toExternalForm()));
        fitSizeWhenAvailable(controller.helpUrgentImage, controller.helpUrgentPane);

        controller.helpInputImage.setImage(new Image(Ecdar.class.getResource("ic_help_input.png").toExternalForm()));
        fitSizeWhenAvailable(controller.helpInputImage, controller.helpInputPane);

        controller.helpOutputImage.setImage(new Image(Ecdar.class.getResource("ic_help_output.png").toExternalForm()));
        fitSizeWhenAvailable(controller.helpOutputImage, controller.helpOutputPane);
    }

    public static void fitSizeWhenAvailable(final ImageView imageView, final StackPane pane) {
        pane.widthProperty().addListener((observable, oldValue, newValue) ->
                imageView.setFitWidth(pane.getWidth()));
        pane.heightProperty().addListener((observable, oldValue, newValue) ->
                imageView.setFitHeight(pane.getHeight()));
    }

    public BooleanProperty toggleFilePane() {
        return controller.ecdarPresentation.toggleFilePane();
    }

    public BooleanProperty toggleQueryPane() {
        return controller.ecdarPresentation.toggleQueryPane();
    }

    public BooleanProperty toggleGrid() {
        return controller.ecdarPresentation.toggleGrid();
    }

    public BooleanProperty toggleLeftSimPane() { return controller.simulatorPresentation.toggleLeftPane(); }

    public BooleanProperty toggleRightSimPane() { return controller.simulatorPresentation.toggleRightPane(); }

    public void showHelp() {
        controller.dialogContainer.setVisible(true);
        controller.dialog.show(controller.dialogContainer);
    }

    private void initializeSnackbar() {
        controller.snackbar = new JFXSnackbar(controller.root);
        controller.snackbar.setPrefWidth(568);
        controller.snackbar.autosize();
    }

    private void initializeMessageContainer() {
        // The element of which you drag to resize should be equal to the width of the window (main stage)
        controller.tabPaneResizeElement.sceneProperty().addListener((observableScene, oldScene, newScene) -> {
            if (oldScene == null && newScene != null) {
                // scene is set for the first time. Now its the time to listen stage changes.
                newScene.windowProperty().addListener((observableWindow, oldWindow, newWindow) -> {
                    if (oldWindow == null && newWindow != null) {
                        newWindow.widthProperty().addListener((observableWidth, oldWidth, newWidth) -> {
                            controller.tabPaneResizeElement.setWidth(newWidth.doubleValue() - 30);
                        });
                    }
                });
            }
        });

        // Resize cursor
        controller.tabPaneResizeElement.setCursor(Cursor.N_RESIZE);

        controller.tabPaneContainer.maxHeightProperty().addListener((obs, oldHeight, newHeight) -> {
            if (newHeight.doubleValue() > 35) {
                controller.collapseMessagesIcon.setIconLiteral("gmi-close");
                controller.collapseMessagesIcon.setIconSize(24);
            } else {
                controller.tabPane.getSelectionModel().clearSelection(); // Clear the currently selected tab (so that the view will open again when selecting a tab)
                controller.collapseMessagesIcon.setIconLiteral("gmi-expand-less");
                controller.collapseMessagesIcon.setIconSize(24);
            }
        });

        // Remove the background of the scroll panes
        controller.errorsScrollPane.setStyle("-fx-background-color: transparent;");
        controller.warningsScrollPane.setStyle("-fx-background-color: transparent;");

        final Runnable collapseIfNoErrorsOrWarnings = () -> {
            new Thread(() -> {
                // Wait for a second to check if new warnings or errors occur
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                // Check if any warnings or errors occurred
                if (CodeAnalysis.getBackendErrors().size() + CodeAnalysis.getErrors().size() + CodeAnalysis.getWarnings().size() == 0) {
                    controller.collapseMessagesIfNotCollapsed();
                }
            }).start();
        };

        // Update the tab-text and expand/collapse the view
        CodeAnalysis.getBackendErrors().addListener(new InvalidationListener() {
            @Override
            public void invalidated(final Observable observable) {
                final int errors = CodeAnalysis.getBackendErrors().size();
                if (errors == 0) {
                    controller.backendErrorsTab.setText("Backend Errors");
                } else {
                    controller.backendErrorsTab.setText("Backend Errors (" + errors + ")");
                    controller.expandMessagesIfNotExpanded();
                    controller.tabPane.getSelectionModel().select(controller.backendErrorsTab);
                }

                collapseIfNoErrorsOrWarnings.run();
            }
        });

        // Update the tab-text and expand/collapse the view
        CodeAnalysis.getErrors().addListener(new InvalidationListener() {
            @Override
            public void invalidated(final Observable observable) {
                final int errors = CodeAnalysis.getErrors().size();
                if (errors == 0) {
                    controller.errorsTab.setText("Errors");
                } else {
                    controller.errorsTab.setText("Errors (" + errors + ")");
                    controller.expandMessagesIfNotExpanded();
                    controller.tabPane.getSelectionModel().select(controller.errorsTab);
                }

                collapseIfNoErrorsOrWarnings.run();
            }
        });


        // Update the tab-text and expand/collapse the view
        CodeAnalysis.getWarnings().addListener(new InvalidationListener() {
            @Override
            public void invalidated(final Observable observable) {
                final int warnings = CodeAnalysis.getWarnings().size();
                if (warnings == 0) {
                    controller.warningsTab.setText("Warnings");
                } else {
                    controller.warningsTab.setText("Warnings (" + warnings + ")");
                    //We must select the warnings tab but we don't want the messages areas to open
                    controller.shouldISkipOpeningTheMessagesContainer = true;
                    controller.tabPane.getSelectionModel().select(controller.warningsTab);
                }

                collapseIfNoErrorsOrWarnings.run();
            }
        });
    }

    private void initializeQueryDetailsDialog() {
        final Color modalBarColor = Color.GREY_BLUE;
        final Color.Intensity modalBarColorIntensity = Color.Intensity.I500;

        // Set the background of the modal bar
        controller.modalBar.setBackground(new Background(new BackgroundFill(
                modalBarColor.getColor(modalBarColorIntensity),
                CornerRadii.EMPTY,
                Insets.EMPTY
        )));
    }

    public void showSnackbarMessage(final String message) {
        controller.snackbar.enqueue(new JFXSnackbar.SnackbarEvent(new Text(message)));
    }

    public MainController getController() {
        return controller;
    }
}
