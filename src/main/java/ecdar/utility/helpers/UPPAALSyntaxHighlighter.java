package ecdar.utility.helpers;

import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import java.util.Collection;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UPPAALSyntaxHighlighter {
    private static final String uppaalKeywords = "clock|chan|urgent|broadcast";
    private static final String cKeywords = "auto|bool|break|case|char|const|continue|default|do|double|else|enum|extern|float|for|goto|if|int|long|register|return|short|signed|sizeof|static|struct|switch|typedef|union|unsigned|void|volatile|while";
    private static final Pattern UPPAAL = Pattern.compile(""
            + "(" + uppaalKeywords + ")"
            + "|(" + cKeywords + ")"
            + "|(//.*|(\"(?:\\\\[^\"]|\\\\\"|.)*?\")|(?s)/\\*.*?\\*/)");

    public static StyleSpans<Collection<String>> computeHighlighting(final String text) {
        final Matcher matcher = UPPAAL.matcher(text);
        int lastKwEnd = 0;
        final StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();
        while (matcher.find()) {
            spansBuilder.add(Collections.emptyList(), matcher.start() - lastKwEnd);

            if (matcher.group(1) != null) {
                spansBuilder.add(Collections.singleton("uppaal-keyword"), matcher.end(1) - matcher.start(1));
            } else if (matcher.group(2) != null) {
                spansBuilder.add(Collections.singleton("c-keyword"), matcher.end(2) - matcher.start(2));
            } else if (matcher.group(3) != null) {
                spansBuilder.add(Collections.singleton("comment"), matcher.end(3) - matcher.start(3));
            }

            lastKwEnd = matcher.end();
        }

        spansBuilder.add(Collections.emptyList(), text.length() - lastKwEnd);
        return spansBuilder.create();
    }
}
