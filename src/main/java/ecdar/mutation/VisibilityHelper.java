package ecdar.mutation;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.shape.SVGPath;

public class VisibilityHelper {
    private final static int ARROW_HEIGHT = 19; // Height of view containing the arrow for expanding and collapsing views. For labels, this is 19

    /**
     * Shows some regions and set them to be managed.
     * @param regions the regions
     */
    public static void show(final Node... regions) {
        for (final Node region : regions) {
            region.setManaged(true);
            region.setVisible(true);
        }
    }

    /**
     * Hides some regions and set them to not be managed.
     * @param regions the regions
     */
    public static void hide(final Node... regions) {
        for (final Node region : regions) {
            region.setManaged(false);
            region.setVisible(false);
        }
    }

    /**
     * Creates an arrow head path to draw expand and collapse graphics.
     * From: http://tech.chitgoks.com/2013/05/19/how-to-create-a-listview-with-title-header-that-expands-collapses-in-java-fx/
     * @param height height of the view it should match
     * @param up true iff the arrow should point up
     * @return the arrow head graphics
     */
    public static SVGPath createArrowPath(final int height, final boolean up) {
        final SVGPath svg = new SVGPath();
        final int width = height / 4;

        if (up)
            svg.setContent("M" + width + " 0 L" + (width * 2) + " " + width + " L0 " + width + " Z");
        else
            svg.setContent("M0 0 L" + (width * 2) + " 0 L" + width + " " + width + " Z");

        return svg;
    }

    public static void updateExpand(final boolean shouldShow, final Label header, final Node content) {
        if (shouldShow) {
            header.setGraphic(createArrowPath(true));
            show(content);
        } else {
            header.setGraphic(createArrowPath(false));
            hide(content);
        }
    }

    /**
     * Creates an arrow head path to draw expand and collapse graphics.
     * @param up true iff the arrow should point up
     * @return the arrow head graphics
     */
    public static SVGPath createArrowPath(final boolean up) {
        return createArrowPath(ARROW_HEIGHT, up);
    }

    /**
     * Makes a region show/hide when pressing a label.
     * @param label the label
     * @param region the region
     */
    public static void initializeExpand(final Label label, final Region region) {
        initializeExpand(new SimpleBooleanProperty(false), label, region);
    }

    /**
     * Makes a region show/hide when pressing a label.
     * @param label the label
     * @param region the region
     */
    public static void initializeExpand(final BooleanProperty show, final Label label, final Region region) {
        label.setGraphicTextGap(10);

        updateExpand(show.get(), label, region);

        label.setOnMousePressed(event -> {
            show.set(!show.get());
            updateExpand(show.get(), label, region);
        });
    }
}
