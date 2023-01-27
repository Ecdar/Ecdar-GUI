package ecdar.presentations;

import ecdar.abstractions.Component;
import ecdar.abstractions.EdgeStatus;
import ecdar.controllers.EcdarController;
import ecdar.controllers.SignatureArrowController;
import ecdar.utility.colors.Color;
import ecdar.utility.Highlightable;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;

import java.util.Objects;

/***
 * Creates input and output arrows, that can for example be used on the side of components to show its signature
 */
public class SignatureArrow extends Group implements Highlightable {

    private SignatureArrowController controller;

    public String getSignatureArrowLabel() {
        return controller.signatureArrowLabel.getText();
    }


    /***
     * Create a new signature arrow with a label for an edge, and different look depending on whether it's
     * an input or output arrow
     * @param edgeName The string to show in the arrow's label
     * @param edgeStatus The EdgeStatus of the arrow
     * @param component The Component where the edge is located
     */
    public SignatureArrow(final String edgeName, final EdgeStatus edgeStatus, final Component component) {
        controller = new EcdarFXMLLoader().loadAndGetController("SignatureArrowPresentation.fxml", this);

        controller.setComponent(component);
        controller.setEdgeStatus(edgeStatus);
        controller.setSyncText(edgeName);

        drawArrow(edgeName, edgeStatus);
        unhighlight();
        initializeMouseEvents();
    }

    /***
     * Initializes the mouse events e.g. mouseEntered leads to the arrow being highlighted
     */
    private void initializeMouseEvents() {
        controller.arrowBox.onMouseEnteredProperty().set(event -> {
            controller.mouseEntered();
            this.highlight();
        });
        controller.arrowBox.onMouseExitedProperty().set(event -> {
            controller.mouseExited();
            this.unhighlight();
        });
    }

    /***
     * Draws an arrow (dashed or solid) with a label above
     * @param edgeName The string to show in the arrow's label
     * @param edgeStatus The EdgeStatus of the arrow
     */
    private void drawArrow(final String edgeName, final EdgeStatus edgeStatus) {
        controller.signatureArrowLabel.setText(edgeName);
        controller.signatureArrowLabel.setMaxWidth(100); // Limit the length of text on the arrow
        controller.signatureArrowLabel.setEllipsisString("…");// Inserts … when there's no more room for letters

        // Draw the straight line of the arrow
        final Path arrowLine = controller.signatureArrowPath;
        final int yValue = 0;
        final int xValue = 0;
        final MoveTo move1 = new MoveTo(xValue, yValue); // Starting loc of the arrow
        final LineTo line1 = new LineTo(xValue + 50, yValue); // Straight forward line

        arrowLine.getElements().addAll(move1, line1);
        arrowLine.setStrokeWidth(1.0);

        // Draw the arrow head separately, so it is for example possible to have a dashed line and solid head
        final Path arrowHead = controller.signatureArrowHeadPath;
        final MoveTo moveHead = new MoveTo(xValue + 50, yValue);
        final LineTo lineHead1 = new LineTo(xValue + 35, yValue - 5); // Upper line in the arrow head
        final LineTo lineHead2 = new LineTo(xValue + 35, yValue + 5); // Lower line in the arrow head

        arrowHead.getElements().addAll(moveHead, lineHead1, moveHead, lineHead2);
        arrowHead.setStrokeWidth(1.0);

        final double radius = 2.0; // Radius for the circle
        if (edgeStatus == EdgeStatus.OUTPUT) {
            arrowLine.getStyleClass().add("dashed");
            controller.arrowBox.setAlignment(Pos.CENTER_LEFT);
            controller.signatureArrowCircle.setCenterX(xValue - radius); // Place the circle partly on the line
            controller.signatureArrowCircle.setCenterY(yValue);
            controller.signatureArrowCircle.setRadius(radius);
        } else {
            controller.arrowBox.setAlignment(Pos.CENTER_RIGHT);
            controller.signatureArrowCircle.setCenterX(xValue + 50 + radius); // The circle should be at arrow head
            controller.signatureArrowCircle.setCenterY(yValue);
            controller.signatureArrowCircle.setRadius(radius);
        }
    }

    /***
     * Highlights the arrow's components by coloring them orange
     */
    @Override
    public void highlight() {
        final Color color = Color.DEEP_ORANGE;
        final Color.Intensity intensity = Color.Intensity.I500;

        this.colorArrowComponents(color, intensity);
    }

    /***
     * Removes the highlight from the arrow's components
     */
    @Override
    public void unhighlight() {
        Color color;
        // If sync is both input and output within component, make the arrow red
        if (controller.getComponent().getInputStrings().contains(controller.getSyncText())
                && controller.getComponent().getOutputStrings().contains(controller.getSyncText())) {
            color = Color.RED;

            // Color matching SignatureArrow red
            SignatureArrow matchingSignatureArrow;
            if (controller.getEdgeStatus().equals(EdgeStatus.INPUT)) {
                matchingSignatureArrow = (SignatureArrow) EcdarController.getActiveCanvasPresentation().getController()
                        .activeComponentPresentation.getController()
                        .outputSignatureContainer.getChildren()
                        .stream().filter(node -> node instanceof SignatureArrow && ((SignatureArrow) node).controller.getSyncText().equals(controller.getSyncText()))
                        .findFirst().orElse(null);

            } else {
                matchingSignatureArrow = (SignatureArrow) EcdarController.getActiveCanvasPresentation().getController()
                        .activeComponentPresentation.getController()
                        .inputSignatureContainer.getChildren()
                        .stream().filter(node -> node instanceof SignatureArrow && ((SignatureArrow) node).controller.getSyncText().equals(controller.getSyncText()))
                        .findFirst().orElse(null);

            }
            if (matchingSignatureArrow != null) matchingSignatureArrow.recolorToRed();

        } else {
            color = Color.GREY_BLUE;
        }

        Color.Intensity intensity = Color.Intensity.I800;
        this.colorArrowComponents(color, intensity);

        for (String s : controller.getComponent().getFailingIOStrings()) {
            if (Objects.equals(getSignatureArrowLabel(), s) && controller.getComponent().getIsFailing()) {
                this.recolorToRed();
            }
        }
    }

    /**
     * Set the color of the SignatureArrow to Color.RED
     */
    public void recolorToRed() {
        Color color = Color.RED;
        Color.Intensity intensity = Color.Intensity.I800;
        this.colorArrowComponents(color, intensity);
    }

    public void recolorToGray() {
        Color color = Color.GREY;
        Color.Intensity intensity = Color.Intensity.I800;
        this.colorArrowComponents(color, intensity);
    }

    /**
     * Colors the components of the arrow
     * @param color The Color to color the components
     * @param intensity The Intensity of the color
     */
    private void colorArrowComponents(Color color, Color.Intensity intensity) {
        controller.signatureArrowPath.setFill(color.getColor(intensity));
        controller.signatureArrowPath.setStroke(color.getColor(intensity.next(2)));

        controller.signatureArrowHeadPath.setFill(color.getColor(intensity));
        controller.signatureArrowHeadPath.setStroke(color.getColor(intensity.next(2)));

        controller.signatureArrowCircle.setFill(color.getColor(intensity));
        controller.signatureArrowCircle.setStroke(color.getColor(intensity.next(2)));

        controller.signatureArrowLabel.setTextFill(color.getColor(intensity));
    }
}
