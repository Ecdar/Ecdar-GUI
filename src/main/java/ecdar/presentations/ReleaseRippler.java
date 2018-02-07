package ecdar.presentations;

import com.jfoenix.controls.JFXRippler;
import javafx.scene.Node;

/***
 * We really needed the {@link JFXRippler#releaseRipple()} method, so we made this wrapper class to expose it
 */
public class ReleaseRippler extends JFXRippler {
    public ReleaseRippler(Node node) {
        super(node);
    }

    /***
     * Exposes the protected {@link JFXRippler#releaseRipple()} method to the public
     */
    public void release() {
        this.releaseRipple();
    }
}
