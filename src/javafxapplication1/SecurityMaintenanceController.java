package javafxapplication1;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.SpinnerValueFactory;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.FileWriter;
import java.net.URL;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import java.util.regex.Pattern;

public class SecurityMaintenanceController implements Initializable {

    private static final Logger logger = Logger.getLogger(SecurityMaintenanceController.class.getName());
    
    // Database connection parameters
    private static final String DB_URL = DatabaseConfig.getDbUrl();
    private static final String DB_USER = DatabaseConfig.getDbUser();
    private static final String DB_PASSWORD = DatabaseConfig.getDbPassword();
    private Connection connection;
    
    // Preferences for storing settings
    private Preferences preferences;

    // Password Policy Controls - COMMENTED OUT
    /*
    @FXML private Spinner<Integer> minPasswordLength;
    @FXML private Spinner<Integer> passwordExpiration;
    @FXML private Spinner<Integer> passwordHistory;
    @FXML private CheckBox requireUppercase;
    @FXML private CheckBox requireLowercase;
    @FXML private CheckBox requireNumbers;
    @FXML private CheckBox requireSpecialChars;
    */

    // Security Monitoring Controls
    @FXML private TextField searchField;
    @FXML private ComboBox<String> eventTypeFilter;
    @FXML private ComboBox<String> severityFilter;
    @FXML private TableView<SecurityEvent> securityEventTable;
    @FXML private TableColumn<SecurityEvent, String> colTimestamp;
    @FXML private TableColumn<SecurityEvent, String> colEventType;
    @FXML private TableColumn<SecurityEvent, String> colSeverity;
    @FXML private TableColumn<SecurityEvent, String> colUser;
    @FXML private TableColumn<SecurityEvent, String> colDescription;
    @FXML private TableColumn<SecurityEvent, String> colIPAddress;
    @FXML private TableColumn<SecurityEvent, String> colEventStatus;

    // Tab Pane
    @FXML private TabPane securityTabPane;
    
    // Action buttons
    @FXML private Button refreshButton;
    @FXML private Button exportLogButton;
    @FXML private Button searchButton;
    @FXML private Button clearLogButton;

    // Data Collections
    private final ObservableList<SecurityEvent> securityEvents = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Initialize database connection
        initializeDatabase();
        
        // Initialize preferences
        preferences = Preferences.userNodeForPackage(SecurityMaintenanceController.class);
        
        // Setup UI components
        // setupPasswordPolicySpinners(); // COMMENTED OUT - Password Policy
        setupSecurityEventTable();
        setupEventFilters();
        
        // Load data
        // loadPasswordPolicySettings(); // COMMENTED OUT - Password Policy
        loadSecurityEvents();
        
        // Setup permission-based button visibility
        setupPermissionBasedVisibility();
        
