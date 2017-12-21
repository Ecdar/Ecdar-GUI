package SW9.presentations;

import SW9.Debug;
import SW9.abstractions.EdgeStatus;
import SW9.utility.colors.Color;
import SW9.utility.Highlightable;
import SW9.utility.helpers.SelectHelper;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.Group;
import javafx.scene.shape.Line;

public class Link extends Group implements SelectHelper.Selectable, Highlightable {

    private final static double HOVER_LINE_STROKE_WIDTH = 10d;
    private final DoubleProperty startX;
    private final DoubleProperty endX;
    private final DoubleProperty startY;
    private final DoubleProperty endY;
    private Line shownLine;

    /**
     * Creates a link with a solid line.
     */
    public Link() {
        this(0,0,0,0);
    }

    public Link(final double startX, final double endX, final double startY, final double endY) {
        // Set the initial values
        this.startX = new SimpleDoubleProperty(startX);
        this.endX = new SimpleDoubleProperty(endX);
        this.startY = new SimpleDoubleProperty(startY);
        this.endY = new SimpleDoubleProperty(endY);

        setUpLine();

        final Line hiddenHoverLine = new Line();

        // Add them
        getChildren().add(hiddenHoverLine);

        // Bind the hidden line
        hiddenHoverLine.startXProperty().bind(shownLine.startXProperty());
        hiddenHoverLine.endXProperty().bind(shownLine.endXProperty());
        hiddenHoverLine.startYProperty().bind(shownLine.startYProperty());
        hiddenHoverLine.endYProperty().bind(shownLine.endYProperty());

        // Style the hidden line
        hiddenHoverLine.setStrokeWidth(HOVER_LINE_STROKE_WIDTH);

        // Debug visuals
        hiddenHoverLine.setStroke(Debug.hoverableAreaColor.getColor(Debug.hoverableAreaColorIntensity));
        hiddenHoverLine.opacityProperty().bind(Debug.hoverableAreaOpacity);
    }

    /**
     * Creates the line to show.
     */
    private void setUpLine() {
        // Create the two lines
        shownLine = new Line();

        // Add them
        getChildren().add(shownLine);

        // Bind the shown line
        shownLine.startXProperty().bind(this.startX);
        shownLine.endXProperty().bind(this.endX);
        shownLine.startYProperty().bind(this.startY);
        shownLine.endYProperty().bind(this.endY);
    }

    /**
     * makes the link dashed.
     */
    public void makeDashed() {
        shownLine.getStrokeDashArray().addAll(6d);
    }


    /**
     * Make. the link solid.
     */
    public void makeSolid() {
        shownLine.getStrokeDashArray().clear();
    }

    public double getStartX() {
        return startX.get();
    }

    public void setStartX(final double startX) {
        this.startX.set(startX);
    }

    public DoubleProperty startXProperty() {
        return startX;
    }

    public double getEndX() {
        return endX.get();
    }

    public void setEndX(final double endX) {
        this.endX.set(endX);
    }

    public DoubleProperty endXProperty() {
        return endX;
    }

    public double getStartY() {
        return startY.get();
    }

    public void setStartY(final double startY) {
        this.startY.set(startY);
    }

    public DoubleProperty startYProperty() {
        return startY;
    }

    public double getEndY() {
        return endY.get();
    }

    public void setEndY(final double endY) {
        this.endY.set(endY);
    }

    public DoubleProperty endYProperty() {
        return endY;
    }

    @Override
    public void select() {
        shownLine.setStroke(SelectHelper.getNormalColor());
    }

    @Override
    public void deselect() {
        shownLine.setStroke(Color.GREY.getColor(Color.Intensity.I900));
    }

    /***
     * Highlights the Link with the normal color
     */
    @Override
    public void highlight() { shownLine.setStroke(SelectHelper.getNormalColor()); }

    /***
     * Removes the highlight by setting the color to grey
     */
    @Override
    public void unhighlight() {
        shownLine.setStroke(Color.GREY.getColor(Color.Intensity.I900));
    }
}
