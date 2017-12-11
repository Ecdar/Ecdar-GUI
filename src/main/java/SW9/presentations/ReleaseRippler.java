package SW9.presentations;

import com.jfoenix.controls.JFXRippler;
import javafx.scene.Node;

public class ReleaseRippler extends JFXRippler {
    public ReleaseRippler(Node node) {
        super(node);
    }

    public void release() {
        this.releaseRipple();
    }
}
