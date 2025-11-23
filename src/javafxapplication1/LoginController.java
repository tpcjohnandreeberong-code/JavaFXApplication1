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
import java.util.prefs.Preferences;
import java.util.Base64;
import java.security.SecureRandom;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * FXML Controller class
 *
 * @author marke
 */
public class LoginController implements Initializable {

    private static final Logger logger = Logger.getLogger(LoginController.class.getName());
    
    // Email validation pattern
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
    );
    
    private DatabaseAuthService authService;
    
    // Remember me functionality
    private static final String PREF_USERNAME = "remembered_username";
    private static final String PREF_PASSWORD = "remembered_password";
    private static final String PREF_REMEMBER = "remember_me";
    private static final String PREF_ENCRYPTION_KEY = "encryption_key";
    private Preferences preferences;
    
    // Login attempt tracking
    private static final int MAX_LOGIN_ATTEMPTS = 5;
    private int loginAttemptCount = 0;
    
    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Initialize database authentication service
        authService = new DatabaseAuthService();
        
        // Initialize preferences for Remember Me functionality
        preferences = Preferences.userNodeForPackage(LoginController.class);
        
        // Set up password visibility toggle
        setupPasswordVisibilityToggle();
        
        // Load remembered credentials if available
        loadRememberedCredentials();
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
            // password = "admin123";
            messageLabel.setText("Admin login detected - using static password");
        }
        // Staff and other users must use their actual passwords

        // Authenticate using database
        DatabaseAuthService.AuthenticationResult result = authenticate(usernameOrEmail, password);
        if (result.isSuccess()) {
            messageLabel.setText("");
            
            // Log successful login (SecurityLogger will auto-detect IP)
            String clientIP = SecurityLogger.getClientIP();
            SecurityLogger.logSecurityEvent(
                "LOGIN_SUCCESS", 
                "LOW", 
                result.getUsername(),
                "User logged in successfully from " + clientIP
            );
            
            // Reset login attempt counter
            loginAttemptCount = 0;
            
            // Handle Remember Me functionality
            handleRememberMe(usernameOrEmail, password);
            
            // Store user session data (you can create a session manager class)
            storeUserSession(result);
            navigateToMain();
        } else {
            // Increment login attempt counter
            loginAttemptCount++;
            
            // Determine severity based on attempt count
            String severity = loginAttemptCount >= MAX_LOGIN_ATTEMPTS ? "HIGH" : "MEDIUM";
            String clientIP = SecurityLogger.getClientIP();
            
            // Log failed login attempt
            SecurityLogger.logSecurityEvent(
                "LOGIN_FAILED", 
                severity, 
                usernameOrEmail,
                "Failed login attempt #" + loginAttemptCount + " from " + clientIP + ": " + result.getMessage()
            );
            
            // Check if we should lock the account or show warning
            if (loginAttemptCount >= MAX_LOGIN_ATTEMPTS) {
                SecurityLogger.logSecurityEvent(
                    "SUSPICIOUS_ACTIVITY", 
                    "CRITICAL", 
                    usernameOrEmail,
                    "Maximum login attempts exceeded (" + MAX_LOGIN_ATTEMPTS + ") from " + clientIP + ". Possible brute force attack."
                );
                
                messageLabel.setText("Account temporarily locked due to multiple failed attempts. Please contact administrator.");
                showAlert(AlertType.ERROR, "Account Locked", 
                    "Too many failed login attempts. Please contact the administrator.");
            } else {
                int remainingAttempts = MAX_LOGIN_ATTEMPTS - loginAttemptCount;
                messageLabel.setText(result.getMessage() + " (Attempts remaining: " + remainingAttempts + ")");
                showAlert(AlertType.ERROR, "Login Failed", result.getMessage());
            }
            
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
    
    // Remember Me Functionality Methods
    
    /**
     * Handle Remember Me functionality after successful login
     */
    private void handleRememberMe(String username, String password) {
        try {
            if (rememberMeCheck.isSelected()) {
                // Save credentials securely
                saveCredentials(username, password);
                preferences.putBoolean(PREF_REMEMBER, true);
                logger.info("Credentials saved for user: " + username);
            } else {
                // Clear saved credentials if unchecked
                clearSavedCredentials();
                preferences.putBoolean(PREF_REMEMBER, false);
                logger.info("Credentials cleared for user: " + username);
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error handling Remember Me functionality", e);
            // Don't show error to user as this is not critical
        }
    }
    
    /**
     * Load remembered credentials on application startup
     */
    private void loadRememberedCredentials() {
        try {
            boolean rememberMe = preferences.getBoolean(PREF_REMEMBER, false);
            
            if (rememberMe) {
                String savedUsername = preferences.get(PREF_USERNAME, "");
                String encryptedPassword = preferences.get(PREF_PASSWORD, "");
                
                if (!savedUsername.isEmpty() && !encryptedPassword.isEmpty()) {
                    // Decrypt and load the password
                    String decryptedPassword = decryptPassword(encryptedPassword);
                    
                    // Populate the fields
                    usernameField.setText(savedUsername);
                    passwordField.setText(decryptedPassword);
                    passwordVisibleField.setText(decryptedPassword);
                    rememberMeCheck.setSelected(true);
                    
                    logger.info("Loaded remembered credentials for user: " + savedUsername);
                }
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error loading remembered credentials", e);
            // Clear invalid saved data
            clearSavedCredentials();
        }
    }
    
    /**
     * Save credentials securely using encryption
     */
    private void saveCredentials(String username, String password) throws Exception {
        // Save username as plain text (not sensitive)
        preferences.put(PREF_USERNAME, username);
        
        // Encrypt and save password
        String encryptedPassword = encryptPassword(password);
        preferences.put(PREF_PASSWORD, encryptedPassword);
    }
    
    /**
     * Clear all saved credentials
     */
    private void clearSavedCredentials() {
        try {
            preferences.remove(PREF_USERNAME);
            preferences.remove(PREF_PASSWORD);
            preferences.remove(PREF_REMEMBER);
            preferences.remove(PREF_ENCRYPTION_KEY);
            logger.info("All saved credentials cleared");
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error clearing saved credentials", e);
        }
    }
    
    /**
     * Encrypt password for secure storage
     */
    private String encryptPassword(String password) throws Exception {
        SecretKey key = getOrCreateEncryptionKey();
        
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        
        byte[] encryptedBytes = cipher.doFinal(password.getBytes("UTF-8"));
        return Base64.getEncoder().encodeToString(encryptedBytes);
    }
    
    /**
     * Decrypt password from storage
     */
    private String decryptPassword(String encryptedPassword) throws Exception {
        SecretKey key = getOrCreateEncryptionKey();
        
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, key);
        
        byte[] decodedBytes = Base64.getDecoder().decode(encryptedPassword);
        byte[] decryptedBytes = cipher.doFinal(decodedBytes);
        
        return new String(decryptedBytes, "UTF-8");
    }
    
    /**
     * Get existing encryption key or create a new one
     */
    private SecretKey getOrCreateEncryptionKey() throws Exception {
        String encodedKey = preferences.get(PREF_ENCRYPTION_KEY, "");
        
        if (encodedKey.isEmpty()) {
            // Create new key
            KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
            keyGenerator.init(128);
            SecretKey newKey = keyGenerator.generateKey();
            
            // Save the key
            String encodedNewKey = Base64.getEncoder().encodeToString(newKey.getEncoded());
            preferences.put(PREF_ENCRYPTION_KEY, encodedNewKey);
            
            return newKey;
        } else {
            // Load existing key
            byte[] decodedKey = Base64.getDecoder().decode(encodedKey);
            return new SecretKeySpec(decodedKey, "AES");
        }
    }
    
    /**
     * Add method to clear remember me when user logs out
     */
    public void clearRememberMe() {
        rememberMeCheck.setSelected(false);
        clearSavedCredentials();
    }
    
    /**
     * Log logout event (to be called when user logs out)
     */
    public void logLogout(String username) {
        SecurityLogger.logSecurityEvent(
            "LOGOUT", 
            "LOW", 
            username,
            "User logged out from " + SecurityLogger.getClientIP()
        );
    }
    
    /**
     * Close database connections when controller is destroyed
     */
    public void cleanup() {
        // SecurityLogger uses its own database connections (auto-closed)
        // No cleanup needed for security logging
    }
    
}
