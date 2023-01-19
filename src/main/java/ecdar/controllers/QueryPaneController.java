package ecdar.controllers;

import ecdar.Ecdar;
import ecdar.abstractions.Query;
import ecdar.abstractions.QueryState;
import ecdar.backend.*;
import ecdar.presentations.Grid;
import ecdar.presentations.QueryPresentation;
import com.jfoenix.controls.JFXRippler;
import ecdar.utility.UndoRedoStack;
import ecdar.utility.colors.Color;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Rectangle;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

public class QueryPaneController implements Initializable {
    public StackPane root;
    public StackPane resizeAnchor;
    public Label toolbarTitle;
    public HBox toolbar;
    public VBox queriesList;
    public ScrollPane scrollPane;

    public JFXRippler runAllQueriesButton;
    public JFXRippler clearAllQueriesButton;
    public JFXRippler addButton;

    private final Map<Query, QueryPresentation> queryPresentationMap = new HashMap<>();

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        Ecdar.getProject().getQueries().addListener((ListChangeListener<Query>) change -> {
            while (change.next()) {
                for (final Query removeQuery : change.getRemoved()) {
                    queriesList.getChildren().remove(queryPresentationMap.get(removeQuery));
                    queryPresentationMap.remove(removeQuery);
                }

                for (final Query newQuery : change.getAddedSubList()) {
                    final QueryPresentation newQueryPresentation = new QueryPresentation(newQuery);
                    queryPresentationMap.put(newQuery, newQueryPresentation);
                    queriesList.getChildren().add(newQueryPresentation);
                }
            }
        });

        for (final Query newQuery : Ecdar.getProject().getQueries()) {
            queriesList.getChildren().add(new QueryPresentation(newQuery));
        }

        initializeResizeAnchor();
    }

    private void initializeResizeAnchor() {
        resizeAnchor.setCursor(Cursor.E_RESIZE);

        final Color color = Color.GREY;
        final Color.Intensity colorIntensity = Color.Intensity.I300;

        // Set the background of the toolbar
        resizeAnchor.setBackground(new Background(new BackgroundFill(color.getColor(colorIntensity), CornerRadii.EMPTY, Insets.EMPTY)));
    }

    @FXML
    private void addButtonClicked() {
        Ecdar.getProject().getQueries().add(new Query("", "", QueryState.UNKNOWN));
    }

    @FXML
    private void runAllQueriesButtonClicked() {
        Ecdar.getProject().getQueries().forEach(query -> {
            if (query.getType() == null) return;
            query.cancel();
            Ecdar.getQueryExecutor().executeQuery(query);
        });
    }

    @FXML
    private void clearAllQueriesButtonClicked() {
        Ecdar.getProject().getQueries().forEach(query -> query.setQueryState(QueryState.UNKNOWN));
    }
}
