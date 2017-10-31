package SW9.backend;

import SW9.abstractions.Query;
import SW9.abstractions.QueryState;
import SW9.controllers.EcdarController;
import com.uppaal.engine.QueryFeedback;
import com.uppaal.model.system.symbolic.SymbolicTransition;
import javafx.application.Platform;

import java.util.Vector;

public class QueryListener implements QueryFeedback {

    private Query query;

    public QueryListener() {
        this(new Query("Unknown", "Unknown", QueryState.UNKNOWN));
    }

    public QueryListener(final Query query) {
        this.query = query;
    }

    @Override
    public void setLength(final int i) {

    }

    @Override
    public void setCurrent(final int i) {

    }

    @Override
    public void setTrace(char c, String s, Vector<SymbolicTransition> vector, int i) {

    }

    @Override
    public void setFeedback(final String s) {
        if (s.contains("inf") || s.contains("sup")) {
            Platform.runLater(() -> {
                EcdarController.openQueryDialog(query, s.split("\n")[1]);
            });
        }
    }
}
