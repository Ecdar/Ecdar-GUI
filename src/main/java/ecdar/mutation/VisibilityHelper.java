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
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.Text;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.Arrays;

/**
 * Collection of helper methods to show, hide, expand, and collapse UI elements.
 */
public class VisibilityHelper {
    public static final int EXPAND_INSET = 5;
    private static final Paint PASS_COLOR = Color.GREEN;
    private static final Paint INC_COLOR = Color.DARKORANGE;
    private static final Paint FAIL_COLOR = Color.RED;

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
     * Shows or hides some test result content and adjusts the graphic on the header with an arrow.
     * @param shouldShow true iff should show
     * @param header the header
     * @param content the content
     * @param verdict the verdict of the test result. This is used for applying the correct color on the arrow graphics
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

    /**
     * Gets the color that symbolises a specified verdict.
     * @param verdict the verdict
     * @return the color
     */
    private static Paint getColor(final TestResult.Verdict verdict) {
        if (verdict == TestResult.Verdict.PASS) return PASS_COLOR;
        if (Arrays.stream(TestResult.getIncVerdicts()).anyMatch(v -> v == verdict)) return INC_COLOR;
        if (Arrays.stream(TestResult.getFailedVerdicts()).anyMatch(v -> v == verdict)) return FAIL_COLOR;
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

    /**
     * Sets the text and fill (text color) of a text used for displaying the number of passed test-cases.
     * @param number the number to display
     * @param text the text to display
     */
    public static void setPassedText(final int number, final Text text) {
        text.setText(Integer.toString(number));
        text.setFill(getPassedColor(number));
    }

    /**
     * Sets the text and fill (text color) of a text used for displaying the number of inconclusive test-cases.
     * @param number the number to display
     * @param text the text to display
     */
    public static void setIncText(final int number, final Text text) {
        text.setText(Integer.toString(number));
        text.setFill(getIncColor(number));
    }

    /**
     * Sets the text and fill (text color) of a text used for displaying the number of failed test-cases.
     * @param number the number to display
     * @param text the text to display
     */
    public static void setFailedText(final int number, final Text text) {
        text.setText(Integer.toString(number));
        text.setFill(getFailedColor(number));
    }

    /**
     * Gets the color to be used on a number that symbolises a passed test-case.
     * If the number is 0, this method returns the default text color, as this means that there is no passed test-cases.
     * @param number the number of passed test-cases to display
     * @return the color to be used
     */
    public static Paint getPassedColor(final int number) {
        if (number == 0) return getDefaultTextColor();
        else return PASS_COLOR;
    }

    /**
     * Gets the color to be used on a number that symbolises an inconclusive test-case.
     * If the number is 0, this method returns the default text color, as this means that there is no inconclusive test-cases.
     * @param number the number of inconclusive test-cases to display
     * @return the color to be used
     */
    public static Paint getIncColor(final int number) {
        if (number == 0) return getDefaultTextColor();
        else return INC_COLOR;
    }

    /**
     * Gets the color to be used on a number that symbolises a failed test-case.
     * If the number is 0, this method returns the default text color, as this means that there is no failed test-cases.
     * @param number the number of failed test-cases to display
     * @return the color to be used
     */
    public static Paint getFailedColor(final int number) {
        if (number == 0) return getDefaultTextColor();
        else return FAIL_COLOR;
    }

    /**
     * Gets the text color used by default be JavaFX.
     * @return the text color
     */
    public static Paint getDefaultTextColor() {
        return Color.web("#333333");
    }

    /**
     * Surround with a vertical separator on the left.
     * @param content the content to surround
     * @return the HBox containing the separator and the content
     */
    public static HBox surround(final Node content) {
        final HBox hBox = new HBox(8, new Separator(Orientation.VERTICAL), content);
        hBox.setPadding(new Insets(0, 0, 0, EXPAND_INSET));
        return hBox;
    }
}
