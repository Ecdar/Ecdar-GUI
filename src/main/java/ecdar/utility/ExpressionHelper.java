package ecdar.utility;

import com.bpodgursky.jbool_expressions.*;
import org.springframework.expression.spel.standard.SpelExpressionParser;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Helper for working with expressions like the ones used in guards.
 */
public class ExpressionHelper {
    private static final String REGEX_SIMPLE_NEGATEABLE_GUARD = "^([^<>=!]+)(<|<=|>|>=|!=)([^<>=!]+)$";
    public static final String REGEX_UPDATE = "^(\\w+)\\s*:?=\\s*([\\S]+)$";

    /**
     * Searches recursively through the expression.
     * If a Not expression of a simple expression (e.g. x < 2) is found,
     * this is replaced with the negated expression (x >= 2).
     * To use effectively, make sure to put the expression into DNF before calling this method.
     * @param expression the expression to simplify
     * @return the simplified expression
     */
    public static Expression<String> simplifyNegatedSimpleExpressions(final Expression<String> expression) {
        switch (expression.getExprType()) {
            case Variable.EXPR_TYPE:
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
     * The guard can be a simple guard (e.g. x < 2) or a conjunction of simple guards.
     * @param guard the guard to parse
     * @return the expression
     */
    private static Expression<String> parseGuard(final String guard) {
        return And.of(
                Arrays.stream(guard.split("&&"))
                        .map(String::trim)
                        .map(ExpressionHelper::parseSimpleGuard)
                        .collect(Collectors.toList())
        );
    }

    /**
     * Parses a simple guards (without conjunctions) to an expression.
     * The equal operator (==) is parsed as a conjunction of <= and >=,
     * since the engine does not allow for != for clock valuations.
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
     * Gets if an expression is satisfied given some valuations.
     * @param expression expression to evaluate
     * @param valuations valuations of variables. These must include (but not necessarily limited to) all variables used in the condition
     * @return true iff the condition is satisfied
     */
    public static boolean evaluateBooleanExpression(String expression, final Map<String, Double> valuations) {
        for (final Map.Entry<String, Double> entry : valuations.entrySet()) {
            expression = expression.replace(entry.getKey(), String.valueOf(entry.getValue()));
        }

        return new SpelExpressionParser().parseExpression(expression).getValue(Boolean.class);
    }

    /**
     * Parses an update property to a map of valuations.
     * @param updateProperty the property
     * @return the valuations as a map from variable names to values
     */
    public static Map<String, Double> parseUpdateProperty(final String updateProperty) {
        final Map<String, Double> valuations = new HashMap<>();

        if (updateProperty.trim().isEmpty()) return valuations;

        for (final String update : updateProperty.split(",")) {
            final Matcher matcher = Pattern.compile(REGEX_UPDATE).matcher(update.trim());

            if (!matcher.find()) throw new RuntimeException("Update " + update + " does not match " + REGEX_UPDATE);

            valuations.put(matcher.group(1), Double.valueOf(matcher.group(2)));
        }

        return valuations;
    }
}
