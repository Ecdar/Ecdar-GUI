package SW9.abstractions;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * A project of models.
 */
public class Project {
    private final ObservableList<Query> queries;
    private final ObservableList<Component> components;
    private final ObjectProperty<Component> mainComponent;
    private final ObjectProperty<Declarations> globalDeclarations;
    private final ObjectProperty<Declarations> systemDeclarations;

    public Project() {
        queries = FXCollections.observableArrayList();
        components = FXCollections.observableArrayList();
        mainComponent = new SimpleObjectProperty<>();
        globalDeclarations = new SimpleObjectProperty<>(new Declarations("Global Declarations"));
        systemDeclarations = new SimpleObjectProperty<>(new Declarations("System Declarations"));
    }

    /**
     * Resets components.
     * After this, there is only one component.
     */
    public void reset() {
        components.clear();
        components.add(new Component(true));
    }

    public ObservableList<Query> getQueries() {
        return queries;
    }

    public ObservableList<Component> getComponents() {
        return components;
    }

    public Component getMainComponent() {
        return mainComponent.get();
    }

    public ObjectProperty<Component> mainComponentProperty() {
        return mainComponent;
    }

    public void setMainComponent(final Component mainComponent) {
        this.mainComponent.set(mainComponent);
    }

    public Declarations getGlobalDeclarations() {
        return globalDeclarations.get();
    }

    public Declarations getSystemDeclarations() {
        return systemDeclarations.get();
    }
}
