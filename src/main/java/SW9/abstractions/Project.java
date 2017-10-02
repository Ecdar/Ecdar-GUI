package SW9.abstractions;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;


public class Project {

    private final ObservableList<Query> queries = FXCollections.observableArrayList();
    private final ObservableList<Component> components = FXCollections.observableArrayList();
    private final ObjectProperty<Component> mainComponent = new SimpleObjectProperty<>();
    private ObjectProperty<Declarations> globalDeclarations = new SimpleObjectProperty<>();
    private ObjectProperty<Declarations> systemDeclarations = new SimpleObjectProperty<>();

    /**
     * Resets declarations and components.
     */
    public void reset() {
        globalDeclarations = new SimpleObjectProperty<>();

        systemDeclarations = new SimpleObjectProperty<>();

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
