package javafxapplication1;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.regex.Pattern;

/**
 * FXML Controller class for Edit Profile page
 *
 * @author marke
 */
public class EditProfileController implements Initializable {

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
        loadCurrentUserData();
    }
    
    /**
     * Load current user data into the form fields
     * In a real application, this would fetch data from a database
     */
    private void loadCurrentUserData() {
        // Sample data - replace with actual user data from database
        fullNameField.setText("Admin User");
        usernameField.setText("admin");
        emailField.setText("admin@tpc.edu.ph");
    }
    
    @FXML
    private void onUpdateProfile() {
        if (validateProfileFields()) {
            // In a real application, save to database here
            showSuccessAlert("Profile updated successfully!");
            loadCurrentUserData(); // Reload data to show updated values
        }
    }
    
    @FXML
    private void onChangePassword() {
        if (validatePasswordFields()) {
            // In a real application, verify current password and update new password in database
            showSuccessAlert("Password changed successfully!");
            clearPasswordFields();
        }
    }
    
    @FXML
    private void onClearPassword() {
        clearPasswordFields();
    }
    
    @FXML
    private void onCancel() {
        // Reset form to original values
        loadCurrentUserData();
        clearPasswordFields();
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
        
        // In a real application, verify current password against database
        // For demo purposes, we'll assume current password is "admin123"
        if (!currentPassword.equals("admin123")) {
            showErrorAlert("Validation Error", "Current password is incorrect.");
            currentPasswordField.requestFocus();
            return false;
        }
        
        return true;
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
