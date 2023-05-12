package ecdar.presentations;

import javafx.fxml.FXMLLoader;
import javafx.fxml.JavaFXBuilderFactory;

import java.io.IOException;
import java.net.URL;

/**
 * Loader to load an object hierarchy from an XML document
 */
public class EcdarFXMLLoader extends FXMLLoader {
    /**
     * Loads an object hierarchy from an XML document.
     * @param resourceName the name of the resource used to resolve relative path attribute values
     * @param <T> type of controller to get
     * @param root the root of the object hierarchy
     * @return the controller
     */
    public <T> T loadAndGetController(final String resourceName, final Object root) {
        final URL url = root.getClass().getResource(resourceName);

        setLocation(url);
        setBuilderFactory(new JavaFXBuilderFactory());

        setRoot(root);
        try {
            load(url.openStream());
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        }

        return getController();
    }
}
