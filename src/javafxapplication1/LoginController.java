/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/javafx/FXMLController.java to edit this template
 */
package javafxapplication1;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.fxml.Initializable;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

/**
 * FXML Controller class
 *
 * @author marke
 */
public class LoginController implements Initializable {

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // TODO
    }    
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;
    @FXML private CheckBox rememberMeCheck;
    @FXML private Label messageLabel;

    @FXML
    private void onLogin() {
        String username = usernameField.getText() == null ? "" : usernameField.getText().trim();
        String password = passwordField.getText() == null ? "" : passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            messageLabel.setText("Please enter username and password.");
            shakeFields();
            return;
        }

        boolean success = authenticate(username, password);
        if (success) {
            messageLabel.setText("");
			navigateToMain();
        } else {
            messageLabel.setText("Invalid credentials.");
            showAlert(AlertType.ERROR, "Login Failed", "Invalid username or password.");
        }
    }

    @FXML
    private void onForgotPassword() {
        showAlert(AlertType.INFORMATION, "Forgot Password", "Please contact the administrator.");
    }

    private boolean authenticate(String username, String password) {
        // Placeholder auth logic. Replace with database/service call.
        return ("admin".equalsIgnoreCase(username) && "admin123".equals(password))
                || ("user".equalsIgnoreCase(username) && "user123".equals(password));
    }

    private void showAlert(AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void shakeFields() {
        // Simple visual cue: request focus sequence
        usernameField.requestFocus();
        passwordField.requestFocus();
    }

	private void navigateToMain() {
		try {
			Parent root = FXMLLoader.load(getClass().getResource("/javafxapplication1/Main.fxml"));
			Stage stage = (Stage) loginButton.getScene().getWindow();
			Scene scene = new Scene(root);
			stage.setTitle("School Payroll System");
			stage.setScene(scene);
			stage.setResizable(true);
			stage.setMinWidth(1024);
			stage.setMinHeight(640);
			stage.setMaximized(true);
			stage.centerOnScreen();
		} catch (Exception ex) {
			showAlert(AlertType.ERROR, "Navigation Error", "Cannot open main window: " + ex.getMessage());
		}
	}
    
}
