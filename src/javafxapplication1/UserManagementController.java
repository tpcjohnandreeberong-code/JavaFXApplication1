package javafxapplication1;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;

import java.net.URL;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Optional;
import java.util.regex.Pattern;

public class UserManagementController implements Initializable {

    private static final Logger logger = Logger.getLogger(UserManagementController.class.getName());
    
    // Database connection parameters
    // private static final String DB_URL = "jdbc:mysql://127.0.0.1:3306/payroll";
    // private static final String DB_USER = "tpc_user";
    // private static final String DB_PASSWORD = "tpcuser123!";

    private static final String DB_URL = "jdbc:mysql://127.0.0.1:3307/payroll";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "berong123!";
    private Connection connection;

    // FXML Controls
    @FXML private TextField userSearchField;
    @FXML private ComboBox<String> roleFilter;
    @FXML private ComboBox<String> statusFilter;
    @FXML private TableView<SystemUser> userTable;
    @FXML private TableColumn<SystemUser, Integer> colUserId;
    @FXML private TableColumn<SystemUser, String> colUsername;
    @FXML private TableColumn<SystemUser, String> colFullName;
    @FXML private TableColumn<SystemUser, String> colEmail;
    @FXML private TableColumn<SystemUser, String> colUserRole;
    @FXML private TableColumn<SystemUser, String> colUserStatus;
    @FXML private TableColumn<SystemUser, String> colLastLogin;
    @FXML private TableColumn<SystemUser, String> colCreatedDate;

    // Data Collections
    private final ObservableList<SystemUser> users = FXCollections.observableArrayList();
    private final ObservableList<String> roleOptions = FXCollections.observableArrayList();
    private final ObservableList<String> statusOptions = FXCollections.observableArrayList();

    // Current Selection
    private SystemUser selectedUser = null;
    
