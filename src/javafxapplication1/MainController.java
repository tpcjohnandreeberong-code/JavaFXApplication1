package javafxapplication1;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Alert;

public class MainController {
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;

    @FXML private Label errorLabel;

    @FXML
    private void handleLogin() {
        String user = usernameField.getText();
        String pass = passwordField.getText();


        if (user.isEmpty() || pass.isEmpty()) {
            errorLabel.setText("Please enter username and password!");
            return;
        }

        errorLabel.setText(""); // clear error

        System.out.println("Username: " + user);
        System.out.println("Password: " + pass);

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Login Successful");
        alert.setHeaderText(null);
        alert.setContentText("Welcome, " + user + "!");
        alert.showAndWait();
    }

}
