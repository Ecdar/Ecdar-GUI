package ecdar.utility.helpers;

import ecdar.utility.mouse.MouseTracker;

public interface MouseTrackable extends LocationAware {
    MouseTracker getMouseTracker();
}
