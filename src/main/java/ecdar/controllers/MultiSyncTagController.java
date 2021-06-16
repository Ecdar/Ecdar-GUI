package ecdar.controllers;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXTextField;
import ecdar.abstractions.Edge;
import javafx.beans.InvalidationListener;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableBooleanValue;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.ResourceBundle;

public class MultiSyncTagController implements Initializable {

    public VBox syncList;
    public BorderPane topbar;
    public BorderPane frame;

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {

    }

    public void removeSyncTextfieldOfEdge(Edge newEdge) {
        syncList.getChildren().removeIf(node -> ((JFXTextField) ((StackPane) node).getChildren().get(1)).getText().equals(newEdge.getSync()));
    }

    public ObservableBooleanValue textFieldFocus = new ObservableBooleanValue() {
        @Override
        public boolean get() {
            for (Node child : syncList.getChildren()) {
                if (child instanceof StackPane && ((StackPane) child).getChildren().get(2).isFocused()) {
                    return true;
                }
            }

            return false;
        }

        @Override
        public void addListener(ChangeListener<? super Boolean> listener) {
            for (Node child : syncList.getChildren()) {
                if (child instanceof StackPane) {
                    ((StackPane) child).getChildren().get(2).focusedProperty().addListener(listener);
                }
            }
        }

        @Override
        public void removeListener(ChangeListener<? super Boolean> listener) {

        }

        @Override
        public Boolean getValue() {
            for (Node child : syncList.getChildren()) {
                if (child instanceof StackPane && ((StackPane) child).getChildren().get(2).isFocused()) {
                    return true;
                }
            }

            return false;
        }

        @Override
        public void addListener(InvalidationListener listener) {
        }

        @Override
        public void removeListener(InvalidationListener listener) {

        }
    };
}
