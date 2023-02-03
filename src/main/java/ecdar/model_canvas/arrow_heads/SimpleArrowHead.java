package ecdar.model_canvas.arrow_heads;

import ecdar.utility.colors.Color;
import ecdar.utility.Highlightable;
import ecdar.utility.helpers.SelectHelper;
import javafx.scene.shape.Line;

public class SimpleArrowHead extends ArrowHead implements SelectHelper.Selectable, Highlightable {

    private static final double TRIANGLE_LENGTH = 15;
    private static final double TRIANGLE_WIDTH = 10;

    private Line leftArrow;
    private Line rightArrow;

    public SimpleArrowHead() {
        super();

        getChildren().addAll(initializeLeftArrow(), initializeRightArrow());
    }

    private Line initializeLeftArrow() {
        leftArrow = new Line();
        leftArrow.startXProperty().bind(xProperty());
        leftArrow.startYProperty().bind(yProperty());
        leftArrow.endXProperty().bind(xProperty().subtract(TRIANGLE_WIDTH / 2));
        leftArrow.endYProperty().bind(yProperty().subtract(TRIANGLE_LENGTH));

        leftArrow.getStyleClass().add("link");

        return leftArrow;
    }

    private Line initializeRightArrow() {
        rightArrow = new Line();
        rightArrow.startXProperty().bind(xProperty());
        rightArrow.startYProperty().bind(yProperty());
        rightArrow.endXProperty().bind(xProperty().add(TRIANGLE_WIDTH / 2));
        rightArrow.endYProperty().bind(yProperty().subtract(TRIANGLE_LENGTH));

        rightArrow.getStyleClass().add("link");

        return rightArrow;
    }

    @Override
    public double getHeadHeight() {
        return TRIANGLE_LENGTH;
    }

    @Override
    public double getHeadWidth() {
        return TRIANGLE_WIDTH;
    }

    @Override
    public boolean shouldBindToTip() {
        return true;
    }

    @Override
    public void mark() {
        leftArrow.getStyleClass().add("selected");
        rightArrow.getStyleClass().add("selected");
    }

    @Override
    public void unmark() {
        leftArrow.getStyleClass().remove("selected");
        rightArrow.getStyleClass().remove("selected");
    }

    @Override
    public void select() {
        leftArrow.setStroke(SelectHelper.getNormalColor());
        rightArrow.setStroke(SelectHelper.getNormalColor());
    }

    @Override
    public void deselect() {
        leftArrow.setStroke(Color.GREY.getColor(Color.Intensity.I900));
        rightArrow.setStroke(Color.GREY.getColor(Color.Intensity.I900));
    }

    /***
     * Highlights the two lines in the arrow head by setting their stroke to the normal color
     */
    @Override
    public void highlight() {
        leftArrow.setStroke(SelectHelper.getNormalColor());
        rightArrow.setStroke(SelectHelper.getNormalColor());
    }

    @Override
    public void highlightPurple() {
        leftArrow.setStroke(Color.DEEP_PURPLE.getColor(Color.Intensity.I900));
        rightArrow.setStroke(Color.DEEP_PURPLE.getColor(Color.Intensity.I900));
    }

    /***
     * Removes the highlight by setting the stroke to grey
     */
    @Override
    public void unhighlight() {
        leftArrow.setStroke(Color.GREY.getColor(Color.Intensity.I900));
        rightArrow.setStroke(Color.GREY.getColor(Color.Intensity.I900));
    }

    @Override
    public double getX() {
        return xProperty().get();
    }

    @Override
    public double getY() {
        return yProperty().get();
    }
}
