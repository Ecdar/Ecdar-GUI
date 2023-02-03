package ecdar.presentations;
import ecdar.abstractions.Component;
import ecdar.controllers.ComponentController;
import ecdar.utility.helpers.LocationAware;
import ecdar.utility.helpers.SelectHelper;
import javafx.beans.property.DoubleProperty;

public class ComponentPresentation extends ModelPresentation implements LocationAware, SelectHelper.Selectable {
    private final ComponentController controller;

    public ComponentPresentation(final Component component) {
        controller = new EcdarFXMLLoader().loadAndGetController("ComponentPresentation.fxml", this);
        controller.setComponent(component);
    }

    public ComponentController getController() {
        return controller;
    }

    @Override
    public DoubleProperty xProperty() {
        return layoutXProperty();
    }

    @Override
    public DoubleProperty yProperty() {
        return layoutYProperty();
    }

    @Override
    public double getX() {
        return xProperty().get();
    }

    @Override
    public double getY() {
        return yProperty().get();
    }

    @Override
    public void select() {
        controller.componentSelected();
    }

    @Override
    public void deselect() {
        controller.componentUnselected();
    }
}
