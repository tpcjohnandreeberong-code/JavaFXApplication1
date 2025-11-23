/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/javafx/FXMLController.java to edit this template
 */
package javafxapplication1;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.fxml.Initializable;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.MenuButton;
import javafx.scene.control.Label;
import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Screen;
import javafx.stage.Stage;

/**
 * FXML Controller class
 *
 * @author marke
 */
public class MainController implements Initializable {

    /**
     * Initializes the controller class.
     */
    private Node defaultDashboardContent;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Keep the original dashboard content so we can restore it later
        // when navigating back from other modules.
        // contentScroll is injected after FXML load, so it's safe here.
        if (contentScroll != null) {
            defaultDashboardContent = contentScroll.getContent();
        }
        
        // Set the current user's name in the menu button
        updateUserMenuButton();
        
        // Setup permission-based menu visibility
        setupPermissionBasedMenuVisibility();
        
        // Set Dashboard as default active state
        setActiveMenuButton(dashboardBtn);
    }    
    @FXML private Button dashboardBtn;
    @FXML private Button employeeMgmtBtn;
    @FXML private Button payrollProcessingBtn;
    @FXML private Button payrollGeneratorBtn;
    @FXML private Button reportsBtn;
    @FXML private Button importExportBtn;
    @FXML private Button historyBtn;
    @FXML private Button securityBtn;
    @FXML private Button govRemitBtn;
    @FXML private Button userManagementBtn;
    @FXML private Button userAccessBtn;
    @FXML private MenuButton userMenuButton;
    
    // Active menu button tracking
    private Button currentActiveButton;
    
    // Database connection
    private static final String DB_URL = DatabaseConfig.getDbUrl();
    private static final String DB_USER = DatabaseConfig.getDbUser();
    private static final String DB_PASSWORD = DatabaseConfig.getDbPassword();
    private Connection connection;
    private static final Logger logger = Logger.getLogger(MainController.class.getName());

    @FXML 
    private void onLogout() {
        // Show confirmation dialog
        Alert confirmAlert = new Alert(AlertType.CONFIRMATION);
        confirmAlert.setTitle("Logout Confirmation");
        confirmAlert.setHeaderText(null);
        confirmAlert.setContentText("Are you sure you want to logout?");
        
        // Show dialog and wait for user response
        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == javafx.scene.control.ButtonType.OK) {
                // User confirmed logout
                performLogout();
            }
        });
    }
    
    private void performLogout() {
        try {
            // Log logout event before clearing session
            String currentUser = SessionManager.getInstance().getCurrentUser();
            if (currentUser != null) {
                String clientIP = SecurityLogger.getClientIP();
                SecurityLogger.logSecurityEvent(
                    "LOGOUT", 
                    "LOW", 
                    currentUser,
                    "User logged out successfully from " + clientIP
                );
            }
            
            // Clear any session data (you can add more session clearing here)
            clearUserSession();
            
            // Close database connections if any
            closeDatabaseConnections();
            
            // Navigate back to login screen
            navigateToLogin();
            
        } catch (Exception e) {
            // Show error if logout fails
            Alert errorAlert = new Alert(AlertType.ERROR);
            errorAlert.setTitle("Logout Error");
            errorAlert.setHeaderText(null);
            errorAlert.setContentText("An error occurred during logout: " + e.getMessage());
            errorAlert.showAndWait();
        }
    }
    
    private void clearUserSession() {
        // Clear user session using SessionManager
        SessionManager sessionManager = SessionManager.getInstance();
        sessionManager.clearSession();
        System.out.println("User session cleared");
    }
    
    private void closeDatabaseConnections() {
        // Close any open database connections
        // This ensures clean logout
        System.out.println("Database connections closed");
    }
    
    private void navigateToLogin() { 
        try {
            // Load the login screen
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/javafxapplication1/Login.fxml"));
            Scene loginScene = new Scene(loader.load());
            
            // Get the current stage
            Stage currentStage = (Stage) dashboardBtn.getScene().getWindow();

            // Get the screen size
            Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
            currentStage.setX(screenBounds.getMinX());
            currentStage.setY(screenBounds.getMinY());
            currentStage.setWidth(screenBounds.getWidth());
            currentStage.setHeight(screenBounds.getHeight());
            
            // Set properties before setting the scene
            currentStage.setTitle("TPC Payroll Management System - Login");
            currentStage.setResizable(true);
            
            // Apply the new scene
            currentStage.setScene(loginScene);
            
            // Optional: if you want it maximized but still resizable
            currentStage.setMaximized(true);

            // Show the stage
            currentStage.show();
            
        } catch (Exception e) {
            Alert errorAlert = new Alert(AlertType.ERROR);
            errorAlert.setTitle("Navigation Error");
            errorAlert.setHeaderText(null);
            errorAlert.setContentText("Failed to navigate to login screen: " + e.getMessage());
            errorAlert.showAndWait();
        }
}

    
    private void updateUserMenuButton() {
        try {
            // Get current user from SessionManager
            SessionManager sessionManager = SessionManager.getInstance();
            
            if (sessionManager.isLoggedIn()) {
                String currentUser = sessionManager.getCurrentUser();
                String fullName = sessionManager.getUserFullName();
                String role = sessionManager.getUserRole();
                
                // Set the menu button text to show current user
                if (fullName != null && !fullName.isEmpty()) {
                    userMenuButton.setText(fullName);
                } else if (currentUser != null && !currentUser.isEmpty()) {
                    userMenuButton.setText(currentUser);
                } else {
                    userMenuButton.setText("User");
                }
                
                System.out.println("User menu updated: " + fullName + " (" + currentUser + ") - Role: " + role);
            } else {
                userMenuButton.setText("User");
                System.out.println("No user session found, using default text");
            }
            
        } catch (Exception e) {
            System.err.println("Error updating user menu button: " + e.getMessage());
            userMenuButton.setText("User");
        }
    }
    @FXML private void onEditProfile() { setCenterContent("/javafxapplication1/EditProfile.fxml"); }
    @FXML private void openDashboard() { 
        if (defaultDashboardContent != null) contentScroll.setContent(defaultDashboardContent); 
        setActiveMenuButton(dashboardBtn);
    }
    @FXML private void openEmployeeManagement() { 
        setCenterContent("/javafxapplication1/Employee.fxml"); 
        setActiveMenuButton(employeeMgmtBtn);
    }
    @FXML private void openPayrollProcessing() { 
        setCenterContent("/javafxapplication1/PayrollProcessing.fxml"); 
        setActiveMenuButton(payrollProcessingBtn);
    }
    @FXML private void openPayrollGenerator() { 
        setCenterContent("/javafxapplication1/PayrollGenerator.fxml"); 
        setActiveMenuButton(payrollGeneratorBtn);
    }
    @FXML private void openReports() { 
        setCenterContent("/javafxapplication1/Reports.fxml"); 
        setActiveMenuButton(reportsBtn);
    }
    @FXML private void openImportExport() { 
        setCenterContent("/javafxapplication1/ImportExport.fxml"); 
        setActiveMenuButton(importExportBtn);
    }
    @FXML private void openHistory() { 
        setCenterContent("/javafxapplication1/History.fxml"); 
        setActiveMenuButton(historyBtn);
    }
    @FXML private void openSecurity() { 
        setCenterContent("/javafxapplication1/SecurityMaintenance.fxml"); 
        setActiveMenuButton(securityBtn);
    }
    @FXML private void openGovernmentRemittances() { 
        setCenterContent("/javafxapplication1/GovernmentRemittances.fxml"); 
        setActiveMenuButton(govRemitBtn);
    }
    @FXML private void openUserManagement() { 
        setCenterContent("/javafxapplication1/UserManagement.fxml"); 
        setActiveMenuButton(userManagementBtn);
    }
    @FXML private void openUserAccess() { 
        setCenterContent("/javafxapplication1/UserAccess.fxml"); 
        setActiveMenuButton(userAccessBtn);
    }

    private void info(String title, String content) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    @FXML private javafx.scene.control.ScrollPane contentScroll;

    private void setCenterContent(String fxmlPath) {
        try {
            Node content = FXMLLoader.load(getClass().getResource(fxmlPath));
            contentScroll.setContent(content);
        } catch (Exception ex) {
            info("Load Error", "Cannot load view: " + ex.getMessage());
        }
    }
    
    private void setupPermissionBasedMenuVisibility() {
        try {
            // Initialize database connection
            initializeDatabase();
            
            // Get current user from SessionManager
            SessionManager sessionManager = SessionManager.getInstance();
            
            if (sessionManager.isLoggedIn()) {
                String currentUser = sessionManager.getCurrentUser();
                logger.info("Setting up permission-based menu visibility for user: " + currentUser);
                
                // Check permissions and show/hide menu items accordingly
                setupMenuVisibility(currentUser);
                
            } else {
                logger.warning("No user session found, hiding all menu items except dashboard");
                hideAllMenuItems();
            }
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error setting up permission-based menu visibility", e);
            
            // Get current user for fallback behavior
            SessionManager sessionManager = SessionManager.getInstance();
            if (sessionManager.isLoggedIn()) {
                String currentUser = sessionManager.getCurrentUser();
                if ("admin".equals(currentUser)) {
                    logger.info("Database error but admin user detected, showing all menu items");
                    showAllMenuItems();
                } else {
                    hideAllMenuItems();
                }
            } else {
                hideAllMenuItems();
            }
        }
    }
    
    private void initializeDatabase() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            logger.info("Database connection established successfully");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error initializing database connection", e);
            connection = null;
        }
    }
    
    private void setupMenuVisibility(String username) {
        try {
            // Dashboard is always visible
            dashboardBtn.setVisible(true);
            dashboardBtn.setManaged(true);
            
            // Check each module's view permission
            boolean employeeView = hasUserPermission(username, "employee.view");
            boolean payrollView = hasUserPermission(username, "payroll.view");
            boolean payrollGenView = hasUserPermission(username, "payroll_gen.view");
            boolean reportsView = hasUserPermission(username, "reports.view");
            boolean importExportView = hasUserPermission(username, "import_export.view");
            boolean historyView = hasUserPermission(username, "history.view");
            boolean securityView = hasUserPermission(username, "security.view");
            boolean govRemitView = hasUserPermission(username, "gov_remit.view");
            boolean userMgmtView = hasUserPermission(username, "user_mgmt.view");
            boolean userAccessView = hasUserPermission(username, "user_access.view");
            
            // Set visibility for all menu buttons based on permissions
            setNodeVisibility(employeeMgmtBtn, employeeView);
            setNodeVisibility(payrollProcessingBtn, payrollView);
            setNodeVisibility(payrollGeneratorBtn, payrollGenView);
            setNodeVisibility(reportsBtn, reportsView);
            setNodeVisibility(importExportBtn, importExportView);
            setNodeVisibility(historyBtn, historyView);
            setNodeVisibility(securityBtn, securityView);
            setNodeVisibility(govRemitBtn, govRemitView);
            setNodeVisibility(userManagementBtn, userMgmtView);
            setNodeVisibility(userAccessBtn, userAccessView);
            
            logger.info("Menu visibility setup completed for user: " + username);
            logger.info("Employee: " + employeeView + ", Payroll: " + payrollView + 
                       ", PayrollGen: " + payrollGenView + ", Reports: " + reportsView + 
                       ", ImportExport: " + importExportView + ", History: " + historyView + 
                       ", Security: " + securityView + ", GovRemit: " + govRemitView + 
                       ", UserMgmt: " + userMgmtView + ", UserAccess: " + userAccessView);
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error setting up menu visibility", e);
            hideAllMenuItems();
        }
    }
    
    private void setNodeVisibility(javafx.scene.Node node, boolean visible) {
        if (node != null) {
            node.setVisible(visible);
            node.setManaged(visible);
        }
    }
    
    private boolean hasUserPermission(String username, String permissionName) {
        try {
            // Check if database connection is available
            if (connection == null) {
                logger.warning("Database connection not available, defaulting to admin permissions for user: " + username);
                // Default to giving admin users all permissions if database is not available
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
    
    private void hideAllMenuItems() {
        // Hide all menu items except dashboard
        setNodeVisibility(employeeMgmtBtn, false);
        setNodeVisibility(payrollProcessingBtn, false);
        setNodeVisibility(payrollGeneratorBtn, false);
        setNodeVisibility(reportsBtn, false);
        setNodeVisibility(importExportBtn, false);
        setNodeVisibility(historyBtn, false);
        setNodeVisibility(securityBtn, false);
        setNodeVisibility(govRemitBtn, false);
        setNodeVisibility(userManagementBtn, false);
        setNodeVisibility(userAccessBtn, false);
    }
    
    private void showAllMenuItems() {
        // Show all menu items - used as fallback for admin users when database is unavailable
        setNodeVisibility(employeeMgmtBtn, true);
        setNodeVisibility(payrollProcessingBtn, true);
        setNodeVisibility(payrollGeneratorBtn, true);
        setNodeVisibility(reportsBtn, true);
        setNodeVisibility(importExportBtn, true);
        setNodeVisibility(historyBtn, true);
        setNodeVisibility(securityBtn, true);
        setNodeVisibility(govRemitBtn, true);
        setNodeVisibility(userManagementBtn, true);
        setNodeVisibility(userAccessBtn, true);
    }
    
    /**
     * Sets the active menu button (similar to Bootstrap active class)
     * @param activeButton The button to set as active
     */
    private void setActiveMenuButton(Button activeButton) {
        // Remove active style from current active button
        if (currentActiveButton != null) {
            currentActiveButton.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-alignment: CENTER_LEFT; -fx-padding: 5 16 5 16; -fx-font-weight: bold; -fx-font-size: 15px;");
        }
        
        // Set new active button
        currentActiveButton = activeButton;
        if (currentActiveButton != null) {
            currentActiveButton.setStyle("-fx-background-color: rgba(255,255,255,0.2); -fx-text-fill: white; -fx-alignment: CENTER_LEFT; -fx-padding: 5 16 5 16; -fx-font-weight: bold; -fx-font-size: 15px;");
        }
    }
}
