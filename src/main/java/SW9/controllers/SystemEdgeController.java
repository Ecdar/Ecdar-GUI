package SW9.controllers;

import SW9.abstractions.EcdarSystemEdge;
import SW9.utility.colors.Color;
import SW9.utility.helpers.ItemDragHelper;
import SW9.utility.helpers.SelectHelper;
import com.uppaal.model.system.SystemEdge;
import javafx.beans.property.DoubleProperty;
import javafx.fxml.Initializable;
import javafx.scene.Group;

import java.net.URL;
import java.util.ResourceBundle;

public class SystemEdgeController implements Initializable {
    public Group root;
    private EcdarSystemEdge edge;

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {

    }

    public void setEdge(final EcdarSystemEdge edge) {
        this.edge = edge;
    }
}
