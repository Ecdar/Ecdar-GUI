package ecdar.mutation;

import com.bpodgursky.jbool_expressions.*;
import com.bpodgursky.jbool_expressions.rules.RuleSet;
import ecdar.abstractions.Component;
import ecdar.abstractions.Edge;
import ecdar.abstractions.EdgeStatus;
import ecdar.abstractions.Location;
import ecdar.utility.ExpressionHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ComponentVerificationTransformer { //ToDo NIELS: Test the public methods
    /**
     * Applies demonic completion on this component.
     */
    public static void applyDemonicCompletionToComponent(final Component component) {
        // Make a universal location
        final Location uniLocation = new Location(component, Location.Type.UNIVERSAL, component.generateUniIncId(), 0, 0);
        component.addLocation(uniLocation);

        final Edge inputEdge = uniLocation.addLeftEdge("*", EdgeStatus.INPUT);
        inputEdge.setIsLocked(true);
        component.addEdge(inputEdge);

        final Edge outputEdge = uniLocation.addRightEdge("*", EdgeStatus.OUTPUT);
        outputEdge.setIsLocked(true);
        component.addEdge(outputEdge);

        // Cache input signature, since it could be updated when added edges
        final List<String> inputStrings = new ArrayList<>(component.getInputStrings());

        component.getLocations().forEach(location -> inputStrings.forEach(input -> {
            // Get outgoing input edges that has the chosen input
            final List<Edge> matchingEdges = component.getListOfEdgesFromDisplayableEdges(component.getOutgoingEdges(location)).stream().filter(
                    edge -> edge.getStatus().equals(EdgeStatus.INPUT) &&
                            edge.getSync().equals(input)).collect(Collectors.toList()
            );

            // If no such edges, add an edge to Universal
            if (matchingEdges.isEmpty()) {
                final Edge edge = new Edge(location, EdgeStatus.INPUT);
                edge.setTargetLocation(uniLocation);
                edge.addSyncNail(input);
                component.addEdge(edge);
                return;
            }
            // If an edge has no guard and its target has no invariants, ignore.
            // Component is already input-enabled with respect to this location and input.
            if (matchingEdges.stream().anyMatch(edge -> edge.getGuard().isEmpty() &&
                    edge.getTargetLocation().getInvariant().isEmpty())) return;

            // Extract expression for which edges to create.
            // The expression is in DNF
            // We create edges to Universal for each child expression in the disjunction.
            createDemonicEdgesForComponent(component, location, uniLocation, input, getNegatedEdgeExpressionForComponent(component, matchingEdges));
        }));
    }

    /**
     * Applies angelic completion on this component.
     */
    public static void applyAngelicCompletionForComponent(final Component component) {
        // Cache input signature, since it could be updated when added edges
        final List<String> inputStrings = new ArrayList<>(component.getInputStrings());

        component.getLocations().forEach(location -> inputStrings.forEach(input -> {
            // Get outgoing input edges that has the chosen input
            final List<Edge> matchingEdges = component.getListOfEdgesFromDisplayableEdges(component.getOutgoingEdges(location)).stream().filter(
                    edge -> edge.getStatus().equals(EdgeStatus.INPUT) &&
                            edge.getSync().equals(input)).collect(Collectors.toList()
            );

            // If no such edges, add a self loop without a guard
            if (matchingEdges.isEmpty()) {
                final Edge edge = new Edge(location, EdgeStatus.INPUT);
                edge.setTargetLocation(location);
                edge.addSyncNail(input);
                component.addEdge(edge);
                return;
            }

            // If an edge has no guard and its target has no invariants, ignore.
            // Component is already input-enabled with respect to this location and input.
            if (matchingEdges.stream().anyMatch(edge -> edge.getGuard().isEmpty() &&
                    edge.getTargetLocation().getInvariant().isEmpty())) return;

            // Extract expression for which edges to create.
            // The expression is in DNF
            // We create self loops for each child expression in the disjunction.
            createAngelicSelfLoopsForComponent(component, location, input, getNegatedEdgeExpressionForComponent(component, matchingEdges));
        }));
    }

    /**
     * Creates a clone of another component.
     * Copy objects used for verification (e.g. locations, edges and the declarations).
     * Does not copy UI elements (sizes and positions).
     * Its locations are cloned from the original component. Their ids are the same.
     * Does not initialize io listeners, but copies the input and output strings.
     * Reachability analysis binding is not initialized.
     * @return the clone
     */
    public static Component cloneForVerification(final Component component) {
        final Component clone = new Component();
        addVerificationObjects(component, clone);
        clone.setIncludeInPeriodicCheck(false);
        clone.getInputStrings().addAll(component.getInputStrings());
        clone.getOutputStrings().addAll(component.getOutputStrings());
        clone.setName(component.getName());

        return clone;
    }

    /**
     * Creates edges to a specified Universal location from a location
     * in order to finish missing inputs with a demonic completion.
     *
     * @param location        the location to create self loops on
     * @param universal       the Universal location to create edge to
     * @param input           the input action to use in the synchronization properties
     * @param guardExpression the expression that represents the guards of the edges to create
     */
    private static void createDemonicEdgesForComponent(final Component component, final Location location, final Location universal, final String input, final Expression<String> guardExpression) {
        final Edge edge;

        switch (guardExpression.getExprType()) {
            case Literal.EXPR_TYPE:
                // If false, do not create any edges
                if (!((Literal<String>) guardExpression).getValue()) break;

                // It should never be true, since that should be handled before calling this method
                throw new RuntimeException("Type of expression " + guardExpression + " not accepted");
            case Variable.EXPR_TYPE:
                edge = new Edge(location, EdgeStatus.INPUT);
                edge.setTargetLocation(universal);
                edge.addSyncNail(input);
                edge.addGuardNail(((Variable<String>) guardExpression).getValue());
                component.addEdge(edge);
                break;
            case And.EXPR_TYPE:
                edge = new Edge(location, EdgeStatus.INPUT);
                edge.setTargetLocation(universal);
                edge.addSyncNail(input);
                edge.addGuardNail(guardExpression.getChildren().stream()
                        .map(child -> ((Variable<String>) child).getValue())
                        .collect(Collectors.joining("&&")));
                component.addEdge(edge);
                break;
            case Or.EXPR_TYPE:
                guardExpression.getChildren().forEach(child -> createDemonicEdgesForComponent(component, location, universal, input, child));
                break;
            default:
                throw new RuntimeException("Type of expression " + guardExpression + " not accepted");
        }
    }

    /**
     * Extracts an expression that represents the negation of a list of edges.
     * Multiple edges are resolved as disjunctions.
     * There can be conjunction in guards.
     * We translate each edge to the conjunction of its guard and the invariant of its target location
     * (since it should also be satisfied).
     * The result is converted to disjunctive normal form.
     *
     * @param edges the edges to extract from
     * @return the expression that represents the negation
     */
    private static Expression<String> getNegatedEdgeExpressionForComponent(final Component component, List<Edge> edges) {
        final List<String> clocks = component.getClocks();
        return ExpressionHelper.simplifyNegatedSimpleExpressions(
                RuleSet.toDNF(RuleSet.simplify(Not.of(Or.of(edges.stream()
                        .map(edge -> {
                                    final List<String> clocksToReset = ExpressionHelper.getUpdateSides(edge.getUpdate())
                                            .keySet().stream().filter(clocks::contains).collect(Collectors.toList());
                                    return And.of(
                                            ExpressionHelper.parseGuard(edge.getGuard()),
                                            ExpressionHelper.parseInvariantButIgnore(edge.getTargetLocation().getInvariant(), clocksToReset)
                                    );
                                }
                        ).collect(Collectors.toList()))
                ))));
    }

    /**
     * Creates self loops on a location to finish missing inputs with an angelic completion.
     * The guard expression should be in DNF without negations.
     *
     * @param location        the location to create self loops on
     * @param input           the input action to use in the synchronization properties
     * @param guardExpression the expression that represents the guards of the self loops
     */
    private static void createAngelicSelfLoopsForComponent(final Component component, Location location, final String input, final Expression<String> guardExpression) {
        final Edge edge;

        switch (guardExpression.getExprType()) {
            case Literal.EXPR_TYPE:
                // If false, do not create any loops
                if (!((Literal<String>) guardExpression).getValue()) break;

                // It should never be true, since that should be handled before calling this method
                throw new RuntimeException("Type of expression " + guardExpression + " not accepted");
            case Variable.EXPR_TYPE:
                edge = new Edge(location, EdgeStatus.INPUT);
                edge.setTargetLocation(location);
                edge.addSyncNail(input);
                edge.addGuardNail(((Variable<String>) guardExpression).getValue());
                component.addEdge(edge);
                break;
            case And.EXPR_TYPE:
                edge = new Edge(location, EdgeStatus.INPUT);
                edge.setTargetLocation(location);
                edge.addSyncNail(input);
                edge.addGuardNail(guardExpression.getChildren().stream()
                        .map(child -> {
                            if (!child.getExprType().equals(Variable.EXPR_TYPE))
                                throw new RuntimeException("Child " + child + " of type " +
                                        child.getExprType() + " in and expression " +
                                        guardExpression + " should be a variable");

                            return ((Variable<String>) child).getValue();
                        })
                        .collect(Collectors.joining("&&")));
                component.addEdge(edge);
                break;
            case Or.EXPR_TYPE:
                guardExpression.getChildren().forEach(child -> createAngelicSelfLoopsForComponent(component, location, input, child));
                break;
            default:
                throw new RuntimeException("Type of expression " + guardExpression + " not accepted");
        }
    }

    /**
     * Adds objects used for verifications from original to clone.
     * @param original the component to add from
     * @param clone the component to add to
     */
    private static void addVerificationObjects(final Component original, final Component clone) {
        for (final Location originalLoc : original.getLocations()) {
            clone.addLocation(originalLoc.cloneForVerification());
        }

        clone.getListOfEdgesFromDisplayableEdges(original.getDisplayableEdges()).forEach(edge -> clone.addEdge((edge).cloneForVerification(original)));
        clone.setDeclarationsText(original.getDeclarationsText());
    }
}
