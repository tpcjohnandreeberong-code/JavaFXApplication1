package javafxapplication1;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.fxml.Initializable;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.TextField;
import javafx.scene.control.ComboBox;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TabPane;
import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controller for System Settings Configuration
 * Manages working hours, payroll settings, and system configuration
 */
public class SystemSettingsController implements Initializable {
    
    // Working Hours Tab
    @FXML private TextField startTimeField;
    @FXML private TextField endTimeField;
    @FXML private Spinner<Integer> lunchBreakSpinner;
    @FXML private Spinner<Integer> gracePeriodSpinner;
    
    // Payroll Settings Tab
    @FXML private ComboBox<String> payPeriodCombo;
    @FXML private TextField defaultPagIbigField;
    @FXML private ComboBox<String> taxTypeCombo;
    @FXML private TextField otRateField;
    
    // System Settings Tab
    @FXML private TextField companyNameField;
    @FXML private TextField currencyField;
    @FXML private Spinner<Integer> workingDaysSpinner;
    @FXML private CheckBox enableOvertimeCheckBox;
    
    @FXML private TabPane settingsTabPane;
    
    private Connection connection;
    private static final Logger logger = Logger.getLogger(SystemSettingsController.class.getName());
    
    // Database connection details
    private static final String DB_URL = DatabaseConfig.getDbUrl();
    private static final String DB_USER = DatabaseConfig.getDbUser();
    private static final String DB_PASSWORD = DatabaseConfig.getDbPassword();
    
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        initializeDatabase();
        setupSpinners();
        setupComboBoxes();
        createSystemSettingsTableIfNotExists();
        loadSystemSettings();
    }
    
    private void setupSpinners() {
        // Lunch break spinner: 30-120 minutes, default 60
        lunchBreakSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(30, 120, 60));
        
        // Grace period spinner: 0-30 minutes, default 10
        gracePeriodSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 30, 10));
        
        // Working days spinner: 5-7 days, default 5
        workingDaysSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(5, 7, 5));
    }
    
    private void setupComboBoxes() {
        // Pay period options
        payPeriodCombo.getItems().addAll("Monthly", "Semi-Monthly", "Bi-Weekly", "Weekly");
        payPeriodCombo.setValue("Semi-Monthly");
        
        // Tax computation types
        taxTypeCombo.getItems().addAll("TRAIN Law 2025", "Previous Tax Table", "Fixed Rate");
        taxTypeCombo.setValue("TRAIN Law 2025");
    }
    
    private void initializeDatabase() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            logger.info("Database connection established successfully");
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error initializing database connection", e);
            showAlert("Database Error", "Could not connect to database: " + e.getMessage());
        }
    }
    
    private void createSystemSettingsTableIfNotExists() {
        String createTableSQL = """
            CREATE TABLE IF NOT EXISTS system_settings (
                id INT AUTO_INCREMENT PRIMARY KEY,
                setting_key VARCHAR(100) NOT NULL UNIQUE,
                setting_value TEXT NOT NULL,
                setting_type ENUM('TEXT', 'NUMBER', 'BOOLEAN', 'TIME') DEFAULT 'TEXT',
                description TEXT NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                updated_by VARCHAR(50) NULL,
                INDEX idx_setting_key (setting_key)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """;
        
        try (PreparedStatement stmt = connection.prepareStatement(createTableSQL)) {
            stmt.executeUpdate();
            logger.info("System settings table created or already exists");
            
            // Insert default settings if table is empty
            insertDefaultSettings();
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error creating system settings table", e);
        }
    }
    
    private void insertDefaultSettings() {
        // Check if settings already exist
        String checkSQL = "SELECT COUNT(*) FROM system_settings";
        
        try (PreparedStatement stmt = connection.prepareStatement(checkSQL)) {
            ResultSet rs = stmt.executeQuery();
            if (rs.next() && rs.getInt(1) > 0) {
                return; // Settings already exist
            }
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Error checking existing settings", e);
            return;
        }
        
        // Insert default settings
        String[][] defaultSettings = {
            {"work_start_time", "08:00", "TIME", "Work start time"},
            {"work_end_time", "17:00", "TIME", "Work end time"},
            {"lunch_break_minutes", "60", "NUMBER", "Lunch break duration in minutes"},
            {"grace_period_minutes", "10", "NUMBER", "Grace period for late arrival in minutes"},
            {"pay_period", "Semi-Monthly", "TEXT", "Pay period frequency"},
            {"default_pagibig", "200.00", "NUMBER", "Default Pag-IBIG contribution amount"},
            {"tax_computation", "TRAIN Law 2025", "TEXT", "Tax computation method"},
            {"overtime_rate", "1.25", "NUMBER", "Overtime rate multiplier"},
            {"company_name", "TPC Corporation", "TEXT", "Company name"},
            {"currency_symbol", "₱", "TEXT", "Currency symbol"},
            {"working_days_per_week", "5", "NUMBER", "Working days per week"},
            {"enable_overtime", "true", "BOOLEAN", "Enable overtime computation"}
        };
        
        String insertSQL = "INSERT INTO system_settings (setting_key, setting_value, setting_type, description, updated_by) VALUES (?, ?, ?, ?, ?)";
        
        try (PreparedStatement stmt = connection.prepareStatement(insertSQL)) {
            for (String[] setting : defaultSettings) {
                stmt.setString(1, setting[0]);
                stmt.setString(2, setting[1]);
                stmt.setString(3, setting[2]);
                stmt.setString(4, setting[3]);
                stmt.setString(5, "system");
                stmt.executeUpdate();
            }
            logger.info("Inserted default system settings");
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error inserting default settings", e);
        }
    }
    
    @FXML
    private void onSaveWorkingHours() {
        try {
            // Validate time format
            if (!isValidTimeFormat(startTimeField.getText()) || !isValidTimeFormat(endTimeField.getText())) {
                showAlert("Invalid Time", "Please enter time in HH:MM format (e.g., 08:00)");
                return;
            }
            
            updateSetting("work_start_time", startTimeField.getText());
            updateSetting("work_end_time", endTimeField.getText());
            updateSetting("lunch_break_minutes", String.valueOf(lunchBreakSpinner.getValue()));
            updateSetting("grace_period_minutes", String.valueOf(gracePeriodSpinner.getValue()));
            
            showAlert("Success", "Working hours settings saved successfully!");
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error saving working hours", e);
            showAlert("Error", "Could not save working hours settings: " + e.getMessage());
        }
    }
    
    @FXML
    private void onSavePayrollSettings() {
        try {
            // Validate numeric fields
            if (!isValidDecimal(defaultPagIbigField.getText()) || !isValidDecimal(otRateField.getText())) {
                showAlert("Invalid Input", "Please enter valid decimal numbers for Pag-IBIG and OT rate");
                return;
            }
            
            updateSetting("pay_period", payPeriodCombo.getValue());
            updateSetting("default_pagibig", defaultPagIbigField.getText());
            updateSetting("tax_computation", taxTypeCombo.getValue());
            updateSetting("overtime_rate", otRateField.getText());
            
            showAlert("Success", "Payroll settings saved successfully!");
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error saving payroll settings", e);
            showAlert("Error", "Could not save payroll settings: " + e.getMessage());
        }
    }
    
    @FXML
    private void onSaveSystemSettings() {
        try {
            updateSetting("company_name", companyNameField.getText());
            updateSetting("currency_symbol", currencyField.getText());
            updateSetting("working_days_per_week", String.valueOf(workingDaysSpinner.getValue()));
            updateSetting("enable_overtime", String.valueOf(enableOvertimeCheckBox.isSelected()));
            
            showAlert("Success", "System settings saved successfully!");
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error saving system settings", e);
            showAlert("Error", "Could not save system settings: " + e.getMessage());
        }
    }
    
    @FXML
    private void onSaveAll() {
        try {
            // Validate all inputs first
            if (!isValidTimeFormat(startTimeField.getText()) || !isValidTimeFormat(endTimeField.getText())) {
                showAlert("Invalid Time", "Please enter time in HH:MM format (e.g., 08:00)");
                return;
            }
            
            if (!isValidDecimal(defaultPagIbigField.getText()) || !isValidDecimal(otRateField.getText())) {
                showAlert("Invalid Input", "Please enter valid decimal numbers for Pag-IBIG and OT rate");
                return;
            }
            
            // Save all settings
            updateSetting("work_start_time", startTimeField.getText());
            updateSetting("work_end_time", endTimeField.getText());
            updateSetting("lunch_break_minutes", String.valueOf(lunchBreakSpinner.getValue()));
            updateSetting("grace_period_minutes", String.valueOf(gracePeriodSpinner.getValue()));
            updateSetting("pay_period", payPeriodCombo.getValue());
            updateSetting("default_pagibig", defaultPagIbigField.getText());
            updateSetting("tax_computation", taxTypeCombo.getValue());
            updateSetting("overtime_rate", otRateField.getText());
            updateSetting("company_name", companyNameField.getText());
            updateSetting("currency_symbol", currencyField.getText());
            updateSetting("working_days_per_week", String.valueOf(workingDaysSpinner.getValue()));
            updateSetting("enable_overtime", String.valueOf(enableOvertimeCheckBox.isSelected()));
            
            showAlert("Success", "All system settings saved successfully!");
            logger.info("All system settings saved by user: " + SessionManager.getInstance().getCurrentUser());
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error saving all settings", e);
            showAlert("Error", "Could not save all settings: " + e.getMessage());
        }
    }
    
    private void updateSetting(String key, String value) throws SQLException {
        String sql = "UPDATE system_settings SET setting_value = ?, updated_by = ?, updated_at = CURRENT_TIMESTAMP WHERE setting_key = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, value);
            stmt.setString(2, SessionManager.getInstance().getCurrentUser());
            stmt.setString(3, key);
            
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                // Setting doesn't exist, insert it
                insertSetting(key, value);
            }
        }
    }
    
    private void insertSetting(String key, String value) throws SQLException {
        String sql = "INSERT INTO system_settings (setting_key, setting_value, updated_by) VALUES (?, ?, ?)";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, key);
            stmt.setString(2, value);
            stmt.setString(3, SessionManager.getInstance().getCurrentUser());
            stmt.executeUpdate();
        }
    }
    
    private void loadSystemSettings() {
        String sql = "SELECT setting_key, setting_value FROM system_settings";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                String key = rs.getString("setting_key");
                String value = rs.getString("setting_value");
                
                // Load settings into UI components
                switch (key) {
                    case "work_start_time":
                        startTimeField.setText(value);
                        break;
                    case "work_end_time":
                        endTimeField.setText(value);
                        break;
                    case "lunch_break_minutes":
                        lunchBreakSpinner.getValueFactory().setValue(Integer.parseInt(value));
                        break;
                    case "grace_period_minutes":
                        gracePeriodSpinner.getValueFactory().setValue(Integer.parseInt(value));
                        break;
                    case "pay_period":
                        payPeriodCombo.setValue(value);
                        break;
                    case "default_pagibig":
                        defaultPagIbigField.setText(value);
                        break;
                    case "tax_computation":
                        taxTypeCombo.setValue(value);
                        break;
                    case "overtime_rate":
                        otRateField.setText(value);
                        break;
                    case "company_name":
                        companyNameField.setText(value);
                        break;
                    case "currency_symbol":
                        currencyField.setText(value);
                        break;
                    case "working_days_per_week":
                        workingDaysSpinner.getValueFactory().setValue(Integer.parseInt(value));
                        break;
                    case "enable_overtime":
                        enableOvertimeCheckBox.setSelected(Boolean.parseBoolean(value));
                        break;
                }
            }
            
            logger.info("Loaded system settings");
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error loading system settings", e);
            showAlert("Database Error", "Could not load system settings: " + e.getMessage());
        } catch (NumberFormatException e) {
            logger.log(Level.WARNING, "Invalid number format in settings", e);
        }
    }
    
    private boolean isValidTimeFormat(String time) {
        if (time == null || time.trim().isEmpty()) {
            return false;
        }
        
        try {
            String[] parts = time.split(":");
            if (parts.length != 2) {
                return false;
            }
            
            int hours = Integer.parseInt(parts[0]);
            int minutes = Integer.parseInt(parts[1]);
            
            return hours >= 0 && hours <= 23 && minutes >= 0 && minutes <= 59;
            
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    private boolean isValidDecimal(String value) {
        if (value == null || value.trim().isEmpty()) {
            return false;
        }
        
        try {
            Double.parseDouble(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    private void showAlert(String title, String content) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}