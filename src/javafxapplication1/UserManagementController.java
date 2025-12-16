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

    private static final String DB_URL = DatabaseConfig.getDbUrl();
    private static final String DB_USER = DatabaseConfig.getDbUser();
    private static final String DB_PASSWORD = DatabaseConfig.getDbPassword();

    
    private Connection connection;
    
    // Note: Using SecurityLogger static class for security event logging

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
    
    // Action buttons
    @FXML private Button refreshButton;
    @FXML private Button exportUsersButton;
    @FXML private Button searchButton;
    @FXML private Button clearFiltersButton;
    @FXML private Button addUserButton;
    @FXML private Button editUserButton;
    @FXML private Button deleteUserButton;
    @FXML private Button assignRoleButton;
    @FXML private Button resetPasswordButton;
    @FXML private Button toggleStatusButton;

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
        
        // Setup permission-based button visibility
        setupPermissionBasedVisibility();
        
        // Log module access
        String currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser != null) {
            SecurityLogger.logSecurityEvent(
                "USER_MANAGEMENT_MODULE_ACCESS",
                "LOW",
                currentUser,
                "Accessed User Management module"
            );
        }
    }
    
    private void setupPermissionBasedVisibility() {
        try {
            // Get current user from SessionManager
            SessionManager sessionManager = SessionManager.getInstance();
            
            if (sessionManager.isLoggedIn()) {
                String currentUser = sessionManager.getCurrentUser();
                logger.info("Setting up permission-based visibility for user: " + currentUser);
                
                // Check user permissions
                boolean canView = hasUserPermission(currentUser, "user_mgmt.view");
                boolean canAdd = hasUserPermission(currentUser, "user_mgmt.add");
                boolean canEdit = hasUserPermission(currentUser, "user_mgmt.edit");
                boolean canDelete = hasUserPermission(currentUser, "user_mgmt.delete");
                
                // Show/hide buttons based on permissions
                // View permissions: Refresh, Export Users, Search, Clear Filters
                if (refreshButton != null) {
                    refreshButton.setVisible(canView);
                    refreshButton.setManaged(canView);
                }
                if (exportUsersButton != null) {
                    exportUsersButton.setVisible(canView);
                    exportUsersButton.setManaged(canView);
                }
                if (searchButton != null) {
                    searchButton.setVisible(canView);
                    searchButton.setManaged(canView);
                }
                if (clearFiltersButton != null) {
                    clearFiltersButton.setVisible(canView);
                    clearFiltersButton.setManaged(canView);
                }
                
                // Add permission
                if (addUserButton != null) {
                    addUserButton.setVisible(canAdd);
                    addUserButton.setManaged(canAdd);
                }
                
                // Edit permissions: Edit User, Assign Role, Reset Password, Toggle Status
                if (editUserButton != null) {
                    editUserButton.setVisible(canEdit);
                    editUserButton.setManaged(canEdit);
                }
                if (assignRoleButton != null) {
                    assignRoleButton.setVisible(canEdit);
                    assignRoleButton.setManaged(canEdit);
                }
                if (resetPasswordButton != null) {
                    resetPasswordButton.setVisible(canEdit);
                    resetPasswordButton.setManaged(canEdit);
                }
                if (toggleStatusButton != null) {
                    toggleStatusButton.setVisible(canEdit);
                    toggleStatusButton.setManaged(canEdit);
                }
                
                // Delete permission
                if (deleteUserButton != null) {
                    deleteUserButton.setVisible(canDelete);
                    deleteUserButton.setManaged(canDelete);
                }
                
                logger.info("User Management buttons visibility - View: " + canView + ", Add: " + canAdd + ", Edit: " + canEdit + ", Delete: " + canDelete);
                
            } else {
                logger.warning("No user session found, hiding all action buttons");
                hideAllActionButtons();
            }
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error setting up permission-based visibility", e);
            hideAllActionButtons();
        }
    }
    
    private void hideAllActionButtons() {
        if (refreshButton != null) {
            refreshButton.setVisible(false);
            refreshButton.setManaged(false);
        }
        if (exportUsersButton != null) {
            exportUsersButton.setVisible(false);
            exportUsersButton.setManaged(false);
        }
        if (searchButton != null) {
            searchButton.setVisible(false);
            searchButton.setManaged(false);
        }
        if (clearFiltersButton != null) {
            clearFiltersButton.setVisible(false);
            clearFiltersButton.setManaged(false);
        }
        if (addUserButton != null) {
            addUserButton.setVisible(false);
            addUserButton.setManaged(false);
        }
        if (editUserButton != null) {
            editUserButton.setVisible(false);
            editUserButton.setManaged(false);
        }
        if (deleteUserButton != null) {
            deleteUserButton.setVisible(false);
            deleteUserButton.setManaged(false);
        }
        if (assignRoleButton != null) {
            assignRoleButton.setVisible(false);
            assignRoleButton.setManaged(false);
        }
        if (resetPasswordButton != null) {
            resetPasswordButton.setVisible(false);
            resetPasswordButton.setManaged(false);
        }
        if (toggleStatusButton != null) {
            toggleStatusButton.setVisible(false);
            toggleStatusButton.setManaged(false);
        }
    }
    
    private boolean hasUserPermission(String username, String permissionName) {
        try {
            if (connection == null || connection.isClosed()) {
                logger.warning("Database connection not available, defaulting to admin permissions for user: " + username);
                return username != null && username.equals("admin");
            }
            
            // Get user's role from database
            String getRoleQuery = "SELECT role FROM users WHERE username = ?";
            PreparedStatement getRoleStmt = connection.prepareStatement(getRoleQuery);
            getRoleStmt.setString(1, username);
            ResultSet roleRs = getRoleStmt.executeQuery();
            
            if (roleRs.next()) {
                String userRole = roleRs.getString("role");
                
                // Check if role has the permission
                String checkPermissionQuery = "SELECT COUNT(*) FROM role_permissions rp " +
                                           "JOIN roles r ON rp.role_id = r.role_id " +
                                           "JOIN permissions p ON rp.permission_id = p.permission_id " +
                                           "WHERE r.role_name = ? AND p.permission_name = ? AND rp.granted = TRUE";
                PreparedStatement checkPermissionStmt = connection.prepareStatement(checkPermissionQuery);
                checkPermissionStmt.setString(1, userRole);
                checkPermissionStmt.setString(2, permissionName);
                ResultSet permissionRs = checkPermissionStmt.executeQuery();
                
                if (permissionRs.next()) {
                    boolean hasPermission = permissionRs.getInt(1) > 0;
                    logger.info("User " + username + " (Role: " + userRole + ") has permission " + permissionName + ": " + hasPermission);
                    return hasPermission;
                }
            }
            
            return false;
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error checking user permission", e);
            // If database error occurs, default to admin permissions for admin user
            return username != null && username.equals("admin");
        }
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
        
        // Set column resize policy to auto-fit the table width
        userTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
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
        
        // Log search activity (only if there's actual search criteria)
        if (!searchText.isEmpty() || (roleFilterValue != null && !roleFilterValue.equals("All Roles")) || 
            (statusFilterValue != null && !statusFilterValue.equals("All Status"))) {
            String currentUser = SessionManager.getInstance().getCurrentUser();
            if (currentUser != null) {
                SecurityLogger.logSecurityEvent(
                    "USER_MANAGEMENT_SEARCH",
                    "LOW",
                    currentUser,
                    "Searched users - Keyword: '" + (searchText.isEmpty() ? "(none)" : searchText) + 
                    "', Role: " + (roleFilterValue != null ? roleFilterValue : "All") + 
                    ", Status: " + (statusFilterValue != null ? statusFilterValue : "All")
                );
            }
        }
        
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
        
        // Log search results
        String currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser != null && (!searchText.isEmpty() || (roleFilterValue != null && !roleFilterValue.equals("All Roles")) || 
            (statusFilterValue != null && !statusFilterValue.equals("All Status")))) {
            SecurityLogger.logSecurityEvent(
                "USER_MANAGEMENT_SEARCH_RESULTS",
                "LOW",
                currentUser,
                "Search completed - Results: " + filteredUsers.size() + " users found"
            );
        }
    }

    @FXML
    private void onClearFilters() {
        // Log clear filters
        String currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser != null) {
            SecurityLogger.logSecurityEvent(
                "USER_MANAGEMENT_CLEAR_FILTERS",
                "LOW",
                currentUser,
                "Cleared all user filters"
            );
        }
        
        userSearchField.clear();
        roleFilter.setValue("All Roles");
        statusFilter.setValue("All Status");
        userTable.setItems(users);
    }

    @FXML
    private void onAddUser() {
        // Log add click
        String currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser != null) {
            SecurityLogger.logSecurityEvent(
                "USER_MANAGEMENT_ADD_CLICK",
                "MEDIUM",
                currentUser,
                "Clicked Add User button"
            );
        }
        
        Dialog<SystemUser> dialog = buildUserDialog(null);
        Optional<SystemUser> result = dialog.showAndWait();
        
        if (result.isPresent()) {
            SystemUser user = result.get();
            if (addUserToDatabase(user)) {
                // Log user creation event
                if (currentUser != null) {
                    SecurityLogger.logSecurityEvent(
                        "USER_MANAGEMENT_ADD_SUCCESS",
                        "MEDIUM",
                        currentUser,
                        "Added new user - Username: " + user.getUsername() + 
                        ", Full Name: " + user.getFullName() + 
                        ", Email: " + user.getEmail() + 
                        ", Role: " + user.getRole() + 
                        ", Status: " + user.getStatus()
                    );
                }
                
                loadUsersFromDatabase(); // Refresh the table
                showInfo("Success", "User added successfully: " + user.getFullName());
            } else {
                // Log failed add
                if (currentUser != null) {
                    SecurityLogger.logSecurityEvent(
                        "USER_MANAGEMENT_ADD_FAILED",
                        "MEDIUM",
                        currentUser,
                        "Failed to add user - Username: " + user.getUsername()
                    );
                }
            }
        } else {
            // Log add cancelled
            if (currentUser != null) {
                SecurityLogger.logSecurityEvent(
                    "USER_MANAGEMENT_ADD_CANCELLED",
                    "LOW",
                    currentUser,
                    "Cancelled adding user"
                );
            }
        }
    }

    @FXML
    private void onEditUser() {
        if (selectedUser == null) {
            showError("No User Selected", "Please select a user to edit.");
            
            // Log failed edit attempt
            String currentUser = SessionManager.getInstance().getCurrentUser();
            if (currentUser != null) {
                SecurityLogger.logSecurityEvent(
                    "USER_MANAGEMENT_EDIT_FAILED",
                    "LOW",
                    currentUser,
                    "Attempted to edit user but no user was selected"
                );
            }
            return;
        }
        
        // Log edit click
        String currentUser = SessionManager.getInstance().getCurrentUser();
        String oldUsername = selectedUser.getUsername();
        String oldFullName = selectedUser.getFullName();
        String oldEmail = selectedUser.getEmail();
        String oldRole = selectedUser.getRole();
        String oldStatus = selectedUser.getStatus();
        
        if (currentUser != null) {
            SecurityLogger.logSecurityEvent(
                "USER_MANAGEMENT_EDIT_CLICK",
                "MEDIUM",
                currentUser,
                "Clicked Edit User button - User ID: " + selectedUser.getId() + 
                ", Username: " + oldUsername
            );
        }
        
        Dialog<SystemUser> dialog = buildUserDialog(selectedUser);
        Optional<SystemUser> result = dialog.showAndWait();
        
        if (result.isPresent()) {
            SystemUser user = result.get();
            if (updateUserInDatabase(user)) {
                // Log user update event
                if (currentUser != null) {
                    SecurityLogger.logSecurityEvent(
                        "USER_MANAGEMENT_EDIT_SUCCESS",
                        "MEDIUM",
                        currentUser,
                        "Updated user - User ID: " + user.getId() + 
                        ", Old Username: " + oldUsername + 
                        ", New Username: " + user.getUsername() + 
                        ", Old Full Name: " + oldFullName + 
                        ", New Full Name: " + user.getFullName() + 
                        ", Old Email: " + oldEmail + 
                        ", New Email: " + user.getEmail() + 
                        ", Old Role: " + oldRole + 
                        ", New Role: " + user.getRole() + 
                        ", Old Status: " + oldStatus + 
                        ", New Status: " + user.getStatus()
                    );
                }
                
                loadUsersFromDatabase(); // Refresh the table
                showInfo("Success", "User updated successfully: " + user.getFullName());
            } else {
                // Log failed update
                if (currentUser != null) {
                    SecurityLogger.logSecurityEvent(
                        "USER_MANAGEMENT_EDIT_FAILED",
                        "MEDIUM",
                        currentUser,
                        "Failed to update user - User ID: " + user.getId()
                    );
                }
            }
        } else {
            // Log edit cancelled
            if (currentUser != null) {
                SecurityLogger.logSecurityEvent(
                    "USER_MANAGEMENT_EDIT_CANCELLED",
                    "LOW",
                    currentUser,
                    "Cancelled editing user - User ID: " + selectedUser.getId()
                );
            }
        }
    }

    @FXML
    private void onDeleteUser() {
        if (selectedUser == null) {
            showError("No User Selected", "Please select a user to delete.");
            
            // Log failed delete attempt
            String currentUser = SessionManager.getInstance().getCurrentUser();
            if (currentUser != null) {
                SecurityLogger.logSecurityEvent(
                    "USER_MANAGEMENT_DELETE_FAILED",
                    "LOW",
                    currentUser,
                    "Attempted to delete user but no user was selected"
                );
            }
            return;
        }

        // Log delete click
        String currentUser = SessionManager.getInstance().getCurrentUser();
        int userId = selectedUser.getId();
        String username = selectedUser.getUsername();
        String fullName = selectedUser.getFullName();
        String role = selectedUser.getRole();
        
        if (currentUser != null) {
            SecurityLogger.logSecurityEvent(
                "USER_MANAGEMENT_DELETE_CLICK",
                "HIGH",
                currentUser,
                "Clicked Delete User button - User ID: " + userId + 
                ", Username: " + username + 
                ", Full Name: " + fullName + 
                ", Role: " + role
            );
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete User");
        alert.setHeaderText("Are you sure you want to delete this user?");
        alert.setContentText("User: " + fullName + " (" + username + ")\nThis action cannot be undone.");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                if (deleteUserFromDatabase(userId)) {
                    // Log successful delete
                    if (currentUser != null) {
                        SecurityLogger.logSecurityEvent(
                            "USER_MANAGEMENT_DELETE_SUCCESS",
                            "HIGH",
                            currentUser,
                            "Deleted user - User ID: " + userId + 
                            ", Username: " + username + 
                            ", Full Name: " + fullName + 
                            ", Role: " + role
                        );
                    }
                    
                    loadUsersFromDatabase(); // Refresh the table
                    selectedUser = null;
                    showInfo("Success", "User deleted successfully: " + fullName);
                } else {
                    // Log failed delete
                    if (currentUser != null) {
                        SecurityLogger.logSecurityEvent(
                            "USER_MANAGEMENT_DELETE_FAILED",
                            "HIGH",
                            currentUser,
                            "Failed to delete user - User ID: " + userId
                        );
                    }
                }
            } else {
                // Log delete cancelled
                if (currentUser != null) {
                    SecurityLogger.logSecurityEvent(
                        "USER_MANAGEMENT_DELETE_CANCELLED",
                        "MEDIUM",
                        currentUser,
                        "Cancelled deleting user - User ID: " + userId
                    );
                }
            }
        });
    }

    @FXML
    private void onAssignRole() {
        if (selectedUser == null) {
            showError("No User Selected", "Please select a user to assign a role to.");
            
            // Log failed assign role attempt
            String currentUser = SessionManager.getInstance().getCurrentUser();
            if (currentUser != null) {
                SecurityLogger.logSecurityEvent(
                    "USER_MANAGEMENT_ASSIGN_ROLE_FAILED",
                    "LOW",
                    currentUser,
                    "Attempted to assign role but no user was selected"
                );
            }
            return;
        }

        // Log assign role click
        String currentUser = SessionManager.getInstance().getCurrentUser();
        int userId = selectedUser.getId();
        String username = selectedUser.getUsername();
        String oldRole = selectedUser.getRole();
        
        if (currentUser != null) {
            SecurityLogger.logSecurityEvent(
                "USER_MANAGEMENT_ASSIGN_ROLE_CLICK",
                "MEDIUM",
                currentUser,
                "Clicked Assign Role button - User ID: " + userId + 
                ", Username: " + username + 
                ", Current Role: " + oldRole
            );
        }

        ChoiceDialog<String> dialog = new ChoiceDialog<>(oldRole, roleOptions.subList(1, roleOptions.size()));
        dialog.setTitle("Assign Role");
        dialog.setHeaderText("Select a role for user: " + selectedUser.getFullName());
        dialog.setContentText("Choose role:");

        dialog.showAndWait().ifPresent(role -> {
            if (updateUserRoleInDatabase(userId, role)) {
                // Log successful role assignment
                if (currentUser != null) {
                    SecurityLogger.logSecurityEvent(
                        "USER_MANAGEMENT_ASSIGN_ROLE_SUCCESS",
                        "MEDIUM",
                        currentUser,
                        "Assigned role to user - User ID: " + userId + 
                        ", Username: " + username + 
                        ", Old Role: " + oldRole + 
                        ", New Role: " + role
                    );
                }
                
                loadUsersFromDatabase(); // Refresh the table
                showInfo("Success", "Role assigned successfully: " + role);
            } else {
                // Log failed role assignment
                if (currentUser != null) {
                    SecurityLogger.logSecurityEvent(
                        "USER_MANAGEMENT_ASSIGN_ROLE_FAILED",
                        "MEDIUM",
                        currentUser,
                        "Failed to assign role - User ID: " + userId + 
                        ", New Role: " + role
                    );
                }
            }
        });
    }

    @FXML
    private void onResetPassword() {
        if (selectedUser == null) {
            showError("No User Selected", "Please select a user to reset password for.");
            
            // Log failed reset password attempt
            String currentUser = SessionManager.getInstance().getCurrentUser();
            if (currentUser != null) {
                SecurityLogger.logSecurityEvent(
                    "USER_MANAGEMENT_RESET_PASSWORD_FAILED",
                    "LOW",
                    currentUser,
                    "Attempted to reset password but no user was selected"
                );
            }
            return;
        }

        // Log reset password click
        String currentUser = SessionManager.getInstance().getCurrentUser();
        int userId = selectedUser.getId();
        String username = selectedUser.getUsername();
        String fullName = selectedUser.getFullName();
        
        if (currentUser != null) {
            SecurityLogger.logSecurityEvent(
                "USER_MANAGEMENT_RESET_PASSWORD_CLICK",
                "HIGH",
                currentUser,
                "Clicked Reset Password button - User ID: " + userId + 
                ", Username: " + username + 
                ", Full Name: " + fullName
            );
        }

        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Reset Password");
        confirmAlert.setHeaderText("Reset password for user: " + fullName);
        confirmAlert.setContentText("This will generate a temporary password for the user.\nContinue?");

        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                // Generate temporary password
                String tempPassword = generateTempPassword();
                
                // Update password in database
                if (updateUserPasswordInDatabase(userId, tempPassword)) {
                    // Log password reset event
                    if (currentUser != null) {
                        SecurityLogger.logSecurityEvent(
                            "USER_MANAGEMENT_RESET_PASSWORD_SUCCESS",
                            "HIGH",
                            currentUser,
                            "Reset password for user - User ID: " + userId + 
                            ", Username: " + username + 
                            ", Full Name: " + fullName + 
                            " (Temporary password generated)"
                        );
                    }
                    
                    // Show custom dialog with copy button
                    showPasswordResetDialog(fullName, tempPassword);
                    loadUsersFromDatabase(); // Refresh table
                    logger.info("Password reset for user: " + username + " - New password: " + tempPassword);
                } else {
                    // Log failed password reset
                    if (currentUser != null) {
                        SecurityLogger.logSecurityEvent(
                            "USER_MANAGEMENT_RESET_PASSWORD_FAILED",
                            "HIGH",
                            currentUser,
                            "Failed to reset password - User ID: " + userId
                        );
                    }
                    showError("Reset Failed", "Failed to reset password. Please try again.");
                }
            } else {
                // Log reset password cancelled
                if (currentUser != null) {
                    SecurityLogger.logSecurityEvent(
                        "USER_MANAGEMENT_RESET_PASSWORD_CANCELLED",
                        "MEDIUM",
                        currentUser,
                        "Cancelled resetting password - User ID: " + userId
                    );
                }
            }
        });
    }

    @FXML
    private void onToggleStatus() {
        if (selectedUser == null) {
            showError("No User Selected", "Please select a user to toggle status for.");
            
            // Log failed toggle status attempt
            String currentUser = SessionManager.getInstance().getCurrentUser();
            if (currentUser != null) {
                SecurityLogger.logSecurityEvent(
                    "USER_MANAGEMENT_TOGGLE_STATUS_FAILED",
                    "LOW",
                    currentUser,
                    "Attempted to toggle user status but no user was selected"
                );
            }
            return;
        }

        String currentStatus = selectedUser.getStatus();
        String newStatus = currentStatus.equals("Active") ? "Inactive" : "Active";
        
        // Log toggle status click
        String currentUser = SessionManager.getInstance().getCurrentUser();
        int userId = selectedUser.getId();
        String username = selectedUser.getUsername();
        String fullName = selectedUser.getFullName();
        
        if (currentUser != null) {
            SecurityLogger.logSecurityEvent(
                "USER_MANAGEMENT_TOGGLE_STATUS_CLICK",
                "MEDIUM",
                currentUser,
                "Clicked Toggle Status button - User ID: " + userId + 
                ", Username: " + username + 
                ", Current Status: " + currentStatus + 
                ", New Status: " + newStatus
            );
        }
        
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Toggle User Status");
        alert.setHeaderText("Change user status from " + currentStatus + " to " + newStatus + "?");
        alert.setContentText("User: " + fullName);

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                if (updateUserStatusInDatabase(userId, newStatus)) {
                    // Log successful status toggle
                    if (currentUser != null) {
                        SecurityLogger.logSecurityEvent(
                            "USER_MANAGEMENT_TOGGLE_STATUS_SUCCESS",
                            "MEDIUM",
                            currentUser,
                            "Toggled user status - User ID: " + userId + 
                            ", Username: " + username + 
                            ", Old Status: " + currentStatus + 
                            ", New Status: " + newStatus
                        );
                    }
                    
                    loadUsersFromDatabase(); // Refresh the table
                    showInfo("Success", "User status changed to " + newStatus);
                } else {
                    // Log failed status toggle
                    if (currentUser != null) {
                        SecurityLogger.logSecurityEvent(
                            "USER_MANAGEMENT_TOGGLE_STATUS_FAILED",
                            "MEDIUM",
                            currentUser,
                            "Failed to toggle user status - User ID: " + userId
                        );
                    }
                }
            } else {
                // Log toggle status cancelled
                if (currentUser != null) {
                    SecurityLogger.logSecurityEvent(
                        "USER_MANAGEMENT_TOGGLE_STATUS_CANCELLED",
                        "LOW",
                        currentUser,
                        "Cancelled toggling user status - User ID: " + userId
                    );
                }
            }
        });
    }

    @FXML
    private void onRefresh() {
        // Log refresh click
        String currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser != null) {
            SecurityLogger.logSecurityEvent(
                "USER_MANAGEMENT_REFRESH",
                "LOW",
                currentUser,
                "Refreshed user data from database"
            );
        }
        
        loadDataFromDatabase();
        showInfo("Refreshed", "User data refreshed from database!");
    }

    @FXML
    private void onExportUsers() {
        // Log export click
        String currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser != null) {
            SecurityLogger.logSecurityEvent(
                "USER_MANAGEMENT_EXPORT_CLICK",
                "MEDIUM",
                currentUser,
                "Clicked Export Users button"
            );
        }
        
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
                    
                    // Log successful export
                    if (currentUser != null) {
                        SecurityLogger.logSecurityEvent(
                            "USER_MANAGEMENT_EXPORT_SUCCESS",
                            "MEDIUM",
                            currentUser,
                            "Exported users to file - Filename: " + file.getName() + 
                            ", Records: " + users.size()
                        );
                    }
                    
                    showInfo("Export Success", "Users exported successfully to: " + file.getName());
                }
            } else {
                // Log export cancelled
                if (currentUser != null) {
                    SecurityLogger.logSecurityEvent(
                        "USER_MANAGEMENT_EXPORT_CANCELLED",
                        "LOW",
                        currentUser,
                        "Cancelled exporting users"
                    );
                }
            }
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error exporting users", e);
            
            // Log export error
            if (currentUser != null) {
                SecurityLogger.logSecurityEvent(
                    "USER_MANAGEMENT_EXPORT_FAILED",
                    "MEDIUM",
                    currentUser,
                    "Failed to export users - Error: " + e.getMessage()
                );
            }
            
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
    
    private boolean updateUserPasswordInDatabase(int userId, String newPassword) {
        try {
            String hashedPassword = hashPassword(newPassword);
            String query = "UPDATE users SET password_hash = ? WHERE user_id = ?";
            PreparedStatement stmt = connection.prepareStatement(query);
            stmt.setString(1, hashedPassword);
            stmt.setInt(2, userId);
            
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error updating user password in database", e);
            showError("Database Error", "Failed to update user password: " + e.getMessage());
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
        
        // Remove default header
        dialog.setHeaderText(null);
        
        // Custom button types with styled appearance
        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        // Create main container with green gradient background
        javafx.scene.layout.VBox mainContainer = new javafx.scene.layout.VBox();
        mainContainer.setStyle("-fx-background-color: linear-gradient(to bottom right, #2e7d32, #66bb6a); -fx-padding: 0;");
        
        // Create content container with white background
        javafx.scene.layout.VBox contentContainer = new javafx.scene.layout.VBox();
        contentContainer.setStyle("-fx-background-color: white;");
        contentContainer.setPadding(new javafx.geometry.Insets(40, 40, 40, 40));
        contentContainer.setSpacing(20);
        
        // Title Label
        Label titleLabel = new Label(user == null ? "Add New User" : "Edit User");
        titleLabel.setStyle(
            "-fx-text-fill: #1b5e20; " +
            "-fx-font-size: 24px; " +
            "-fx-font-weight: bold; " +
            "-fx-alignment: center;"
        );
        
        // Subtitle Label
        // Label subtitleLabel = new Label(user == null ? "Create a new system user account" : "Update user information");
        // subtitleLabel.setStyle(
        //     "-fx-text-fill: #666666; " +
        //     "-fx-font-size: 14px; " +
        //     "-fx-alignment: center;"
        // );
        
        // Form fields container
        javafx.scene.layout.VBox formContainer = new javafx.scene.layout.VBox();
        formContainer.setSpacing(18);
        
        // Styled input fields
        String fieldStyle = 
            "-fx-background-radius: 8; " +
            "-fx-border-radius: 8; " +
            "-fx-border-color: #c8e6c9; " +
            "-fx-border-width: 1; " +
            "-fx-focus-color: #66bb6a; " +
            "-fx-faint-focus-color: transparent; " +
            "-fx-padding: 6 10 6 10; " +
            "-fx-font-size: 14px; " +
            "-fx-pref-width: 350;";
            
        String labelStyle = 
            "-fx-text-fill: #1b5e20; " +
            "-fx-font-size: 14px; " +
            "-fx-font-weight: bold;";

        // Username field
        javafx.scene.layout.VBox usernameContainer = new javafx.scene.layout.VBox(8);
        Label usernameLabel = new Label("Username");
        usernameLabel.setStyle(labelStyle);
        TextField usernameField = new TextField();
        usernameField.setPromptText("Enter username");
        usernameField.setStyle(fieldStyle);
        usernameContainer.getChildren().addAll(usernameLabel, usernameField);
        
        // Full Name field
        javafx.scene.layout.VBox fullNameContainer = new javafx.scene.layout.VBox(8);
        Label fullNameLabel = new Label("Full Name");
        fullNameLabel.setStyle(labelStyle);
        TextField fullNameField = new TextField();
        fullNameField.setPromptText("Enter full name");
        fullNameField.setStyle(fieldStyle);
        fullNameContainer.getChildren().addAll(fullNameLabel, fullNameField);
        
        // Email field
        javafx.scene.layout.VBox emailContainer = new javafx.scene.layout.VBox(8);
        Label emailLabel = new Label("Email Address");
        emailLabel.setStyle(labelStyle);
        TextField emailField = new TextField();
        emailField.setPromptText("Enter email address");
        emailField.setStyle(fieldStyle);
        emailContainer.getChildren().addAll(emailLabel, emailField);
        
        // Password field (only for new users)
        javafx.scene.layout.VBox passwordContainer = new javafx.scene.layout.VBox(8);
        Label passwordLabel = new Label("Password");
        passwordLabel.setStyle(labelStyle);
        TextField passwordField = new TextField();
        passwordField.setPromptText(user == null ? "Enter password" : "Leave blank to keep current password");
        passwordField.setStyle(fieldStyle);
        passwordContainer.getChildren().addAll(passwordLabel, passwordField);
        
        // Role and Status in horizontal layout
        javafx.scene.layout.HBox roleStatusContainer = new javafx.scene.layout.HBox(20);
        
        // Role field
        javafx.scene.layout.VBox roleContainer = new javafx.scene.layout.VBox(8);
        Label roleLabel = new Label("Role");
        roleLabel.setStyle(labelStyle);
        ComboBox<String> roleCombo = new ComboBox<>();
        roleCombo.getItems().addAll("Admin", "Payroll Maker", "Staff");
        roleCombo.setValue("Staff");
        roleCombo.setStyle(fieldStyle + "-fx-pref-width: 165;");
        roleContainer.getChildren().addAll(roleLabel, roleCombo);
        
        // Status field
        javafx.scene.layout.VBox statusContainer = new javafx.scene.layout.VBox(8);
        Label statusLabel = new Label("Status");
        statusLabel.setStyle(labelStyle);
        ComboBox<String> statusCombo = new ComboBox<>();
        statusCombo.getItems().addAll("Active", "Inactive", "Suspended");
        statusCombo.setValue("Active");
        statusCombo.setStyle(fieldStyle + "-fx-pref-width: 165;");
        statusContainer.getChildren().addAll(statusLabel, statusCombo);
        
        roleStatusContainer.getChildren().addAll(roleContainer, statusContainer);

        // Populate fields if editing user
        if (user != null) {
            usernameField.setText(user.getUsername());
            fullNameField.setText(user.getFullName());
            emailField.setText(user.getEmail());
            passwordField.setPromptText("Leave blank to keep current password");
            roleCombo.setValue(user.getRole());
            statusCombo.setValue(user.getStatus());
        }

        // Add all form elements to form container
        formContainer.getChildren().addAll(
            usernameContainer,
            fullNameContainer, 
            emailContainer,
            passwordContainer,
            roleStatusContainer
        );
        
        // Add all elements to content container
        contentContainer.getChildren().addAll(titleLabel, formContainer);
        
        // Add content to main container with minimal padding
        mainContainer.getChildren().add(contentContainer);
        mainContainer.setPadding(new javafx.geometry.Insets(0));
        
        dialog.getDialogPane().setContent(mainContainer);
        
        // Style the buttons
        dialog.getDialogPane().lookupButton(saveButtonType).setStyle(
            "-fx-background-color: linear-gradient(to right, #2e7d32, #43a047); " +
            "-fx-text-fill: white; " +
            "-fx-font-weight: bold; " +
            "-fx-background-radius: 8; " +
            "-fx-padding: 10 20 10 20; " +
            "-fx-font-size: 14px;"
        );
        
        dialog.getDialogPane().lookupButton(ButtonType.CANCEL).setStyle(
            "-fx-background-color: #f5f5f5; " +
            "-fx-text-fill: #666; " +
            "-fx-background-radius: 8; " +
            "-fx-padding: 10 20 10 20; " +
            "-fx-font-size: 14px;" +
            "-fx-border-color: #ddd; " +
            "-fx-border-radius: 8;"
        );
        
        // Set dialog size to fit content better
        dialog.getDialogPane().setPrefWidth(450);
        dialog.getDialogPane().setPrefHeight(550);

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
                String password = (user == null || !passwordField.getText().trim().isEmpty()) ? 
                    passwordField.getText().trim() : "******";
                return new SystemUser(
                    user != null ? user.getId() : users.size() + 1,
                    usernameField.getText().trim(),
                    fullNameField.getText().trim(),
                    emailField.getText().trim(),
                    password,
                    roleCombo.getValue(),
                    statusCombo.getValue(),
                    user != null ? user.getLastLogin() : LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
                    user != null ? user.getCreatedDate() : LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
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
            // SecurityLogger manages its own connections (auto-closed)
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error closing database connection", e);
        }
    }
    
    private String generateTempPassword() {
        // Generate a secure temporary password with mixed case, numbers and symbols
        String upperCase = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String lowerCase = "abcdefghijklmnopqrstuvwxyz";
        String numbers = "0123456789";
        String symbols = "@#$%&*";
        String allChars = upperCase + lowerCase + numbers + symbols;
        
        StringBuilder tempPassword = new StringBuilder();
        java.util.Random random = new java.util.Random();
        
        // Ensure at least one character from each category
        tempPassword.append(upperCase.charAt(random.nextInt(upperCase.length())));
        tempPassword.append(lowerCase.charAt(random.nextInt(lowerCase.length())));
        tempPassword.append(numbers.charAt(random.nextInt(numbers.length())));
        tempPassword.append(symbols.charAt(random.nextInt(symbols.length())));
        
        // Fill the rest to make it 8 characters total
        for (int i = 4; i < 8; i++) {
            tempPassword.append(allChars.charAt(random.nextInt(allChars.length())));
        }
        
        // Shuffle the password to randomize positions
        String password = tempPassword.toString();
        char[] passwordArray = password.toCharArray();
        for (int i = passwordArray.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            char temp = passwordArray[i];
            passwordArray[i] = passwordArray[j];
            passwordArray[j] = temp;
        }
        
        return new String(passwordArray);
    }
    
    private void showPasswordResetDialog(String fullName, String tempPassword) {
        // Create custom dialog
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Password Reset Successful");
        dialog.setHeaderText(null);
        
        // Create main container with green theme
        javafx.scene.layout.VBox mainContainer = new javafx.scene.layout.VBox();
        mainContainer.setStyle("-fx-background-color: linear-gradient(to bottom right, #2e7d32, #66bb6a);");
        
        // Create content container
        javafx.scene.layout.VBox contentContainer = new javafx.scene.layout.VBox();
        contentContainer.setStyle("-fx-background-color: white;");
        contentContainer.setPadding(new javafx.geometry.Insets(30, 40, 30, 40));
        contentContainer.setSpacing(20);
        
        // Title
        Label titleLabel = new Label("🔑 Password Reset Successful");
        titleLabel.setStyle(
            "-fx-text-fill: #1b5e20; " +
            "-fx-font-size: 22px; " +
            "-fx-font-weight: bold; " +
            "-fx-alignment: center;"
        );
        
        // User info
        Label userLabel = new Label("Temporary password generated for: " + fullName);
        userLabel.setStyle(
            "-fx-text-fill: #666; " +
            "-fx-font-size: 14px; " +
            "-fx-alignment: center;"
        );
        
        // Password container with copy functionality
        javafx.scene.layout.VBox passwordContainer = new javafx.scene.layout.VBox(10);
        
        Label passwordLabel = new Label("Temporary Password:");
        passwordLabel.setStyle(
            "-fx-text-fill: #1b5e20; " +
            "-fx-font-size: 14px; " +
            "-fx-font-weight: bold;"
        );
        
        // Password field and copy button container
        javafx.scene.layout.HBox passwordFieldContainer = new javafx.scene.layout.HBox(10);
        passwordFieldContainer.setAlignment(javafx.geometry.Pos.CENTER);
        
        TextField passwordField = new TextField(tempPassword);
        passwordField.setEditable(false);
        passwordField.setStyle(
            "-fx-background-color: #f8f9fa; " +
            "-fx-border-color: #28a745; " +
            "-fx-border-width: 2; " +
            "-fx-border-radius: 8; " +
            "-fx-background-radius: 8; " +
            "-fx-padding: 10; " +
            "-fx-font-size: 16px; " +
            "-fx-font-weight: bold; " +
            "-fx-text-fill: #1b5e20; " +
            "-fx-pref-width: 200;"
        );
        
        Button copyButton = new Button("📋 Copy");
        copyButton.setStyle(
            "-fx-background-color: #28a745; " +
            "-fx-text-fill: white; " +
            "-fx-font-weight: bold; " +
            "-fx-background-radius: 8; " +
            "-fx-padding: 10 15 10 15; " +
            "-fx-font-size: 14px; " +
            "-fx-cursor: hand;"
        );
        
        // Copy functionality
        copyButton.setOnAction(e -> {
            javafx.scene.input.Clipboard clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
            javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
            content.putString(tempPassword);
            clipboard.setContent(content);
            
            // Visual feedback
            copyButton.setText("✅ Copied!");
            copyButton.setStyle(
                "-fx-background-color: #218838; " +
                "-fx-text-fill: white; " +
                "-fx-font-weight: bold; " +
                "-fx-background-radius: 8; " +
                "-fx-padding: 10 15 10 15; " +
                "-fx-font-size: 14px;"
            );
            
            // Reset button after 2 seconds
            javafx.animation.Timeline timeline = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(javafx.util.Duration.seconds(2), evt -> {
                    copyButton.setText("📋 Copy");
                    copyButton.setStyle(
                        "-fx-background-color: #28a745; " +
                        "-fx-text-fill: white; " +
                        "-fx-font-weight: bold; " +
                        "-fx-background-radius: 8; " +
                        "-fx-padding: 10 15 10 15; " +
                        "-fx-font-size: 14px; " +
                        "-fx-cursor: hand;"
                    );
                })
            );
            timeline.play();
        });
        
        passwordFieldContainer.getChildren().addAll(passwordField, copyButton);
        passwordContainer.getChildren().addAll(passwordLabel, passwordFieldContainer);
        
        // Security note
        Label securityNote = new Label("⚠️ Please share this password securely with the user.\nThey should change it upon first login.");
        securityNote.setStyle(
            "-fx-text-fill: #856404; " +
            "-fx-font-size: 12px; " +
            "-fx-alignment: center; " +
            "-fx-text-alignment: center;"
        );
        
        // Add all elements to content container
        contentContainer.getChildren().addAll(titleLabel, userLabel, passwordContainer, securityNote);
        
        // Add content to main container
        mainContainer.getChildren().add(contentContainer);
        
        dialog.getDialogPane().setContent(mainContainer);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        
        // Style close button
        Button closeBtn = (Button) dialog.getDialogPane().lookupButton(ButtonType.CLOSE);
        closeBtn.setText("Close");
        closeBtn.setStyle(
            "-fx-background-color: #6c757d; " +
            "-fx-text-fill: white; " +
            "-fx-background-radius: 8; " +
            "-fx-padding: 10 20 10 20; " +
            "-fx-font-size: 14px;"
        );
        
        // Set dialog size
        dialog.getDialogPane().setPrefWidth(450);
        dialog.getDialogPane().setPrefHeight(350);
        
        dialog.showAndWait();
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
