package ecdar.utility;

import com.bpodgursky.jbool_expressions.*;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ExpressionHelper {
    private static final String REGEX_SIMPLE_NEGATEABLE_GUARD = "^([^<>=!]+)(<|<=|>|>=|!=)([^<>=!]+)$";

    public static Expression<String> negateSimpleExpressions(final Expression<String> expression) {
        switch (expression.getExprType()) {
            case Variable.EXPR_TYPE:
                return expression;
            case Not.EXPR_TYPE:
                final Expression<String> child = ((Not<String>) expression).getE();

                if (!child.getExprType().equals(Variable.EXPR_TYPE))
                    throw new RuntimeException("Unexpected negation. Expressions \"" + expression.toString() + "\" should be in CNF");

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
                        return Variable.of(matcher.group(1) + ">" + matcher.group(3));
                    case "!=":
                        return Variable.of(matcher.group(1) + "==" + matcher.group(3));
                    default:
                        throw new RuntimeException("Unexpected operator " + matcher.group(2) + "");
                }
            case And.EXPR_TYPE:
                return And.of(((And<String>) expression).getChildren().stream()
                        .map(ExpressionHelper::negateSimpleExpressions)
                        .collect(Collectors.toList()));
            case Or.EXPR_TYPE:
                return Or.of(((Or<String>) expression).getChildren().stream()
                        .map(ExpressionHelper::negateSimpleExpressions)
                        .collect(Collectors.toList()));
            default:
                throw new RuntimeException("Type of expression " + expression + " not accepted");
        }
    }

    public static Expression<String> toExpression(final List<String> guards) {
        return Or.of(
                guards.stream()
                        .map(ExpressionHelper::guardToExpression)
                        .collect(Collectors.toList())
        );
    }

    private static Expression<String> simpleGuardToExpression(final String simpleGuard) {
        if (simpleGuard.contains("=="))
            return And.of(
                    Variable.of(simpleGuard.replaceFirst("==", "<=")),
                    Variable.of(simpleGuard.replaceFirst("==",">="))
            );
        else
            return Variable.of(simpleGuard);
    }

    private static Expression<String> guardToExpression(final String guard) {
        return And.of(
                Arrays.stream(guard.split("&&"))
                        .map(String::trim)
                        .map(ExpressionHelper::simpleGuardToExpression)
                        .collect(Collectors.toList())
        );
    }
}
