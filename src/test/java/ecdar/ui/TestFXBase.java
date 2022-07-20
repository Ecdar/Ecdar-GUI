package ecdar.ui;

import com.google.common.util.concurrent.SettableFuture;
import ecdar.Ecdar;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.testfx.api.FxToolkit;
import org.testfx.framework.junit.ApplicationTest;

import java.util.concurrent.TimeoutException;

public class TestFXBase extends ApplicationTest {
    public static SettableFuture<Stage> primaryStageFuture = SettableFuture.create();

    @BeforeEach
    public void setUp() throws Exception {
        ApplicationTest.launch(Ecdar.class);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        primaryStage.initStyle(StageStyle.UNDECORATED);
        primaryStageFuture.set(primaryStage);
    }

    @AfterEach
    public void afterEachTest() throws TimeoutException {
        FxToolkit.hideStage();
        release(new KeyCode[]{});
        release(new MouseButton[]{});
    }
}