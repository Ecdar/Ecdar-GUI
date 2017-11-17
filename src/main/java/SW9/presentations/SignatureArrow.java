package SW9.presentations;

import SW9.abstractions.Edge;
import SW9.abstractions.EdgeStatus;
import SW9.controllers.SignatureArrowController;
import javafx.fxml.FXMLLoader;
import javafx.fxml.JavaFXBuilderFactory;
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
            //final Edge firstEdge = edges.get(0);
            //String syncText = firstEdge.getSync();
            controller.signatureArrowLabel.setText(edgeName);


            //final Path initialPath = new Path();
            final int yValue = 0;
            final int xValue = 0;
            final MoveTo move1 = new MoveTo(xValue, yValue);
            final LineTo line1 = new LineTo(xValue + 40, yValue);
            final LineTo line2 = new LineTo(xValue + 30, yValue-5);
            final LineTo line3 = new LineTo(xValue + 30, yValue+5);

            signatureArrow.getElements().addAll(move1, line1, line2, line1, line3);

            //final Color componentColor = controller.getComponent().getColor();
            //final Color.Intensity componentColorIntensity = controller.getComponent().getColorIntensity();

            //initialPath.setFill(componentColor.getColor(componentColorIntensity.next(-1)));
            //initialPath.setStroke(componentColor.getColor(componentColorIntensity.next(2)));
            signatureArrow.setStrokeWidth(1.0);
            if(edgeStatus == EdgeStatus.OUTPUT){
                signatureArrow.getStyleClass().add("dashed");

                final double radius = 2.0;
                controller.signatureArrowCircle.setCenterX(xValue - radius);
                controller.signatureArrowCircle.setCenterY(yValue);
                controller.signatureArrowCircle.setRadius(radius);
            } else {
                final double radius = 2.0;
                controller.signatureArrowCircle.setCenterX(xValue + 40 + radius);
                controller.signatureArrowCircle.setCenterY(yValue);
                controller.signatureArrowCircle.setRadius(radius);
            }
        } catch (final IOException ioe) {
            throw new IllegalStateException(ioe);
        }
    }
}
