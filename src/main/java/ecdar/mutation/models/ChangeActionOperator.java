package ecdar.mutation.models;

import ecdar.abstractions.Component;
import ecdar.abstractions.Edge;
import ecdar.abstractions.EdgeStatus;
import ecdar.abstractions.Location;
import ecdar.mutation.MutationTestingException;

import java.util.ArrayList;
import java.util.List;

public abstract class ChangeActionOperator extends MutationOperator {
    MutationTestCase generateTestCase(final Component original, final int edgeIndex, final String sync, final EdgeStatus status) {
        final Edge originalEdge = original.getEdges().get(edgeIndex);

        // If action is the action of the original edge, ignore
        if (originalEdge.getStatus().equals(status) && originalEdge.getSync().equals(sync)) return null;

        final Component mutant = original.cloneForVerification();

        // Mutate
        final Edge mutantEdge = mutant.getEdges().get(edgeIndex);
        mutantEdge.setStatus(status);
        mutantEdge.setSync(sync);

        return new MutationTestCase(original, mutant,
                getCodeName() + "_" + edgeIndex + "_" + sync,
                "Changed action of edge " + originalEdge.getSourceLocation().getId() + " -> " +
                        originalEdge.getTargetLocation().getId() + " from " +
                        (originalEdge.getStatus().equals(EdgeStatus.INPUT) ? "input" : "output") +
                        originalEdge.getSync() + "to " + (status.equals(EdgeStatus.INPUT) ? "input" : "output") +
                        originalEdge.getSync());
    }
}
