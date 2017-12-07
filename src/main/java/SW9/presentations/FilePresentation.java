package SW9.presentations;

import SW9.abstractions.Component;
import SW9.abstractions.HighLevelModelObject;
import SW9.abstractions.SystemModel;
import SW9.controllers.CanvasController;
import SW9.controllers.FileController;
import SW9.utility.colors.Color;
import com.jfoenix.controls.JFXRippler;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.function.BiConsumer;

public class FilePresentation extends AnchorPane {
    private final SimpleObjectProperty<HighLevelModelObject> model = new SimpleObjectProperty<>(null);

    private final FileController controller;

    public FilePresentation(final HighLevelModelObject model) {
        controller = new EcdarFXMLLoader().loadAndGetController("FilePresentation.fxml", this);

        this.model.set(model);

        initializeIcon();
        initializeFileName();
        initializeColors();
        initializeRippler();
        initializeMoreInformationButton();
    }

    private void initializeMoreInformationButton() {
        if (getModel() instanceof Component || getModel() instanceof SystemModel) {
            controller.moreInformation.setVisible(true);
            controller.moreInformation.setMaskType(JFXRippler.RipplerMask.CIRCLE);
            controller.moreInformation.setPosition(JFXRippler.RipplerPos.BACK);
            controller.moreInformation.setRipplerFill(Color.GREY_BLUE.getColor(Color.Intensity.I500));
        }
    }

    private void initializeRippler() {
        final JFXRippler rippler = (JFXRippler) lookup("#rippler");

        final Color color = Color.GREY_BLUE;
        final Color.Intensity colorIntensity = Color.Intensity.I400;


        rippler.setMaskType(JFXRippler.RipplerMask.RECT);
        rippler.setRipplerFill(color.getColor(colorIntensity));
        rippler.setPosition(JFXRippler.RipplerPos.BACK);
    }

    private void initializeFileName() {
        final Label label = (Label) lookup("#fileName");

        model.get().nameProperty().addListener((obs, oldName, newName) -> label.setText(newName));
        label.setText(model.get().getName());
    }

    private void initializeIcon() {
        final Circle circle = (Circle) lookup("#iconBackground");
        final FontIcon icon = (FontIcon) lookup("#icon");

        model.get().colorProperty().addListener((obs, oldColor, newColor) -> {
            circle.setFill(newColor.getColor(model.get().getColorIntensity()));
            icon.setFill(newColor.getTextColor(model.get().getColorIntensity()));
        });

        circle.setFill(model.get().getColor().getColor(model.get().getColorIntensity()));
        icon.setFill(model.get().getColor().getTextColor(model.get().getColorIntensity()));
    }

    private void initializeColors() {
        final FontIcon moreInformationIcon = (FontIcon) lookup("#moreInformationIcon");

        final Color color = Color.GREY_BLUE;
        final Color.Intensity colorIntensity = Color.Intensity.I50;

        final BiConsumer<Color, Color.Intensity> setBackground = (newColor, newIntensity) -> {
            setBackground(new Background(new BackgroundFill(
                    newColor.getColor(newIntensity),
                    CornerRadii.EMPTY,
                    Insets.EMPTY
            )));

            setBorder(new Border(new BorderStroke(
                    newColor.getColor(newIntensity.next(2)),
                    BorderStrokeStyle.SOLID,
                    CornerRadii.EMPTY,
                    new BorderWidths(0, 0, 1, 0)
            )));

            moreInformationIcon.setFill(newColor.getColor(newIntensity.next(5)));
        };

        // Update the background when hovered
        setOnMouseEntered(event -> {
            if(CanvasController.getActiveModel().equals(model.get())) {
                setBackground.accept(color, colorIntensity.next(2));
            } else {
                setBackground.accept(color, colorIntensity.next());
            }
            setCursor(Cursor.HAND);
        });
        setOnMouseExited(event -> {
            if(CanvasController.getActiveModel().equals(model.get())) {
                setBackground.accept(color, colorIntensity.next(1));
            } else {
                setBackground.accept(color, colorIntensity);
            }
            setCursor(Cursor.DEFAULT);
        });

        CanvasController.activeComponentProperty().addListener((obs, oldActiveComponent, newActiveComponent) -> {
            if (newActiveComponent == null) return;


            if (newActiveComponent.equals(model.get())) {
                setBackground.accept(color, colorIntensity.next(2));
            } else {
                setBackground.accept(color, colorIntensity);
            }
        });

        // Update the background initially
        setBackground.accept(color, colorIntensity);
    }

    public HighLevelModelObject getModel() {
        return model.get();
    }
}
