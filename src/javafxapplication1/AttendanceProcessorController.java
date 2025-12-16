package javafxapplication1;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.fxml.Initializable;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.TableView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TextField;
import javafx.scene.control.DatePicker;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.beans.property.SimpleStringProperty;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.math.BigDecimal;
import java.math.RoundingMode;
import javafx.scene.control.Button;

/**
 * Controller for Attendance Processing Engine
 * Converts raw attendance logs into processed attendance data with computations
 */
public class AttendanceProcessorController implements Initializable {
    
    @FXML private DatePicker dateFromPicker;
    @FXML private DatePicker dateToPicker;
    @FXML private TextField searchField;
    @FXML private TableView<AttendanceProcessRecord> table;
    @FXML private TableColumn<AttendanceProcessRecord, String> colEmployeeId;
    @FXML private TableColumn<AttendanceProcessRecord, String> colEmployeeName;
    @FXML private TableColumn<AttendanceProcessRecord, String> colProcessDate;
    @FXML private TableColumn<AttendanceProcessRecord, String> colLateMinutes;
    @FXML private TableColumn<AttendanceProcessRecord, String> colUndertimeMinutes;
    @FXML private TableColumn<AttendanceProcessRecord, String> colHoursWorked;
    @FXML private TableColumn<AttendanceProcessRecord, String> colAbsentDays;
    @FXML private TableColumn<AttendanceProcessRecord, String> colHalfDays;
    @FXML private TableColumn<AttendanceProcessRecord, String> colOvertimeHours;
    @FXML private TableColumn<AttendanceProcessRecord, String> colStatus;
    
    // Action buttons
    @FXML private Button processAllButton;
    @FXML private Button processDateRangeButton;
    @FXML private Button viewDetailsButton;
    
    private ObservableList<AttendanceProcessRecord> processedData = FXCollections.observableArrayList();
    private Connection connection;
    private static final Logger logger = Logger.getLogger(AttendanceProcessorController.class.getName());
    
    // Database connection details
    private static final String DB_URL = DatabaseConfig.getDbUrl();
    private static final String DB_USER = DatabaseConfig.getDbUser();
    private static final String DB_PASSWORD = DatabaseConfig.getDbPassword();
    
    // Work schedule constants (should be configurable in future)
    private static final LocalTime WORK_START_TIME = LocalTime.of(8, 0); // 8:00 AM
    private static final LocalTime WORK_END_TIME = LocalTime.of(17, 0);   // 5:00 PM
    private static final int GRACE_PERIOD_MINUTES = 10;
    private static final int LUNCH_BREAK_MINUTES = 60;
    private static final double STANDARD_WORK_HOURS = 8.0;
    
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupTableColumns();
        initializeDatabase();
        setupDatePickers();
        loadProcessedAttendanceData();
        
        // Setup permission-based button visibility
        setupPermissionBasedVisibility();
        
