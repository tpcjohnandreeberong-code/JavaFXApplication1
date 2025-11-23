package javafxapplication1;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import java.net.URL;
import java.sql.*;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * FXML Controller class for Edit Profile page
 *
 * @author marke
 */
public class EditProfileController implements Initializable {

    private static final Logger logger = Logger.getLogger(EditProfileController.class.getName());
    
    // Database connection parameters
    private static final String DB_URL = DatabaseConfig.getDbUrl();
    private static final String DB_USER = DatabaseConfig.getDbUser();
    private static final String DB_PASSWORD = DatabaseConfig.getDbPassword();
    private Connection connection;
    
    // Note: Using SecurityLogger static class for security event logging
    
    // Current user data
    private int currentUserId;
    private String originalUsername;
    private String originalFullName;
    private String originalEmail;

    // Profile Information Fields
    @FXML private TextField fullNameField;
    @FXML private TextField usernameField;
    @FXML private TextField emailField;
    
    // Password Change Fields
    @FXML private PasswordField currentPasswordField;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;
    
    // Email validation pattern
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
    );

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        initializeDatabase();
        loadCurrentUserData();
    }
    
    private void initializeDatabase() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            logger.info("Database connection established for EditProfile");
        } catch (ClassNotFoundException | SQLException e) {
            logger.log(Level.SEVERE, "Database connection failed", e);
            showErrorAlert("Database Error", "Cannot connect to database: " + e.getMessage());
        }
    }
    
    /**
     * Load current user data from database into the form fields
     */
    private void loadCurrentUserData() {
        // Get current user from SessionManager
        String currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser == null) {
            showErrorAlert("Session Error", "No user session found. Please login again.");
            return;
        }
        
        try {
            String query = "SELECT user_id, username, full_name, email FROM users WHERE username = ? AND status = 'Active'";
            PreparedStatement stmt = connection.prepareStatement(query);
            stmt.setString(1, currentUser);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                currentUserId = rs.getInt("user_id");
                originalUsername = rs.getString("username");
                originalFullName = rs.getString("full_name");
                originalEmail = rs.getString("email");
                
                // Populate fields with current data
                fullNameField.setText(originalFullName != null ? originalFullName : "");
                usernameField.setText(originalUsername != null ? originalUsername : "");
                emailField.setText(originalEmail != null ? originalEmail : "");
                
                logger.info("Loaded profile for user: " + originalUsername);
            } else {
                showErrorAlert("Error", "Current user profile not found in database.");
            }
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error loading user profile", e);
            showErrorAlert("Database Error", "Failed to load user profile: " + e.getMessage());
        }
    }
    
    @FXML
    private void onUpdateProfile() {
        if (!validateProfileFields()) {
            return;
        }
        
        String newFullName = fullNameField.getText().trim();
        String newUsername = usernameField.getText().trim();
        String newEmail = emailField.getText().trim();
        
        // Check if anything actually changed
        if (newFullName.equals(originalFullName) && 
            newUsername.equals(originalUsername) && 
            newEmail.equals(originalEmail)) {
            showSuccessAlert("No changes detected.");
            return;
        }
        
        try {
            // Check for duplicate username (excluding current user)
            if (!newUsername.equals(originalUsername) && isUsernameExists(newUsername)) {
                showErrorAlert("Validation Error", "Username already exists. Please choose a different username.");
                usernameField.requestFocus();
                return;
            }
            
            // Check for duplicate email (excluding current user)
            if (!newEmail.equals(originalEmail) && isEmailExists(newEmail)) {
                showErrorAlert("Validation Error", "Email address already exists. Please use a different email.");
                emailField.requestFocus();
                return;
            }
            
            String updateQuery = "UPDATE users SET full_name = ?, username = ?, email = ? WHERE user_id = ?";
            PreparedStatement stmt = connection.prepareStatement(updateQuery);
            stmt.setString(1, newFullName);
            stmt.setString(2, newUsername);
            stmt.setString(3, newEmail);
            stmt.setInt(4, currentUserId);
            
            int rowsAffected = stmt.executeUpdate();
            
            if (rowsAffected > 0) {
                // Update SessionManager if username changed
                if (!newUsername.equals(originalUsername)) {
                    SessionManager.getInstance().setCurrentUser(newUsername);
                }
                
                // Update original values
                originalFullName = newFullName;
                originalUsername = newUsername;
                originalEmail = newEmail;
                
                showSuccessAlert("Profile updated successfully!");
                logger.info("Profile updated for user ID: " + currentUserId);
            } else {
                showErrorAlert("Error", "Failed to update profile. Please try again.");
            }
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error updating user profile", e);
            showErrorAlert("Database Error", "Failed to update profile: " + e.getMessage());
        }
    }
    
    @FXML
    private void onChangePassword() {
        if (!validatePasswordFields()) {
            return;
        }
        
        String currentPassword = currentPasswordField.getText();
        String newPassword = newPasswordField.getText();
        
        try {
            // First, verify current password
            String verifyQuery = "SELECT password_hash FROM users WHERE user_id = ?";
            PreparedStatement verifyStmt = connection.prepareStatement(verifyQuery);
            verifyStmt.setInt(1, currentUserId);
            ResultSet rs = verifyStmt.executeQuery();
            
            if (rs.next()) {
                String storedHash = rs.getString("password_hash");
                
                if (!verifyPassword(currentPassword, storedHash)) {
                    showErrorAlert("Validation Error", "Current password is incorrect.");
                    currentPasswordField.requestFocus();
                    return;
                }
                
                // Update to new password
                String newPasswordHash = hashPassword(newPassword);
                String updateQuery = "UPDATE users SET password_hash = ? WHERE user_id = ?";
                PreparedStatement updateStmt = connection.prepareStatement(updateQuery);
                updateStmt.setString(1, newPasswordHash);
                updateStmt.setInt(2, currentUserId);
                
                int rowsAffected = updateStmt.executeUpdate();
                
                if (rowsAffected > 0) {
                    clearPasswordFields();
                    showSuccessAlert("Password changed successfully!");
                    logger.info("Password changed for user ID: " + currentUserId);
                } else {
                    showErrorAlert("Error", "Failed to change password. Please try again.");
                }
            } else {
                showErrorAlert("Error", "User not found.");
            }
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error changing password", e);
            showErrorAlert("Database Error", "Failed to change password: " + e.getMessage());
        }
    }
    
    @FXML
    private void onClearPassword() {
        clearPasswordFields();
    }
    
    @FXML
    private void onCancel() {
        // Reset form to original values
        fullNameField.setText(originalFullName != null ? originalFullName : "");
        usernameField.setText(originalUsername != null ? originalUsername : "");
        emailField.setText(originalEmail != null ? originalEmail : "");
        clearPasswordFields();
        showSuccessAlert("Changes cancelled. Original values restored.");
    }
    
    
    /**
     * Validate profile information fields
     */
    private boolean validateProfileFields() {
        // Check required fields
        if (fullNameField.getText().trim().isEmpty()) {
            showErrorAlert("Validation Error", "Full name is required.");
            fullNameField.requestFocus();
            return false;
        }
        
        if (usernameField.getText().trim().isEmpty()) {
            showErrorAlert("Validation Error", "Username is required.");
            usernameField.requestFocus();
            return false;
        }
        
        if (emailField.getText().trim().isEmpty()) {
            showErrorAlert("Validation Error", "Email is required.");
            emailField.requestFocus();
            return false;
        }
        
        // Validate email format
        if (!EMAIL_PATTERN.matcher(emailField.getText().trim()).matches()) {
            showErrorAlert("Validation Error", "Please enter a valid email address.");
            emailField.requestFocus();
            return false;
        }
        
        return true;
    }
    
    /**
     * Validate password change fields
     */
    private boolean validatePasswordFields() {
        String currentPassword = currentPasswordField.getText();
        String newPassword = newPasswordField.getText();
        String confirmPassword = confirmPasswordField.getText();
        
        // Check if current password is provided
        if (currentPassword.isEmpty()) {
            showErrorAlert("Validation Error", "Current password is required.");
            currentPasswordField.requestFocus();
            return false;
        }
        
        // Check if new password is provided
        if (newPassword.isEmpty()) {
            showErrorAlert("Validation Error", "New password is required.");
            newPasswordField.requestFocus();
            return false;
        }
        
        // Check if confirm password is provided
        if (confirmPassword.isEmpty()) {
            showErrorAlert("Validation Error", "Please confirm your new password.");
            confirmPasswordField.requestFocus();
            return false;
        }
        
        // Check if new password meets requirements
        if (newPassword.length() < 8) {
            showErrorAlert("Validation Error", "New password must be at least 8 characters long.");
            newPasswordField.requestFocus();
            return false;
        }
        
        // Check if passwords match
        if (!newPassword.equals(confirmPassword)) {
            showErrorAlert("Validation Error", "New password and confirm password do not match.");
            confirmPasswordField.requestFocus();
            return false;
        }
        
        // Check if new password is different from current password
        if (currentPassword.equals(newPassword)) {
            showErrorAlert("Validation Error", "New password must be different from current password.");
            newPasswordField.requestFocus();
            return false;
        }
        
        // Note: Current password verification is now done in onChangePassword method
        // against the actual database, not here in validation
        
        return true;
    }
    
    /**
     * Check if username exists in database (excluding current user)
     */
    private boolean isUsernameExists(String username) {
        try {
            String query = "SELECT COUNT(*) FROM users WHERE username = ? AND user_id != ?";
            PreparedStatement stmt = connection.prepareStatement(query);
            stmt.setString(1, username);
            stmt.setInt(2, currentUserId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
            return false;
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error checking username existence", e);
            return false;
        }
    }
    
    /**
     * Check if email exists in database (excluding current user)
     */
    private boolean isEmailExists(String email) {
        try {
            String query = "SELECT COUNT(*) FROM users WHERE email = ? AND user_id != ?";
            PreparedStatement stmt = connection.prepareStatement(query);
            stmt.setString(1, email);
            stmt.setInt(2, currentUserId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
            return false;
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error checking email existence", e);
            return false;
        }
    }
    
    /**
     * Hash password using SHA-256 with salt
     */
    private String hashPassword(String password) {
        try {
            String salt = "TPC_PAYROLL_SALT_2024";
            String passwordWithSalt = password + salt;
            
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(passwordWithSalt.getBytes("UTF-8"));
            
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            return hexString.toString();
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error hashing password", e);
            return String.valueOf(password.hashCode());
        }
    }
    
    /**
     * Verify password against stored hash
     */
    private boolean verifyPassword(String password, String hash) {
        String hashedInput = hashPassword(password);
        return hashedInput.equals(hash);
    }
    
    /**
     * Close database connection when controller is destroyed
     */
    public void closeDatabaseConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                logger.info("Database connection closed for EditProfile");
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error closing database connection", e);
        }
    }
    
    /**
     * Clear all password fields
     */
    private void clearPasswordFields() {
        currentPasswordField.clear();
        newPasswordField.clear();
        confirmPasswordField.clear();
    }
    
    /**
     * Show success alert
     */
    private void showSuccessAlert(String message) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.getDialogPane().setStyle("-fx-background-color: #f5f5f5;");
        alert.showAndWait();
    }
    
    /**
     * Show error alert
     */
    private void showErrorAlert(String title, String message) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.getDialogPane().setStyle("-fx-background-color: #f5f5f5;");
        alert.showAndWait();
    }
}
