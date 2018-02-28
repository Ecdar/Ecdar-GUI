package ecdar.mutation.models;

import ecdar.abstractions.Component;
import ecdar.abstractions.Edge;
import ecdar.abstractions.EdgeStatus;

/**
 * Mutation operator that changes a synchronization action to another action.
 */
abstract class ChangeActionOperator extends MutationOperator {

    /**
     * Creates a mutant and generates a test-case with it.
     * @param original the original component to mutate
     * @param edgeIndex the index of the edge to mutate
     * @param sync the new synchronization action without ? or !
     * @param status the new edge status of the edge to mutate
     * @return the generated test-case
     */
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
