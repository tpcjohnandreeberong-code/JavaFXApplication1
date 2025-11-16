package javafxapplication1;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;

public class App extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/javafxapplication1/Login.fxml"));
        Scene scene = new Scene(loader.load());

        stage.setTitle("TPC Payroll Management System - Login");
        stage.setScene(scene);
        stage.setResizable(true);

        // ✅ Get the user's screen resolution dynamically
        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
        stage.setX(screenBounds.getMinX());
        stage.setY(screenBounds.getMinY());
        stage.setWidth(screenBounds.getWidth());
        stage.setHeight(screenBounds.getHeight());

        // Optional: maximize (still keeps title bar)
        stage.setMaximized(true);

        // Center on screen (no effect if maximized, but safe)
        stage.centerOnScreen();

        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
