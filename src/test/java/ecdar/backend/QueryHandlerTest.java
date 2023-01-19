package ecdar.backend;

import ecdar.abstractions.Query;
import ecdar.abstractions.QueryState;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

public class QueryHandlerTest {
    @Test
    public void testExecuteQuery() {
        BackendDriver bd = mock(BackendDriver.class);
        QueryHandler qh = new QueryHandler(bd);

        Query q = new Query("refinement: (Administration || Machine || Researcher) \u003c\u003d Spec", "", QueryState.UNKNOWN);
        qh.executeQuery(q);

        verify(bd, times(1)).addRequestToExecutionQueue(any());
    }
}
