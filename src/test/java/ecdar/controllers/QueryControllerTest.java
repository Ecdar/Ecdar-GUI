package ecdar.backend;

import ecdar.abstractions.Query;
import ecdar.abstractions.QueryState;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

public class QueryTest {
    @Test
    public void testExecuteQuery() {
        Engine e = mock(Engine.class);
        Query q = new Query("refinement: (Administration || Machine || Researcher) <= Spec", "", QueryState.UNKNOWN, e);
        q.execute();

        verify(e, times(1)).enqueueQuery(eq(q), any(), any());
    }
}
