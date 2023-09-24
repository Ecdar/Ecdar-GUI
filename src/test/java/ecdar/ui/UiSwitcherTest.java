package ecdar.ui;

import org.junit.jupiter.api.Test;
import org.testfx.util.WaitForAsyncUtils;

import com.jfoenix.controls.JFXComboBox;

import ecdar.Ecdar;
import ecdar.abstractions.Query;
import ecdar.abstractions.QueryState;
import ecdar.controllers.EcdarController;
import ecdar.controllers.EcdarController.Mode;
import javafx.application.Platform;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class UiSwitcherTest extends TestFXBase {
    @Test
    public void UiSwitcherNoComponentsTest() {
        Ecdar.setUpForTest();
        WaitForAsyncUtils.waitForFxEvents();
        clickOn("#switchGuiView");
        WaitForAsyncUtils.waitForFxEvents();
        JFXComboBox<String> simDialog = lookup("#simulationComboBox").query();
        WaitForAsyncUtils.waitForFxEvents();
        assertFalse(simDialog.isShowing());

        WaitForAsyncUtils.waitForFxEvents();
        Platform.runLater(() -> Ecdar.getProject().getQueries().add(new Query("(Component1)", "null", QueryState.UNKNOWN)));
        WaitForAsyncUtils.waitForFxEvents();
        clickOn("#switchGuiView");
        WaitForAsyncUtils.waitForFxEvents();
        JFXComboBox<String> simDialogComboBox = lookup("#simulationComboBox").query();
        Platform.runLater(() -> simDialogComboBox.selectionModelProperty().get().select(0));
        WaitForAsyncUtils.waitForFxEvents();
        clickOn("#startButton");
        WaitForAsyncUtils.waitForFxEvents();
        assertEquals(EcdarController.currentMode.get(), Mode.Simulator);
    }
}
