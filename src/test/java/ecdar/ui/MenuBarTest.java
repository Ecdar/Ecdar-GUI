package ecdar.ui;

import org.junit.jupiter.api.Test;
import org.testfx.util.NodeQueryUtils;
import org.testfx.util.WaitForAsyncUtils;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.testfx.api.FxAssert.verifyThat;

public class MenuBarTest extends TestFXBase {
    @Test
    public void gridMenuItemHidesGrid() throws TimeoutException {
        clickOn("View");
        WaitForAsyncUtils.waitFor(20, TimeUnit.SECONDS, () ->
                lookup("#menuBarViewGrid").match(NodeQueryUtils.isVisible()).tryQuery().isPresent());
        clickOn("#menuBarViewGrid");

        verifyThat(lookup("#grid"), g -> g.getOpacity() == 0);
    }
}
