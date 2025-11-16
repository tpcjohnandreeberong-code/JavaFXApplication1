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
import java.util.regex.Pattern;

/**
 * FXML Controller class
 *
 * @author marke
 */
public class LoginController implements Initializable {

    // Email validation pattern
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
    );
    
    private DatabaseAuthService authService;
    
    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Initialize database authentication service
        authService = new DatabaseAuthService();
        
        // Set up password visibility toggle
        setupPasswordVisibilityToggle();
    }    
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private TextField passwordVisibleField;
    @FXML private Button showPasswordButton;
    @FXML private Button loginButton;
    @FXML private CheckBox rememberMeCheck;
    @FXML private Label messageLabel;

    @FXML
    private void onTogglePasswordVisibility() {
        if (passwordField.isVisible()) {
            // Show password
            passwordVisibleField.setText(passwordField.getText());
            passwordField.setVisible(false);
            passwordVisibleField.setVisible(true);
            showPasswordButton.setText("🙈");
        } else {
            // Hide password
            passwordField.setText(passwordVisibleField.getText());
            passwordVisibleField.setVisible(false);
            passwordField.setVisible(true);
            showPasswordButton.setText("👁");
        }
    }
    
    private void setupPasswordVisibilityToggle() {
        // Initially hide the visible password field
        passwordVisibleField.setVisible(false);
        
        // Sync text between password fields
        passwordField.textProperty().addListener((obs, oldText, newText) -> {
            if (passwordField.isVisible()) {
                passwordVisibleField.setText(newText);
            }
        });
        
        passwordVisibleField.textProperty().addListener((obs, oldText, newText) -> {
            if (passwordVisibleField.isVisible()) {
                passwordField.setText(newText);
            }
        });
    }
    
    private String getCurrentPassword() {
        if (passwordField.isVisible()) {
            return passwordField.getText() == null ? "" : passwordField.getText();
        } else {
            return passwordVisibleField.getText() == null ? "" : passwordVisibleField.getText();
        }
    }
    
    @FXML
    private void onLogin() {
        String usernameOrEmail = usernameField.getText() == null ? "" : usernameField.getText().trim();
        String password = getCurrentPassword();

        if (usernameOrEmail.isEmpty() || password.isEmpty()) {
            messageLabel.setText("Please enter username/email and password.");
            shakeFields();
            return;
        }

        // Static admin login only - if username is "admin", use "admin123" as password
        if (usernameOrEmail.equalsIgnoreCase("admin")) {
            password = "admin123";
            messageLabel.setText("Admin login detected - using static password");
        }
        // Staff and other users must use their actual passwords

        // Authenticate using database
        DatabaseAuthService.AuthenticationResult result = authenticate(usernameOrEmail, password);
        if (result.isSuccess()) {
            messageLabel.setText("");
            // Store user session data (you can create a session manager class)
            storeUserSession(result);
            navigateToMain();
        } else {
            messageLabel.setText(result.getMessage());
            showAlert(AlertType.ERROR, "Login Failed", result.getMessage());
            shakeFields();
        }
    }

    @FXML
    private void onForgotPassword() {
        showAlert(AlertType.INFORMATION, "Forgot Password", "Please contact the administrator.");
    }

    private DatabaseAuthService.AuthenticationResult authenticate(String usernameOrEmail, String password) {
        try {
            // Get client IP and user agent (simplified for now)
            String ipAddress = "127.0.0.1";
            String userAgent = "JavaFX Application";
            
            // Authenticate using database service
            return authService.authenticateUser(usernameOrEmail, password, ipAddress, userAgent);
            
        } catch (Exception e) {
            // Handle any authentication errors
            DatabaseAuthService.AuthenticationResult errorResult = new DatabaseAuthService.AuthenticationResult();
            errorResult.setSuccess(false);
            errorResult.setMessage("System error. Please try again.");
            return errorResult;
        }
    }
    
    /**
     * Store user session data after successful login
     */
    private void storeUserSession(DatabaseAuthService.AuthenticationResult result) {
        // Store user session data using SessionManager
        SessionManager sessionManager = SessionManager.getInstance();
        sessionManager.setUserSession(
            result.getUsername(),
            result.getFullName(),
            result.getEmail(),
            result.getRoles().get(0) // Get first role
        );
        
        System.out.println("User logged in: " + result.getFullName() + " (" + result.getUsername() + ")");
        System.out.println("Role: " + result.getRoles());
        System.out.println("Permissions: " + result.getPermissions());
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
			
			// Set properties before setting the scene
			stage.setTitle("TPC Payroll Management System - Main");
			stage.setResizable(true);
			stage.setMinWidth(1024);
			stage.setMinHeight(640);
			
			// Set the scene
			stage.setScene(scene);
			
			// Set to maximized mode (keeps title bar and window controls)
			stage.setMaximized(true);
			
			// Force the window to stay visible
			stage.show();
			
		} catch (Exception ex) {
			showAlert(AlertType.ERROR, "Navigation Error", "Cannot open main window: " + ex.getMessage());
		}
	}
    
}
