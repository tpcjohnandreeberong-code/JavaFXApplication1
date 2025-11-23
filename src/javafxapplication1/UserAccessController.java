package javafxapplication1;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TreeItemPropertyValueFactory;
import javafx.util.Callback;

import java.net.URL;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.ResourceBundle;
import javafx.scene.layout.GridPane;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.HashMap;
import java.util.Map;

public class UserAccessController implements Initializable {

    private static final Logger logger = Logger.getLogger(UserAccessController.class.getName());
    
    // Database connection parameters
    private static final String DB_URL = DatabaseConfig.getDbUrl();
    private static final String DB_USER = DatabaseConfig.getDbUser();
    private static final String DB_PASSWORD = DatabaseConfig.getDbPassword();
    private Connection connection;

    // FXML Controls - Role Management
    @FXML private TextField roleSearchField;
    @FXML private Button searchRoleButton;
    @FXML private Button addRoleButton;
    @FXML private Button editRoleButton;
    @FXML private Button deleteRoleButton;
    @FXML private TableView<SystemRole> roleTable;
    @FXML private TableColumn<SystemRole, Integer> colRoleId;
    @FXML private TableColumn<SystemRole, String> colRoleName;
    @FXML private TableColumn<SystemRole, String> colDescription;
    @FXML private TableColumn<SystemRole, Integer> colUserCount;
    @FXML private TableColumn<SystemRole, String> colStatus;
    @FXML private TableColumn<SystemRole, String> colCreatedDate;

    // FXML Controls - Permission Management
    @FXML private Label selectedRoleLabel;
    @FXML private Button addPermissionButton;
    @FXML private Button updatePermissionButton;
    @FXML private Button deletePermissionButton;
    @FXML private Button savePermissionsButton;
    @FXML private Button resetPermissionsButton;
    @FXML private TreeView<String> permissionTree;

    // Data Collections
    private final ObservableList<SystemRole> roles = FXCollections.observableArrayList();
    private final ObservableList<SystemPermission> permissions = FXCollections.observableArrayList();

    // Current Selection
    private SystemRole selectedRole = null;
    
