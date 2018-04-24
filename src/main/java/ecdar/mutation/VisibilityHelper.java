package ecdar.mutation;

import ecdar.mutation.models.TestResult;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.Text;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.Arrays;

/**
 * Collection of helper methods to show, hide, expand, and collapse UI elements.
 */
public class VisibilityHelper {
    private final static int ARROW_HEIGHT = 19; // Height of view containing the arrow for expanding and collapsing views. For labels, this is 19
    public static final int EXPAND_INSET = 5;


    /**
     * Shows or hides som regions.
     * @param show true if should show, false if should hide
     * @param regions the regions to show or hide
     */
    public static void setVisibility(final boolean show, final Node... regions) {
        if (show) show(regions);
        else hide(regions);
    }

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
     * Shows or hides some content and adjusts the graphic on the header with an arrow.
     * @param shouldShow true iff should show
     * @param header the header
     * @param content the content
     */
    public static void updateExpand(final boolean shouldShow, final Label header, final Node content) {
        if (shouldShow) {
            header.setGraphic(getExpandLess());
            show(content);
        } else {
            header.setGraphic(getExpandMore());
            hide(content);
        }
    }

    private static FontIcon getExpandLess() {
        return new FontIcon("gmi-expand-less");
    }

    private static FontIcon getExpandMore() {
        return new FontIcon("gmi-expand-more");
    }

    /**
     * Shows or hides some content and adjusts the graphic on the header with an arrow.
     * @param shouldShow true iff should show
     * @param header the header
     * @param content the content
     */
    public static void updateExpand(final boolean shouldShow, final Label header, final Node content, final TestResult.Verdict verdict) {
        if (shouldShow) {
            final FontIcon icon = getExpandLess();
            icon.setIconColor(getColor(verdict));
            header.setGraphic(icon);
            show(content);
        } else {
            final FontIcon icon = new FontIcon("gmi-expand-more");
            icon.setIconColor(getColor(verdict));
            header.setGraphic(icon);
            hide(content);
        }
    }

    private static Paint getColor(final TestResult.Verdict verdict) {
        if (verdict == TestResult.Verdict.PASS) return getPassedColor();
        if (Arrays.stream(TestResult.getIncVerdicts()).anyMatch(v -> v == verdict)) return getIncColor();
        if (Arrays.stream(TestResult.getFailedVerdicts()).anyMatch(v -> v == verdict)) return getFailedColor();
        throw new IllegalArgumentException("Verdict " + verdict + " not expected");
    }

    /**
     * Makes some content show/hide when pressing a header.
     * @param header the header
     * @param content the the content
     */
    public static void initializeExpand(final Label header, final Region content) {
        initializeExpand(new SimpleBooleanProperty(false), header, content);
    }

    /**
     * Makes some content show/hide when pressing a header.
     * @param show boolean variable to content if the content should be shown
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

    public static void setPassedText(final int number, final Text text) {
        text.setText(Integer.toString(number));
        text.setFill(getPassedPaint(number));
    }

    public static void setIncText(final int number, final Text text) {
        text.setText(Integer.toString(number));
        text.setFill(getIncPaint(number));
    }

    public static void setFailedText(final int number, final Text text) {
        text.setText(Integer.toString(number));
        text.setFill(getFailedPaint(number));
    }

    public static Paint getPassedPaint(final int number) {
        if (number == 0) return getDefaultTextColor();
        else return Color.GREEN;
    }

    private static Paint getPassedColor() {
        return Color.GREEN;
    }

    public static Paint getIncPaint(final int number) {
        if (number == 0) return getDefaultTextColor();
        else return getIncColor();
    }

    private static Paint getIncColor() {
        return Color.DARKORANGE;
    }

    public static Paint getFailedPaint(final int number) {
        if (number == 0) return getDefaultTextColor();
        else return Color.RED;
    }

    private static Paint getFailedColor() {
        return Color.RED;
    }

    public static Paint getDefaultTextColor() {
        return Color.web("#333333");
    }

    public static HBox expand(final Node content) {
        final HBox hBox = new HBox(8, new Separator(Orientation.VERTICAL), content);
        hBox.setPadding(new Insets(0, 0, 0, EXPAND_INSET));
        return hBox;
    }
}
