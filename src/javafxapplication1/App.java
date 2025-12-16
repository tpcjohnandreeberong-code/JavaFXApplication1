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
        java.net.URL resource = getClass().getResource("/javafxapplication1/Login.fxml");
        if (resource == null) {
            // Try alternative path without leading slash
            resource = getClass().getResource("Login.fxml");
        }
        if (resource == null) {
            throw new RuntimeException("Cannot find Login.fxml. Please ensure the file exists in the classpath.");
        }
        
        FXMLLoader loader = new FXMLLoader(resource);
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