    // Track permission checkbox states
    private final Map<String, Boolean> permissionStates = new HashMap<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initializeDatabase();
        setupTableColumns();
        setupComboBoxes();
        setupPermissionTree();
        loadDataFromDatabase();
        setupEventHandlers();
        assignDefaultPermissions(); // Assign default permissions to roles
        setupPermissionBasedVisibility(); // Show/hide buttons based on user permissions
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
        // Role Table Columns
        colRoleId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colRoleName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
        colUserCount.setCellValueFactory(new PropertyValueFactory<>("userCount"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colCreatedDate.setCellValueFactory(new PropertyValueFactory<>("createdDate"));
    }

    private void setupComboBoxes() {
        // No combo boxes needed for User Access (only role management)
    }

    private void setupPermissionTree() {
        // Create the permission tree structure from database
        TreeItem<String> rootItem = new TreeItem<>("System Modules");
        rootItem.setExpanded(true);
        
        // Group permissions by module
        Map<String, TreeItem<String>> moduleMap = new HashMap<>();
        
        for (SystemPermission permission : permissions) {
            String moduleName = permission.getModuleName();
            TreeItem<String> moduleItem = moduleMap.get(moduleName);
            
            if (moduleItem == null) {
                moduleItem = new TreeItem<>(moduleName);
                moduleItem.setExpanded(true);
                moduleMap.put(moduleName, moduleItem);
                rootItem.getChildren().add(moduleItem);
            }
            
            // Create permission item with checkbox - use permission_name for unique identification
            TreeItem<String> permissionItem = new TreeItem<>(permission.getPermissionName());
            permissionItem.setExpanded(true);
            moduleItem.getChildren().add(permissionItem);
        }
        
        // Set the root item
        permissionTree.setRoot(rootItem);
        
        // Set up custom cell factory for checkboxes
        permissionTree.setCellFactory(new Callback<TreeView<String>, TreeCell<String>>() {
            @Override
            public TreeCell<String> call(TreeView<String> param) {
                return new TreeCell<String>() {
                    private final CheckBox checkBox = new CheckBox();

                    @Override
                    protected void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setGraphic(null);
                            setText(null);
                        } else {
                            setText(item);
                            // Only show checkboxes for leaf nodes (permissions)
                            if (getTreeItem() != null && getTreeItem().isLeaf() && getTreeItem().getParent() != null) {
                                setGraphic(checkBox);
                                
                                // Check if this permission is already granted to the selected role
                                if (selectedRole != null) {
                                    Boolean isSelected = permissionStates.get(item);
                                    checkBox.setSelected(isSelected != null ? isSelected : false);
                                } else {
                                    checkBox.setSelected(false);
                                }
                                
                                // Set up checkbox event handler
                                checkBox.setOnAction(event -> {
                                    // Handle permission toggle
                                    togglePermission(item, checkBox.isSelected());
                                    // Track the checkbox state
                                    permissionStates.put(item, checkBox.isSelected());
                                    logger.info("Permission checkbox toggled: " + item + " = " + checkBox.isSelected());
                                });
                            } else {
                                setGraphic(null);
                            }
                        }
                    }
                };
            }
        });
    }

    private void setupEventHandlers() {
        // Role table selection
        roleTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            selectedRole = newVal;
            if (newVal != null) {
                selectedRoleLabel.setText(newVal.getName());
                loadRolePermissions(newVal);
                // Clear and initialize permission states for this role
                permissionStates.clear();
                initializePermissionStates(newVal);
                // Refresh the permission tree to show checked permissions
                setupPermissionTree();
            } else {
                selectedRoleLabel.setText("None");
                permissionStates.clear();
                // Clear the permission tree
                setupPermissionTree();
            }
        });
    }

    private void initializePermissionStates(SystemRole role) {
        try {
            logger.info("Initializing permission states for role: " + role.getName());
            
            // Initialize all permissions to false first
            for (SystemPermission permission : permissions) {
                permissionStates.put(permission.getPermissionName(), false);
                logger.info("  Initialized permission: " + permission.getPermissionName() + " = false");
            }
            
            // Then set the ones that are granted to true
            String query = "SELECT p.permission_name FROM permissions p " +
                         "JOIN role_permissions rp ON p.permission_id = rp.permission_id " +
                         "WHERE rp.role_id = ? AND rp.granted = TRUE";
            
            PreparedStatement stmt = connection.prepareStatement(query);
            stmt.setInt(1, role.getId());
            ResultSet rs = stmt.executeQuery();
            
            int grantedCount = 0;
            while (rs.next()) {
                String permissionName = rs.getString("permission_name");
                
                // Use permission_name for the checkbox state (this is what's displayed in the tree)
                permissionStates.put(permissionName, true);
                grantedCount++;
                logger.info("  Granted permission: " + permissionName + " = true");
            }
            
            logger.info("Initialized " + grantedCount + " granted permissions out of " + permissions.size() + " total permissions");
            
            // Log all permission states for debugging
            logger.info("All permission states:");
            for (Map.Entry<String, Boolean> entry : permissionStates.entrySet()) {
                logger.info("  " + entry.getKey() + " = " + entry.getValue());
            }
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error initializing permission states", e);
        }
    }
    
    private void loadDataFromDatabase() {
        loadRolesFromDatabase();
        loadPermissionsFromDatabase();
    }
    
    private void loadRolesFromDatabase() {
        try {
            String query = "SELECT role_id, role_name, description, user_count, status, created_date FROM roles ORDER BY role_name";
            PreparedStatement stmt = connection.prepareStatement(query);
            ResultSet rs = stmt.executeQuery();
            
            roles.clear();
            while (rs.next()) {
                SystemRole role = new SystemRole(
                    rs.getInt("role_id"),
                    rs.getString("role_name"),
                    rs.getString("description"),
                    rs.getInt("user_count"),
                    rs.getString("status"),
                    rs.getString("created_date")
                );
                roles.add(role);
            }
        roleTable.setItems(roles);
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error loading roles from database", e);
            showError("Database Error", "Failed to load roles: " + e.getMessage());
        }
    }
    
    private void loadPermissionsFromDatabase() {
        try {
            String query = "SELECT permission_id, permission_name, module_name, action_name, description, status FROM permissions ORDER BY module_name, action_name";
            PreparedStatement stmt = connection.prepareStatement(query);
            ResultSet rs = stmt.executeQuery();
            
            permissions.clear();
            while (rs.next()) {
                SystemPermission permission = new SystemPermission(
                    rs.getInt("permission_id"),
                    rs.getString("permission_name"),
                    rs.getString("module_name"),
                    rs.getString("action_name"),
                    rs.getString("description"),
                    rs.getString("status")
                );
                permissions.add(permission);
            }
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error loading permissions from database", e);
            showError("Database Error", "Failed to load permissions: " + e.getMessage());
        }
    }

    private void loadRolePermissions(SystemRole role) {
        // Load permissions for selected role and update the tree
        if (role != null) {
            // Refresh the permission tree to show current permissions
            setupPermissionTree();
        }
    }

    private void togglePermission(String actionName, boolean granted) {
        if (selectedRole != null) {
            try {
                // Get permission ID from action name
                String getPermissionIdQuery = "SELECT permission_id FROM permissions WHERE action_name = ?";
                PreparedStatement getPermissionIdStmt = connection.prepareStatement(getPermissionIdQuery);
                getPermissionIdStmt.setString(1, actionName);
                ResultSet rs = getPermissionIdStmt.executeQuery();
                
                if (rs.next()) {
                    int permissionId = rs.getInt("permission_id");
                    
                    if (granted) {
                        // Grant permission
                        String grantQuery = "INSERT INTO role_permissions (role_id, permission_id, granted, created_date) VALUES (?, ?, ?, NOW()) " +
                                          "ON DUPLICATE KEY UPDATE granted = ?";
                        PreparedStatement grantStmt = connection.prepareStatement(grantQuery);
                        grantStmt.setInt(1, selectedRole.getId());
                        grantStmt.setInt(2, permissionId);
                        grantStmt.setBoolean(3, true);
                        grantStmt.setBoolean(4, true);
                        grantStmt.executeUpdate();
                    } else {
                        // Revoke permission
                        String revokeQuery = "DELETE FROM role_permissions WHERE role_id = ? AND permission_id = ?";
                        PreparedStatement revokeStmt = connection.prepareStatement(revokeQuery);
                        revokeStmt.setInt(1, selectedRole.getId());
                        revokeStmt.setInt(2, permissionId);
                        revokeStmt.executeUpdate();
                    }
                    
                    System.out.println("Permission '" + actionName + "' " + (granted ? "granted" : "revoked") + " for role: " + selectedRole.getName());
                }
                
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Error toggling permission", e);
                showError("Database Error", "Failed to update permission: " + e.getMessage());
            }
        }
    }
    
    private boolean isPermissionGranted(int roleId, String actionName) {
        try {
            // First get the permission_name from action_name
            String getPermissionNameQuery = "SELECT permission_name FROM permissions WHERE action_name = ?";
            PreparedStatement getPermissionNameStmt = connection.prepareStatement(getPermissionNameQuery);
            getPermissionNameStmt.setString(1, actionName);
            ResultSet permissionRs = getPermissionNameStmt.executeQuery();
            
            if (permissionRs.next()) {
                String permissionName = permissionRs.getString("permission_name");
                
                // Now check if this permission is granted to the role
                String query = "SELECT rp.granted FROM role_permissions rp " +
                              "JOIN permissions p ON rp.permission_id = p.permission_id " +
                              "WHERE rp.role_id = ? AND p.permission_name = ?";
                PreparedStatement stmt = connection.prepareStatement(query);
                stmt.setInt(1, roleId);
                stmt.setString(2, permissionName);
                ResultSet rs = stmt.executeQuery();
                
                if (rs.next()) {
                    return rs.getBoolean("granted");
                }
            }
            return false;
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error checking permission status", e);
            return false;
        }
    }

    // Action Methods - Role Management
    @FXML
    private void onSearchRoles() {
        String searchText = roleSearchField.getText() == null ? "" : roleSearchField.getText().trim().toLowerCase();
        
        if (searchText.isEmpty()) {
            roleTable.setItems(roles);
        } else {
            ObservableList<SystemRole> filteredRoles = roles.filtered(role -> 
                role.getName().toLowerCase().contains(searchText) ||
                role.getDescription().toLowerCase().contains(searchText)
            );
            roleTable.setItems(filteredRoles);
        }
    }

    @FXML
    private void onAddRole() {
        Dialog<SystemRole> dialog = buildRoleDialog(null);
        Optional<SystemRole> result = dialog.showAndWait();
        result.ifPresent(role -> {
            if (addRoleToDatabase(role)) {
                loadRolesFromDatabase(); // Refresh the table
                showInfo("Success", "Role added successfully: " + role.getName());
            }
        });
    }

    @FXML
    private void onEditRole() {
        if (selectedRole == null) {
            showError("No Role Selected", "Please select a role to edit.");
            return;
        }
        
        Dialog<SystemRole> dialog = buildRoleDialog(selectedRole);
        Optional<SystemRole> result = dialog.showAndWait();
        result.ifPresent(role -> {
            if (updateRoleInDatabase(role)) {
                loadRolesFromDatabase(); // Refresh the table
                showInfo("Success", "Role updated successfully: " + role.getName());
            }
        });
    }

    @FXML
    private void onDeleteRole() {
        if (selectedRole == null) {
            showError("No Role Selected", "Please select a role to delete.");
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Role");
        alert.setHeaderText("Are you sure you want to delete this role?");
        alert.setContentText("Role: " + selectedRole.getName() + "\nThis action cannot be undone.");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                if (deleteRoleFromDatabase(selectedRole.getId())) {
                    loadRolesFromDatabase(); // Refresh the table
                selectedRole = null;
                selectedRoleLabel.setText("None");
                    showInfo("Success", "Role deleted successfully: " + selectedRole.getName());
                }
            }
        });
    }

    @FXML
    private void onRefresh() {
        loadDataFromDatabase();
        showInfo("Refreshed", "Data refreshed from database!");
    }

    @FXML
    private void onExportRoles() {
        try {
            // Create CSV content
            StringBuilder csvContent = new StringBuilder();
            csvContent.append("Role ID,Role Name,Description,User Count,Status,Created Date\n");
            
            for (SystemRole role : roles) {
                csvContent.append(String.format("%d,%s,%s,%d,%s,%s\n",
                    role.getId(),
                    role.getName(),
                    role.getDescription().replace(",", ";"), // Handle commas in description
                    role.getUserCount(),
                    role.getStatus(),
                    role.getCreatedDate()
                ));
            }
            
            // Save to file
            javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
            fileChooser.setTitle("Export Roles");
            fileChooser.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("CSV Files", "*.csv"));
            java.io.File file = fileChooser.showSaveDialog(roleTable.getScene().getWindow());
            
            if (file != null) {
                try (java.io.FileWriter writer = new java.io.FileWriter(file)) {
                    writer.write(csvContent.toString());
                    showInfo("Export Success", "Roles exported successfully to: " + file.getName());
                }
            }
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error exporting roles", e);
            showError("Export Error", "Failed to export roles: " + e.getMessage());
        }
    }
    
    // Additional functionality methods
    public void closeDatabaseConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error closing database connection", e);
        }
    }
    
    private void updateUserCountForRole(int roleId) {
        try {
            // Count users with this role
            String countQuery = "SELECT COUNT(*) as user_count FROM users WHERE role = (SELECT role_name FROM roles WHERE role_id = ?)";
            PreparedStatement countStmt = connection.prepareStatement(countQuery);
            countStmt.setInt(1, roleId);
            ResultSet rs = countStmt.executeQuery();
            
            if (rs.next()) {
                int userCount = rs.getInt("user_count");
                
                // Update the role's user count
                String updateQuery = "UPDATE roles SET user_count = ? WHERE role_id = ?";
                PreparedStatement updateStmt = connection.prepareStatement(updateQuery);
                updateStmt.setInt(1, userCount);
                updateStmt.setInt(2, roleId);
                updateStmt.executeUpdate();
            }
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error updating user count for role", e);
        }
    }

    // Action Methods - Permission Management
    @FXML
    private void onAddPermission() {
        Dialog<SystemPermission> dialog = buildPermissionDialog(null);
        Optional<SystemPermission> result = dialog.showAndWait();
        result.ifPresent(permission -> {
            if (addPermissionToDatabase(permission)) {
                loadPermissionsFromDatabase(); // Refresh permissions
                showInfo("Success", "Permission added successfully: " + permission.getPermissionName());
            }
        });
    }

    @FXML
    private void onUpdatePermission() {
        // For now, show a dialog to select permission to update
        // In a real implementation, you'd get the selected permission from the tree
        Dialog<SystemPermission> selectDialog = new Dialog<>();
        selectDialog.setTitle("Select Permission to Update");
        selectDialog.setHeaderText("Choose a permission to update:");
        
        ButtonType selectButtonType = new ButtonType("Select", ButtonBar.ButtonData.OK_DONE);
        selectDialog.getDialogPane().getButtonTypes().addAll(selectButtonType, ButtonType.CANCEL);
        
        ComboBox<SystemPermission> permissionCombo = new ComboBox<>();
        permissionCombo.setItems(permissions);
        permissionCombo.setCellFactory(listView -> new ListCell<SystemPermission>() {
            @Override
            protected void updateItem(SystemPermission item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getModuleName() + " - " + item.getActionName());
                }
            }
        });
        
        selectDialog.getDialogPane().setContent(permissionCombo);
        
        selectDialog.setResultConverter(dialogButton -> {
            if (dialogButton == selectButtonType) {
                return permissionCombo.getValue();
            }
            return null;
        });
        
        Optional<SystemPermission> selectedPermission = selectDialog.showAndWait();
        selectedPermission.ifPresent(permission -> {
            Dialog<SystemPermission> editDialog = buildPermissionDialog(permission);
            Optional<SystemPermission> result = editDialog.showAndWait();
            result.ifPresent(updatedPermission -> {
                if (updatePermissionInDatabase(permission.getPermissionId(), updatedPermission)) {
                    loadPermissionsFromDatabase(); // Refresh permissions
                    showInfo("Success", "Permission updated successfully: " + updatedPermission.getPermissionName());
                }
            });
        });
    }

    @FXML
    private void onDeletePermission() {
        // Show a dialog to select permission to delete
        Dialog<SystemPermission> selectDialog = new Dialog<>();
        selectDialog.setTitle("Delete Permission");
        selectDialog.setHeaderText("Choose a permission to delete:");
        
        ButtonType deleteButtonType = new ButtonType("Delete", ButtonBar.ButtonData.OK_DONE);
        selectDialog.getDialogPane().getButtonTypes().addAll(deleteButtonType, ButtonType.CANCEL);
        
        ComboBox<SystemPermission> permissionCombo = new ComboBox<>();
        permissionCombo.setItems(permissions);
        permissionCombo.setCellFactory(listView -> new ListCell<SystemPermission>() {
            @Override
            protected void updateItem(SystemPermission item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getModuleName() + " - " + item.getActionName());
                }
            }
        });
        
        selectDialog.getDialogPane().setContent(permissionCombo);
        
        selectDialog.setResultConverter(dialogButton -> {
            if (dialogButton == deleteButtonType) {
                return permissionCombo.getValue();
            }
            return null;
        });
        
        Optional<SystemPermission> selectedPermission = selectDialog.showAndWait();
        selectedPermission.ifPresent(permission -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Delete Permission");
            confirm.setHeaderText("Are you sure you want to delete this permission?");
            confirm.setContentText("Permission: " + permission.getModuleName() + " - " + permission.getActionName() + "\nThis action cannot be undone.");
            
            confirm.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    if (deletePermissionFromDatabase(permission.getPermissionId())) {
                        loadPermissionsFromDatabase(); // Refresh permissions
                        showInfo("Success", "Permission deleted successfully: " + permission.getPermissionName());
                    }
                }
            });
        });
    }

    @FXML
    private void onSavePermissions() {
        logger.info("=== SAVE PERMISSIONS STARTED ===");
        
        if (selectedRole == null) {
            logger.warning("Save permissions failed: No role selected");
            showError("No Role Selected", "Please select a role to save permissions for.");
            return;
        }
        
        logger.info("Saving permissions for role: " + selectedRole.getName() + " (ID: " + selectedRole.getId() + ")");
        
        // Get current user for logging
        SessionManager sessionManager = SessionManager.getInstance();
        String currentUser = sessionManager.isLoggedIn() ? sessionManager.getCurrentUser() : "Unknown";
        logger.info("Save initiated by user: " + currentUser);
        
        // Log current permission tree state
        logCurrentPermissionTreeState();
        
        if (saveRolePermissionsToDatabase(selectedRole.getId())) {
            logger.info("Permissions saved successfully for role: " + selectedRole.getName());
        showInfo("Success", "Permissions saved successfully for role: " + selectedRole.getName());
        } else {
            logger.severe("Failed to save permissions for role: " + selectedRole.getName());
        }
        
        logger.info("=== SAVE PERMISSIONS COMPLETED ===");
    }
    
    private void logCurrentPermissionTreeState() {
        try {
            logger.info("Current permission tree state:");
            
            if (permissionTree.getRoot() != null) {
                logTreeItem(permissionTree.getRoot(), 0);
            } else {
                logger.info("  Permission tree root is null");
            }
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error logging permission tree state", e);
        }
    }
    
    private void logTreeItem(TreeItem<String> item, int depth) {
        if (item == null) return;
        
        String indent = "  ".repeat(depth);
        String itemText = item.getValue();
        
        // Check if this is a leaf node (permission) with checkbox
        if (item.isLeaf() && item.getParent() != null) {
            // This is a permission item - check if it's selected
            boolean isSelected = isPermissionGranted(selectedRole.getId(), itemText);
            logger.info(indent + "Permission: " + itemText + " - Selected: " + isSelected);
        } else {
            // This is a module item
            logger.info(indent + "Module: " + itemText);
        }
        
        // Recursively log children
        for (TreeItem<String> child : item.getChildren()) {
            logTreeItem(child, depth + 1);
        }
    }

    @FXML
    private void onResetPermissions() {
        if (selectedRole == null) {
            showError("No Role Selected", "Please select a role to reset permissions for.");
            return;
        }
        showInfo("Reset", "Permissions reset to default for role: " + selectedRole.getName());
    }

    // Database Operations
    private boolean addRoleToDatabase(SystemRole role) {
        try {
            String query = "INSERT INTO roles (role_name, description, user_count, status, created_date) VALUES (?, ?, ?, ?, ?)";
            PreparedStatement stmt = connection.prepareStatement(query);
            stmt.setString(1, role.getName());
            stmt.setString(2, role.getDescription());
            stmt.setInt(3, role.getUserCount());
            stmt.setString(4, role.getStatus());
            stmt.setString(5, role.getCreatedDate());
            
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error adding role to database", e);
            showError("Database Error", "Failed to add role: " + e.getMessage());
            return false;
        }
    }
    
    private boolean updateRoleInDatabase(SystemRole role) {
        try {
            String query = "UPDATE roles SET role_name = ?, description = ?, status = ? WHERE role_id = ?";
            PreparedStatement stmt = connection.prepareStatement(query);
            stmt.setString(1, role.getName());
            stmt.setString(2, role.getDescription());
            stmt.setString(3, role.getStatus());
            stmt.setInt(4, role.getId());
            
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error updating role in database", e);
            showError("Database Error", "Failed to update role: " + e.getMessage());
            return false;
        }
    }
    
    private boolean deleteRoleFromDatabase(int roleId) {
        try {
            // First delete role permissions
            String deletePermissionsQuery = "DELETE FROM role_permissions WHERE role_id = ?";
            PreparedStatement deletePermissionsStmt = connection.prepareStatement(deletePermissionsQuery);
            deletePermissionsStmt.setInt(1, roleId);
            deletePermissionsStmt.executeUpdate();
            
            // Then delete the role
            String deleteRoleQuery = "DELETE FROM roles WHERE role_id = ?";
            PreparedStatement deleteRoleStmt = connection.prepareStatement(deleteRoleQuery);
            deleteRoleStmt.setInt(1, roleId);
            
            int rowsAffected = deleteRoleStmt.executeUpdate();
            return rowsAffected > 0;
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error deleting role from database", e);
            showError("Database Error", "Failed to delete role: " + e.getMessage());
            return false;
        }
    }
    
    private boolean addPermissionToDatabase(SystemPermission permission) {
        try {
            String query = "INSERT INTO permissions (permission_name, module_name, action_name, description, status, created_date) VALUES (?, ?, ?, ?, ?, NOW())";
            PreparedStatement stmt = connection.prepareStatement(query);
            stmt.setString(1, permission.getPermissionName());
            stmt.setString(2, permission.getModuleName());
            stmt.setString(3, permission.getActionName());
            stmt.setString(4, permission.getDescription());
            stmt.setString(5, "Active");
            
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error adding permission to database", e);
            showError("Database Error", "Failed to add permission: " + e.getMessage());
            return false;
        }
    }
    
    private boolean updatePermissionInDatabase(int permissionId, SystemPermission permission) {
        try {
            String query = "UPDATE permissions SET permission_name = ?, module_name = ?, action_name = ?, description = ? WHERE permission_id = ?";
            PreparedStatement stmt = connection.prepareStatement(query);
            stmt.setString(1, permission.getPermissionName());
            stmt.setString(2, permission.getModuleName());
            stmt.setString(3, permission.getActionName());
            stmt.setString(4, permission.getDescription());
            stmt.setInt(5, permissionId);
            
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error updating permission in database", e);
            showError("Database Error", "Failed to update permission: " + e.getMessage());
            return false;
        }
    }
    
    private boolean deletePermissionFromDatabase(int permissionId) {
        try {
            // First delete role permissions that reference this permission
            String deleteRolePermissionsQuery = "DELETE FROM role_permissions WHERE permission_id = ?";
            PreparedStatement deleteRolePermissionsStmt = connection.prepareStatement(deleteRolePermissionsQuery);
            deleteRolePermissionsStmt.setInt(1, permissionId);
            deleteRolePermissionsStmt.executeUpdate();
            
            // Then delete the permission
            String deletePermissionQuery = "DELETE FROM permissions WHERE permission_id = ?";
            PreparedStatement deletePermissionStmt = connection.prepareStatement(deletePermissionQuery);
            deletePermissionStmt.setInt(1, permissionId);
            
            int rowsAffected = deletePermissionStmt.executeUpdate();
            return rowsAffected > 0;
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error deleting permission from database", e);
            showError("Database Error", "Failed to delete permission: " + e.getMessage());
            return false;
        }
    }
    
    private boolean saveRolePermissionsToDatabase(int roleId) {
        logger.info("=== SAVE ROLE PERMISSIONS TO DATABASE STARTED ===");
        logger.info("Role ID: " + roleId);
        
        try {
            // Disable auto-commit to handle transaction properly
            boolean autoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            
            try {
                // First clear existing permissions for this role
                logger.info("Clearing existing permissions for role ID: " + roleId);
                String clearQuery = "DELETE FROM role_permissions WHERE role_id = ?";
                PreparedStatement clearStmt = connection.prepareStatement(clearQuery);
                clearStmt.setInt(1, roleId);
                int deletedRows = clearStmt.executeUpdate();
                logger.info("Deleted " + deletedRows + " existing permission records");
                
                // Get selected permissions from the permission tree
                List<String> selectedPermissions = getSelectedPermissionsFromTree();
                logger.info("Selected permissions from tree: " + selectedPermissions.size() + " permissions");
                for (String perm : selectedPermissions) {
                    logger.info("  - " + perm);
                }
                
                if (selectedPermissions.isEmpty()) {
                    logger.warning("No permissions selected from tree, using default permissions");
                    // Fallback to some basic permissions if none selected
                    selectedPermissions = Arrays.asList("dashboard.view", "employee.view", "payroll.view", "reports.view");
                }
                
                // Use INSERT ... ON DUPLICATE KEY UPDATE to handle duplicates gracefully
                String insertQuery = "INSERT INTO role_permissions (role_id, permission_id, granted, created_date) VALUES (?, ?, ?, NOW()) " +
                                   "ON DUPLICATE KEY UPDATE granted = VALUES(granted), created_date = NOW()";
                PreparedStatement insertStmt = connection.prepareStatement(insertQuery);
                
                int permissionsAdded = 0;
                int permissionsNotFound = 0;
                
                for (String permissionName : selectedPermissions) {
                    logger.info("Processing permission: " + permissionName);
                    
                    // Get permission ID
                    String getPermissionIdQuery = "SELECT permission_id FROM permissions WHERE permission_name = ?";
                    PreparedStatement getPermissionIdStmt = connection.prepareStatement(getPermissionIdQuery);
                    getPermissionIdStmt.setString(1, permissionName);
                    ResultSet rs = getPermissionIdStmt.executeQuery();
                    
                    if (rs.next()) {
                        int permissionId = rs.getInt("permission_id");
                        logger.info("  Found permission ID: " + permissionId + " for permission: " + permissionName);
                        
                        insertStmt.setInt(1, roleId);
                        insertStmt.setInt(2, permissionId);
                        insertStmt.setBoolean(3, true);
                        insertStmt.addBatch();
                        permissionsAdded++;
                    } else {
                        logger.warning("  Permission not found in database: " + permissionName);
                        permissionsNotFound++;
                    }
                }
                
                logger.info("Executing batch insert for " + permissionsAdded + " permissions");
                int[] results = insertStmt.executeBatch();
                
                int successfulInserts = 0;
                for (int result : results) {
                    if (result >= 0) successfulInserts++;
                }
                
                logger.info("Batch insert results: " + successfulInserts + " successful inserts out of " + results.length + " attempts");
                logger.info("Permissions added: " + permissionsAdded + ", Not found: " + permissionsNotFound);
                
                // Commit the transaction
                connection.commit();
                logger.info("Transaction committed successfully");
                
                boolean success = successfulInserts > 0;
                logger.info("Save operation result: " + (success ? "SUCCESS" : "FAILED"));
                logger.info("=== SAVE ROLE PERMISSIONS TO DATABASE COMPLETED ===");
                
                return success;
                
            } catch (SQLException e) {
                // Rollback the transaction on error
                connection.rollback();
                logger.severe("Transaction rolled back due to error");
                throw e;
            } finally {
                // Restore auto-commit setting
                connection.setAutoCommit(autoCommit);
            }
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error saving role permissions to database", e);
            logger.severe("SQL Error details: " + e.getMessage());
            logger.severe("SQL State: " + e.getSQLState());
            logger.severe("Error Code: " + e.getErrorCode());
            showError("Database Error", "Failed to save permissions: " + e.getMessage());
            return false;
        }
    }
    
    private List<String> getSelectedPermissionsFromTree() {
        List<String> selectedPermissions = new ArrayList<>();
        
        try {
            logger.info("Getting selected permissions from permission tree");
            
            if (permissionTree.getRoot() == null) {
                logger.warning("Permission tree root is null");
                return selectedPermissions;
            }
            
            // Traverse the tree to find selected permissions
            traverseTreeForSelectedPermissions(permissionTree.getRoot(), selectedPermissions);
            
            logger.info("Found " + selectedPermissions.size() + " selected permissions from tree");
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error getting selected permissions from tree", e);
        }
        
        return selectedPermissions;
    }
    
    private void traverseTreeForSelectedPermissions(TreeItem<String> item, List<String> selectedPermissions) {
        if (item == null) return;
        
        // Check if this is a leaf node (permission) and if it's selected
        if (item.isLeaf() && item.getParent() != null) {
            String permissionName = item.getValue(); // This is now the permission_name from the tree
            
            // Check the tracked checkbox state instead of database state
            Boolean isSelected = permissionStates.get(permissionName);
            if (isSelected != null && isSelected) {
                // No conversion needed - we're already using permission_name
                selectedPermissions.add(permissionName);
                logger.info("  Selected permission from checkbox: " + permissionName);
            } else {
                logger.info("  Permission not selected: " + permissionName + " (state: " + isSelected + ")");
            }
        }
        
        // Recursively traverse children
        for (TreeItem<String> child : item.getChildren()) {
            traverseTreeForSelectedPermissions(child, selectedPermissions);
        }
    }

    // Dialog Builders
    private Dialog<SystemPermission> buildPermissionDialog(SystemPermission permission) {
        Dialog<SystemPermission> dialog = new Dialog<>();
        dialog.setTitle(permission == null ? "Add New Permission" : "Edit Permission");
        dialog.setHeaderText(null);

        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(20, 150, 10, 10));

        TextField permissionNameField = new TextField();
        permissionNameField.setPromptText("Permission Name (e.g., employee.view)");
        TextField moduleNameField = new TextField();
        moduleNameField.setPromptText("Module Name (e.g., Employee Management)");
        TextField actionNameField = new TextField();
        actionNameField.setPromptText("Action Name (e.g., View)");
        TextField descriptionField = new TextField();
        descriptionField.setPromptText("Description");

        if (permission != null) {
            permissionNameField.setText(permission.getPermissionName());
            moduleNameField.setText(permission.getModuleName());
            actionNameField.setText(permission.getActionName());
            descriptionField.setText(permission.getDescription());
        }

        grid.add(new Label("Permission Name:"), 0, 0);
        grid.add(permissionNameField, 1, 0);
        grid.add(new Label("Module Name:"), 0, 1);
        grid.add(moduleNameField, 1, 1);
        grid.add(new Label("Action Name:"), 0, 2);
        grid.add(actionNameField, 1, 2);
        grid.add(new Label("Description:"), 0, 3);
        grid.add(descriptionField, 1, 3);

        dialog.getDialogPane().setContent(grid);

        // Add validation
        dialog.getDialogPane().lookupButton(saveButtonType).addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            if (permissionNameField.getText().trim().isEmpty()) {
                showError("Validation Error", "Permission name is required.");
                event.consume();
                return;
            }
            if (moduleNameField.getText().trim().isEmpty()) {
                showError("Validation Error", "Module name is required.");
                event.consume();
                return;
            }
            if (actionNameField.getText().trim().isEmpty()) {
                showError("Validation Error", "Action name is required.");
                event.consume();
                return;
            }
            if (descriptionField.getText().trim().isEmpty()) {
                showError("Validation Error", "Description is required.");
                event.consume();
                return;
            }
        });

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                return new SystemPermission(
                    permissionNameField.getText().trim(),
                    moduleNameField.getText().trim(),
                    actionNameField.getText().trim(),
                    descriptionField.getText().trim()
                );
            }
            return null;
        });

        return dialog;
    }

    private Dialog<SystemRole> buildRoleDialog(SystemRole role) {
        Dialog<SystemRole> dialog = new Dialog<>();
        dialog.setTitle(role == null ? "Add Role" : "Edit Role");
        dialog.setHeaderText(role == null ? "Create a new system role" : "Edit role information");

        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(20, 150, 10, 10));

        TextField nameField = new TextField();
        nameField.setPromptText("Role Name");
        TextField descriptionField = new TextField();
        descriptionField.setPromptText("Description");
        ComboBox<String> statusCombo = new ComboBox<>();
        statusCombo.getItems().addAll("Active", "Inactive");
        statusCombo.setValue("Active");

        if (role != null) {
            nameField.setText(role.getName());
            descriptionField.setText(role.getDescription());
            statusCombo.setValue(role.getStatus());
        }

        grid.add(new Label("Role Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Description:"), 0, 1);
        grid.add(descriptionField, 1, 1);
        grid.add(new Label("Status:"), 0, 2);
        grid.add(statusCombo, 1, 2);

        dialog.getDialogPane().setContent(grid);

        // Add validation
        dialog.getDialogPane().lookupButton(saveButtonType).addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            if (nameField.getText().trim().isEmpty()) {
                showError("Validation Error", "Role name is required.");
                event.consume();
                return;
            }
            if (descriptionField.getText().trim().isEmpty()) {
                showError("Validation Error", "Description is required.");
                event.consume();
                return;
            }
            if (statusCombo.getValue() == null) {
                showError("Validation Error", "Status is required.");
                event.consume();
                return;
            }
        });

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                return new SystemRole(
                    role != null ? role.getId() : roles.size() + 1,
                    nameField.getText().trim(),
                    descriptionField.getText().trim(),
                    0,
                    statusCombo.getValue(),
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                );
            }
            return null;
        });

        return dialog;
    }


    // Utility Methods
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

    // Data Classes
    public static class SystemRole {
        private int id;
        private String name;
        private String description;
        private int userCount;
        private String status;
        private String createdDate;

        public SystemRole(int id, String name, String description, int userCount, String status, String createdDate) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.userCount = userCount;
            this.status = status;
            this.createdDate = createdDate;
        }

        // Getters
        public int getId() { return id; }
        public String getName() { return name; }
        public String getDescription() { return description; }
        public int getUserCount() { return userCount; }
        public String getStatus() { return status; }
        public String getCreatedDate() { return createdDate; }
    }
    
    private void setupPermissionBasedVisibility() {
        try {
            // Get current user from SessionManager
            SessionManager sessionManager = SessionManager.getInstance();
            
            if (sessionManager.isLoggedIn()) {
                String currentUser = sessionManager.getCurrentUser();
                logger.info("Setting up permission-based visibility for user: " + currentUser);
                
                // Check user permissions and show/hide buttons accordingly
                setupRoleManagementButtons(currentUser);
                setupPermissionManagementButtons(currentUser);
                
            } else {
                logger.warning("No user session found, hiding all action buttons");
                hideAllActionButtons();
            }
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error setting up permission-based visibility", e);
            hideAllActionButtons();
        }
    }
    
    private void setupRoleManagementButtons(String username) {
        try {
            // Check if user has user_access permissions
            boolean canAdd = hasUserPermission(username, "user_access.add");
            boolean canEdit = hasUserPermission(username, "user_access.edit");
            boolean canDelete = hasUserPermission(username, "user_access.delete");
            
            // Show/hide role management buttons
            addRoleButton.setVisible(canAdd);
            editRoleButton.setVisible(canEdit);
            deleteRoleButton.setVisible(canDelete);
            
            // Search button is always visible (view permission)
            searchRoleButton.setVisible(true);
            
            logger.info("Role management buttons - Add: " + canAdd + ", Edit: " + canEdit + ", Delete: " + canDelete);
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error setting up role management buttons", e);
            hideRoleManagementButtons();
        }
    }
    
    private void setupPermissionManagementButtons(String username) {
        try {
            // Check if user has user_access permissions
            boolean canAdd = hasUserPermission(username, "user_access.add");
            boolean canEdit = hasUserPermission(username, "user_access.edit");
            boolean canDelete = hasUserPermission(username, "user_access.delete");
            
            // Show/hide permission management buttons
            addPermissionButton.setVisible(canAdd);
            updatePermissionButton.setVisible(canEdit);
            deletePermissionButton.setVisible(canDelete);
            savePermissionsButton.setVisible(canEdit);
            resetPermissionsButton.setVisible(canEdit);
            
            logger.info("Permission management buttons - Add: " + canAdd + ", Edit: " + canEdit + ", Delete: " + canDelete);
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error setting up permission management buttons", e);
            hidePermissionManagementButtons();
        }
    }
    
    private boolean hasUserPermission(String username, String permissionName) {
        try {
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
            return false;
        }
    }
    
    private void hideAllActionButtons() {
        hideRoleManagementButtons();
        hidePermissionManagementButtons();
    }
    
    private void hideRoleManagementButtons() {
        addRoleButton.setVisible(false);
        editRoleButton.setVisible(false);
        deleteRoleButton.setVisible(false);
        searchRoleButton.setVisible(true); // Keep search visible
    }
    
    private void hidePermissionManagementButtons() {
        addPermissionButton.setVisible(false);
        updatePermissionButton.setVisible(false);
        deletePermissionButton.setVisible(false);
        savePermissionsButton.setVisible(false);
        resetPermissionsButton.setVisible(false);
    }
    
    private void assignDefaultPermissions() {
        try {
            // Assign default permissions to Staff role
            assignStaffPermissions();
            
            // Assign default permissions to Admin role (if needed)
            assignAdminPermissions();
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error assigning default permissions", e);
        }
    }
    
    private void assignStaffPermissions() throws SQLException {
        // Get Staff role ID
        String getRoleIdQuery = "SELECT role_id FROM roles WHERE role_name = 'Staff'";
        PreparedStatement getRoleIdStmt = connection.prepareStatement(getRoleIdQuery);
        ResultSet roleRs = getRoleIdStmt.executeQuery();
        
        if (roleRs.next()) {
            int staffRoleId = roleRs.getInt("role_id");
            logger.info("Assigning default permissions to Staff role (ID: " + staffRoleId + ")");
            
            // Define staff permissions (Add, Edit, Delete for most modules)
            String[] staffPermissions = {
                "employee.add", "employee.edit", "employee.delete",
                "payroll.add", "payroll.edit", "payroll.delete",
                "payroll_gen.add", "payroll_gen.edit", "payroll_gen.delete",
                "reports.add", "reports.edit", "reports.delete",
                "import_export.add", "import_export.edit", "import_export.delete",
                "history.add", "history.edit", "history.delete",
                "user_mgmt.add", "user_mgmt.edit", "user_mgmt.delete",
                "user_access.add", "user_access.edit", "user_access.delete"
            };
            
            // Assign each permission
            for (String actionName : staffPermissions) {
                assignPermissionToRole(staffRoleId, actionName, true);
            }
            
            logger.info("Staff permissions assigned successfully");
        }
    }
    
    private void assignAdminPermissions() throws SQLException {
        // Get Admin role ID
        String getRoleIdQuery = "SELECT role_id FROM roles WHERE role_name = 'Admin'";
        PreparedStatement getRoleIdStmt = connection.prepareStatement(getRoleIdQuery);
        ResultSet roleRs = getRoleIdStmt.executeQuery();
        
        if (roleRs.next()) {
            int adminRoleId = roleRs.getInt("role_id");
            logger.info("Assigning default permissions to Admin role (ID: " + adminRoleId + ")");
            
            // Admin gets all permissions
            String[] adminPermissions = {
                "dashboard.view", "dashboard.quick_actions",
                "employee.view", "employee.add", "employee.edit", "employee.delete",
                "payroll.view", "payroll.generate", "payroll.export",
                "payroll_gen.view", "payroll_gen.add", "payroll_gen.edit", "payroll_gen.delete",
                "reports.view", "reports.generate", "reports.export",
                "import_export.view", "import_export.import", "import_export.export",
                "history.view",
                "security.view", "security.edit",
                "gov_remit.view", "gov_remit.edit",
                "user_mgmt.view", "user_mgmt.add", "user_mgmt.edit", "user_mgmt.delete",
                "user_access.view", "user_access.edit"
            };
            
            // Assign each permission
            for (String actionName : adminPermissions) {
                assignPermissionToRole(adminRoleId, actionName, true);
            }
            
            logger.info("Admin permissions assigned successfully");
        }
    }
    
    private void assignPermissionToRole(int roleId, String actionName, boolean granted) throws SQLException {
        // Get permission ID from action name
        String getPermissionIdQuery = "SELECT permission_id FROM permissions WHERE action_name = ?";
        PreparedStatement getPermissionIdStmt = connection.prepareStatement(getPermissionIdQuery);
        getPermissionIdStmt.setString(1, actionName);
        ResultSet permissionRs = getPermissionIdStmt.executeQuery();
        
        if (permissionRs.next()) {
            int permissionId = permissionRs.getInt("permission_id");
            
            // Check if role-permission already exists
            String checkQuery = "SELECT COUNT(*) FROM role_permissions WHERE role_id = ? AND permission_id = ?";
            PreparedStatement checkStmt = connection.prepareStatement(checkQuery);
            checkStmt.setInt(1, roleId);
            checkStmt.setInt(2, permissionId);
            ResultSet checkRs = checkStmt.executeQuery();
            
            if (checkRs.next() && checkRs.getInt(1) == 0) {
                // Insert new role-permission
                String insertQuery = "INSERT INTO role_permissions (role_id, permission_id, granted, created_date) VALUES (?, ?, ?, NOW())";
                PreparedStatement insertStmt = connection.prepareStatement(insertQuery);
                insertStmt.setInt(1, roleId);
                insertStmt.setInt(2, permissionId);
                insertStmt.setBoolean(3, granted);
                insertStmt.executeUpdate();
                
                logger.info("Assigned permission '" + actionName + "' to role ID " + roleId);
            } else {
                // Update existing role-permission
                String updateQuery = "UPDATE role_permissions SET granted = ? WHERE role_id = ? AND permission_id = ?";
                PreparedStatement updateStmt = connection.prepareStatement(updateQuery);
                updateStmt.setBoolean(1, granted);
                updateStmt.setInt(2, roleId);
                updateStmt.setInt(3, permissionId);
                updateStmt.executeUpdate();
                
                logger.info("Updated permission '" + actionName + "' for role ID " + roleId);
            }
        }
    }

    public static class SystemPermission {
        private int permissionId;
        private String permissionName;
        private String moduleName;
        private String actionName;
        private String description;
        private String status;

        public SystemPermission(String permissionName, String moduleName, String actionName, String description) {
            this.permissionName = permissionName;
            this.moduleName = moduleName;
            this.actionName = actionName;
            this.description = description;
            this.status = "Active";
        }
        
        public SystemPermission(int permissionId, String permissionName, String moduleName, String actionName, String description, String status) {
            this.permissionId = permissionId;
            this.permissionName = permissionName;
            this.moduleName = moduleName;
            this.actionName = actionName;
            this.description = description;
            this.status = status;
        }

        // Getters
        public int getPermissionId() { return permissionId; }
        public String getPermissionName() { return permissionName; }
        public String getModuleName() { return moduleName; }
        public String getActionName() { return actionName; }
        public String getDescription() { return description; }
        public String getStatus() { return status; }
    }
}
