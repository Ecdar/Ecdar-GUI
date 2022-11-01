package ecdar;

import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.testfx.api.FxToolkit;
import org.testfx.framework.junit.ApplicationTest;

import java.util.concurrent.TimeoutException;

public class TestFXBase extends ApplicationTest {
    @BeforeAll
    static void setUp() throws Exception {
        ApplicationTest.launch(Ecdar.class);
    }

    @Override
    public void start(Stage stage) {
        stage.show();
    }

    @AfterAll
    public void afterEachTest() throws TimeoutException {
        FxToolkit.hideStage();
        release(new KeyCode[]{});
        release(new MouseButton[]{});
    }
}
