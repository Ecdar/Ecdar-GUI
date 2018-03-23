package ecdar.mutation.models;

import javafx.scene.text.TextFlow;

/**
 * Some content with a title.
 * The content can be hidden.
 */
public class ExpandableContent {
    private final TextFlow title;
    private final String content;
    private boolean hidden = true;

    /**
     * Constructs.
     * @param title title
     * @param content content
     */
    public ExpandableContent(final TextFlow title, final String content) {
        this.title = title;
        this.content = content;
    }

    public TextFlow getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public boolean isHidden() {
        return hidden;
    }

    public void setHidden(final boolean hidden) {
        this.hidden = hidden;
    }
}
