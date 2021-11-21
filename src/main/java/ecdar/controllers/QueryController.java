package ecdar.controllers;

import com.jfoenix.controls.JFXComboBox;
import com.jfoenix.controls.JFXRippler;
import ecdar.abstractions.Query;
import ecdar.abstractions.QueryType;
import ecdar.utility.colors.Color;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.Initializable;
import javafx.scene.control.Tooltip;
import javafx.scene.text.Text;
import org.kordamp.ikonli.javafx.FontIcon;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

public class QueryController implements Initializable {
    public JFXRippler actionButton;
    public JFXRippler queryTypeExpand;
    public Text queryTypeSymbol;
    public JFXComboBox<String> backendsDropdown;
    private Query query;
    private final Map<QueryType, SimpleBooleanProperty> queryTypeListElementsSelectedState = new HashMap<>();
    private final Tooltip noQueryTypeSetTooltip = new Tooltip("Please select a query type beneath the status icon");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initializeActionButton();
    }

    public void setQuery(Query query) {
        this.query = query;
        this.query.getTypeProperty().addListener(((observable, oldValue, newValue) -> {
            if(newValue != null) {
                actionButton.setDisable(false);
                ((FontIcon) actionButton.lookup("#actionButtonIcon")).setIconColor(Color.GREY.getColor(Color.Intensity.I900));
                Platform.runLater(() -> {
                    Tooltip.uninstall(actionButton.getParent(), noQueryTypeSetTooltip);
                });
            } else {
                actionButton.setDisable(true);
                ((FontIcon) actionButton.lookup("#actionButtonIcon")).setIconColor(Color.GREY.getColor(Color.Intensity.I500));
                Platform.runLater(() -> {
                    Tooltip.install(actionButton.getParent(), noQueryTypeSetTooltip);
                });
            }
        }));
    }

    public Query getQuery() {
        return query;
    }

    private void initializeActionButton() {
        Platform.runLater(() -> {
            if (query.getType() == null) {
                actionButton.setDisable(true);
                ((FontIcon) actionButton.lookup("#actionButtonIcon")).setIconColor(Color.GREY.getColor(Color.Intensity.I500));
                Tooltip.install(actionButton.getParent(), noQueryTypeSetTooltip);
            }
        });
    }

    public Map<QueryType, SimpleBooleanProperty> getQueryTypeListElementsSelectedState() {
        return queryTypeListElementsSelectedState;
    }
}
