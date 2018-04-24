package ecdar.utility;

import com.bpodgursky.jbool_expressions.*;
import org.springframework.expression.spel.standard.SpelExpressionParser;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Helper for working with expressions like the ones used in guards.
 */
public class ExpressionHelper {
    private static final String REGEX_SIMPLE_NEGATEABLE_GUARD = "^([^<>=!]+)(<|<=|>|>=|==|!=)([^<>=!]+)$";

    /**
     * Searches recursively through the expression.
     * If a Not expression of a simple expression (e.g. {@code x < 2}) is found,
     * this is replaced with the negated expression ({@code x >= 2}).
     * To use effectively, make sure to put the expression into DNF before calling this method.
     * @param expression the expression to simplify
     * @return the simplified expression
     */
    public static Expression<String> simplifyNegatedSimpleExpressions(final Expression<String> expression) {
        switch (expression.getExprType()) {
            case Variable.EXPR_TYPE:
            case Literal.EXPR_TYPE:
                return expression;
            case Not.EXPR_TYPE:
                final Expression<String> child = ((Not<String>) expression).getE();

                if (!child.getExprType().equals(Variable.EXPR_TYPE))
                    throw new RuntimeException("Unexpected negation. Expressions \"" + expression.toString() + "\" should be in DNF");

                final String guard = ((Variable<String>) child).getValue();
                final Matcher matcher = Pattern.compile(REGEX_SIMPLE_NEGATEABLE_GUARD).matcher(guard);

                if (!matcher.find()) {
                    throw new RuntimeException("Guard " + guard + " did not match pattern " + REGEX_SIMPLE_NEGATEABLE_GUARD);
                }

                switch (matcher.group(2)) {
                    case "<":
                        return Variable.of(matcher.group(1) + ">=" + matcher.group(3));
                    case "<=":
                        return Variable.of(matcher.group(1) + ">" + matcher.group(3));
                    case ">":
                        return Variable.of(matcher.group(1) + "<=" + matcher.group(3));
                    case ">=":
                        return Variable.of(matcher.group(1) + "<" + matcher.group(3));
                    case "==":
                        return Variable.of(matcher.group(1) + "!=" + matcher.group(3));
                    case "!=":
                        return Variable.of(matcher.group(1) + "==" + matcher.group(3));
                    default:
                        throw new RuntimeException("Unexpected operator " + matcher.group(2) + "");
                }
            case And.EXPR_TYPE:
                return And.of(((And<String>) expression).getChildren().stream()
                        .map(ExpressionHelper::simplifyNegatedSimpleExpressions)
                        .collect(Collectors.toList()));
            case Or.EXPR_TYPE:
                return Or.of(((Or<String>) expression).getChildren().stream()
                        .map(ExpressionHelper::simplifyNegatedSimpleExpressions)
                        .collect(Collectors.toList()));
            default:
                throw new RuntimeException("Type of expression " + expression + " not accepted");
        }
    }

    /**
     * Parses a disjunction of guards to an expression.
     * @param guards the disjunction of guards
     * @return the expression
     */
    public static Expression<String> parseDisjunctionOfGuards(final List<String> guards) {
        return Or.of(
                guards.stream()
                        .map(ExpressionHelper::parseGuard)
                        .collect(Collectors.toList())
        );
    }

    /**
     * Parses a guard to an expression.
     * The guard can be a simple guard (e.g. {@code x < 2}) or a conjunction of simple guards.
     * @param guard the guard to parse
     * @return the expression
     */
    public static Expression<String> parseGuard(final String guard) {
        if (guard.trim().isEmpty()) return Literal.getTrue();

        return And.of(
                Arrays.stream(guard.split("&&"))
                        .map(String::trim)
                        .map(ExpressionHelper::parseSimpleGuard)
                        .collect(Collectors.toList())
        );
    }

    /**
     * Parses a simple guards (without conjunctions) to an expression.
     * The equal operator ({@code ==}) is parsed as a conjunction of {@code <=} and {@code >=},
     * since the engine does not allow for != for clock valuations (in case it is negated).
     * @param simpleGuard the simple guard to parse
     * @return the expression
     */
    private static Expression<String> parseSimpleGuard(final String simpleGuard) {
        if (simpleGuard.contains("=="))
            return And.of(
                    Variable.of(simpleGuard.replaceFirst("==", "<=")),
                    Variable.of(simpleGuard.replaceFirst("==",">="))
            );
        else
            return Variable.of(simpleGuard);
    }