        // Log module access
        String currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser != null) {
            SecurityLogger.logSecurityEvent(
                "ATTENDANCE_PROCESSOR_MODULE_ACCESS",
                "LOW",
                currentUser,
                "Accessed Attendance Processor module"
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
                boolean canView = hasUserPermission(currentUser, "attendance_process.view");
                boolean canEdit = hasUserPermission(currentUser, "attendance_process.edit");
                boolean canAdd = hasUserPermission(currentUser, "attendance_process.add");
                
                // Processing buttons require edit or add permission (they create/modify data)
                boolean canProcess = canEdit || canAdd;
                
                // Show/hide buttons based on permissions
                if (processAllButton != null) {
                    processAllButton.setVisible(canProcess);
                    processAllButton.setManaged(canProcess);
                }
                if (processDateRangeButton != null) {
                    processDateRangeButton.setVisible(canProcess);
                    processDateRangeButton.setManaged(canProcess);
                }
                if (viewDetailsButton != null) {
                    viewDetailsButton.setVisible(canView);
                    viewDetailsButton.setManaged(canView);
                }
                
                logger.info("Attendance Processor buttons visibility - View: " + canView + ", Process (Edit/Add): " + canProcess);
                
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
        if (processAllButton != null) {
            processAllButton.setVisible(false);
            processAllButton.setManaged(false);
        }
        if (processDateRangeButton != null) {
            processDateRangeButton.setVisible(false);
            processDateRangeButton.setManaged(false);
        }
        if (viewDetailsButton != null) {
            viewDetailsButton.setVisible(false);
            viewDetailsButton.setManaged(false);
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
    
    private void setupTableColumns() {
        colEmployeeId.setCellValueFactory(new PropertyValueFactory<>("employeeId"));
        colEmployeeName.setCellValueFactory(new PropertyValueFactory<>("employeeName"));
        colProcessDate.setCellValueFactory(new PropertyValueFactory<>("processDate"));
        colLateMinutes.setCellValueFactory(new PropertyValueFactory<>("lateMinutes"));
        colUndertimeMinutes.setCellValueFactory(new PropertyValueFactory<>("undertimeMinutes"));
        colHoursWorked.setCellValueFactory(new PropertyValueFactory<>("hoursWorked"));
        colAbsentDays.setCellValueFactory(new PropertyValueFactory<>("absentDays"));
        colHalfDays.setCellValueFactory(new PropertyValueFactory<>("halfDays"));
        colOvertimeHours.setCellValueFactory(new PropertyValueFactory<>("overtimeHours"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        
        table.setItems(processedData);
    }
    
    private void setupDatePickers() {
        // Set default date range to current month
        LocalDate now = LocalDate.now();
        dateFromPicker.setValue(now.withDayOfMonth(1)); // First day of current month
        dateToPicker.setValue(now); // Today
    }
    
    private void initializeDatabase() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            logger.info("Database connection established successfully");
            
            // Create processed attendance table if it doesn't exist
            createProcessedAttendanceTableIfNotExists();
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error initializing database connection", e);
            showAlert("Database Error", "Could not connect to database: " + e.getMessage());
        }
    }
    
    private void createProcessedAttendanceTableIfNotExists() {
        String createTableSQL = """
            CREATE TABLE IF NOT EXISTS processed_attendance (
                id INT AUTO_INCREMENT PRIMARY KEY,
                employee_id INT NOT NULL,
                process_date DATE NOT NULL,
                late_minutes INT DEFAULT 0,
                undertime_minutes INT DEFAULT 0,
                hours_worked DECIMAL(4,2) DEFAULT 0.00,
                absent_days INT DEFAULT 0,
                half_days INT DEFAULT 0,
                overtime_hours DECIMAL(4,2) DEFAULT 0.00,
                status ENUM('Present', 'Absent', 'Half-Day', 'Late', 'Undertime', 'Complete') DEFAULT 'Present',
                raw_time_in TIME NULL,
                raw_time_out TIME NULL,
                processed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                processed_by VARCHAR(50) NULL,
                FOREIGN KEY (employee_id) REFERENCES employees(id),
                UNIQUE KEY unique_employee_date (employee_id, process_date),
                INDEX idx_process_date (process_date),
                INDEX idx_employee_id (employee_id)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """;
        
        try (PreparedStatement stmt = connection.prepareStatement(createTableSQL)) {
            stmt.executeUpdate();
            logger.info("Processed attendance table created or already exists");
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error creating processed attendance table", e);
        }
    }
    
    @FXML
    private void onProcessAll() {
        // Log process all click
        String currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser != null) {
            SecurityLogger.logSecurityEvent(
                "ATTENDANCE_PROCESSOR_PROCESS_ALL_CLICK",
                "HIGH",
                currentUser,
                "Clicked Process All Attendance button"
            );
        }
        
        Alert confirmAlert = new Alert(AlertType.CONFIRMATION);
        confirmAlert.setTitle("Process All Attendance");
        confirmAlert.setHeaderText("Process All Attendance Records");
        confirmAlert.setContentText("This will process all unprocessed attendance logs and generate computations. Continue?");
        
        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == javafx.scene.control.ButtonType.OK) {
                int processedCount = processAllAttendance();
                
                // Log successful processing
                if (currentUser != null) {
                    SecurityLogger.logSecurityEvent(
                        "ATTENDANCE_PROCESSOR_PROCESS_ALL_SUCCESS",
                        "HIGH",
                        currentUser,
                        "Successfully processed all attendance records - Processed " + processedCount + " records"
                    );
                }
                
                loadProcessedAttendanceData();
            } else {
                // Log process all cancelled
                if (currentUser != null) {
                    SecurityLogger.logSecurityEvent(
                        "ATTENDANCE_PROCESSOR_PROCESS_ALL_CANCELLED",
                        "MEDIUM",
                        currentUser,
                        "Cancelled processing all attendance records"
                    );
                }
            }
        });
    }
    
    @FXML
    private void onProcessDateRange() {
        if (dateFromPicker.getValue() == null || dateToPicker.getValue() == null) {
            showAlert("Invalid Date Range", "Please select both From and To dates");
            
            // Log failed process date range attempt
            String currentUser = SessionManager.getInstance().getCurrentUser();
            if (currentUser != null) {
                SecurityLogger.logSecurityEvent(
                    "ATTENDANCE_PROCESSOR_PROCESS_DATE_RANGE_FAILED",
                    "MEDIUM",
                    currentUser,
                    "Attempted to process date range but dates were not selected"
                );
            }
            return;
        }
        
        if (dateFromPicker.getValue().isAfter(dateToPicker.getValue())) {
            showAlert("Invalid Date Range", "From date cannot be after To date");
            
            // Log invalid date range
            String currentUser = SessionManager.getInstance().getCurrentUser();
            if (currentUser != null) {
                SecurityLogger.logSecurityEvent(
                    "ATTENDANCE_PROCESSOR_PROCESS_DATE_RANGE_FAILED",
                    "MEDIUM",
                    currentUser,
                    "Attempted to process date range but From date (" + dateFromPicker.getValue() + 
                    ") is after To date (" + dateToPicker.getValue() + ")"
                );
            }
            return;
        }
        
        // Log process date range click
        String currentUser = SessionManager.getInstance().getCurrentUser();
        LocalDate fromDate = dateFromPicker.getValue();
        LocalDate toDate = dateToPicker.getValue();
        
        if (currentUser != null) {
            SecurityLogger.logSecurityEvent(
                "ATTENDANCE_PROCESSOR_PROCESS_DATE_RANGE_CLICK",
                "HIGH",
                currentUser,
                "Clicked Process Date Range button - From: " + fromDate + ", To: " + toDate
            );
        }
        
        int processedCount = processAttendanceForDateRange(fromDate, toDate);
        
        // Log successful processing
        if (currentUser != null) {
            SecurityLogger.logSecurityEvent(
                "ATTENDANCE_PROCESSOR_PROCESS_DATE_RANGE_SUCCESS",
                "HIGH",
                currentUser,
                "Successfully processed attendance for date range - From: " + fromDate + 
                ", To: " + toDate + ", Processed " + processedCount + " records"
            );
        }
        
        loadProcessedAttendanceData();
    }
    
    private int processAllAttendance() {
        // Get all unprocessed attendance logs
        String selectUnprocessedSQL = """
            SELECT DISTINCT al.account_number, e.id as employee_id, DATE(al.log_datetime) as attendance_date,
                   MIN(al.log_datetime) as time_in,
                   MAX(al.log_datetime) as time_out,
                   e.full_name as employee_name
            FROM attendance al
            JOIN employees e ON al.account_number = e.account_number
            LEFT JOIN processed_attendance pa ON e.id = pa.employee_id 
                AND DATE(al.log_datetime) = pa.process_date
            WHERE pa.id IS NULL
            GROUP BY al.account_number, e.id, DATE(al.log_datetime)
            ORDER BY attendance_date, al.account_number
            """;
        
        try (PreparedStatement stmt = connection.prepareStatement(selectUnprocessedSQL)) {
            ResultSet rs = stmt.executeQuery();
            int processedCount = 0;
            
            while (rs.next()) {
                int employeeId = rs.getInt("employee_id");
                Date attendanceDate = rs.getDate("attendance_date");
                Timestamp timeIn = rs.getTimestamp("time_in");
                Timestamp timeOut = rs.getTimestamp("time_out");
                String employeeName = rs.getString("employee_name");
                
                processAttendanceRecord(employeeId, attendanceDate.toLocalDate(), timeIn, timeOut);
                processedCount++;
            }
            
            showAlert("Success", "Processed " + processedCount + " attendance records!");
            logger.info("Processed " + processedCount + " attendance records");
            
            return processedCount;
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error processing attendance records", e);
            
            // Log processing error
            String currentUser = SessionManager.getInstance().getCurrentUser();
            if (currentUser != null) {
                SecurityLogger.logSecurityEvent(
                    "ATTENDANCE_PROCESSOR_PROCESS_ALL_FAILED",
                    "HIGH",
                    currentUser,
                    "Failed to process all attendance records - Error: " + e.getMessage()
                );
            }
            
            showAlert("Database Error", "Could not process attendance records: " + e.getMessage());
            return 0;
        }
    }
    
    private int processAttendanceForDateRange(LocalDate fromDate, LocalDate toDate) {
        String selectRangeSQL = """
            SELECT DISTINCT al.account_number, e.id as employee_id, DATE(al.log_datetime) as attendance_date,
                   MIN(al.log_datetime) as time_in,
                   MAX(al.log_datetime) as time_out,
                   e.full_name as employee_name
            FROM attendance al
            JOIN employees e ON al.account_number = e.account_number
            WHERE DATE(al.log_datetime) BETWEEN ? AND ?
            GROUP BY al.account_number, e.id, DATE(al.log_datetime)
            ORDER BY attendance_date, al.account_number
            """;
        
        try (PreparedStatement stmt = connection.prepareStatement(selectRangeSQL)) {
            stmt.setDate(1, Date.valueOf(fromDate));
            stmt.setDate(2, Date.valueOf(toDate));
            
            ResultSet rs = stmt.executeQuery();
            int processedCount = 0;
            
            // First, delete existing processed records for the date range
            deleteProcessedRecordsForDateRange(fromDate, toDate);
            
            while (rs.next()) {
                int employeeId = rs.getInt("employee_id");
                Date attendanceDate = rs.getDate("attendance_date");
                Timestamp timeIn = rs.getTimestamp("time_in");
                Timestamp timeOut = rs.getTimestamp("time_out");
                
                processAttendanceRecord(employeeId, attendanceDate.toLocalDate(), timeIn, timeOut);
                processedCount++;
            }
            
            showAlert("Success", "Processed " + processedCount + " attendance records for selected date range!");
            logger.info("Processed " + processedCount + " attendance records for date range: " + fromDate + " to " + toDate);
            
            return processedCount;
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error processing attendance for date range", e);
            
            // Log processing error
            String currentUser = SessionManager.getInstance().getCurrentUser();
            if (currentUser != null) {
                SecurityLogger.logSecurityEvent(
                    "ATTENDANCE_PROCESSOR_PROCESS_DATE_RANGE_FAILED",
                    "HIGH",
                    currentUser,
                    "Failed to process attendance for date range - From: " + fromDate + 
                    ", To: " + toDate + ", Error: " + e.getMessage()
                );
            }
            
            showAlert("Database Error", "Could not process attendance for date range: " + e.getMessage());
            return 0;
        }
    }
    
    private void deleteProcessedRecordsForDateRange(LocalDate fromDate, LocalDate toDate) {
        String deleteSQL = "DELETE FROM processed_attendance WHERE process_date BETWEEN ? AND ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(deleteSQL)) {
            stmt.setDate(1, Date.valueOf(fromDate));
            stmt.setDate(2, Date.valueOf(toDate));
            int deletedCount = stmt.executeUpdate();
            logger.info("Deleted " + deletedCount + " existing processed records for date range");
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Error deleting existing processed records", e);
        }
    }
    
    private void processAttendanceRecord(int employeeId, LocalDate attendanceDate, Timestamp timeIn, Timestamp timeOut) {
        try {
            // Calculate attendance metrics
            AttendanceMetrics metrics = calculateAttendanceMetrics(timeIn, timeOut);
            
            // Insert processed record
            String insertSQL = """
                INSERT INTO processed_attendance 
                (employee_id, process_date, late_minutes, undertime_minutes, hours_worked, 
                 absent_days, half_days, overtime_hours, status, raw_time_in, raw_time_out, processed_by) 
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                late_minutes = VALUES(late_minutes),
                undertime_minutes = VALUES(undertime_minutes),
                hours_worked = VALUES(hours_worked),
                absent_days = VALUES(absent_days),
                half_days = VALUES(half_days),
                overtime_hours = VALUES(overtime_hours),
                status = VALUES(status),
                raw_time_in = VALUES(raw_time_in),
                raw_time_out = VALUES(raw_time_out),
                processed_at = CURRENT_TIMESTAMP,
                processed_by = VALUES(processed_by)
                """;
            
            try (PreparedStatement stmt = connection.prepareStatement(insertSQL)) {
                stmt.setInt(1, employeeId);
                stmt.setDate(2, Date.valueOf(attendanceDate));
                stmt.setInt(3, metrics.lateMinutes);
                stmt.setInt(4, metrics.undertimeMinutes);
                stmt.setBigDecimal(5, metrics.hoursWorked);
                stmt.setInt(6, metrics.absentDays);
                stmt.setInt(7, metrics.halfDays);
                stmt.setBigDecimal(8, metrics.overtimeHours);
                stmt.setString(9, metrics.status);
                stmt.setTime(10, timeIn != null ? Time.valueOf(timeIn.toLocalDateTime().toLocalTime()) : null);
                stmt.setTime(11, timeOut != null ? Time.valueOf(timeOut.toLocalDateTime().toLocalTime()) : null);
                stmt.setString(12, SessionManager.getInstance().getCurrentUser());
                
                stmt.executeUpdate();
            }
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error inserting processed attendance record", e);
        }
    }
    
    private AttendanceMetrics calculateAttendanceMetrics(Timestamp timeIn, Timestamp timeOut) {
        AttendanceMetrics metrics = new AttendanceMetrics();
        
        // Check if absent (no time in/out records)
        if (timeIn == null || timeOut == null) {
            metrics.absentDays = 1;
            metrics.status = "Absent";
            return metrics;
        }
        
        LocalTime actualTimeIn = timeIn.toLocalDateTime().toLocalTime();
        LocalTime actualTimeOut = timeOut.toLocalDateTime().toLocalTime();
        
        // Calculate late minutes
        if (actualTimeIn.isAfter(WORK_START_TIME.plusMinutes(GRACE_PERIOD_MINUTES))) {
            metrics.lateMinutes = (int) java.time.Duration.between(
                WORK_START_TIME.plusMinutes(GRACE_PERIOD_MINUTES), actualTimeIn).toMinutes();
        }
        
        // Calculate undertime minutes
        if (actualTimeOut.isBefore(WORK_END_TIME)) {
            metrics.undertimeMinutes = (int) java.time.Duration.between(actualTimeOut, WORK_END_TIME).toMinutes();
        }
        
        // Calculate total hours worked (excluding lunch break)
        long totalMinutesWorked = java.time.Duration.between(actualTimeIn, actualTimeOut).toMinutes();
        totalMinutesWorked = Math.max(0, totalMinutesWorked - LUNCH_BREAK_MINUTES); // Subtract lunch break
        
        metrics.hoursWorked = new BigDecimal(totalMinutesWorked).divide(new BigDecimal(60), 2, RoundingMode.HALF_UP);
        
        // Check for half day (less than 4 hours worked)
        if (metrics.hoursWorked.compareTo(new BigDecimal("4.0")) < 0) {
            metrics.halfDays = 1;
            metrics.status = "Half-Day";
        }
        
        // Calculate overtime (more than 8 hours worked)
        if (metrics.hoursWorked.compareTo(new BigDecimal(STANDARD_WORK_HOURS)) > 0) {
            metrics.overtimeHours = metrics.hoursWorked.subtract(new BigDecimal(STANDARD_WORK_HOURS));
        }
        
        // Determine final status
        if ("Half-Day".equals(metrics.status)) {
            // Already set
        } else if (metrics.lateMinutes > 0 && metrics.undertimeMinutes > 0) {
            metrics.status = "Late & Undertime";
        } else if (metrics.lateMinutes > 0) {
            metrics.status = "Late";
        } else if (metrics.undertimeMinutes > 0) {
            metrics.status = "Undertime";
        } else {
            metrics.status = "Complete";
        }
        
        return metrics;
    }
    
    @FXML
    private void onViewDetails() {
        AttendanceProcessRecord selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("No Selection", "Please select a processed attendance record to view details");
            
            // Log failed view details attempt
            String currentUser = SessionManager.getInstance().getCurrentUser();
            if (currentUser != null) {
                SecurityLogger.logSecurityEvent(
                    "ATTENDANCE_PROCESSOR_VIEW_DETAILS_FAILED",
                    "LOW",
                    currentUser,
                    "Attempted to view attendance details but no record was selected"
                );
            }
            return;
        }
        
        // Log view details click
        String currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser != null) {
            SecurityLogger.logSecurityEvent(
                "ATTENDANCE_PROCESSOR_VIEW_DETAILS",
                "LOW",
                currentUser,
                "Viewed attendance details - Employee: " + selected.getEmployeeName() + 
                " (ID: " + selected.getEmployeeId() + "), Date: " + selected.getProcessDate() + 
                ", Status: " + selected.getStatus()
            );
        }
        
        // Show detailed information
        String details = String.format("""
            Employee: %s (ID: %s)
            Date: %s
            Status: %s
            
            Time Details:
            • Late Minutes: %s
            • Undertime Minutes: %s
            • Hours Worked: %s
            • Overtime Hours: %s
            
            Attendance Summary:
            • Absent Days: %s
            • Half Days: %s
            """,
            selected.getEmployeeName(), selected.getEmployeeId(),
            selected.getProcessDate(), selected.getStatus(),
            selected.getLateMinutes(), selected.getUndertimeMinutes(),
            selected.getHoursWorked(), selected.getOvertimeHours(),
            selected.getAbsentDays(), selected.getHalfDays()
        );
        
        Alert detailAlert = new Alert(AlertType.INFORMATION);
        detailAlert.setTitle("Attendance Details");
        detailAlert.setHeaderText("Processed Attendance Details");
        detailAlert.setContentText(details);
        detailAlert.showAndWait();
    }
    
    private void loadProcessedAttendanceData() {
        String sql = """
            SELECT pa.*, e.full_name as employee_name
            FROM processed_attendance pa
            JOIN employees e ON pa.employee_id = e.id
            ORDER BY pa.process_date DESC, e.full_name
            """;
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            processedData.clear();
            
            while (rs.next()) {
                AttendanceProcessRecord record = new AttendanceProcessRecord(
                    String.valueOf(rs.getInt("employee_id")),
                    rs.getString("employee_name"),
                    rs.getDate("process_date").toString(),
                    String.valueOf(rs.getInt("late_minutes")),
                    String.valueOf(rs.getInt("undertime_minutes")),
                    String.format("%.2f", rs.getBigDecimal("hours_worked")),
                    String.valueOf(rs.getInt("absent_days")),
                    String.valueOf(rs.getInt("half_days")),
                    String.format("%.2f", rs.getBigDecimal("overtime_hours")),
                    rs.getString("status")
                );
                processedData.add(record);
            }
            
            logger.info("Loaded " + processedData.size() + " processed attendance records");
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error loading processed attendance data", e);
            showAlert("Database Error", "Could not load processed attendance data: " + e.getMessage());
        }
    }
    
    private void showAlert(String title, String content) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
    
    // Inner classes for data models
    private static class AttendanceMetrics {
        int lateMinutes = 0;
        int undertimeMinutes = 0;
        BigDecimal hoursWorked = BigDecimal.ZERO;
        int absentDays = 0;
        int halfDays = 0;
        BigDecimal overtimeHours = BigDecimal.ZERO;
        String status = "Present";
    }
    
    public static class AttendanceProcessRecord {
        private final SimpleStringProperty employeeId;
        private final SimpleStringProperty employeeName;
        private final SimpleStringProperty processDate;
        private final SimpleStringProperty lateMinutes;
        private final SimpleStringProperty undertimeMinutes;
        private final SimpleStringProperty hoursWorked;
        private final SimpleStringProperty absentDays;
        private final SimpleStringProperty halfDays;
        private final SimpleStringProperty overtimeHours;
        private final SimpleStringProperty status;
        
        public AttendanceProcessRecord(String employeeId, String employeeName, String processDate,
                                     String lateMinutes, String undertimeMinutes, String hoursWorked,
                                     String absentDays, String halfDays, String overtimeHours, String status) {
            this.employeeId = new SimpleStringProperty(employeeId);
            this.employeeName = new SimpleStringProperty(employeeName);
            this.processDate = new SimpleStringProperty(processDate);
            this.lateMinutes = new SimpleStringProperty(lateMinutes);
            this.undertimeMinutes = new SimpleStringProperty(undertimeMinutes);
            this.hoursWorked = new SimpleStringProperty(hoursWorked);
            this.absentDays = new SimpleStringProperty(absentDays);
            this.halfDays = new SimpleStringProperty(halfDays);
            this.overtimeHours = new SimpleStringProperty(overtimeHours);
            this.status = new SimpleStringProperty(status);
        }
        
        public String getEmployeeId() { return employeeId.get(); }
        public String getEmployeeName() { return employeeName.get(); }
        public String getProcessDate() { return processDate.get(); }
        public String getLateMinutes() { return lateMinutes.get(); }
        public String getUndertimeMinutes() { return undertimeMinutes.get(); }
        public String getHoursWorked() { return hoursWorked.get(); }
        public String getAbsentDays() { return absentDays.get(); }
        public String getHalfDays() { return halfDays.get(); }
        public String getOvertimeHours() { return overtimeHours.get(); }
        public String getStatus() { return status.get(); }
    }
}