package ecdar.controllers;

import com.jfoenix.controls.JFXPopup;
import com.jfoenix.controls.JFXRippler;
import com.jfoenix.controls.JFXTooltip;
import ecdar.abstractions.Query;
import ecdar.abstractions.QueryType;
import ecdar.presentations.DropDownMenu;
import ecdar.presentations.MenuElement;
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
    private Query query;
    private final Map<QueryType, SimpleBooleanProperty> queryTypeListElementsSelectedState = new HashMap<>();
    private final Tooltip noQueryTypeSatTooltip = new Tooltip("Please select a query type beneath the status icon");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initializeMoreInformationButtonAndQueryTypeSymbol();
        initializeActionButton();

        Platform.runLater(() -> {
            Tooltip.install(actionButton.getParent(), noQueryTypeSatTooltip);
        });
    }

    public void setQuery(Query query) {
        this.query = query;
        this.query.getTypeProperty().addListener(((observable, oldValue, newValue) -> {
            if(newValue != null) {
                actionButton.setDisable(false);
                ((FontIcon) actionButton.lookup("#actionButtonIcon")).setIconColor(Color.GREY.getColor(Color.Intensity.I900));
                Platform.runLater(() -> {
                    Tooltip.uninstall(actionButton.getParent(), noQueryTypeSatTooltip);
                });
            } else {
                actionButton.setDisable(true);
                ((FontIcon) actionButton.lookup("#actionButtonIcon")).setIconColor(Color.GREY.getColor(Color.Intensity.I500));
                Platform.runLater(() -> {
                    Tooltip.install(actionButton.getParent(), noQueryTypeSatTooltip);
                });
            }
        }));
    }

    public Query getQuery() {
        return query;
    }

    private void initializeMoreInformationButtonAndQueryTypeSymbol() {
        queryTypeExpand.setVisible(true);
        queryTypeExpand.setMaskType(JFXRippler.RipplerMask.RECT);
        queryTypeExpand.setPosition(JFXRippler.RipplerPos.BACK);
        queryTypeExpand.setRipplerFill(Color.GREY_BLUE.getColor(Color.Intensity.I500));

        final DropDownMenu queryTypeDropDown = new DropDownMenu(queryTypeExpand);

        queryTypeDropDown.addListElement("Query Type");
        QueryType[] queryTypes = QueryType.values();
        for (QueryType type : queryTypes) {
            addQueryTypeListElement(type, queryTypeDropDown);
        }

        queryTypeExpand.setOnMousePressed((e) -> {
            e.consume();
            queryTypeDropDown.show(JFXPopup.PopupVPosition.TOP, JFXPopup.PopupHPosition.LEFT, 16, 64);
        });
    }

    private void addQueryTypeListElement(final QueryType type, final DropDownMenu dropDownMenu) {
        MenuElement listElement = new MenuElement(type.getQueryName() + " [" + type.getSymbol() + "]", "gmi-done", mouseEvent -> {
            query.setType(type);
            queryTypeSymbol.setText(type.getSymbol());
            dropDownMenu.hide();

            // Reflect the selection on the dropdown menu
            for(Map.Entry<QueryType, SimpleBooleanProperty> pair : queryTypeListElementsSelectedState.entrySet()) {
                pair.getValue().set(pair.getKey().equals(type));
            }
        });

        // Add boolean to the element to handle selection
        SimpleBooleanProperty selected = new SimpleBooleanProperty(false);
        queryTypeListElementsSelectedState.put(type, selected);
        listElement.setToggleable(selected);

        dropDownMenu.addMenuElement(listElement);
    }

    private void initializeActionButton() {
        actionButton.setDisable(true);
    }
}
