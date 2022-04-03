//package ecdar.presentations;
//
//import com.jfoenix.controls.JFXRippler;
//import ecdar.controllers.CanvasController;
//import ecdar.controllers.CanvasShellController;
//import ecdar.controllers.EcdarController;
//import ecdar.utility.colors.Color;
//import javafx.application.Platform;
//import javafx.beans.property.BooleanProperty;
//import javafx.beans.property.SimpleBooleanProperty;
//import javafx.geometry.Insets;
//import javafx.scene.input.MouseEvent;
//import javafx.scene.layout.*;
//import javafx.scene.shape.Rectangle;
//
//public class CanvasPresentation extends StackPane {
//    private final CanvasShellController controller;
//
//
//    public CanvasShellPresentation() {
//        controller = new EcdarFXMLLoader().loadAndGetController("CanvasShellPresentation.fxml", this);
//
//
//
//
//
//        getStyleClass().add("canvas-shell-presentation");
//    }
//
//
//
//
//
//    private void setClipForChildren() {
//        Platform.runLater(() -> setClip(new Rectangle(getWidth(), getHeight())));
//    }
//
//    public CanvasShellController getController() {
//        return controller;
//    }
//
//    public CanvasController getCanvasController() {
//        return controller.canvasPresentation.getController();
//    }
//}
