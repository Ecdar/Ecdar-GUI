package ecdar.mutation;

import ecdar.Ecdar;
import ecdar.abstractions.Component;
import ecdar.abstractions.Edge;
import ecdar.abstractions.Location;
import ecdar.controllers.EcdarController;
import ecdar.utility.helpers.SelectHelper;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.scene.Cursor;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.util.List;

/**
 * A builder for creating a {@link TextFlow}.
 */
public class TextFlowBuilder {
    private final TextFlow flow;

    /**
     * Constructs.
     */
    public TextFlowBuilder() {
        flow = new TextFlow();
    }

    /**
     * Adds a {@link Text}.
     * @param content the content of the text.
     * @return this
     */
    public TextFlowBuilder text(final String content) {
        flow.getChildren().add(new Text(content));
        return this;
    }

    /**
     * Adds a bold {@link Text}.
     * @param content the content of the text
     * @return this
     */
    public TextFlowBuilder boldText(final String content) {
        flow.getChildren().add(createBoldText(content));
        return this;
    }

    /**
     * Creates a bold {@link Text}.
     * @param content the content of the text
     * @return the text
     */
    public static Text createBoldText(final String content) {
        final Text text = new Text(content);
        text.setStyle("-fx-font-weight: bold");
        return text;
    }

    /**
     * Adds a {@link Text} that links to a location.
     * It content of the text is the location id.
     * When pressing the text, Ecdar will show the component of the location and select the location.
     * @param locationId the id of the location to link to
     * @param componentName the name of the component containing the location
     * @return this
     */
    public TextFlowBuilder locationLink(final String locationId, final String componentName) {
        flow.getChildren().add(createLocationLink(locationId, componentName));
        return this;
    }

    /**
     * Adds a {@link Text} that links to a location.
     * @param locationId the id of the location to link to
     * @param componentName the name of the component containing the location
     * @return the text
     * @see #locationLink(String, String)
     */
    public static Text createLocationLink(final String locationId, final String componentName) {
        final Text text = new Text(locationId);
        text.setStyle("-fx-underline: true;");

        text.setCursor(Cursor.HAND);

        text.setOnMousePressed(event -> {
            final Component component = Ecdar.getProject().findComponent(componentName);

            // It could be null, if the user deleted the component
            if (component == null) {
                Ecdar.showToast("Could not find component " + componentName);
                return;
            }

            SelectHelper.elementsToBeSelected = FXCollections.observableArrayList();
            Ecdar.getPresentation().getController().setActiveModelPresentationForActiveCanvas(Ecdar.getComponentPresentationOfComponent(component));

            // Use a list, since there could be multiple locations (e.i. Universal locations)
            final List<Location> locations = component.getLocations().filtered(loc -> loc.getId().equals(locationId));

            // It could be empty if the user deleted the location
            if (locations.isEmpty()) {
                Ecdar.showToast("Could not find location " + locationId);
                return;
            }

            Platform.runLater(() -> {
                SelectHelper.clearSelectedElements();
                locations.forEach(SelectHelper::select);
            });
        });

        return text;
    }

    /**
     * Writes an edge in a readable way, e.g. "edge L0 → L1". The location ids are location links.
     * @param edge the edge
     * @param componentName the name of the component containing the edge
     * @return this
     * @see #locationLink(String, String)
     */
    public TextFlowBuilder edgeLinks(final Edge edge, final String componentName) {
        return text("edge ")
                .locationLink(edge.getSourceLocation().getId(), componentName)
                .text(" → ")
                .locationLink(edge.getTargetLocation().getId(), componentName);
    }

    /**
     * Builds the {@link TextFlow}.
     * @return the text flow
     */
    public TextFlow build() {
        return flow;
    }
}
