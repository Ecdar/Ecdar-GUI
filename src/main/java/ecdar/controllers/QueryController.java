package ecdar.controllers;

import com.jfoenix.controls.JFXPopup;
import com.jfoenix.controls.JFXRippler;
import ecdar.abstractions.Query;
import ecdar.abstractions.QueryState;
import ecdar.abstractions.QueryType;
import ecdar.presentations.DropDownMenu;
import ecdar.utility.colors.Color;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import org.kordamp.ikonli.javafx.FontIcon;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.function.BiConsumer;

public class QueryController implements Initializable {
    public JFXRippler moreInformation;
    public JFXRippler actionButton;
    public Text queryTypeSymbol;
    private Query query;
    private final Map<QueryType, SimpleBooleanProperty> queryTypeListElementsSelectedState = new HashMap<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initializeMoreInformationButtonAndQueryTypeSymbol();
        initializeActionButton();

        Platform.runLater(() -> {
            FontIcon moreInformationIcon = (FontIcon) moreInformation.lookup("#moreInformationIcon");
            query.queryStateProperty().addListener(((observable, oldValue, newValue) -> {
                if(newValue == QueryState.UNKNOWN) {
                    moreInformationIcon.setIconColor(new javafx.scene.paint.Color(0.75, 0.75, 0.75, 1));
                    queryTypeSymbol.setFill(new javafx.scene.paint.Color(0.75, 0.75, 0.75, 1));
                } else {
                    moreInformationIcon.setIconColor(new javafx.scene.paint.Color(1, 1, 1, 1));
                    queryTypeSymbol.setFill(new javafx.scene.paint.Color(1, 1, 1, 1));
                }
            }));

            moreInformationIcon.setIconColor(new javafx.scene.paint.Color(0.75, 0.75, 0.75, 1));
            queryTypeSymbol.setFill(new javafx.scene.paint.Color(0.75, 0.75, 0.75, 1));
        });
    }

    public void setQuery(Query query) {
        this.query = query;
        this.query.getTypeProperty().addListener(((observable, oldValue, newValue) -> {
            if(newValue != null) {
                actionButton.setDisable(false);
                ((FontIcon) actionButton.lookup("#actionButtonIcon")).setIconColor(Color.GREY.getColor(Color.Intensity.I900));
            }
        }));
    }

    public Query getQuery() {
        return query;
    }

    private void initializeMoreInformationButtonAndQueryTypeSymbol() {
        moreInformation.setVisible(true);
        moreInformation.setMaskType(JFXRippler.RipplerMask.CIRCLE);
        moreInformation.setPosition(JFXRippler.RipplerPos.BACK);
        moreInformation.setRipplerFill(Color.GREY_BLUE.getColor(Color.Intensity.I500));

        final DropDownMenu moreInformationDropDown = new DropDownMenu(moreInformation);

        moreInformationDropDown.addListElement("Query Type");

        addQueryTypeListElement(QueryType.REFINEMENT, moreInformationDropDown);
        addQueryTypeListElement(QueryType.QUOTIENT, moreInformationDropDown);
        addQueryTypeListElement(QueryType.SPECIFICATION, moreInformationDropDown);
        addQueryTypeListElement(QueryType.IMPLEMENTATION, moreInformationDropDown);
        addQueryTypeListElement(QueryType.LOCAL_CONSISTENCY, moreInformationDropDown);
        addQueryTypeListElement(QueryType.GLOBAL_CONSISTENCY, moreInformationDropDown);
        addQueryTypeListElement(QueryType.BISIM_MIN, moreInformationDropDown);
        addQueryTypeListElement(QueryType.GET_NEW_COMPONENT, moreInformationDropDown);

        moreInformation.setOnMousePressed((e) -> {
            e.consume();
            moreInformationDropDown.show(JFXPopup.PopupVPosition.TOP, JFXPopup.PopupHPosition.LEFT, 20, 20);
        });
    }

    private void addQueryTypeListElement(final QueryType type, final DropDownMenu dropDownMenu) {
        // Create the list element
        HBox listElement = new HBox();
        listElement.setAlignment(Pos.CENTER_LEFT);
        listElement.setBorder(new Border(new BorderStroke(Color.GREY.getColor(Color.Intensity.I500), null, null, null,
                BorderStrokeStyle.SOLID, BorderStrokeStyle.NONE, BorderStrokeStyle.NONE, BorderStrokeStyle.NONE,
                CornerRadii.EMPTY, BorderStroke.THIN, Insets.EMPTY)));

        SimpleBooleanProperty selected = new SimpleBooleanProperty(false);

        queryTypeListElementsSelectedState.put(type, selected);

        // Handle element pressed
        listElement.setOnMousePressed(event -> {
            query.setType(type);
            dropDownMenu.hide();
            for(Map.Entry<QueryType, SimpleBooleanProperty> pair : queryTypeListElementsSelectedState.entrySet()) {
                pair.getValue().set(pair.getKey().equals(type));
            }
            queryTypeSymbol.setText(type.getSymbol());
        });

        // Add the symbol of the query type
        StackPane symbol = new StackPane();
        symbol.setMinWidth(64);
        symbol.setMaxWidth(64);
        symbol.setBorder(new Border(new BorderStroke(null, Color.GREY.getColor(Color.Intensity.I500), null, null,
                BorderStrokeStyle.NONE, BorderStrokeStyle.SOLID, BorderStrokeStyle.NONE, BorderStrokeStyle.NONE,
                CornerRadii.EMPTY, BorderStroke.THIN, Insets.EMPTY)));
        symbol.setPadding(new Insets(10));
        symbol.getChildren().add(new Text(type.getSymbol()));
        listElement.getChildren().add(symbol);

        // Add the name of the query type
        StackPane name = new StackPane();
        name.setPadding(new Insets(10));
        name.prefWidthProperty().bind(listElement.widthProperty());
        name.getChildren().add(new Text(type.getQueryName()));
        listElement.getChildren().add(name);

        // Handle background color
        final Color color = Color.GREY;
        final Color.Intensity colorIntensity = Color.Intensity.I50;

        final BiConsumer<Color, Color.Intensity> setBackground = (newColor, newIntensity) -> {
            symbol.setBackground(new Background(new BackgroundFill(
                    newColor.getColor(newIntensity),
                    CornerRadii.EMPTY,
                    Insets.EMPTY
            )));

            name.setBackground(new Background(new BackgroundFill(
                    newColor.getColor(newIntensity),
                    CornerRadii.EMPTY,
                    Insets.EMPTY
            )));
        };

        // Update the background when hovered
        listElement.setOnMouseEntered(event -> {
            if(selected.get()) {
                setBackground.accept(color, colorIntensity.next(4));
            } else {
                setBackground.accept(color, colorIntensity.next(1));
            }
            listElement.setCursor(Cursor.HAND);
        });

        listElement.setOnMouseExited(event -> {
            if(selected.get()) {
                setBackground.accept(color, colorIntensity.next(3));
            } else {
                setBackground.accept(color, colorIntensity);
            }
            listElement.setCursor(Cursor.DEFAULT);
        });

        listElement.backgroundProperty().bind(Bindings.when(selected)
                .then(new Background(new BackgroundFill(color.getColor(colorIntensity.next(3)), CornerRadii.EMPTY, Insets.EMPTY)))
                .otherwise(new Background(new BackgroundFill(color.getColor(colorIntensity), CornerRadii.EMPTY, Insets.EMPTY))));

        dropDownMenu.addCustomElement(listElement);
    }

    private void initializeActionButton() {
        actionButton.setDisable(true);
    }
}
