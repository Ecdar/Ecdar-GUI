package ecdar.ui;

import ecdar.presentations.FilePresentation;
import ecdar.utility.colors.Color;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;

import java.util.Set;
import java.util.function.Predicate;

public class SidePaneTest extends TestFXBase {
    @Test
    public void activeFilePresentationHasDifferentColorInProjectPane() {
        clickOn("#createComponent");

        var baseColorIntensity = Color.Intensity.I50;
        var baseBackgroundFill = new BackgroundFill(
                ecdar.utility.colors.Color.GREY_BLUE.getColor(baseColorIntensity),
                CornerRadii.EMPTY,
                Insets.EMPTY
        );

        FilePresentation comp1 = from(lookup("#leftPane").queryAll()).lookup((Predicate<Node>) child -> child instanceof FilePresentation &&
                ((FilePresentation) child).getModel().getName().equals("Component1")).query();
        Assertions.assertEquals(comp1.getBackground().getFills().get(0), baseBackgroundFill);

        var activeColorIntensity = Color.Intensity.I50.next(1);
        var activeBackgroundFill = new BackgroundFill(
                ecdar.utility.colors.Color.GREY_BLUE.getColor(activeColorIntensity),
                CornerRadii.EMPTY,
                Insets.EMPTY
        );

        FilePresentation comp2 = from(lookup("#leftPane").queryAll()).lookup((Predicate<Node>) child -> child instanceof FilePresentation &&
                ((FilePresentation) child).getModel().getName().equals("Component2")).query();
        Assertions.assertEquals(comp2.getBackground().getFills().get(0), activeBackgroundFill);
    }

    @Test
    public void whenDeclarationIsPressedFilePresentationsAreNotActive() {
        clickOn("Global Declarations");

        var baseColorIntensity = Color.Intensity.I50;
        var baseBackgroundFill = new BackgroundFill(
                ecdar.utility.colors.Color.GREY_BLUE.getColor(baseColorIntensity),
                CornerRadii.EMPTY,
                Insets.EMPTY
        );

        Set<FilePresentation> filePresentations = from(lookup("#leftPane").queryAll()).lookup((Predicate<Node>) child -> child instanceof FilePresentation &&
                !((FilePresentation) child).getModel().getName().equals("Global Declarations")).queryAll();
        for (FilePresentation fp : filePresentations) {
            Assertions.assertEquals(fp.getBackground().getFills().get(0), baseBackgroundFill);
        }
    }
}
