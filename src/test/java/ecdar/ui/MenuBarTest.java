package ecdar.ui;

import org.junit.jupiter.api.Test;
import org.testfx.util.NodeQueryUtils;
import org.testfx.util.WaitForAsyncUtils;

import static org.testfx.api.FxAssert.verifyThat;

public class MenuBarTest extends TestFXBase {
    @Test
    public void gridMenuItemHidesGrid() {
        clickOn("View");
        WaitForAsyncUtils.waitForFxEvents(2);
        assert lookup("#menuBarViewGrid").match(NodeQueryUtils.isVisible()).tryQuery().isPresent();
        clickOn("#menuBarViewGrid");

        verifyThat(lookup("#grid"), g -> g.getOpacity() == 0);
    }
}
