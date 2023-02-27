package ecdar.controllers;

import ecdar.Ecdar;
import ecdar.abstractions.Query;
import ecdar.abstractions.QueryState;
import ecdar.backend.BackendDriver;
import ecdar.backend.BackendHelper;
import ecdar.presentations.QueryPresentation;
import com.jfoenix.controls.JFXRippler;
import ecdar.utility.colors.Color;
import ecdar.utility.helpers.DropShadowHelper;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.*;

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
    private BackendDriver backendDriver;

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        Platform.runLater(() -> {
            Ecdar.getProject().getQueries().addListener((ListChangeListener<Query>) change -> {
                while (change.next()) {
                    for (final Query removeQuery : change.getRemoved()) {
                        queriesList.getChildren().remove(queryPresentationMap.get(removeQuery));
                        queryPresentationMap.remove(removeQuery);
                    }

                    for (final Query newQuery : change.getAddedSubList()) {
                        final QueryPresentation newQueryPresentation = new QueryPresentation(newQuery, this.backendDriver);
                        queryPresentationMap.put(newQuery, newQueryPresentation);
                        queriesList.getChildren().add(newQueryPresentation);
                    }
                }
            });
            for (final Query newQuery : Ecdar.getProject().getQueries()) {
                queriesList.getChildren().add(new QueryPresentation(newQuery, this.backendDriver));
            }
        });

        initializeLeftBorder();
        initializeToolbar();
        initializeBackground();
        initializeResizeAnchor();

        BackendHelper.addBackendInstanceListener(() -> {
            // When the backend instances change, reset the backendDriver and
            // cancel all queries to prevent dangling connections and queries
            this.stopAllQueries();
            backendDriver.reset();
        });
    }

    private void initializeResizeAnchor() {
        resizeAnchor.setCursor(Cursor.E_RESIZE);

        final Color color = Color.GREY;
        final Color.Intensity colorIntensity = Color.Intensity.I300;

        // Set the background of the toolbar
        resizeAnchor.setBackground(new Background(new BackgroundFill(color.getColor(colorIntensity), CornerRadii.EMPTY, Insets.EMPTY)));
    }

    private void initializeLeftBorder() {
        toolbar.setBorder(new Border(new BorderStroke(
                Color.GREY_BLUE.getColor(Color.Intensity.I900),
                BorderStrokeStyle.SOLID,
                CornerRadii.EMPTY,
                new BorderWidths(0, 0, 0, 1)
        )));

        showBottomInset(true);
    }

    private void initializeBackground() {
        queriesList.setBackground(new Background(new BackgroundFill(
                Color.GREY.getColor(Color.Intensity.I200),
                CornerRadii.EMPTY,
                Insets.EMPTY
        )));
    }

    private void initializeToolbar() {
        final Color color = Color.GREY_BLUE;
        final Color.Intensity colorIntensity = Color.Intensity.I800;

        // Set the background of the toolbar
        toolbar.setBackground(new Background(new BackgroundFill(
                color.getColor(colorIntensity),
                CornerRadii.EMPTY,
                Insets.EMPTY)));

        // Set the font color of elements in the toolbar
        toolbarTitle.setTextFill(color.getTextColor(colorIntensity));

        runAllQueriesButton.setBackground(new Background(new BackgroundFill(
                javafx.scene.paint.Color.TRANSPARENT,
                new CornerRadii(100),
                Insets.EMPTY)));

        addButton.setMaskType(JFXRippler.RipplerMask.CIRCLE);
        addButton.setRipplerFill(color.getTextColor(colorIntensity));
        Tooltip.install(addButton, new Tooltip("Add query"));

        runAllQueriesButton.setMaskType(JFXRippler.RipplerMask.CIRCLE);
        runAllQueriesButton.setRipplerFill(color.getTextColor(colorIntensity));
        Tooltip.install(runAllQueriesButton, new Tooltip("Run all queries"));

        clearAllQueriesButton.setMaskType(JFXRippler.RipplerMask.CIRCLE);
        clearAllQueriesButton.setRipplerFill(color.getTextColor(colorIntensity));
        Tooltip.install(clearAllQueriesButton, new Tooltip("Clear all queries"));

        // Set the elevation of the toolbar
        toolbar.setEffect(DropShadowHelper.generateElevationShadow(8));
    }


    /**
     * Inserts an edge/inset at the bottom of the scrollView
     * which is used to push up the elements of the scrollview
     * @param shouldShow boolean indicating whether to push up the items
     */
    public void showBottomInset(final Boolean shouldShow) {
        double bottomInsetWidth = 0;
        if(shouldShow) {
            bottomInsetWidth = 20;
        }

        scrollPane.setBorder(new Border(new BorderStroke(
                Color.GREY.getColor(Color.Intensity.I400),
                BorderStrokeStyle.NONE,
                CornerRadii.EMPTY,
                new BorderWidths(0, 1, bottomInsetWidth, 0)
        )));
    }
    
    public void setBackendDriver(BackendDriver backendDriver) {
        this.backendDriver = backendDriver;
    }

    public void stopAllQueries() {
        for (Node qp : queriesList.getChildren()) {
            ((QueryPresentation) qp).getController().cancelQuery();
        }
    }

    @FXML
    private void addButtonClicked() {
        Ecdar.getProject().getQueries().add(new Query("", "", QueryState.UNKNOWN));
    }

    @FXML
    private void runAllQueriesButtonClicked() {
        for (Node qp : queriesList.getChildren()) {
            if (!(qp instanceof QueryPresentation)) continue;
            QueryController controller = ((QueryPresentation) qp).getController();

            if (controller.getQuery().getType() == null) return;
            controller.cancelQuery();
            controller.runQuery();
        }
    }

    @FXML
    private void clearAllQueriesButtonClicked() {
        Ecdar.getProject().getQueries().forEach(query -> query.setQueryState(QueryState.UNKNOWN));
    }
}
