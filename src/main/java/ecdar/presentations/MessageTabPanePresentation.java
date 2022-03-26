package ecdar.presentations;

import ecdar.code_analysis.CodeAnalysis;
import ecdar.controllers.MessageTabPaneController;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.scene.Cursor;
import javafx.scene.layout.StackPane;

public class MessageTabPanePresentation extends StackPane {
    private final MessageTabPaneController controller;

    public MessageTabPanePresentation() {
        controller = new EcdarFXMLLoader().loadAndGetController("MessageTabPanePresentation.fxml", this);
        initializeMessageContainer();
    }

    public MessageTabPaneController getController() {
        return controller;
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

        controller.setRunnableForOpeningAndClosingMessageTabPane(() -> {
            if (controller.isOpen()) {
                controller.collapseMessagesIcon.setIconLiteral("gmi-close");
            } else {
                controller.tabPane.getSelectionModel().clearSelection(); // Clear the currently selected tab (so that the view will open again when selecting a tab)
                controller.collapseMessagesIcon.setIconLiteral("gmi-expand-less");
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

        controller.collapseMessagesIcon.getStyleClass().add("icon-size-medium");
    }
}