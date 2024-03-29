package ecdar.controllers;

import ecdar.Debug;
import ecdar.presentations.BackgroundThreadEntryPresentation;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.fxml.Initializable;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

public class BackgroundThreadController implements Initializable {

    public VBox threadContainer;
    private final Map<Thread, BackgroundThreadEntryPresentation> threadToPresentationMap = new HashMap<>();

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        Debug.backgroundThreads.addListener(new ListChangeListener<Thread>() {
            @Override
            public void onChanged(final Change<? extends Thread> c) {
                while (c.next()) {
                    c.getAddedSubList().forEach(thread -> {
                        Platform.runLater(() -> { // Run on the UI thread
                            final BackgroundThreadEntryPresentation label = new BackgroundThreadEntryPresentation(thread);
                            threadToPresentationMap.put(thread, label);
                            threadContainer.getChildren().add(label);
                        });
                    });

                    c.getRemoved().forEach(thread -> {
                        Platform.runLater(() -> { // Run on the UI thread
                            final BackgroundThreadEntryPresentation label = threadToPresentationMap.get(thread);

                            threadContainer.getChildren().remove(label);
                        });
                    });
                }
            }
        });
    }
}
