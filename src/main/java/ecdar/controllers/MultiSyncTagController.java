package ecdar.controllers;

import com.jfoenix.controls.JFXButton;
import javafx.beans.InvalidationListener;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableBooleanValue;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.ResourceBundle;

public class MultiSyncTagController implements Initializable {

    public VBox syncList;
    public JFXButton addSyncBtn;
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

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {

    }
}
