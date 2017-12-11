package SW9.presentations;

import com.jfoenix.controls.JFXRippler;
import javafx.scene.Node;

/***
 * We really needed the releaseRipple() method, so we made this wrapper class to expose it... shhh....
 */
public class ReleaseRippler extends JFXRippler {
    public ReleaseRippler(Node node) {
        super(node);
    }

    /***
     * Exposes the private releaseRipple() method to the public
     */
    public void release() {
        this.releaseRipple();
    }
}
