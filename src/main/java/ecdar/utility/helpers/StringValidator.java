package ecdar.utility.helpers;

public class StringValidator {
    public static boolean validateQuery(String input) {
        int openingParentheses = 0, closingParentheses = 0;
        
        for (char c : input.toCharArray()) {
            if (c == '(') openingParentheses++;
            else if (c == ')') closingParentheses++;
        }

        return openingParentheses == closingParentheses;
    }

    public static boolean validateComponentName(String input) {
        return !input.contains(".");
    }
}
