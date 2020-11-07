package ecdar.backend;

import ecdar.abstractions.Query;
import ecdar.abstractions.QueryState;
import ecdar.controllers.EcdarController;
import javafx.application.Platform;

public class QueryListener {

    private final Query query;

    public QueryListener() {
        this(new Query("Unknown", "Unknown", QueryState.UNKNOWN));
    }

    public QueryListener(final Query query) {
        this.query = query;
    }

    public void setLength(final int i) {

    }

    public void setCurrent(final int i) {

    }

    /* ToDo: Handle without using UPPAAL
    public void setTrace(char c, String s, Vector<SymbolicTransition> vector, int i) {

    }*/

    public void setFeedback(final String s) {
        if (s.contains("inf") || s.contains("sup")) {
            Platform.runLater(() -> {
                EcdarController.openQueryDialog(query, s.split("\n")[1]);
            });
        }
    }
}
