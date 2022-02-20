package ecdar.presentations;

import ecdar.utility.UndoRedoStack;
import ecdar.utility.colors.Color;
import com.jfoenix.controls.JFXRippler;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import org.kordamp.ikonli.javafx.FontIcon;

public class UndoRedoHistoryEntryPresentation extends AnchorPane {

    private final UndoRedoStack.Command command;

    private final Color color;
    private final Color.Intensity colorIntensity;

    public UndoRedoHistoryEntryPresentation(final UndoRedoStack.Command command, final boolean isUndo) {
        this.command = command;

        new EcdarFXMLLoader().loadAndGetController("UndoRedoHistoryEntryPresentation.fxml", this);

        // Must be the indicator for the current state
        if (command == null) {
            color = Color.GREY_BLUE;
            colorIntensity = Color.Intensity.I500;
        } else if (isUndo) {
            color = Color.GREEN;
            colorIntensity = Color.Intensity.I600;
        } else {
            color = Color.DEEP_ORANGE;
            colorIntensity = Color.Intensity.I800;
        }

        initializeRippler();
        initializeIcon();
        initializeBackground();
        initializeLabel();
    }

    private void initializeLabel() {
        final Label label = (Label) lookup("#label");

        label.setTextFill(color.getTextColor(colorIntensity.next(-5)));

        if (command != null) {
            label.setText(command.getDescription());
        } else {
            label.setText("Current state");
        }
    }

    private void initializeRippler() {
        final JFXRippler rippler = (JFXRippler) lookup("#rippler");

        rippler.setMaskType(JFXRippler.RipplerMask.RECT);
        rippler.setRipplerFill(color.getColor(colorIntensity));
        rippler.setPosition(JFXRippler.RipplerPos.BACK);
    }

    private void initializeIcon() {
        final Circle circle = (Circle) lookup("#iconBackground");
        final FontIcon icon = (FontIcon) lookup("#icon");

        circle.setFill(color.getColor(colorIntensity));
        icon.setFill(color.getTextColor(colorIntensity));

        if (command != null) {
            icon.setIconLiteral("gmi-" + command.getIcon());
        }
    }

    private void initializeBackground() {
        setBackground(new Background(new BackgroundFill(
                color.getColor(colorIntensity.next(-5)),
                CornerRadii.EMPTY,
                Insets.EMPTY
        )));

        setBorder(new Border(new BorderStroke(
                color.getColor(colorIntensity.next(-5).next(2)),
                BorderStrokeStyle.SOLID,
                CornerRadii.EMPTY,
                new BorderWidths(0, 0, 1, 0)
        )));
    }

}
