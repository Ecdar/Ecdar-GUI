package SW9.abstractions;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class Project {

    private final ObservableList<Query> queries;
    private final ObservableList<Component> components;
    private final ObjectProperty<Component> mainComponent;
    private ObjectProperty<Declarations> globalDeclarations;
    private ObjectProperty<Declarations> systemDeclarations;

    public Project() {
        queries = FXCollections.observableArrayList();
        components = FXCollections.observableArrayList();
        mainComponent = new SimpleObjectProperty<>();
        globalDeclarations = new SimpleObjectProperty<>(new Declarations("Global Declarations"));
        systemDeclarations = new SimpleObjectProperty<>(new Declarations("System Declarations"));
    }

    /**
     * Resets declarations and components.
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

    public ObjectProperty<Declarations> globalDeclarationsProperty() {
        return globalDeclarations;
    }

    public void setGlobalDeclarations(Declarations globalDeclarations) {
        this.globalDeclarations.set(globalDeclarations);
    }

    public Declarations getSystemDeclarations() {
        return systemDeclarations.get();
    }

    public ObjectProperty<Declarations> systemDeclarationsProperty() {
        return systemDeclarations;
    }

    public void setSystemDeclarations(Declarations systemDeclarations) {
        this.systemDeclarations.set(systemDeclarations);
    }
}
