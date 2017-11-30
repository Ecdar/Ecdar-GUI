package SW9.presentations;

import SW9.abstractions.Edge;
import SW9.abstractions.EdgeStatus;
import SW9.controllers.SignatureArrowController;
import javafx.fxml.FXMLLoader;
import javafx.fxml.JavaFXBuilderFactory;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.shape.Circle;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;

import java.io.IOException;
import java.net.URL;
import java.util.List;

public class SignatureArrow extends Group {

    private SignatureArrowController controller;

    public SignatureArrow(final String edgeName, final EdgeStatus edgeStatus){

        final URL location = this.getClass().getResource("SignatureArrowPresentation.fxml");

        final FXMLLoader fxmlLoader = new FXMLLoader();
        fxmlLoader.setLocation(location);
        fxmlLoader.setBuilderFactory(new JavaFXBuilderFactory());

        try {
            fxmlLoader.setRoot(this);
            fxmlLoader.load(location.openStream());

            controller = fxmlLoader.getController();


            //if(edgeName.isEmpty())
             //   return;

            final Path signatureArrow = controller.signatureArrowPath;
            controller.signatureArrowLabel.setText(edgeName);
            controller.signatureArrowLabel.setMaxWidth(100); // Limit the length of text on the arrow
            controller.signatureArrowLabel.setEllipsisString("…");// Inserts … when there's no more room for letters

            final int yValue = 0;
            final int xValue = 0;
            final MoveTo move1 = new MoveTo(xValue, yValue); // Starting loc of the arrow
            final LineTo line1 = new LineTo(xValue + 50, yValue); // Straight forward line
            final LineTo line2 = new LineTo(xValue + 35 , yValue-5); // Upper line in the arrow head
            final LineTo line3 = new LineTo(xValue + 35 , yValue+5); // Lower line in the arrow head

            signatureArrow.getElements().addAll(move1, line1, line2, line1, line3);

            signatureArrow.setStrokeWidth(1.0);
            final double radius = 2.0;
            if(edgeStatus == EdgeStatus.OUTPUT){
                signatureArrow.getStyleClass().add("dashed");
                controller.arrowBox.setAlignment(Pos.CENTER_LEFT);
                controller.signatureArrowCircle.setCenterX(xValue - radius);
                controller.signatureArrowCircle.setCenterY(yValue);
                controller.signatureArrowCircle.setRadius(radius);
            } else {
                controller.arrowBox.setAlignment(Pos.CENTER_RIGHT);
                controller.signatureArrowCircle.setCenterX(xValue + 50 + radius); // The circle should be at arrow head
                controller.signatureArrowCircle.setCenterY(yValue);
                controller.signatureArrowCircle.setRadius(radius);
            }
        } catch (final IOException ioe) {
            throw new IllegalStateException(ioe);
        }
    }
}
