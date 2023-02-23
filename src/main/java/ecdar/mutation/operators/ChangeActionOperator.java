package ecdar.mutation.operators;

import ecdar.abstractions.Component;
import ecdar.abstractions.Edge;
import ecdar.abstractions.EdgeStatus;
import ecdar.mutation.ComponentVerificationTransformer;
import ecdar.mutation.TextFlowBuilder;
import ecdar.mutation.models.MutationTestCase;

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

        final Component mutant = ComponentVerificationTransformer.cloneForVerification(original);

        // Mutate
        final Edge mutantEdge = mutant.getEdges().get(edgeIndex);
        mutantEdge.setStatus(status);
        mutantEdge.setSync(sync);

        return new MutationTestCase(original, mutant,
                getCodeName() + "_" + edgeIndex + "_" + sync,
                new TextFlowBuilder().text("Changed ").boldText("action").text(" of ")
                        .edgeLinks(originalEdge, original.getName())
                        .text(" from " + (originalEdge.getStatus().equals(EdgeStatus.INPUT) ? "input" : "output") + " ")
                        .boldText(originalEdge.getSync())
                        .text(" to " + (status.equals(EdgeStatus.INPUT) ? "input" : "output") + " ")
                        .boldText(mutantEdge.getSync()).build()
        );
    }
}
