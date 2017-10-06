package SW9.controllers;

import SW9.Ecdar;
import SW9.abstractions.Query;
import SW9.abstractions.QueryState;
import SW9.backend.BackendException;
import SW9.backend.UPPAALDriver;
import SW9.presentations.QueryPresentation;
import com.jfoenix.controls.JFXRippler;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

public class QueryPaneController implements Initializable {

    public Label toolbarTitle;
    public AnchorPane toolbar;
    public VBox queriesList;
    public StackPane root;
    public ScrollPane scrollPane;

    public JFXRippler runAllQueriesButton;
    public JFXRippler clearAllQueriesButton;
    public JFXRippler addButton;

    private Map<Query, QueryPresentation> queryPresentationMap = new HashMap<>();

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
    }

    @FXML
    private void addButtonClicked() {
        Ecdar.getProject().getQueries().add(new Query("", "", QueryState.UNKNOWN));
    }

    @FXML
    private void runAllQueriesButtonClicked() {
        try {
            UPPAALDriver.buildEcdarDocument();
            Ecdar.getProject().getQueries().forEach(query -> {
                query.cancel();
                query.run(false);
            });
        } catch (final BackendException e) {
            Ecdar.showToast("An Error occurred during setup of a query. I got the error: " + e.getMessage());
        }
    }

    @FXML
    private void clearAllQueriesButtonClicked() {
        Ecdar.getProject().getQueries().forEach(query -> query.setQueryState(QueryState.UNKNOWN));
    }
}
