package ecdar;

import ecdar.abstractions.Component;
import ecdar.controllers.ComponentController;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class ComponentControllerTest {

    @BeforeAll
    static void setup() {
        Ecdar.setUpForTest();
    }

    @Test
    public void testSomething() {
        // ToDo NIELS: Implement tests
        final Component comp = new Component(false);
        final ComponentController controller = new ComponentController();

        controller.setComponent(comp);

        Assertions.assertEquals(comp, controller.getComponent());
    }
}