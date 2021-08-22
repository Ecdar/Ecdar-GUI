package ecdar.abstractions;

import com.jfoenix.controls.JFXSnackbar;
import com.jfoenix.controls.JFXSnackbarLayout;
import javafx.scene.layout.Pane;

import java.util.ArrayList;

public class Snackbar extends JFXSnackbar {
    private final ArrayList<SnackbarEvent> awaitingEvents = new ArrayList<>();
    private long millisecondsOfEventsInQueue = 0;

    public Snackbar(Pane snackbarContainer) {
        super(snackbarContainer);
    }

    @Override
    public void enqueue(SnackbarEvent event) {
        if (getCurrentEvent() == null || !getCurrentEvent().equals(event) && !awaitingEvents.contains(event)) {

            JFXSnackbarLayout content = (JFXSnackbarLayout) event.getContent();
            for (SnackbarEvent e : awaitingEvents) {
                if (content.getToast().equals(((JFXSnackbarLayout) e.getContent()).getToast())) {
                    return;
                }
            }

            millisecondsOfEventsInQueue += event.getTimeout().toMillis();

            new java.util.Timer().schedule(
                    new java.util.TimerTask() {
                        @Override
                        public void run() {
                            awaitingEvents.remove(event);
                            millisecondsOfEventsInQueue -= event.getTimeout().toMillis();
                        }
                    },
                    millisecondsOfEventsInQueue
            );

            awaitingEvents.add(event);
            super.enqueue(event);
        }
    }
}