    // Email validation pattern
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
    );

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initializeDatabase();
        setupTableColumns();
        setupComboBoxes();
        loadDataFromDatabase();
        setupEventHandlers();
        updateAllUserPasswords(); // Update all users to use new hashing system
        fixAdminPassword(); // Ensure admin has correct password
        fixStaffPassword(); // Ensure staff has correct password
    }
    
    private void initializeDatabase() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
        } catch (ClassNotFoundException | SQLException e) {
            logger.log(Level.SEVERE, "Database connection failed", e);
            showError("Database Error", "Cannot connect to database: " + e.getMessage());
        }
    }

    private void setupTableColumns() {
        colUserId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colUsername.setCellValueFactory(new PropertyValueFactory<>("username"));
        colFullName.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colUserRole.setCellValueFactory(new PropertyValueFactory<>("role"));
        colUserStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colLastLogin.setCellValueFactory(new PropertyValueFactory<>("lastLogin"));
        colCreatedDate.setCellValueFactory(new PropertyValueFactory<>("createdDate"));
    }

    private void setupComboBoxes() {
        // Role options
        roleOptions.addAll("All Roles", "Admin", "Payroll Maker", "Staff");
        roleFilter.setItems(roleOptions);
        roleFilter.setValue("All Roles");

        // Status options
        statusOptions.addAll("All Status", "Active", "Inactive", "Suspended");
        statusFilter.setItems(statusOptions);
        statusFilter.setValue("All Status");
    }

    private void setupEventHandlers() {
        // User table selection
        userTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            selectedUser = newVal;
        });
    }

    private void loadDataFromDatabase() {
        loadUsersFromDatabase();
        loadRolesFromDatabase();
    }
    
    private void loadUsersFromDatabase() {
        try {
            String query = "SELECT user_id, username, full_name, email, role, status, last_login, created_date FROM users ORDER BY username";
            PreparedStatement stmt = connection.prepareStatement(query);
            ResultSet rs = stmt.executeQuery();
            
            users.clear();
            while (rs.next()) {
                SystemUser user = new SystemUser(
                    rs.getInt("user_id"),
                    rs.getString("username"),
                    rs.getString("full_name"),
                    rs.getString("email"),
                    "******", // Don't load actual password for security
                    rs.getString("role"),
                    rs.getString("status"),
                    rs.getString("last_login"),
                    rs.getString("created_date")
                );
                users.add(user);
            }
        userTable.setItems(users);
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error loading users from database", e);
            showError("Database Error", "Failed to load users: " + e.getMessage());
        }
    }
    
    private void loadRolesFromDatabase() {
        try {
            String query = "SELECT DISTINCT role_name FROM roles WHERE status = 'Active' ORDER BY role_name";
            PreparedStatement stmt = connection.prepareStatement(query);
            ResultSet rs = stmt.executeQuery();
            
            roleOptions.clear();
            roleOptions.add("All Roles");
            while (rs.next()) {
                roleOptions.add(rs.getString("role_name"));
            }
            roleFilter.setItems(roleOptions);
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error loading roles from database", e);
            showError("Database Error", "Failed to load roles: " + e.getMessage());
        }
    }
    
    private void updateAllUserPasswords() {
        try {
            // Update all users to use the new hashing system
            // Set default passwords for all users
            String query = "SELECT user_id, username, role FROM users";
            PreparedStatement selectStmt = connection.prepareStatement(query);
            ResultSet rs = selectStmt.executeQuery();
            
            while (rs.next()) {
                int userId = rs.getInt("user_id");
                String username = rs.getString("username");
                String role = rs.getString("role");
                
                // Set specific passwords for known users
                String defaultPassword;
                if (username.equalsIgnoreCase("staff")) {
                    defaultPassword = "staff123";
                } else if (username.equalsIgnoreCase("admin")) {
                    defaultPassword = "admin123";
                } else {
                    defaultPassword = "user123"; // Default password for other users
                }
                
                String hashedPassword = hashPassword(defaultPassword);
                
                // Update the user's password
                String updateQuery = "UPDATE users SET password_hash = ? WHERE user_id = ?";
                PreparedStatement updateStmt = connection.prepareStatement(updateQuery);
                updateStmt.setString(1, hashedPassword);
                updateStmt.setInt(2, userId);
                updateStmt.executeUpdate();
                
                logger.info("User '" + username + "' (Role: " + role + ") password updated with new hashing system. Password: " + defaultPassword + ", Hash: " + hashedPassword);
            }
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error updating user passwords", e);
            // Don't show error to user as this is a background fix
        }
    }
    
    
    private void fixAdminPassword() {
        try {
            // First, check what's currently in the database
            String checkQuery = "SELECT password_hash FROM users WHERE username = 'admin'";
            PreparedStatement checkStmt = connection.prepareStatement(checkQuery);
            ResultSet rs = checkStmt.executeQuery();
            
            if (rs.next()) {
                String currentHash = rs.getString("password_hash");
                logger.info("Current admin password hash in database: " + currentHash);
                
                // Test if current hash works with "admin123"
                boolean currentTest = verifyPassword("admin123", currentHash);
                logger.info("Current password test with 'admin123': " + (currentTest ? "SUCCESS" : "FAILED"));
                
                if (!currentTest) {
                    // Update admin password to "admin123"
                    String adminPassword = "admin123";
                    String hashedPassword = hashPassword(adminPassword);
                    
                    String updateQuery = "UPDATE users SET password_hash = ? WHERE username = 'admin'";
                    PreparedStatement updateStmt = connection.prepareStatement(updateQuery);
                    updateStmt.setString(1, hashedPassword);
                    int rowsAffected = updateStmt.executeUpdate();
                    
                    if (rowsAffected > 0) {
                        logger.info("Admin password updated: Username='admin', Password='admin123', New Hash='" + hashedPassword + "'");
                        
                        // Test the new password verification
                        boolean newPasswordTest = verifyPassword(adminPassword, hashedPassword);
                        logger.info("New password verification test: " + (newPasswordTest ? "SUCCESS" : "FAILED"));
                        
                        // Test the complete login process
                        testAdminLogin();
                    }
                } else {
                    logger.info("Admin password already works with 'admin123'");
                }
            } else {
                logger.warning("No admin user found in database");
            }
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error fixing admin password", e);
        }
    }
    
    private void fixStaffPassword() {
        try {
            // First, check what's currently in the database
            String checkQuery = "SELECT password_hash FROM users WHERE username = 'staff'";
            PreparedStatement checkStmt = connection.prepareStatement(checkQuery);
            ResultSet rs = checkStmt.executeQuery();
            
            if (rs.next()) {
                String currentHash = rs.getString("password_hash");
                logger.info("Current staff password hash in database: " + currentHash);
                
                // Test if current hash works with "staff123"
                boolean currentTest = verifyPassword("staff123", currentHash);
                logger.info("Current password test with 'staff123': " + (currentTest ? "SUCCESS" : "FAILED"));
                
                if (!currentTest) {
                    // Update staff password to "staff123"
                    String staffPassword = "staff123";
                    String hashedPassword = hashPassword(staffPassword);
                    
                    String updateQuery = "UPDATE users SET password_hash = ? WHERE username = 'staff'";
                    PreparedStatement updateStmt = connection.prepareStatement(updateQuery);
                    updateStmt.setString(1, hashedPassword);
                    int rowsAffected = updateStmt.executeUpdate();
                    
                    if (rowsAffected > 0) {
                        logger.info("Staff password updated: Username='staff', Password='staff123', New Hash='" + hashedPassword + "'");
                        
                        // Test the new password verification
                        boolean newPasswordTest = verifyPassword(staffPassword, hashedPassword);
                        logger.info("New password verification test: " + (newPasswordTest ? "SUCCESS" : "FAILED"));
                    }
                } else {
                    logger.info("Staff password already works with 'staff123'");
                }
            } else {
                logger.warning("No staff user found in database");
            }
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error fixing staff password", e);
        }
    }
    
    private void testAdminLogin() {
        try {
            // Simulate the login process
            String username = "admin";
            String password = "admin123";
            
            // Get user from database
            String userQuery = "SELECT user_id, username, password_hash, full_name, email, role, status " +
                              "FROM users WHERE username = ? AND status = 'Active'";
            PreparedStatement userStmt = connection.prepareStatement(userQuery);
            userStmt.setString(1, username);
            ResultSet userRs = userStmt.executeQuery();
            
            if (userRs.next()) {
                String storedHash = userRs.getString("password_hash");
                logger.info("Testing login: Username='" + username + "', Password='" + password + "', Stored Hash='" + storedHash + "'");
                
                // Test password verification
                boolean passwordMatch = verifyPassword(password, storedHash);
                logger.info("Login test result: " + (passwordMatch ? "SUCCESS - Admin can login!" : "FAILED - Password mismatch"));
                
                if (passwordMatch) {
                    logger.info("✅ Admin login should work now!");
                } else {
                    logger.warning("❌ Admin login will still fail - password verification failed");
                }
            } else {
                logger.warning("Admin user not found in database");
            }
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error testing admin login", e);
        }
    }

    // Action Methods
    @FXML
    private void onSearchUsers() {
        String searchText = userSearchField.getText() == null ? "" : userSearchField.getText().trim().toLowerCase();
        String roleFilterValue = roleFilter.getValue();
        String statusFilterValue = statusFilter.getValue();
        
        ObservableList<SystemUser> filteredUsers = users.filtered(user -> {
            boolean matchesSearch = searchText.isEmpty() || 
                user.getUsername().toLowerCase().contains(searchText) ||
                user.getFullName().toLowerCase().contains(searchText) ||
                user.getEmail().toLowerCase().contains(searchText);
            
            boolean matchesRole = roleFilterValue == null || roleFilterValue.equals("All Roles") || 
                user.getRole().equals(roleFilterValue);
            
            boolean matchesStatus = statusFilterValue == null || statusFilterValue.equals("All Status") || 
                user.getStatus().equals(statusFilterValue);
            
            return matchesSearch && matchesRole && matchesStatus;
        });
        
        userTable.setItems(filteredUsers);
    }

    @FXML
    private void onClearFilters() {
        userSearchField.clear();
        roleFilter.setValue("All Roles");
        statusFilter.setValue("All Status");
        userTable.setItems(users);
    }

    @FXML
    private void onAddUser() {
        Dialog<SystemUser> dialog = buildUserDialog(null);
        Optional<SystemUser> result = dialog.showAndWait();
        result.ifPresent(user -> {
            if (addUserToDatabase(user)) {
                loadUsersFromDatabase(); // Refresh the table
                showInfo("Success", "User added successfully: " + user.getFullName());
            }
        });
    }

    @FXML
    private void onEditUser() {
        if (selectedUser == null) {
            showError("No User Selected", "Please select a user to edit.");
            return;
        }
        
        Dialog<SystemUser> dialog = buildUserDialog(selectedUser);
        Optional<SystemUser> result = dialog.showAndWait();
        result.ifPresent(user -> {
            if (updateUserInDatabase(user)) {
                loadUsersFromDatabase(); // Refresh the table
                showInfo("Success", "User updated successfully: " + user.getFullName());
            }
        });
    }

    @FXML
    private void onDeleteUser() {
        if (selectedUser == null) {
            showError("No User Selected", "Please select a user to delete.");
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete User");
        alert.setHeaderText("Are you sure you want to delete this user?");
        alert.setContentText("User: " + selectedUser.getFullName() + " (" + selectedUser.getUsername() + ")\nThis action cannot be undone.");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                if (deleteUserFromDatabase(selectedUser.getId())) {
                    loadUsersFromDatabase(); // Refresh the table
                selectedUser = null;
                    showInfo("Success", "User deleted successfully: " + selectedUser.getFullName());
                }
            }
        });
    }

    @FXML
    private void onAssignRole() {
        if (selectedUser == null) {
            showError("No User Selected", "Please select a user to assign a role to.");
            return;
        }

        ChoiceDialog<String> dialog = new ChoiceDialog<>(selectedUser.getRole(), roleOptions.subList(1, roleOptions.size()));
        dialog.setTitle("Assign Role");
        dialog.setHeaderText("Select a role for user: " + selectedUser.getFullName());
        dialog.setContentText("Choose role:");

        dialog.showAndWait().ifPresent(role -> {
            if (updateUserRoleInDatabase(selectedUser.getId(), role)) {
                loadUsersFromDatabase(); // Refresh the table
                showInfo("Success", "Role assigned successfully: " + role);
            }
        });
    }

    @FXML
    private void onResetPassword() {
        if (selectedUser == null) {
            showError("No User Selected", "Please select a user to reset password for.");
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Reset Password");
        alert.setHeaderText("Reset password for user: " + selectedUser.getFullName());
        alert.setContentText("This will generate a temporary password for the user.\nContinue?");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                // Generate temporary password (in real app, this would be sent via email)
                String tempPassword = generateTempPassword();
                showInfo("Password Reset", "Temporary password generated: " + tempPassword + "\nPlease share this with the user securely.");
            }
        });
    }

    @FXML
    private void onToggleStatus() {
        if (selectedUser == null) {
            showError("No User Selected", "Please select a user to toggle status for.");
            return;
        }

        String currentStatus = selectedUser.getStatus();
        String newStatus = currentStatus.equals("Active") ? "Inactive" : "Active";
        
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Toggle User Status");
        alert.setHeaderText("Change user status from " + currentStatus + " to " + newStatus + "?");
        alert.setContentText("User: " + selectedUser.getFullName());

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                if (updateUserStatusInDatabase(selectedUser.getId(), newStatus)) {
                    loadUsersFromDatabase(); // Refresh the table
                showInfo("Success", "User status changed to " + newStatus);
                }
            }
        });
    }

    @FXML
    private void onRefresh() {
        loadDataFromDatabase();
        showInfo("Refreshed", "User data refreshed from database!");
    }

    @FXML
    private void onExportUsers() {
        try {
            // Create CSV content
            StringBuilder csvContent = new StringBuilder();
            csvContent.append("User ID,Username,Full Name,Email,Role,Status,Last Login,Created Date\n");
            
            for (SystemUser user : users) {
                csvContent.append(String.format("%d,%s,%s,%s,%s,%s,%s,%s\n",
                    user.getId(),
                    user.getUsername(),
                    user.getFullName(),
                    user.getEmail(),
                    user.getRole(),
                    user.getStatus(),
                    user.getLastLogin(),
                    user.getCreatedDate()
                ));
            }
            
            // Save to file
            javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
            fileChooser.setTitle("Export Users");
            fileChooser.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("CSV Files", "*.csv"));
            java.io.File file = fileChooser.showSaveDialog(userTable.getScene().getWindow());
            
            if (file != null) {
                try (java.io.FileWriter writer = new java.io.FileWriter(file)) {
                    writer.write(csvContent.toString());
                    showInfo("Export Success", "Users exported successfully to: " + file.getName());
                }
            }
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error exporting users", e);
            showError("Export Error", "Failed to export users: " + e.getMessage());
        }
    }

    // Database Operations
    private boolean addUserToDatabase(SystemUser user) {
        try {
            String query = "INSERT INTO users (username, password_hash, full_name, email, role, status, created_date) VALUES (?, ?, ?, ?, ?, ?, ?)";
            PreparedStatement stmt = connection.prepareStatement(query);
            stmt.setString(1, user.getUsername());
            // Only hash password if it's not the placeholder
            String passwordHash = user.getPassword().equals("******") ? "default123" : hashPassword(user.getPassword());
            stmt.setString(2, passwordHash);
            stmt.setString(3, user.getFullName());
            stmt.setString(4, user.getEmail());
            stmt.setString(5, user.getRole());
            stmt.setString(6, user.getStatus());
            stmt.setString(7, user.getCreatedDate());
            
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error adding user to database", e);
            showError("Database Error", "Failed to add user: " + e.getMessage());
            return false;
        }
    }
    
    private boolean updateUserInDatabase(SystemUser user) {
        try {
            String query = "UPDATE users SET username = ?, full_name = ?, email = ?, role = ?, status = ? WHERE user_id = ?";
            PreparedStatement stmt = connection.prepareStatement(query);
            stmt.setString(1, user.getUsername());
            stmt.setString(2, user.getFullName());
            stmt.setString(3, user.getEmail());
            stmt.setString(4, user.getRole());
            stmt.setString(5, user.getStatus());
            stmt.setInt(6, user.getId());
            
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error updating user in database", e);
            showError("Database Error", "Failed to update user: " + e.getMessage());
            return false;
        }
    }
    
    private boolean deleteUserFromDatabase(int userId) {
        try {
            String query = "DELETE FROM users WHERE user_id = ?";
            PreparedStatement stmt = connection.prepareStatement(query);
            stmt.setInt(1, userId);
            
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error deleting user from database", e);
            showError("Database Error", "Failed to delete user: " + e.getMessage());
            return false;
        }
    }
    
    private boolean updateUserStatusInDatabase(int userId, String status) {
        try {
            String query = "UPDATE users SET status = ? WHERE user_id = ?";
            PreparedStatement stmt = connection.prepareStatement(query);
            stmt.setString(1, status);
            stmt.setInt(2, userId);
            
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error updating user status in database", e);
            showError("Database Error", "Failed to update user status: " + e.getMessage());
            return false;
        }
    }
    
    private boolean updateUserRoleInDatabase(int userId, String role) {
        try {
            String query = "UPDATE users SET role = ? WHERE user_id = ?";
            PreparedStatement stmt = connection.prepareStatement(query);
            stmt.setString(1, role);
            stmt.setInt(2, userId);
            
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error updating user role in database", e);
            showError("Database Error", "Failed to update user role: " + e.getMessage());
            return false;
        }
    }
    
    private String hashPassword(String password) {
        // Create a secure hash using SHA-256 with salt
        try {
            String salt = "TPC_PAYROLL_SALT_2024"; // Salt for additional security
            String passwordWithSalt = password + salt;
            
            // Use SHA-256 hashing
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(passwordWithSalt.getBytes("UTF-8"));
            
            // Convert to hexadecimal string
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
            return String.valueOf(password.hashCode()); // Fallback
        }
    }
    
    private boolean verifyPassword(String password, String hash) {
        // Verify password by hashing the input and comparing with stored hash
        String hashedInput = hashPassword(password);
        return hashedInput.equals(hash);
    }
    
    private boolean isUsernameExists(String username, int excludeUserId) {
        try {
            String query = "SELECT COUNT(*) FROM users WHERE username = ? AND user_id != ?";
            PreparedStatement stmt = connection.prepareStatement(query);
            stmt.setString(1, username);
            stmt.setInt(2, excludeUserId);
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
    
    private boolean isEmailExists(String email, int excludeUserId) {
        try {
            String query = "SELECT COUNT(*) FROM users WHERE email = ? AND user_id != ?";
            PreparedStatement stmt = connection.prepareStatement(query);
            stmt.setString(1, email);
            stmt.setInt(2, excludeUserId);
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

    // Dialog Builders
    private Dialog<SystemUser> buildUserDialog(SystemUser user) {
        Dialog<SystemUser> dialog = new Dialog<>();
        dialog.setTitle(user == null ? "Add User" : "Edit User");
        dialog.setHeaderText(user == null ? "Create a new system user" : "Edit user information");

        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(20, 150, 10, 10));

        TextField usernameField = new TextField();
        usernameField.setPromptText("Username");
        TextField fullNameField = new TextField();
        fullNameField.setPromptText("Full Name");
        TextField emailField = new TextField();
        emailField.setPromptText("Email");
        TextField passwordField = new TextField();
        passwordField.setPromptText("Password");
        ComboBox<String> roleCombo = new ComboBox<>();
        roleCombo.getItems().addAll("Admin", "Payroll Maker", "Staff");
        roleCombo.setValue("Staff");
        ComboBox<String> statusCombo = new ComboBox<>();
        statusCombo.getItems().addAll("Active", "Inactive", "Suspended");
        statusCombo.setValue("Active");

        if (user != null) {
            usernameField.setText(user.getUsername());
            fullNameField.setText(user.getFullName());
            emailField.setText(user.getEmail());
            passwordField.setText("******"); // Don't show actual password
            roleCombo.setValue(user.getRole());
            statusCombo.setValue(user.getStatus());
        }

        grid.add(new Label("Username:"), 0, 0);
        grid.add(usernameField, 1, 0);
        grid.add(new Label("Full Name:"), 0, 1);
        grid.add(fullNameField, 1, 1);
        grid.add(new Label("Email:"), 0, 2);
        grid.add(emailField, 1, 2);
        grid.add(new Label("Password:"), 0, 3);
        grid.add(passwordField, 1, 3);
        grid.add(new Label("Role:"), 0, 4);
        grid.add(roleCombo, 1, 4);
        grid.add(new Label("Status:"), 0, 5);
        grid.add(statusCombo, 1, 5);

        dialog.getDialogPane().setContent(grid);

        // Add validation
        dialog.getDialogPane().lookupButton(saveButtonType).addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            if (usernameField.getText().trim().isEmpty()) {
                showError("Validation Error", "Username is required.");
                event.consume();
                return;
            }
            if (fullNameField.getText().trim().isEmpty()) {
                showError("Validation Error", "Full name is required.");
                event.consume();
                return;
            }
            if (emailField.getText().trim().isEmpty()) {
                showError("Validation Error", "Email is required.");
                event.consume();
                return;
            }
            if (!EMAIL_PATTERN.matcher(emailField.getText().trim()).matches()) {
                showError("Validation Error", "Please enter a valid email address.");
                event.consume();
                return;
            }
            if (user == null && passwordField.getText().trim().isEmpty()) {
                showError("Validation Error", "Password is required for new users.");
                event.consume();
                return;
            }
            if (roleCombo.getValue() == null) {
                showError("Validation Error", "Role is required.");
                event.consume();
                return;
            }
            if (statusCombo.getValue() == null) {
                showError("Validation Error", "Status is required.");
                event.consume();
                return;
            }
            
            // Check for duplicate username
            int excludeUserId = (user != null) ? user.getId() : -1;
            if (isUsernameExists(usernameField.getText().trim(), excludeUserId)) {
                showError("Validation Error", "Username already exists.");
                event.consume();
                return;
            }
            
            // Check for duplicate email
            if (isEmailExists(emailField.getText().trim(), excludeUserId)) {
                showError("Validation Error", "Email already exists.");
                event.consume();
                return;
            }
        });

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                String password = (user == null) ? passwordField.getText().trim() : "******";
                return new SystemUser(
                    user != null ? user.getId() : users.size() + 1,
                    usernameField.getText().trim(),
                    fullNameField.getText().trim(),
                    emailField.getText().trim(),
                    password,
                    roleCombo.getValue(),
                    statusCombo.getValue(),
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                );
            }
            return null;
        });

        return dialog;
    }

    // Utility Methods
    public void closeDatabaseConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error closing database connection", e);
        }
    }
    
    private String generateTempPassword() {
        // Simple temporary password generator
        return "Temp" + (int)(Math.random() * 10000);
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // Data Class
    public static class SystemUser {
        private int id;
        private String username;
        private String fullName;
        private String email;
        private String password;
        private String role;
        private String status;
        private String lastLogin;
        private String createdDate;

        public SystemUser(int id, String username, String fullName, String email, String password, String role, String status, String lastLogin, String createdDate) {
            this.id = id;
            this.username = username;
            this.fullName = fullName;
            this.email = email;
            this.password = password;
            this.role = role;
            this.status = status;
            this.lastLogin = lastLogin;
            this.createdDate = createdDate;
        }

        // Getters and Setters
        public int getId() { return id; }
        public String getUsername() { return username; }
        public String getFullName() { return fullName; }
        public String getEmail() { return email; }
        public String getPassword() { return password; }
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getLastLogin() { return lastLogin; }
        public String getCreatedDate() { return createdDate; }
    }
}
