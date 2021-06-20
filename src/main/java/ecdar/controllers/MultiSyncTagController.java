package ecdar.controllers;

import ecdar.presentations.SyncTextFieldPresentation;
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

    public ObservableBooleanValue textFieldFocus = new ObservableBooleanValue() {
        @Override
        public boolean get() {
            for (Node child : syncList.getChildren()) {
                if (child instanceof StackPane && ((SyncTextFieldPresentation) child).getController().textField.isFocused()) {
                    return true;
                }
            }

            return false;
        }

        @Override
        public void addListener(ChangeListener<? super Boolean> listener) {
            for (Node child : syncList.getChildren()) {
                if (child instanceof StackPane) {
                    ((SyncTextFieldPresentation) child).getController().textField.focusedProperty().addListener(listener);
                }
            }
        }

        @Override
        public void removeListener(ChangeListener<? super Boolean> listener) {

        }

        @Override
        public Boolean getValue() {
            for (Node child : syncList.getChildren()) {
                if (child instanceof StackPane && ((SyncTextFieldPresentation) child).getController().textField.isFocused()) {
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
