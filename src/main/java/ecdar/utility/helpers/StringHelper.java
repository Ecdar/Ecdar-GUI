package ecdar.utility.helpers;

public final class StringHelper {
    public static String ConvertSymbolsToUnicode(String stringToReplace){
        return stringToReplace.replace(">=","\u2265").replace("<=","\u2264");
    }

    public static String ConvertUnicodeToSymbols(String stringToReplace){
        return stringToReplace.replace("\u2264","<=").replace("\u2265",">=");
    }
}
