package ecdar.utility.helpers;

public class StringValidator {
    public static StringValidationType queryValidation = new StringValidationType() {
        @Override
        public boolean validate(String input) {
            int openingParentheses = 0, closingParentheses = 0;


            for (char c : input.toCharArray()) {
                if (c == '(') openingParentheses++;
                else if (c == ')') closingParentheses++;
            }

            return openingParentheses == closingParentheses;
        }
    };
    public static StringValidationType componentNameValidation = new StringValidationType() {
        @Override
        public boolean validate(String input) {
            return !input.contains(".");
        }
    };

    public static boolean validateString(String input, StringValidationType validationType) {
        return validationType.validate(input);
    };

    private abstract static class StringValidationType {
        public abstract boolean validate(String input);
    }
}
