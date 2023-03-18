package ecdar.controllers;

import ecdar.abstractions.Query;
import ecdar.abstractions.QueryState;
import ecdar.backend.BackendHelper;
import ecdar.backend.Engine;
import ecdar.presentations.QueryPresentation;
import javafx.embed.swing.JFXPanel;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

public class QueryControllerTest {
    @Test
    public void testRunQuery() {
        JFXPanel fxPanel = new JFXPanel();
        Engine e = mock(Engine.class);
        BackendHelper.getEngines().add(e);
        Query q = new Query("refinement: (Administration || Machine || Researcher) <= Spec", "", QueryState.UNKNOWN, e);
        QueryPresentation presentation = new QueryPresentation(q);
        presentation.getController().setQuery(q);

        presentation.getController().runQuery();

        verify(e, times(1)).enqueueQuery(eq(q), any(), any());
    }
}
