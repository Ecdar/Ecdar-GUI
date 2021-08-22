package ecdar.abstractions;

import com.jfoenix.controls.JFXSnackbar;
import com.jfoenix.controls.JFXSnackbarLayout;
import javafx.scene.layout.Pane;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Snackbar extends JFXSnackbar {
    private final Queue<SnackbarEvent> eventConcurrentLinkedQueue = new ConcurrentLinkedQueue<>();

    public Snackbar(Pane snackbarContainer) {
        super(snackbarContainer);
    }

    @Override
    public void enqueue(SnackbarEvent event) {
        if (getCurrentEvent() == null || !getCurrentEvent().equals(event) && !eventConcurrentLinkedQueue.contains(event)) {

            JFXSnackbarLayout content = (JFXSnackbarLayout) event.getContent();
            for (SnackbarEvent e : eventConcurrentLinkedQueue) {
                if (content.getToast().equals(((JFXSnackbarLayout) e.getContent()).getToast())) {
                    return;
                }
            }
            
            new java.util.Timer().schedule(
                    new java.util.TimerTask() {
                        @Override
                        public void run() {
                            eventConcurrentLinkedQueue.remove(event);
                        }
                    },
                    (long) event.getTimeout().toMillis()
            );

            super.enqueue(event);
        }
    }
}
