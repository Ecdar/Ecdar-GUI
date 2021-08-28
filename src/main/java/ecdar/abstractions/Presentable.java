package ecdar.abstractions;

/**
 * An interface used on views, that can be notified so they for example can disable keyboard shortcuts
 */
public interface Presentable {
    /**
     * Called when a view is about to be presented, allows the view to for example enable keybindings
     */
    void willShow();
    /**
     * Called when a view is about to be hidden, allows the view to for example disable keybindings
     */
    void willHide();
}
