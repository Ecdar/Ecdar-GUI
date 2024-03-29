package ecdar.controllers;

import com.jfoenix.controls.JFXRippler;
import ecdar.Ecdar;
import ecdar.abstractions.Component;
import ecdar.abstractions.EcdarSystem;
import ecdar.abstractions.HighLevelModel;
import ecdar.mutation.models.MutationTestPlan;
import ecdar.utility.colors.Color;
import ecdar.utility.colors.EnabledColor;
import ecdar.utility.helpers.ImageScaler;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import org.kordamp.ikonli.javafx.FontIcon;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Controller for a file in the project pane.
 */
public class FileController implements Initializable {
    public AnchorPane root;
    public JFXRippler rippler;
    public Circle iconBackground;
    public JFXRippler moreInformation;
    public FontIcon moreInformationIcon;
    public Label fileName;
    public ImageView fileImage;
    public StackPane fileImageStackPane;

    private final SimpleObjectProperty<HighLevelModel> model = new SimpleObjectProperty<>(null);
    private final BooleanProperty isActive = new SimpleBooleanProperty(false);
    private final EnabledColor color = EnabledColor.getDefault().getLowestIntensity();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        Platform.runLater(() -> {
            initializeIcon();
            initializeFileIcons();
            initializeFileName();
            initializeColors();
            initializeRippler();
            initializeMoreInformationButton();
        });
    }

    private void initializeMoreInformationButton() {
        if (getModel() instanceof Component || getModel() instanceof EcdarSystem || getModel() instanceof MutationTestPlan) {
            moreInformation.setVisible(true);
            moreInformation.setMaskType(JFXRippler.RipplerMask.CIRCLE);
            moreInformation.setPosition(JFXRippler.RipplerPos.BACK);
            moreInformation.setRipplerFill(Color.GREY_BLUE.getColor(Color.Intensity.I500));
        }
    }

    private void initializeRippler() {
        final Color color = Color.GREY_BLUE;
        final Color.Intensity colorIntensity = Color.Intensity.I400;

        rippler.setMaskType(JFXRippler.RipplerMask.RECT);
        rippler.setRipplerFill(color.getColor(colorIntensity));
        rippler.setPosition(JFXRippler.RipplerPos.BACK);
    }

    private void initializeFileName() {
        model.get().nameProperty().addListener((obs, oldName, newName) -> fileName.setText(newName));
        fileName.setText(model.get().getName());
    }

    private void initializeIcon() {
        model.get().colorProperty().addListener((obs, oldColor, newColor) -> iconBackground.setFill(newColor.getPaintColor()));
        iconBackground.setFill(model.get().getColor().getPaintColor());
    }

    private void initializeColors() {
        // Update the background when hovered
        root.setOnMouseEntered(event -> {
            if (isActive.get()) {
                setBackground(color.nextIntensity(2));
            } else {
                setBackground(color.nextIntensity());
            }
            root.setCursor(Cursor.HAND);
        });

        root.setOnMouseExited(event -> {
            if (isActive.get()) {
                setBackground(color.nextIntensity());
            } else {
                setBackground(color);
            }
            root.setCursor(Cursor.DEFAULT);
        });

        // Update the background initially
        setBackground(color);
    }

    /**
     * Initialises the icons for the three different file types in Ecdar: Component, System and Declarations
     */
    private void initializeFileIcons() {
        if (model.get() instanceof Component) {
            fileImage.setImage(new Image(Ecdar.class.getResource("component_frame.png").toExternalForm()));
        } else if (model.get() instanceof EcdarSystem) {
            fileImage.setImage(new Image(Ecdar.class.getResource("system_frame.png").toExternalForm()));
        } else if (model.get() instanceof MutationTestPlan) {
            fileImage.setImage(new Image(Ecdar.class.getResource("test_frame.png").toExternalForm()));
        } else {
            fileImage.setImage(new Image(Ecdar.class.getResource("description_frame.png").toExternalForm()));
        }

        ImageScaler.fitImageToPane(fileImage, fileImageStackPane);
    }

    private void setBackground(EnabledColor newColor) {
        root.setBackground(new Background(new BackgroundFill(
                newColor.getPaintColor(),
                CornerRadii.EMPTY,
                Insets.EMPTY
        )));

        root.setBorder(new Border(new BorderStroke(
                newColor.getStrokeColor(),
                BorderStrokeStyle.SOLID,
                CornerRadii.EMPTY,
                new BorderWidths(0, 0, 1, 0)
        )));

        moreInformationIcon.setFill(newColor.nextIntensity(5).getPaintColor());
    }

    public void setModel(HighLevelModel newModel) {
        model.set(newModel);
    }

    public HighLevelModel getModel() {
        return model.get();
    }

    public void setIsActive(boolean active) {
        isActive.set(active);

        if (active) {
            setBackground(color.nextIntensity());
        } else {
            setBackground(color);
        }
    }
}
