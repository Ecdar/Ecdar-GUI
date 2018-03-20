package ecdar.mutation.models;

/**
 * Some content with a title.
 * The content can be hidden.
 */
public class ExpandableContent {
    private final String title;
    private final String content;
    private boolean hidden = true;

    /**
     * Constructs.
     * @param title title
     * @param content content
     */
    public ExpandableContent(final String title, final String content) {
        this.title = title;
        this.content = content;
    }

    public String getTitle() {
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