    /**
     * Parses an invariant to an expression.
     * This method simply parses it as a guard,
     * since the set of invariants is included in the set of guards.
     * @param invariant the invariant to parse
     * @return the expression
     */
    public static Expression<String> parseInvariant(final String invariant) {
        return parseGuard(invariant);
    }

    /**
     * Parses an invariant to an expression.
     * Ignores some variables.
     * E.g. if x is ignored in the invariant {@code x<=1 && y<=2}, then only {@code y<=2} is extracted.
     * @param invariant the invariant to parse
     * @param ignored the variables to ignore
     * @return the parsed expression
     */
    public static Expression<String> parseInvariantButIgnore(final String invariant, final List<String> ignored) {
        if (invariant.trim().isEmpty()) return Literal.getTrue();

        final List<Expression<String>> expressions = new ArrayList<>();

        for (final String simpleInv : invariant.split("&&")) {
            final String REGEX = "^(\\w+)\\W.*";
            final Matcher matcher = Pattern.compile(REGEX).matcher(simpleInv.trim());

            if (!matcher.find()) throw new RuntimeException("Simple invariant " + simpleInv.trim() + " does not match " + REGEX);

            if (ignored.contains(matcher.group(1))) continue;

            expressions.add(Variable.of(simpleInv.trim()));
        }

        if (expressions.isEmpty()) return Literal.getTrue();

        return And.of(expressions);
    }

    /**
     * Gets if an expression is satisfied given some valuations.
     * @param expression expression to evaluate
     * @param valuations valuations of variables. These must include (but not necessarily limited to) all variables used in the condition
     * @return true iff the condition is satisfied
     */
    public static boolean evaluateBooleanExpression(String expression, final Map<String, Number> valuations) {
        for (final Map.Entry<String, Number> entry : valuations.entrySet()) {
            expression = expression.replace(entry.getKey(), String.valueOf(entry.getValue()));
        }

        return new SpelExpressionParser().parseExpression(expression).getValue(Boolean.class);
    }

    /**
     * Gets if an expression is satisfied given some valuations.
     * @param expression expression to evaluate
     * @param valuations valuations of variables. These must include (but not necessarily limited to) all variables used in the condition
     * @return true iff the condition is satisfied
     */
    public static boolean evaluateBooleanExpressionFromDoubles(final String expression, final Map<String, Double> valuations) {
        return evaluateBooleanExpression(expression, new HashMap<>(valuations));
    }

    /**
     * Parses an update property to a map of valuations.
     * Examples of update properties:
     * a=2
     * a:=2
     * a = 2
     * a=2,b=3
     * a=a+1
     * @param updateProperty the update property to parse
     * @param locals the local variables used to evaluate the right sides
     * @return a map of valuations
     */
    public static Map<String, Integer> parseUpdate(final String updateProperty, final Map<String, Integer> locals) {
        final Map<String, String> sides = getUpdateSides(updateProperty);

        final Map<String, Integer> valuations = new HashMap<>();

        sides.forEach((left, right) -> {
            final String[] expr = {right};

            locals.forEach((key, value1) -> expr[0] = expr[0].replace(key, String.valueOf(value1)));

            valuations.put(left, new SpelExpressionParser().parseExpression(expr[0]).getValue(Integer.TYPE));
        });

        return valuations;
    }

    /**
     * Get the left and right sides of an update property
     * @param updateProperty the property
     * @return the sides
     */
    public static Map<String, String> getUpdateSides(final String updateProperty) {

        final Map<String, String> sides = new HashMap<>();

        if (updateProperty.trim().isEmpty()) return sides;

        for (final String update : updateProperty.split(",")) {
            final String REGEX_UPDATE = "^(\\w+)\\s*:?=\\s*(.+)$";
            final Matcher matcher = Pattern.compile(REGEX_UPDATE).matcher(update.trim());

            if (!matcher.find()) throw new RuntimeException("Update " + update + " does not match " + REGEX_UPDATE);

            sides.put(matcher.group(1), matcher.group(2).trim());
        }

        return sides;
    }
}