        // Log module access
        String currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser != null) {
            SecurityLogger.logSecurityEvent(
                "SECURITY_MAINTENANCE_MODULE_ACCESS",
                "LOW",
                currentUser,
                "Accessed Security Maintenance module"
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
                boolean canView = hasUserPermission(currentUser, "security.view");
                boolean canEdit = hasUserPermission(currentUser, "security.edit");
                
                // Show/hide buttons based on permissions
                // View permissions: Refresh, Export Log, Search
                if (refreshButton != null) {
                    refreshButton.setVisible(canView);
                    refreshButton.setManaged(canView);
                }
                if (exportLogButton != null) {
                    exportLogButton.setVisible(canView);
                    exportLogButton.setManaged(canView);
                }
                if (searchButton != null) {
                    searchButton.setVisible(canView);
                    searchButton.setManaged(canView);
                }
                
                // Edit permissions: Clear Log (modifies data)
                if (clearLogButton != null) {
                    clearLogButton.setVisible(canEdit);
                    clearLogButton.setManaged(canEdit);
                }
                
                logger.info("Security Maintenance buttons visibility - View: " + canView + ", Edit: " + canEdit);
                
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
        if (exportLogButton != null) {
            exportLogButton.setVisible(false);
            exportLogButton.setManaged(false);
        }
        if (searchButton != null) {
            searchButton.setVisible(false);
            searchButton.setManaged(false);
        }
        if (clearLogButton != null) {
            clearLogButton.setVisible(false);
            clearLogButton.setManaged(false);
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
            
            // Create security_events table if it doesn't exist
            createSecurityEventTableIfNotExists();
            
            logger.info("Database connection established for SecurityMaintenance");
        } catch (ClassNotFoundException | SQLException e) {
            logger.log(Level.SEVERE, "Database connection failed", e);
            showAlert(Alert.AlertType.ERROR, "Database Error", "Cannot connect to database: " + e.getMessage());
        }
    }
    
    private void createSecurityEventTableIfNotExists() throws SQLException {
        String createTableSQL = """
            CREATE TABLE IF NOT EXISTS security_events (
                event_id INT(11) AUTO_INCREMENT PRIMARY KEY,
                timestamp DATETIME NOT NULL,
                event_type VARCHAR(100) NOT NULL,
                severity VARCHAR(20) NOT NULL,
                username VARCHAR(100) NULL,
                description TEXT NOT NULL,
                ip_address VARCHAR(45) NULL,
                user_agent TEXT NULL,
                event_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                
                INDEX idx_timestamp (timestamp),
                INDEX idx_event_type (event_type),
                INDEX idx_severity (severity),
                INDEX idx_username (username),
                INDEX idx_event_status (event_status)
            )
        """;
        
        try (PreparedStatement stmt = connection.prepareStatement(createTableSQL)) {
            stmt.executeUpdate();
            logger.info("Security events table verified/created");
            
            // Check if table is empty and add sample data
            insertSampleDataIfEmpty();
        }
    }
    
    private void insertSampleDataIfEmpty() throws SQLException {
        // Check if table has data
        String countQuery = "SELECT COUNT(*) FROM security_events";
        try (PreparedStatement countStmt = connection.prepareStatement(countQuery);
             ResultSet rs = countStmt.executeQuery()) {
            
            if (rs.next() && rs.getInt(1) == 0) {
                // Table is empty, insert sample data
                String insertQuery = """
                    INSERT INTO security_events (timestamp, event_type, severity, username, description, ip_address, event_status) VALUES
                    (NOW() - INTERVAL 1 HOUR, 'LOGIN_SUCCESS', 'LOW', 'admin', 'Administrator logged in successfully', '192.168.1.100', 'ACTIVE'),
                    (NOW() - INTERVAL 2 HOUR, 'LOGIN_FAILED', 'MEDIUM', 'user1', 'Failed login attempt - incorrect password', '192.168.1.101', 'ACTIVE'),
                    (NOW() - INTERVAL 3 HOUR, 'PASSWORD_CHANGE', 'LOW', 'admin', 'Password changed successfully', '192.168.1.100', 'ACTIVE'),
                    (NOW() - INTERVAL 4 HOUR, 'LOGIN_FAILED', 'MEDIUM', 'user2', 'Failed login attempt - account not found', '192.168.1.102', 'ACTIVE'),
                    (NOW() - INTERVAL 5 HOUR, 'LOGIN_FAILED', 'HIGH', 'user2', 'Multiple failed login attempts detected', '192.168.1.102', 'ACTIVE'),
                    (NOW() - INTERVAL 6 HOUR, 'PASSWORD_RESET', 'MEDIUM', 'admin', 'Password reset for user: user1', '192.168.1.100', 'ACTIVE'),
                    (NOW() - INTERVAL 7 HOUR, 'LOGIN_SUCCESS', 'LOW', 'user1', 'User logged in successfully after password reset', '192.168.1.101', 'ACTIVE'),
                    (NOW() - INTERVAL 8 HOUR, 'INVALID_ACCESS', 'HIGH', 'guest', 'Attempt to access restricted area without authentication', '192.168.1.200', 'ACTIVE'),
                    (NOW() - INTERVAL 9 HOUR, 'USER_CREATED', 'LOW', 'admin', 'New user account created: user3', '192.168.1.100', 'ACTIVE'),
                    (NOW() - INTERVAL 10 HOUR, 'PASSWORD_POLICY_UPDATE', 'MEDIUM', 'admin', 'Password policy settings updated', '192.168.1.100', 'ACTIVE')
                """;
                
                try (PreparedStatement insertStmt = connection.prepareStatement(insertQuery)) {
                    insertStmt.executeUpdate();
                    logger.info("Sample security events data inserted");
                }
            }
        }
    }

    // COMMENTED OUT - Password Policy Methods
    /*
    private void setupPasswordPolicySpinners() {
        minPasswordLength.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(4, 50, 8));
        passwordExpiration.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 365, 90));
        passwordHistory.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 20, 5));
    }
    */

    private void setupSecurityEventTable() {
        colTimestamp.setCellValueFactory(new PropertyValueFactory<>("timestampFormatted"));
        colEventType.setCellValueFactory(new PropertyValueFactory<>("eventType"));
        colSeverity.setCellValueFactory(new PropertyValueFactory<>("severity"));
        colUser.setCellValueFactory(new PropertyValueFactory<>("username"));
        colDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
        colIPAddress.setCellValueFactory(new PropertyValueFactory<>("ipAddress"));
        colEventStatus.setCellValueFactory(new PropertyValueFactory<>("eventStatus"));
        
        securityEventTable.setItems(securityEvents);
        
        // Set auto-resize policy
        securityEventTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }
    
    private void setupEventFilters() {
        // Event Types
        eventTypeFilter.getItems().addAll(
            "All Events", "LOGIN_SUCCESS", "LOGIN_FAILED", "LOGOUT", 
            "PASSWORD_CHANGE", "PASSWORD_RESET", "ACCOUNT_LOCKED", 
            "ACCOUNT_UNLOCKED", "USER_CREATED", "USER_UPDATED", 
            "INVALID_ACCESS", "SUSPICIOUS_ACTIVITY"
        );
        eventTypeFilter.setValue("All Events");

        // Severity Levels
        severityFilter.getItems().addAll("All Severity", "LOW", "MEDIUM", "HIGH", "CRITICAL");
        severityFilter.setValue("All Severity");
        
        // Add listeners for filter changes
        eventTypeFilter.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.equals(oldVal)) {
                String currentUser = SessionManager.getInstance().getCurrentUser();
                if (currentUser != null) {
                    SecurityLogger.logSecurityEvent(
                        "SECURITY_MAINTENANCE_FILTER_EVENT_TYPE",
                        "LOW",
                        currentUser,
                        "Changed event type filter to: " + newVal
                    );
                }
            }
        });
        
        severityFilter.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.equals(oldVal)) {
                String currentUser = SessionManager.getInstance().getCurrentUser();
                if (currentUser != null) {
                    SecurityLogger.logSecurityEvent(
                        "SECURITY_MAINTENANCE_FILTER_SEVERITY",
                        "LOW",
                        currentUser,
                        "Changed severity filter to: " + newVal
                    );
                }
            }
        });
    }

    /*
    private void loadPasswordPolicySettings() {
        // Load from preferences or set defaults
        minPasswordLength.getValueFactory().setValue(preferences.getInt("minPasswordLength", 8));
        passwordExpiration.getValueFactory().setValue(preferences.getInt("passwordExpiration", 90));
        passwordHistory.getValueFactory().setValue(preferences.getInt("passwordHistory", 5));
        
        requireUppercase.setSelected(preferences.getBoolean("requireUppercase", true));
        requireLowercase.setSelected(preferences.getBoolean("requireLowercase", true));
        requireNumbers.setSelected(preferences.getBoolean("requireNumbers", true));
        requireSpecialChars.setSelected(preferences.getBoolean("requireSpecialChars", true));
    }
    */

    private void loadSecurityEvents() {
        try {
            String query = """
                SELECT event_id, timestamp, event_type, severity, username, 
                       description, ip_address, event_status 
                FROM security_events 
                ORDER BY timestamp DESC 
                LIMIT 1000
            """;
            
            PreparedStatement stmt = connection.prepareStatement(query);
            ResultSet rs = stmt.executeQuery();
            
            securityEvents.clear();
            while (rs.next()) {
                SecurityEvent event = new SecurityEvent(
                    rs.getInt("event_id"),
                    rs.getTimestamp("timestamp").toLocalDateTime(),
                    rs.getString("event_type"),
                    rs.getString("severity"),
                    rs.getString("username"),
                    rs.getString("description"),
                    rs.getString("ip_address"),
                    rs.getString("event_status")
                );
                securityEvents.add(event);
            }
            
            logger.info("Loaded " + securityEvents.size() + " security events");
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error loading security events", e);
            showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to load security events: " + e.getMessage());
        }
    }

    // Password Policy Actions - COMMENTED OUT
    /*
    @FXML
    private void onSavePasswordPolicy() {
        try {
            // Save to preferences
            preferences.putInt("minPasswordLength", minPasswordLength.getValue());
            preferences.putInt("passwordExpiration", passwordExpiration.getValue());
            preferences.putInt("passwordHistory", passwordHistory.getValue());
            preferences.putBoolean("requireUppercase", requireUppercase.isSelected());
            preferences.putBoolean("requireLowercase", requireLowercase.isSelected());
            preferences.putBoolean("requireNumbers", requireNumbers.isSelected());
            preferences.putBoolean("requireSpecialChars", requireSpecialChars.isSelected());
            
            // Log the event
            logSecurityEvent("PASSWORD_POLICY_UPDATE", "MEDIUM", 
                SessionManager.getInstance().getCurrentUser(),
                "Password policy settings updated", getClientIP());
            
            showAlert(Alert.AlertType.INFORMATION, "Success", "Password policy saved successfully!");
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error saving password policy", e);
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to save password policy: " + e.getMessage());
        }
    }

    @FXML
    private void onResetPasswordPolicy() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Reset Password Policy");
        confirm.setHeaderText(null);
        confirm.setContentText("Reset password policy to default settings?");
        Optional<ButtonType> result = confirm.showAndWait();
        
        if (result.isPresent() && result.get() == ButtonType.OK) {
            // Reset to defaults
            minPasswordLength.getValueFactory().setValue(8);
            passwordExpiration.getValueFactory().setValue(90);
            passwordHistory.getValueFactory().setValue(5);
            requireUppercase.setSelected(true);
            requireLowercase.setSelected(true);
            requireNumbers.setSelected(true);
            requireSpecialChars.setSelected(true);
            
            // Log the event
            logSecurityEvent("PASSWORD_POLICY_RESET", "LOW", 
                SessionManager.getInstance().getCurrentUser(),
                "Password policy reset to default settings", getClientIP());
            
            showAlert(Alert.AlertType.INFORMATION, "Success", "Password policy reset to default settings!");
        }
    }
    */

    // Security Monitoring Actions
    @FXML
    private void onSearchSecurityEvents() {
        String searchText = searchField.getText() != null ? searchField.getText().trim() : "";
        String eventType = eventTypeFilter.getValue();
        String severity = severityFilter.getValue();
        
        // Log search activity
        String currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser != null) {
            SecurityLogger.logSecurityEvent(
                "SECURITY_MAINTENANCE_SEARCH",
                "LOW",
                currentUser,
                "Searched security events - Keyword: '" + (searchText.isEmpty() ? "(none)" : searchText) + 
                "', Event Type: " + (eventType != null ? eventType : "All") + 
                ", Severity: " + (severity != null ? severity : "All") + 
                "'"
            );
        }
        
        try {
            
            StringBuilder query = new StringBuilder("""
                SELECT event_id, timestamp, event_type, severity, username, 
                       description, ip_address, event_status 
                FROM security_events WHERE 1=1
            """);
            
            // Build dynamic query
            if (!searchText.isEmpty()) {
                query.append(" AND (description LIKE ? OR username LIKE ? OR ip_address LIKE ?)");
            }
            if (eventType != null && !eventType.equals("All Events")) {
                query.append(" AND event_type = ?");
            }
            if (severity != null && !severity.equals("All Severity")) {
                query.append(" AND severity = ?");
            }
            query.append(" ORDER BY timestamp DESC LIMIT 1000");
            
            PreparedStatement stmt = connection.prepareStatement(query.toString());
            int paramIndex = 1;
            
            if (!searchText.isEmpty()) {
                String searchPattern = "%" + searchText + "%";
                stmt.setString(paramIndex++, searchPattern);
                stmt.setString(paramIndex++, searchPattern);
                stmt.setString(paramIndex++, searchPattern);
            }
            if (eventType != null && !eventType.equals("All Events")) {
                stmt.setString(paramIndex++, eventType);
            }
            if (severity != null && !severity.equals("All Severity")) {
                stmt.setString(paramIndex++, severity);
            }
            
            ResultSet rs = stmt.executeQuery();
            
            securityEvents.clear();
            while (rs.next()) {
                SecurityEvent event = new SecurityEvent(
                    rs.getInt("event_id"),
                    rs.getTimestamp("timestamp").toLocalDateTime(),
                    rs.getString("event_type"),
                    rs.getString("severity"),
                    rs.getString("username"),
                    rs.getString("description"),
                    rs.getString("ip_address"),
                    rs.getString("event_status")
                );
                securityEvents.add(event);
            }
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error searching security events", e);
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to search security events: " + e.getMessage());
        }
    }

    @FXML
    private void onClearSecurityLog() {
        // Log clear log click
        String currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser != null) {
            SecurityLogger.logSecurityEvent(
                "SECURITY_LOG_CLEAR_CLICK",
                "HIGH",
                currentUser,
                "Clicked Clear Security Log button"
            );
        }
        
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Clear Security Log");
        confirm.setHeaderText(null);
        confirm.setContentText("Clear all security event logs? This action cannot be undone.");
        Optional<ButtonType> result = confirm.showAndWait();
        
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                String deleteQuery = "DELETE FROM security_events";
                PreparedStatement stmt = connection.prepareStatement(deleteQuery);
                int deletedRows = stmt.executeUpdate();
                
                securityEvents.clear();
                
                // Log this action (using SecurityLogger for consistency)
                String currentUser1 = SessionManager.getInstance().getCurrentUser();
                if (currentUser1 != null) {
                    SecurityLogger.logSecurityEvent(
                        "SECURITY_LOG_CLEARED",
                        "HIGH",
                        currentUser1,
                        "Security log cleared - " + deletedRows + " events deleted"
                    );
                }
                
                showAlert(Alert.AlertType.INFORMATION, "Success", 
                    "Security log cleared successfully! " + deletedRows + " events deleted.");
                
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Error clearing security log", e);
                
                // Log failed clear attempt
                String currentUser2 = SessionManager.getInstance().getCurrentUser();
                if (currentUser2 != null) {
                    SecurityLogger.logSecurityEvent(
                        "SECURITY_LOG_CLEAR_FAILED",
                        "HIGH",
                        currentUser2,
                        "Failed to clear security log - Error: " + e.getMessage()
                    );
                }
                
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to clear security log: " + e.getMessage());
            }
        } else {
            // Log clear cancellation
            String currentUser3 = SessionManager.getInstance().getCurrentUser();
            if (currentUser3 != null) {
                SecurityLogger.logSecurityEvent(
                    "SECURITY_LOG_CLEAR_CANCELLED",
                    "MEDIUM",
                    currentUser3,
                    "Cancelled clearing security log"
                );
            }
        }
    }

    @FXML
    private void onRefresh() {
        // Log refresh click
        String currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser != null) {
            SecurityLogger.logSecurityEvent(
                "SECURITY_MAINTENANCE_REFRESH",
                "LOW",
                currentUser,
                "Refreshed security events list"
            );
        }
        
        loadSecurityEvents();
        showAlert(Alert.AlertType.INFORMATION, "Success", "Security events refreshed successfully!");
    }

    @FXML
    private void onExportSecurityLog() {
        // Log export click
        String currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser != null) {
            SecurityLogger.logSecurityEvent(
                "SECURITY_LOG_EXPORT_CLICK",
                "MEDIUM",
                currentUser,
                "Clicked Export Security Log button"
            );
        }
        
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Security Log");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        fileChooser.setInitialFileName("security_log_" + 
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".csv");
        
        File file = fileChooser.showSaveDialog(null);
        if (file != null) {
            Task<Void> exportTask = new Task<Void>() {
                @Override
                protected Void call() throws Exception {
                    try (FileWriter writer = new FileWriter(file)) {
                        writer.append("Timestamp,Event Type,Severity,User,Description,IP Address,Status\n");
                        for (SecurityEvent event : securityEvents) {
                            writer.append(String.format("%s,%s,%s,%s,\"%s\",%s,%s\n",
                                event.getTimestampFormatted(),
                                event.getEventType(),
                                event.getSeverity(),
                                event.getUsername(),
                                event.getDescription().replace("\"", "\"\""), // Escape quotes
                                event.getIpAddress(),
                                event.getEventStatus()
                            ));
                        }
                    }
                    return null;
                }
            };

            exportTask.setOnSucceeded(e -> {
                Platform.runLater(() -> {
                    // Log successful export (using SecurityLogger for consistency)
                    String currentUser4 = SessionManager.getInstance().getCurrentUser();
                    if (currentUser4 != null) {
                        SecurityLogger.logSecurityEvent(
                            "SECURITY_LOG_EXPORT_SUCCESS",
                            "MEDIUM",
                            currentUser4,
                            "Security log exported successfully to: " + file.getName() + 
                            " (" + securityEvents.size() + " events)"
                        );
                    }
                    
                    showAlert(Alert.AlertType.INFORMATION, "Success", 
                        "Security log exported successfully to: " + file.getName());
                });
            });

            exportTask.setOnFailed(e -> {
                Platform.runLater(() -> {
                    // Log failed export
                    String currentUser5 = SessionManager.getInstance().getCurrentUser();
                    if (currentUser5 != null) {
                        SecurityLogger.logSecurityEvent(
                            "SECURITY_LOG_EXPORT_FAILED",
                            "MEDIUM",
                            currentUser5,
                            "Failed to export security log - Error: " + 
                            (exportTask.getException() != null ? exportTask.getException().getMessage() : "Unknown error")
                        );
                    }
                    
                    showAlert(Alert.AlertType.ERROR, "Error", "Failed to export security log!");
                });
            });

            new Thread(exportTask).start();
        }
    }

    // Helper Methods
    public void logSecurityEvent(String eventType, String severity, String username, 
                                String description, String ipAddress) {
        try {
            String insertQuery = """
                INSERT INTO security_events (timestamp, event_type, severity, username, 
                                           description, ip_address, event_status) 
                VALUES (?, ?, ?, ?, ?, ?, 'ACTIVE')
            """;
            
            PreparedStatement stmt = connection.prepareStatement(insertQuery);
            stmt.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setString(2, eventType);
            stmt.setString(3, severity);
            stmt.setString(4, username);
            stmt.setString(5, description);
            stmt.setString(6, ipAddress);
            
            stmt.executeUpdate();
            
            // Add to current list for immediate display
            Platform.runLater(() -> {
                SecurityEvent event = new SecurityEvent(
                    0, // ID will be set by database
                    LocalDateTime.now(),
                    eventType,
                    severity,
                    username,
                    description,
                    ipAddress,
                    "ACTIVE"
                );
                securityEvents.add(0, event); // Add to top of list
            });
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error logging security event", e);
        }
    }
    
    private String getClientIP() {
        // In a real application, you'd get the actual client IP
        return "127.0.0.1";
    }

    // Password validation method for use by other controllers
    public boolean validatePassword(String password) {
        int minLength = preferences.getInt("minPasswordLength", 8);
        boolean needsUppercase = preferences.getBoolean("requireUppercase", true);
        boolean needsLowercase = preferences.getBoolean("requireLowercase", true);
        boolean needsNumbers = preferences.getBoolean("requireNumbers", true);
        boolean needsSpecialChars = preferences.getBoolean("requireSpecialChars", true);
        
        if (password.length() < minLength) {
            return false;
        }
        
        if (needsUppercase && !password.matches(".*[A-Z].*")) {
            return false;
        }
        
        if (needsLowercase && !password.matches(".*[a-z].*")) {
            return false;
        }
        
        if (needsNumbers && !password.matches(".*[0-9].*")) {
            return false;
        }
        
        if (needsSpecialChars && !password.matches(".*[^A-Za-z0-9].*")) {
            return false;
        }
        
        return true;
    }
    
    public String getPasswordRequirements() {
        StringBuilder requirements = new StringBuilder();
        
        requirements.append("Password must contain:\n");
        requirements.append("• At least ").append(preferences.getInt("minPasswordLength", 8)).append(" characters\n");
        
        if (preferences.getBoolean("requireUppercase", true)) {
            requirements.append("• At least one uppercase letter (A-Z)\n");
        }
        if (preferences.getBoolean("requireLowercase", true)) {
            requirements.append("• At least one lowercase letter (a-z)\n");
        }
        if (preferences.getBoolean("requireNumbers", true)) {
            requirements.append("• At least one number (0-9)\n");
        }
        if (preferences.getBoolean("requireSpecialChars", true)) {
            requirements.append("• At least one special character (!@#$%^&*)\n");
        }
        
        return requirements.toString();
    }

    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle("Security Maintenance");
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    public void closeDatabaseConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                logger.info("Database connection closed for SecurityMaintenance");
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error closing database connection", e);
        }
    }

    // SecurityEvent data class
    public static class SecurityEvent {
        private final int eventId;
        private final LocalDateTime timestamp;
        private final String eventType;
        private final String severity;
        private final String username;
        private final String description;
        private final String ipAddress;
        private final String eventStatus;

        public SecurityEvent(int eventId, LocalDateTime timestamp, String eventType, String severity, 
                           String username, String description, String ipAddress, String eventStatus) {
            this.eventId = eventId;
            this.timestamp = timestamp;
            this.eventType = eventType;
            this.severity = severity;
            this.username = username;
            this.description = description;
            this.ipAddress = ipAddress;
            this.eventStatus = eventStatus;
        }

        // Getters
        public int getEventId() { return eventId; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public String getTimestampFormatted() { 
            return timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")); 
        }
        public String getEventType() { return eventType; }
        public String getSeverity() { return severity; }
        public String getUsername() { return username; }
        public String getDescription() { return description; }
        public String getIpAddress() { return ipAddress; }
        public String getEventStatus() { return eventStatus; }
    }
}