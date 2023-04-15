package ecdar.utility.helpers;

import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;

public class ImageScaler {
    public static void fitImageToPane(final ImageView imageView, final StackPane pane) {
        pane.widthProperty().addListener((observable, oldValue, newValue) ->
                imageView.setFitWidth(pane.getWidth()));
        pane.heightProperty().addListener((observable, oldValue, newValue) ->
                imageView.setFitHeight(pane.getHeight()));

        imageView.setFitWidth(pane.getWidth());
        imageView.setFitHeight(pane.getHeight());
    }
}
