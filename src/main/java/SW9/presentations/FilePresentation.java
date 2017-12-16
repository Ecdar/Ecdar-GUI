package SW9.presentations;

import SW9.Ecdar;
import SW9.abstractions.Component;
import SW9.abstractions.SystemModel;
import SW9.abstractions.HighLevelModelObject;
import SW9.controllers.CanvasController;
import SW9.controllers.FileController;
import SW9.utility.colors.Color;
import com.jfoenix.controls.JFXRippler;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
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
        initializeFileIcons();
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

        model.get().colorProperty().addListener((obs, oldColor, newColor) -> {
            circle.setFill(newColor.getColor(model.get().getColorIntensity()));
        });

        circle.setFill(model.get().getColor().getColor(model.get().getColorIntensity()));
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

    /**
     * Initialises the icons for the three different file types in Ecdar: Component, System and Declarations
     */
    private void initializeFileIcons() {
        if(model.get() instanceof Component){
            controller.fileImage.setImage(new Image(Ecdar.class.getResource("component_frame.png").toExternalForm()));
        } else if(model.get() instanceof SystemModel){
            controller.fileImage.setImage(new Image(Ecdar.class.getResource("system_frame.png").toExternalForm()));
        } else {
            controller.fileImage.setImage(new Image(Ecdar.class.getResource("description_frame.png").toExternalForm()));
        }
        EcdarPresentation.fitSizeWhenAvailable(controller.fileImage, controller.filePane);
    }

    public HighLevelModelObject getModel() {
        return model.get();
    }
}
