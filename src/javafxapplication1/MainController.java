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
import javafx.scene.layout.HBox;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import javafx.scene.control.DatePicker;

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
        
        // Initialize database connection
        initializeDatabase();
        
        // Set the current user's name in the menu button
        updateUserMenuButton();
        
        // Setup permission-based menu visibility
        setupPermissionBasedMenuVisibility();
        
        // Load dashboard data
        loadDashboardData();
        
        // Initialize date pickers with default range (last 30 days)
        if (chartStartDatePicker != null && chartEndDatePicker != null) {
            chartEndDatePicker.setValue(LocalDate.now());
            chartStartDatePicker.setValue(LocalDate.now().minusDays(30));
        }
        
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
    @FXML private Button salaryReferenceBtn;
    @FXML private Button attendanceProcessorBtn;
    @FXML private Button loanManagementBtn;
    @FXML private Button governmentContributionsBtn;
    @FXML private Button systemSettingsBtn;
    @FXML private Button userManagementBtn;
    @FXML private Button userAccessBtn;
    @FXML private MenuButton userMenuButton;
    @FXML private Label totalEmployeesLabel;
    @FXML private Label payrollProcessedLabel;
    @FXML private Label averageSalaryLabel;
    @FXML private VBox recentActivitiesContainer;
    @FXML private VBox attendanceChartContainer;
    @FXML private DatePicker chartStartDatePicker;
    @FXML private DatePicker chartEndDatePicker;
    
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
        
        // Refresh dashboard data when opening dashboard
        loadDashboardData();
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
    // @FXML private void openGovernmentRemittances() { 
    //     setCenterContent("/javafxapplication1/GovernmentRemittances.fxml"); 
    //     setActiveMenuButton(govRemitBtn);
    // }
    @FXML private void openSalaryReference() { 
        setCenterContent("/javafxapplication1/SalaryReference.fxml"); 
        setActiveMenuButton(salaryReferenceBtn);
    }
    @FXML private void openAttendanceProcessor() { 
        setCenterContent("/javafxapplication1/AttendanceProcessor.fxml"); 
        setActiveMenuButton(attendanceProcessorBtn);
    }
    @FXML private void openLoanManagement() { 
        setCenterContent("/javafxapplication1/LoanManagement.fxml"); 
        setActiveMenuButton(loanManagementBtn);
    }
    @FXML private void openGovernmentContributions() { 
        setCenterContent("/javafxapplication1/GovernmentContributions.fxml"); 
        setActiveMenuButton(governmentContributionsBtn);
    }
    @FXML private void openSystemSettings() { 
        setCenterContent("/javafxapplication1/SystemSettings.fxml"); 
        setActiveMenuButton(systemSettingsBtn);
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
            if (connection == null || connection.isClosed()) {
                Class.forName("com.mysql.cj.jdbc.Driver");
                connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                logger.info("Database connection established successfully");
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error initializing database connection", e);
            connection = null;
        }
    }
    
    private void loadDashboardData() {
        if (connection == null) {
            initializeDatabase();
        }
        
        if (connection != null) {
            loadStatistics();
            loadRecentActivities();
            loadAttendanceChart(null, null);
        }
    }
    
    @FXML
    private void onFilterChart() {
        LocalDate startDate = chartStartDatePicker.getValue();
        LocalDate endDate = chartEndDatePicker.getValue();
        
        if (startDate != null && endDate != null) {
            if (startDate.isAfter(endDate)) {
                Alert alert = new Alert(AlertType.WARNING);
                alert.setTitle("Invalid Date Range");
                alert.setHeaderText(null);
                alert.setContentText("Start date cannot be after end date.");
                alert.showAndWait();
                return;
            }
            loadAttendanceChart(startDate, endDate);
        } else {
            Alert alert = new Alert(AlertType.WARNING);
            alert.setTitle("Date Range Required");
            alert.setHeaderText(null);
            alert.setContentText("Please select both start and end dates.");
            alert.showAndWait();
        }
    }
    
    private void loadStatistics() {
        try {
            // Load total employees
            String employeesQuery = "SELECT COUNT(*) as total FROM employees WHERE status = 'Active'";
            PreparedStatement stmt = connection.prepareStatement(employeesQuery);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                int totalEmployees = rs.getInt("total");
                totalEmployeesLabel.setText(String.valueOf(totalEmployees));
            }
            
            // Load payroll processed this month
            String payrollQuery = """
                SELECT COUNT(DISTINCT employee_id) as processed 
                FROM payroll_sheet_items 
                WHERE MONTH(created_at) = MONTH(CURRENT_DATE()) 
                AND YEAR(created_at) = YEAR(CURRENT_DATE())
                """;
            stmt = connection.prepareStatement(payrollQuery);
            rs = stmt.executeQuery();
            if (rs.next()) {
                int processed = rs.getInt("processed");
                payrollProcessedLabel.setText(String.valueOf(processed));
            }
            
            // Load average salary
            String avgSalaryQuery = """
                SELECT AVG(sr.monthly_salary) as avg_salary 
                FROM employees e 
                JOIN salary_reference sr ON e.salary_ref_id = sr.id 
                WHERE e.status = 'Active'
                """;
            stmt = connection.prepareStatement(avgSalaryQuery);
            rs = stmt.executeQuery();
            if (rs.next()) {
                double avgSalary = rs.getDouble("avg_salary");
                if (!rs.wasNull()) {
                    averageSalaryLabel.setText(String.format("₱%,.0f", avgSalary));
                }
            }
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error loading dashboard statistics", e);
        }
    }
    
    private void loadRecentActivities() {
        try {
            // Clear existing activities
            if (recentActivitiesContainer != null) {
                recentActivitiesContainer.getChildren().clear();
            }
            
            // Load 10 latest security events
            String query = """
                SELECT event_type, severity, username, description, timestamp 
                FROM security_events 
                ORDER BY timestamp DESC 
                LIMIT 10
                """;
            PreparedStatement stmt = connection.prepareStatement(query);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                String eventType = rs.getString("event_type");
                String severity = rs.getString("severity");
                String username = rs.getString("username");
                String description = rs.getString("description");
                Timestamp timestamp = rs.getTimestamp("timestamp");
                
                // Format timestamp
                String timeAgo = formatTimeAgo(timestamp.toLocalDateTime());
                
                // Determine color based on severity
                String color = getSeverityColor(severity);
                
                // Create activity item
                HBox activityItem = new HBox(10);
                activityItem.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                
                Label bullet = new Label("•");
                bullet.setStyle("-fx-text-fill: " + color + "; -fx-font-weight: bold;");
                
                Label descLabel = new Label(description);
                descLabel.setStyle("-fx-text-fill: #333;");
                
                Label timeLabel = new Label(timeAgo);
                timeLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 12px;");
                
                activityItem.getChildren().addAll(bullet, descLabel, timeLabel);
                
                if (recentActivitiesContainer != null) {
                    recentActivitiesContainer.getChildren().add(activityItem);
                }
            }
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error loading recent activities", e);
        }
    }
    
    private void loadAttendanceChart(LocalDate startDate, LocalDate endDate) {
        try {
            // Clear existing chart
            if (attendanceChartContainer != null) {
                attendanceChartContainer.getChildren().clear();
            }
            
            // Use provided date range or default to last 30 days
            LocalDate actualStartDate = startDate != null ? startDate : LocalDate.now().minusDays(30);
            LocalDate actualEndDate = endDate != null ? endDate : LocalDate.now();
            
            // Load attendance statistics by status
            String query = """
                SELECT status, COUNT(*) as count 
                FROM processed_attendance 
                WHERE process_date >= ? AND process_date <= ?
                GROUP BY status
                """;
            PreparedStatement stmt = connection.prepareStatement(query);
            stmt.setDate(1, java.sql.Date.valueOf(actualStartDate));
            stmt.setDate(2, java.sql.Date.valueOf(actualEndDate));
            ResultSet rs = stmt.executeQuery();
            
            // Create chart data
            ObservableList<XYChart.Series<String, Number>> chartData = FXCollections.observableArrayList();
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Attendance Status (Last 30 Days)");
            
            while (rs.next()) {
                String status = rs.getString("status");
                int count = rs.getInt("count");
                series.getData().add(new XYChart.Data<>(status, count));
            }
            
            chartData.add(series);
            
            // Create axes
            CategoryAxis xAxis = new CategoryAxis();
            xAxis.setLabel("Status");
            NumberAxis yAxis = new NumberAxis();
            yAxis.setLabel("Count");
            
            // Create bar chart
            BarChart<String, Number> barChart = new BarChart<>(xAxis, yAxis);
            barChart.setData(chartData);
            
            // Format date range for title
            String dateRange = actualStartDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")) + 
                              " - " + 
                              actualEndDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"));
            barChart.setTitle("Attendance Statistics (" + dateRange + ")");
            barChart.setLegendVisible(true);
            barChart.setPrefHeight(300);
            barChart.setPrefWidth(800);
            
            // Style the chart
            barChart.setStyle("-fx-background-color: white;");
            
            if (attendanceChartContainer != null) {
                attendanceChartContainer.getChildren().add(barChart);
            }
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error loading attendance chart", e);
        }
    }
    
    private String formatTimeAgo(LocalDateTime dateTime) {
        LocalDateTime now = LocalDateTime.now();
        long minutes = ChronoUnit.MINUTES.between(dateTime, now);
        long hours = ChronoUnit.HOURS.between(dateTime, now);
        long days = ChronoUnit.DAYS.between(dateTime, now);
        
        if (minutes < 1) {
            return "Just now";
        } else if (minutes < 60) {
            return minutes + " minute" + (minutes > 1 ? "s" : "") + " ago";
        } else if (hours < 24) {
            return hours + " hour" + (hours > 1 ? "s" : "") + " ago";
        } else if (days < 7) {
            return days + " day" + (days > 1 ? "s" : "") + " ago";
        } else {
            return dateTime.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"));
        }
    }
    
    private String getSeverityColor(String severity) {
        if (severity == null) return "#666";
        
        switch (severity.toUpperCase()) {
            case "CRITICAL":
            case "HIGH":
                return "#f44336"; // Red
            case "MEDIUM":
                return "#ff9800"; // Orange
            case "LOW":
                return "#4caf50"; // Green
            default:
                return "#2196f3"; // Blue
        }
    }
    
    private void setupMenuVisibility(String username) {
        try {
            // Dashboard is always visible
            dashboardBtn.setVisible(true);
            dashboardBtn.setManaged(true);
            
            // Check each module's permissions - check if user has ANY permission for the module
            // Employee module: check for view, add, edit, or delete permissions
            boolean employeeView = hasUserPermission(username, "employee.view") ||
                                  hasUserPermission(username, "employee.add") ||
                                  hasUserPermission(username, "employee.edit") ||
                                  hasUserPermission(username, "employee.delete");
            
            // Payroll Processing module: check for view, generate, or export permissions
            boolean payrollView = hasUserPermission(username, "payroll.view") ||
                                 hasUserPermission(username, "payroll.generate") ||
                                 hasUserPermission(username, "payroll.export");
            boolean payrollGenView = hasUserPermission(username, "payroll_gen.view");
            boolean reportsView = hasUserPermission(username, "reports.view");
            boolean importExportView = hasUserPermission(username, "import_export.view");
            boolean historyView = hasUserPermission(username, "history.view");
            boolean securityView = hasUserPermission(username, "security.view");
            boolean govRemitView = hasUserPermission(username, "gov_remit.view");
            boolean salaryRefView = hasUserPermission(username, "salary_ref.view");
            boolean attendanceProcessView = hasUserPermission(username, "attendance_process.view");
            boolean loanMgmtView = hasUserPermission(username, "loan_mgmt.view");
            boolean govContribView = hasUserPermission(username, "gov_contrib.view");
            boolean systemSettingsView = hasUserPermission(username, "system_settings.view");
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
            setNodeVisibility(salaryReferenceBtn, salaryRefView);
            setNodeVisibility(attendanceProcessorBtn, attendanceProcessView);
            setNodeVisibility(loanManagementBtn, loanMgmtView);
            setNodeVisibility(governmentContributionsBtn, govContribView);
            setNodeVisibility(systemSettingsBtn, systemSettingsView);
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
        setNodeVisibility(salaryReferenceBtn, false);
        setNodeVisibility(attendanceProcessorBtn, false);
        setNodeVisibility(loanManagementBtn, false);
        setNodeVisibility(governmentContributionsBtn, false);
        setNodeVisibility(systemSettingsBtn, false);
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
        setNodeVisibility(salaryReferenceBtn, true);
        setNodeVisibility(attendanceProcessorBtn, true);
        setNodeVisibility(loanManagementBtn, true);
        setNodeVisibility(governmentContributionsBtn, true);
        setNodeVisibility(systemSettingsBtn, true);
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
