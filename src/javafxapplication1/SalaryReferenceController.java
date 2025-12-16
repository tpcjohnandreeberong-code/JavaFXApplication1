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
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.Dialog;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.beans.property.SimpleStringProperty;
import java.sql.*;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.math.BigDecimal;
import java.math.RoundingMode;
import javafx.scene.control.Button;

/**
 * Controller for Salary Reference Management
 * Manages rate per day, half-day rate, and rate per minute calculations
 */
public class SalaryReferenceController implements Initializable {
    
    @FXML private TextField searchField;
    @FXML private TableView<SalaryReference> table;
    @FXML private TableColumn<SalaryReference, String> colId;
    @FXML private TableColumn<SalaryReference, String> colMonthlySalary;
    @FXML private TableColumn<SalaryReference, String> colRatePerDay;
    @FXML private TableColumn<SalaryReference, String> colHalfDayRate;
    @FXML private TableColumn<SalaryReference, String> colRatePerMinute;
    
    // Action buttons
    @FXML private Button addSalaryRateButton;
    @FXML private Button searchButton;
    @FXML private Button editButton;
    @FXML private Button deleteButton;
    @FXML private Button calculateAllButton;
    
    private ObservableList<SalaryReference> salaryData = FXCollections.observableArrayList();
    private Connection connection;
    private static final Logger logger = Logger.getLogger(SalaryReferenceController.class.getName());
    
    // Database connection details
    private static final String DB_URL = DatabaseConfig.getDbUrl();
    private static final String DB_USER = DatabaseConfig.getDbUser();
    private static final String DB_PASSWORD = DatabaseConfig.getDbPassword();
    
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupTableColumns();
        initializeDatabase();
        loadSalaryReferenceData();
        
        // Setup permission-based button visibility
        setupPermissionBasedVisibility();
        
        // Log module access
        String currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser != null) {
            SecurityLogger.logSecurityEvent(
                "SALARY_REFERENCE_MODULE_ACCESS",
                "LOW",
                currentUser,
                "Accessed Salary Reference module"
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
                boolean canView = hasUserPermission(currentUser, "salary_ref.view");
                boolean canAdd = hasUserPermission(currentUser, "salary_ref.add");
                boolean canEdit = hasUserPermission(currentUser, "salary_ref.edit");
                boolean canDelete = hasUserPermission(currentUser, "salary_ref.delete");
                
                // Show/hide buttons based on permissions
                if (addSalaryRateButton != null) {
                    addSalaryRateButton.setVisible(canAdd);
                    addSalaryRateButton.setManaged(canAdd);
                }
                if (searchButton != null) {
                    searchButton.setVisible(canView);
                    searchButton.setManaged(canView);
                }
                if (editButton != null) {
                    editButton.setVisible(canEdit);
                    editButton.setManaged(canEdit);
                }
                if (deleteButton != null) {
                    deleteButton.setVisible(canDelete);
                    deleteButton.setManaged(canDelete);
                }
                if (calculateAllButton != null) {
                    // Calculate All modifies data, so it requires edit permission
                    calculateAllButton.setVisible(canEdit);
                    calculateAllButton.setManaged(canEdit);
                }
                
                logger.info("Salary Reference buttons visibility - View: " + canView + ", Add: " + canAdd + ", Edit: " + canEdit + ", Delete: " + canDelete);
                
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
        if (addSalaryRateButton != null) {
            addSalaryRateButton.setVisible(false);
            addSalaryRateButton.setManaged(false);
        }
        if (searchButton != null) {
            searchButton.setVisible(false);
            searchButton.setManaged(false);
        }
        if (editButton != null) {
            editButton.setVisible(false);
            editButton.setManaged(false);
        }
        if (deleteButton != null) {
            deleteButton.setVisible(false);
            deleteButton.setManaged(false);
        }
        if (calculateAllButton != null) {
            calculateAllButton.setVisible(false);
            calculateAllButton.setManaged(false);
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
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colMonthlySalary.setCellValueFactory(new PropertyValueFactory<>("monthlySalary"));
        colRatePerDay.setCellValueFactory(new PropertyValueFactory<>("ratePerDay"));
        colHalfDayRate.setCellValueFactory(new PropertyValueFactory<>("halfDayRate"));
        colRatePerMinute.setCellValueFactory(new PropertyValueFactory<>("ratePerMinute"));
        
        table.setItems(salaryData);
    }
    
    private void initializeDatabase() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            logger.info("Database connection established successfully");
            
            // Create salary_reference table if it doesn't exist
            createSalaryReferenceTableIfNotExists();
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error initializing database connection", e);
            showAlert("Database Error", "Could not connect to database: " + e.getMessage());
        }
    }
    
    private void createSalaryReferenceTableIfNotExists() {
        String createTableSQL = """
            CREATE TABLE IF NOT EXISTS salary_reference (
                id INT AUTO_INCREMENT PRIMARY KEY,
                monthly_salary DECIMAL(10,2) NOT NULL,
                rate_per_day DECIMAL(10,2) NOT NULL,
                half_day_rate DECIMAL(10,2) NOT NULL,
                rate_per_minute DECIMAL(10,4) NOT NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                UNIQUE KEY unique_monthly_salary (monthly_salary)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """;
        
        try (PreparedStatement stmt = connection.prepareStatement(createTableSQL)) {
            stmt.executeUpdate();
            logger.info("Salary reference table created or already exists");
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error creating salary reference table", e);
        }
    }
    
    @FXML
    private void onAdd() {
        // Log add click
        String currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser != null) {
            SecurityLogger.logSecurityEvent(
                "SALARY_REFERENCE_ADD_CLICK",
                "MEDIUM",
                currentUser,
                "Clicked Add Salary Reference button"
            );
        }
        
        Dialog<SalaryReferenceData> dialog = buildSalaryReferenceDialog(null);
        Optional<SalaryReferenceData> result = dialog.showAndWait();
        
        if (result.isPresent()) {
            SalaryReferenceData data = result.get();
            if (addSalaryReference(data)) {
                // Log successful add
                if (currentUser != null) {
                    SecurityLogger.logSecurityEvent(
                        "SALARY_REFERENCE_ADD_SUCCESS",
                        "MEDIUM",
                        currentUser,
                        "Added salary reference - Monthly Salary: ₱" + data.monthlySalary + 
                        ", Rate Per Day: ₱" + data.ratePerDay + 
                        ", Employment Type: " + (data.employmentType != null ? data.employmentType : "N/A")
                    );
                }
                
                showAlert("Success", "Salary reference added successfully!");
                loadSalaryReferenceData();
            } else {
                // Log failed add
                if (currentUser != null) {
                    SecurityLogger.logSecurityEvent(
                        "SALARY_REFERENCE_ADD_FAILED",
                        "MEDIUM",
                        currentUser,
                        "Failed to add salary reference - Monthly Salary: ₱" + data.monthlySalary
                    );
                }
            }
        } else {
            // Log add cancelled
            if (currentUser != null) {
                SecurityLogger.logSecurityEvent(
                    "SALARY_REFERENCE_ADD_CANCELLED",
                    "LOW",
                    currentUser,
                    "Cancelled adding salary reference"
                );
            }
        }
    }
    
    private BigDecimal calculateRatePerDay(BigDecimal monthlySalary) {
        // Monthly salary divided by 22 working days (standard)
        return monthlySalary.divide(new BigDecimal("22"), 2, RoundingMode.HALF_UP);
    }
    
    private BigDecimal calculateHalfDayRate(BigDecimal ratePerDay) {
        // Half of the daily rate
        return ratePerDay.divide(new BigDecimal("2"), 2, RoundingMode.HALF_UP);
    }
    
    private BigDecimal calculateRatePerMinute(BigDecimal ratePerDay) {
        // Daily rate divided by 480 minutes (8 hours * 60 minutes)
        return ratePerDay.divide(new BigDecimal("480"), 4, RoundingMode.HALF_UP);
    }
    
    private BigDecimal calculateRatePerUnit(BigDecimal monthlySalary) {
        // For instructors: monthly_salary / 24
        return monthlySalary.divide(new BigDecimal("24"), 2, RoundingMode.HALF_UP);
    }
    
    private Dialog<SalaryReferenceData> buildSalaryReferenceDialog(SalaryReferenceData existing) {
        Dialog<SalaryReferenceData> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "Add Salary Reference" : "Edit Salary Reference");
        
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
        Label titleLabel = new Label(existing == null ? "Add New Salary Reference" : "Edit Salary Reference");
        titleLabel.setStyle(
            "-fx-text-fill: #1b5e20; " +
            "-fx-font-size: 24px; " +
            "-fx-font-weight: bold; " +
            "-fx-alignment: center;"
        );
        
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

        // Monthly Salary field
        javafx.scene.layout.VBox monthlySalaryContainer = new javafx.scene.layout.VBox(8);
        Label monthlySalaryLabel = new Label("Monthly Salary (₱)");
        monthlySalaryLabel.setStyle(labelStyle);
        TextField monthlySalaryField = new TextField();
        monthlySalaryField.setPromptText("Enter monthly salary (e.g., 26749.00)");
        monthlySalaryField.setStyle(fieldStyle);
        monthlySalaryContainer.getChildren().addAll(monthlySalaryLabel, monthlySalaryField);
        
        // Rate Per Day field (auto-calculated, read-only)
        javafx.scene.layout.VBox ratePerDayContainer = new javafx.scene.layout.VBox(8);
        Label ratePerDayLabel = new Label("Rate Per Day (₱) - Auto-calculated");
        ratePerDayLabel.setStyle(labelStyle);
        TextField ratePerDayField = new TextField();
        ratePerDayField.setEditable(false);
        ratePerDayField.setStyle(fieldStyle + " -fx-background-color: #f5f5f5;");
        ratePerDayContainer.getChildren().addAll(ratePerDayLabel, ratePerDayField);
        
        // Half Day Rate field (auto-calculated, read-only)
        javafx.scene.layout.VBox halfDayRateContainer = new javafx.scene.layout.VBox(8);
        Label halfDayRateLabel = new Label("Half Day Rate (₱) - Auto-calculated");
        halfDayRateLabel.setStyle(labelStyle);
        TextField halfDayRateField = new TextField();
        halfDayRateField.setEditable(false);
        halfDayRateField.setStyle(fieldStyle + " -fx-background-color: #f5f5f5;");
        halfDayRateContainer.getChildren().addAll(halfDayRateLabel, halfDayRateField);
        
        // Rate Per Minute field (auto-calculated, read-only)
        javafx.scene.layout.VBox ratePerMinuteContainer = new javafx.scene.layout.VBox(8);
        Label ratePerMinuteLabel = new Label("Rate Per Minute (₱) - Auto-calculated");
        ratePerMinuteLabel.setStyle(labelStyle);
        TextField ratePerMinuteField = new TextField();
        ratePerMinuteField.setEditable(false);
        ratePerMinuteField.setStyle(fieldStyle + " -fx-background-color: #f5f5f5;");
        ratePerMinuteContainer.getChildren().addAll(ratePerMinuteLabel, ratePerMinuteField);
        
        // Rate Per Unit field (auto-calculated, read-only, for instructors)
        javafx.scene.layout.VBox ratePerUnitContainer = new javafx.scene.layout.VBox(8);
        Label ratePerUnitLabel = new Label("Rate Per Unit (₱) - Auto-calculated (for instructors)");
        ratePerUnitLabel.setStyle(labelStyle);
        TextField ratePerUnitField = new TextField();
        ratePerUnitField.setEditable(false);
        ratePerUnitField.setStyle(fieldStyle + " -fx-background-color: #f5f5f5;");
        ratePerUnitContainer.getChildren().addAll(ratePerUnitLabel, ratePerUnitField);
        
        // Employment Type field (optional)
        javafx.scene.layout.VBox employmentTypeContainer = new javafx.scene.layout.VBox(8);
        Label employmentTypeLabel = new Label("Employment Type (Optional)");
        employmentTypeLabel.setStyle(labelStyle);
        ComboBox<String> employmentTypeCombo = new ComboBox<>();
        employmentTypeCombo.getItems().addAll("", "INSTRUCTOR", "TEMPORARY_INSTRUCTOR", "STAFF_NON_TEACHING");
        employmentTypeCombo.setPromptText("Select employment type");
        employmentTypeCombo.setStyle(fieldStyle);
        employmentTypeContainer.getChildren().addAll(employmentTypeLabel, employmentTypeCombo);

        // Auto-calculate rates when monthly salary changes
        monthlySalaryField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.isEmpty()) {
                try {
                    BigDecimal monthlySalary = new BigDecimal(newValue);
                    if (monthlySalary.compareTo(BigDecimal.ZERO) > 0) {
                        BigDecimal ratePerDay = calculateRatePerDay(monthlySalary);
                        BigDecimal halfDayRate = calculateHalfDayRate(ratePerDay);
                        BigDecimal ratePerMinute = calculateRatePerMinute(ratePerDay);
                        BigDecimal ratePerUnit = calculateRatePerUnit(monthlySalary);
                        
                        ratePerDayField.setText(String.format("%.2f", ratePerDay));
                        halfDayRateField.setText(String.format("%.2f", halfDayRate));
                        ratePerMinuteField.setText(String.format("%.4f", ratePerMinute));
                        ratePerUnitField.setText(String.format("%.2f", ratePerUnit));
                    }
                } catch (NumberFormatException e) {
                    // Invalid input, clear calculated fields
                    ratePerDayField.clear();
                    halfDayRateField.clear();
                    ratePerMinuteField.clear();
                    ratePerUnitField.clear();
                }
            } else {
                ratePerDayField.clear();
                halfDayRateField.clear();
                ratePerMinuteField.clear();
                ratePerUnitField.clear();
            }
        });

        // Populate fields if editing
        if (existing != null) {
            monthlySalaryField.setText(existing.monthlySalary.toString());
            ratePerDayField.setText(String.format("%.2f", existing.ratePerDay));
            halfDayRateField.setText(String.format("%.2f", existing.halfDayRate));
            ratePerMinuteField.setText(String.format("%.4f", existing.ratePerMinute));
            if (existing.ratePerUnit != null) {
                ratePerUnitField.setText(String.format("%.2f", existing.ratePerUnit));
            }
            if (existing.employmentType != null) {
                employmentTypeCombo.setValue(existing.employmentType);
            }
        }

        // Add all form elements to form container
        formContainer.getChildren().addAll(
            monthlySalaryContainer,
            ratePerDayContainer,
            halfDayRateContainer,
            ratePerMinuteContainer,
            ratePerUnitContainer,
            employmentTypeContainer
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
            "-fx-font-size: 14px; " +
            "-fx-border-color: #ddd; " +
            "-fx-border-radius: 8;"
        );
        
        // Set dialog size
        dialog.getDialogPane().setPrefWidth(450);
        dialog.getDialogPane().setPrefHeight(650);

        // Add validation
        dialog.getDialogPane().lookupButton(saveButtonType).addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            if (monthlySalaryField.getText().trim().isEmpty()) {
                showAlert("Validation Error", "Monthly salary is required.");
                event.consume();
                return;
            }
            
            try {
                BigDecimal monthlySalary = new BigDecimal(monthlySalaryField.getText().trim());
                if (monthlySalary.compareTo(BigDecimal.ZERO) <= 0) {
                    showAlert("Validation Error", "Monthly salary must be greater than 0.");
                    event.consume();
                    return;
                }
            } catch (NumberFormatException e) {
                showAlert("Validation Error", "Please enter a valid monthly salary amount.");
                event.consume();
                return;
            }
        });

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                try {
                    SalaryReferenceData data = new SalaryReferenceData();
                    data.monthlySalary = new BigDecimal(monthlySalaryField.getText().trim());
                    data.ratePerDay = new BigDecimal(ratePerDayField.getText().trim());
                    data.halfDayRate = new BigDecimal(halfDayRateField.getText().trim());
                    data.ratePerMinute = new BigDecimal(ratePerMinuteField.getText().trim());
                    
                    if (!ratePerUnitField.getText().trim().isEmpty()) {
                        data.ratePerUnit = new BigDecimal(ratePerUnitField.getText().trim());
                    } else {
                        data.ratePerUnit = null;
                    }
                    
                    String empType = employmentTypeCombo.getValue();
                    data.employmentType = (empType != null && !empType.isEmpty()) ? empType : null;
                    
                    return data;
                    
                } catch (NumberFormatException e) {
                    showAlert("Invalid Input", "Please enter valid numeric amounts");
                    return null;
                }
            }
            return null;
        });

        return dialog;
    }
    
    // Inner class for SalaryReferenceData
    private static class SalaryReferenceData {
        BigDecimal monthlySalary;
        BigDecimal ratePerDay;
        BigDecimal halfDayRate;
        BigDecimal ratePerMinute;
        BigDecimal ratePerUnit;
        String employmentType;
    }
    
    private boolean addSalaryReference(SalaryReferenceData data) {
        String sql = """
            INSERT INTO salary_reference 
            (monthly_salary, rate_per_day, half_day_rate, rate_per_minute, rate_per_unit, employment_type) 
            VALUES (?, ?, ?, ?, ?, ?)
            """;
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setBigDecimal(1, data.monthlySalary);
            stmt.setBigDecimal(2, data.ratePerDay);
            stmt.setBigDecimal(3, data.halfDayRate);
            stmt.setBigDecimal(4, data.ratePerMinute);
            
            if (data.ratePerUnit != null) {
                stmt.setBigDecimal(5, data.ratePerUnit);
            } else {
                stmt.setNull(5, Types.DECIMAL);
            }
            
            if (data.employmentType != null && !data.employmentType.isEmpty()) {
                stmt.setString(6, data.employmentType);
            } else {
                stmt.setNull(6, Types.VARCHAR);
            }
            
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected > 0) {
                logger.info("Added salary reference for monthly salary: " + data.monthlySalary);
                return true;
            }
            
        } catch (SQLException e) {
            if (e.getErrorCode() == 1062) { // Duplicate entry
                showAlert("Duplicate Entry", "Salary reference for this monthly salary already exists");
            } else {
                logger.log(Level.SEVERE, "Error adding salary reference", e);
                showAlert("Database Error", "Could not add salary reference: " + e.getMessage());
            }
        }
        return false;
    }
    
    @FXML
    private void onEdit() {
        SalaryReference selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("No Selection", "Please select a salary reference to edit");
            
            // Log failed edit attempt
            String currentUser = SessionManager.getInstance().getCurrentUser();
            if (currentUser != null) {
                SecurityLogger.logSecurityEvent(
                    "SALARY_REFERENCE_EDIT_FAILED",
                    "LOW",
                    currentUser,
                    "Attempted to edit salary reference but no item was selected"
                );
            }
            return;
        }
        
        // Log edit click
        String currentUser = SessionManager.getInstance().getCurrentUser();
        String oldMonthlySalary = selected.getMonthlySalary();
        
        if (currentUser != null) {
            SecurityLogger.logSecurityEvent(
                "SALARY_REFERENCE_EDIT_CLICK",
                "MEDIUM",
                currentUser,
                "Clicked Edit Salary Reference button - ID: " + selected.getId() + 
                ", Current Monthly Salary: ₱" + oldMonthlySalary
            );
        }
        
        // Get full data from database
        SalaryReferenceData existingData = getSalaryReferenceDataFromDatabase(Integer.parseInt(selected.getId()));
        if (existingData == null) {
            showAlert("Error", "Could not load salary reference data for editing");
            
            // Log load error
            if (currentUser != null) {
                SecurityLogger.logSecurityEvent(
                    "SALARY_REFERENCE_EDIT_FAILED",
                    "MEDIUM",
                    currentUser,
                    "Failed to load salary reference data for editing - ID: " + selected.getId()
                );
            }
            return;
        }
        
        Dialog<SalaryReferenceData> dialog = buildSalaryReferenceDialog(existingData);
        Optional<SalaryReferenceData> result = dialog.showAndWait();
        
        if (result.isPresent()) {
            SalaryReferenceData data = result.get();
            if (updateSalaryReference(Integer.parseInt(selected.getId()), data)) {
                // Log successful edit
                if (currentUser != null) {
                    SecurityLogger.logSecurityEvent(
                        "SALARY_REFERENCE_EDIT_SUCCESS",
                        "MEDIUM",
                        currentUser,
                        "Updated salary reference - ID: " + selected.getId() + 
                        ", Old Monthly Salary: ₱" + oldMonthlySalary + 
                        ", New Monthly Salary: ₱" + data.monthlySalary + 
                        ", Employment Type: " + (data.employmentType != null ? data.employmentType : "N/A")
                    );
                }
                
                showAlert("Success", "Salary reference updated successfully!");
                loadSalaryReferenceData();
            } else {
                // Log failed update
                if (currentUser != null) {
                    SecurityLogger.logSecurityEvent(
                        "SALARY_REFERENCE_EDIT_FAILED",
                        "MEDIUM",
                        currentUser,
                        "Failed to update salary reference - ID: " + selected.getId()
                    );
                }
            }
        } else {
            // Log edit cancelled
            if (currentUser != null) {
                SecurityLogger.logSecurityEvent(
                    "SALARY_REFERENCE_EDIT_CANCELLED",
                    "LOW",
                    currentUser,
                    "Cancelled editing salary reference - ID: " + selected.getId()
                );
            }
        }
    }
    
    private SalaryReferenceData getSalaryReferenceDataFromDatabase(int id) {
        String sql = "SELECT * FROM salary_reference WHERE id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                SalaryReferenceData data = new SalaryReferenceData();
                data.monthlySalary = rs.getBigDecimal("monthly_salary");
                data.ratePerDay = rs.getBigDecimal("rate_per_day");
                data.halfDayRate = rs.getBigDecimal("half_day_rate");
                data.ratePerMinute = rs.getBigDecimal("rate_per_minute");
                
                BigDecimal ratePerUnit = rs.getBigDecimal("rate_per_unit");
                data.ratePerUnit = rs.wasNull() ? null : ratePerUnit;
                
                String employmentType = rs.getString("employment_type");
                data.employmentType = rs.wasNull() ? null : employmentType;
                
                return data;
            }
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error loading salary reference data from database", e);
        }
        
        return null;
    }
    
    private boolean updateSalaryReference(int id, SalaryReferenceData data) {
        String sql = """
            UPDATE salary_reference 
            SET monthly_salary = ?, rate_per_day = ?, half_day_rate = ?, 
                rate_per_minute = ?, rate_per_unit = ?, employment_type = ? 
            WHERE id = ?
            """;
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setBigDecimal(1, data.monthlySalary);
            stmt.setBigDecimal(2, data.ratePerDay);
            stmt.setBigDecimal(3, data.halfDayRate);
            stmt.setBigDecimal(4, data.ratePerMinute);
            
            if (data.ratePerUnit != null) {
                stmt.setBigDecimal(5, data.ratePerUnit);
            } else {
                stmt.setNull(5, Types.DECIMAL);
            }
            
            if (data.employmentType != null && !data.employmentType.isEmpty()) {
                stmt.setString(6, data.employmentType);
            } else {
                stmt.setNull(6, Types.VARCHAR);
            }
            
            stmt.setInt(7, id);
            
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected > 0) {
                logger.info("Updated salary reference ID: " + id);
                return true;
            }
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error updating salary reference", e);
            showAlert("Database Error", "Could not update salary reference: " + e.getMessage());
        }
        return false;
    }
    
    @FXML
    private void onDelete() {
        SalaryReference selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("No Selection", "Please select a salary reference to delete");
            
            // Log failed delete attempt
            String currentUser = SessionManager.getInstance().getCurrentUser();
            if (currentUser != null) {
                SecurityLogger.logSecurityEvent(
                    "SALARY_REFERENCE_DELETE_FAILED",
                    "LOW",
                    currentUser,
                    "Attempted to delete salary reference but no item was selected"
                );
            }
            return;
        }
        
        // Log delete click
        String currentUser = SessionManager.getInstance().getCurrentUser();
        String monthlySalary = selected.getMonthlySalary();
        String id = selected.getId();
        
        if (currentUser != null) {
            SecurityLogger.logSecurityEvent(
                "SALARY_REFERENCE_DELETE_CLICK",
                "HIGH",
                currentUser,
                "Clicked Delete Salary Reference button - ID: " + id + 
                ", Monthly Salary: ₱" + monthlySalary
            );
        }
        
        Alert confirmAlert = new Alert(AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirm Delete");
        confirmAlert.setHeaderText("Delete Salary Reference");
        confirmAlert.setContentText("Are you sure you want to delete this salary reference?\nMonthly Salary: ₱" + monthlySalary);
        
        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == javafx.scene.control.ButtonType.OK) {
                if (deleteSalaryReference(Integer.parseInt(id))) {
                    // Log successful delete
                    if (currentUser != null) {
                        SecurityLogger.logSecurityEvent(
                            "SALARY_REFERENCE_DELETE_SUCCESS",
                            "HIGH",
                            currentUser,
                            "Deleted salary reference - ID: " + id + 
                            ", Monthly Salary: ₱" + monthlySalary
                        );
                    }
                } else {
                    // Log failed delete
                    if (currentUser != null) {
                        SecurityLogger.logSecurityEvent(
                            "SALARY_REFERENCE_DELETE_FAILED",
                            "HIGH",
                            currentUser,
                            "Failed to delete salary reference - ID: " + id
                        );
                    }
                }
                loadSalaryReferenceData();
            } else {
                // Log delete cancelled
                if (currentUser != null) {
                    SecurityLogger.logSecurityEvent(
                        "SALARY_REFERENCE_DELETE_CANCELLED",
                        "MEDIUM",
                        currentUser,
                        "Cancelled deleting salary reference - ID: " + id
                    );
                }
            }
        });
    }
    
    private boolean deleteSalaryReference(int id) {
        String sql = "DELETE FROM salary_reference WHERE id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected > 0) {
                showAlert("Success", "Salary reference deleted successfully!");
                logger.info("Deleted salary reference ID: " + id);
                return true;
            }
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error deleting salary reference", e);
            showAlert("Database Error", "Could not delete salary reference: " + e.getMessage());
        }
        return false;
    }
    
    @FXML
    private void onSearch() {
        String searchText = searchField.getText().trim();
        
        // Log search activity (only if there's actual text)
        if (!searchText.isEmpty()) {
            String currentUser = SessionManager.getInstance().getCurrentUser();
            if (currentUser != null) {
                SecurityLogger.logSecurityEvent(
                    "SALARY_REFERENCE_SEARCH",
                    "LOW",
                    currentUser,
                    "Searched salary references with keyword: '" + searchText + "'"
                );
            }
        }
        
        if (searchText.isEmpty()) {
            loadSalaryReferenceData();
            return;
        }
        
        String sql = "SELECT * FROM salary_reference WHERE monthly_salary LIKE ? OR rate_per_day LIKE ? ORDER BY monthly_salary";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            String searchPattern = "%" + searchText + "%";
            stmt.setString(1, searchPattern);
            stmt.setString(2, searchPattern);
            
            ResultSet rs = stmt.executeQuery();
            salaryData.clear();
            
            int resultCount = 0;
            while (rs.next()) {
                SalaryReference salary = new SalaryReference(
                    String.valueOf(rs.getInt("id")),
                    String.format("%.2f", rs.getBigDecimal("monthly_salary")),
                    String.format("%.2f", rs.getBigDecimal("rate_per_day")),
                    String.format("%.2f", rs.getBigDecimal("half_day_rate")),
                    String.format("%.4f", rs.getBigDecimal("rate_per_minute"))
                );
                salaryData.add(salary);
                resultCount++;
            }
            
            // Log search results
            String currentUser = SessionManager.getInstance().getCurrentUser();
            if (currentUser != null && !searchText.isEmpty()) {
                SecurityLogger.logSecurityEvent(
                    "SALARY_REFERENCE_SEARCH_RESULTS",
                    "LOW",
                    currentUser,
                    "Search completed - Keyword: '" + searchText + "', Results: " + resultCount + " records"
                );
            }
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error searching salary references", e);
            
            // Log search error
            String currentUser = SessionManager.getInstance().getCurrentUser();
            if (currentUser != null) {
                SecurityLogger.logSecurityEvent(
                    "SALARY_REFERENCE_SEARCH_FAILED",
                    "LOW",
                    currentUser,
                    "Failed to search salary references - Error: " + e.getMessage()
                );
            }
            
            showAlert("Database Error", "Could not search salary references: " + e.getMessage());
        }
    }
    
    @FXML
    private void onCalculateAll() {
        // Log calculate all click
        String currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser != null) {
            SecurityLogger.logSecurityEvent(
                "SALARY_REFERENCE_CALCULATE_ALL_CLICK",
                "MEDIUM",
                currentUser,
                "Clicked Recalculate All Rates button"
            );
        }
        
        Alert confirmAlert = new Alert(AlertType.CONFIRMATION);
        confirmAlert.setTitle("Recalculate All Rates");
        confirmAlert.setHeaderText("Recalculate All Salary Rates");
        confirmAlert.setContentText("This will recalculate all daily rates, half-day rates, and per-minute rates based on current monthly salaries. Continue?");
        
        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == javafx.scene.control.ButtonType.OK) {
                int updatedCount = recalculateAllRates();
                
                // Log successful recalculation
                if (currentUser != null) {
                    SecurityLogger.logSecurityEvent(
                        "SALARY_REFERENCE_CALCULATE_ALL_SUCCESS",
                        "MEDIUM",
                        currentUser,
                        "Recalculated all salary rates - Updated " + updatedCount + " records"
                    );
                }
                
                loadSalaryReferenceData();
            } else {
                // Log calculate all cancelled
                if (currentUser != null) {
                    SecurityLogger.logSecurityEvent(
                        "SALARY_REFERENCE_CALCULATE_ALL_CANCELLED",
                        "LOW",
                        currentUser,
                        "Cancelled recalculating all salary rates"
                    );
                }
            }
        });
    }
    
    private int recalculateAllRates() {
        String selectSQL = "SELECT id, monthly_salary FROM salary_reference";
        String updateSQL = "UPDATE salary_reference SET rate_per_day = ?, half_day_rate = ?, rate_per_minute = ? WHERE id = ?";
        
        try (PreparedStatement selectStmt = connection.prepareStatement(selectSQL);
             PreparedStatement updateStmt = connection.prepareStatement(updateSQL)) {
            
            ResultSet rs = selectStmt.executeQuery();
            int updatedCount = 0;
            
            while (rs.next()) {
                int id = rs.getInt("id");
                BigDecimal monthlySalary = rs.getBigDecimal("monthly_salary");
                
                BigDecimal ratePerDay = calculateRatePerDay(monthlySalary);
                BigDecimal halfDayRate = calculateHalfDayRate(ratePerDay);
                BigDecimal ratePerMinute = calculateRatePerMinute(ratePerDay);
                
                updateStmt.setBigDecimal(1, ratePerDay);
                updateStmt.setBigDecimal(2, halfDayRate);
                updateStmt.setBigDecimal(3, ratePerMinute);
                updateStmt.setInt(4, id);
                
                updateStmt.executeUpdate();
                updatedCount++;
            }
            
            showAlert("Success", "Recalculated rates for " + updatedCount + " salary references!");
            logger.info("Recalculated rates for " + updatedCount + " salary references");
            
            return updatedCount;
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error recalculating salary rates", e);
            
            // Log recalculation error
            String currentUser = SessionManager.getInstance().getCurrentUser();
            if (currentUser != null) {
                SecurityLogger.logSecurityEvent(
                    "SALARY_REFERENCE_CALCULATE_ALL_FAILED",
                    "MEDIUM",
                    currentUser,
                    "Failed to recalculate all salary rates - Error: " + e.getMessage()
                );
            }
            
            showAlert("Database Error", "Could not recalculate rates: " + e.getMessage());
            return 0;
        }
    }
    
    private void loadSalaryReferenceData() {
        String sql = "SELECT * FROM salary_reference ORDER BY monthly_salary";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            salaryData.clear();
            
            while (rs.next()) {
                SalaryReference salary = new SalaryReference(
                    String.valueOf(rs.getInt("id")),
                    String.format("%.2f", rs.getBigDecimal("monthly_salary")),
                    String.format("%.2f", rs.getBigDecimal("rate_per_day")),
                    String.format("%.2f", rs.getBigDecimal("half_day_rate")),
                    String.format("%.4f", rs.getBigDecimal("rate_per_minute"))
                );
                salaryData.add(salary);
            }
            
            logger.info("Loaded " + salaryData.size() + " salary references");
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error loading salary reference data", e);
            showAlert("Database Error", "Could not load salary reference data: " + e.getMessage());
        }
    }
    
    private void showAlert(String title, String content) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
    
    // Inner class for SalaryReference data model
    public static class SalaryReference {
        private final SimpleStringProperty id;
        private final SimpleStringProperty monthlySalary;
        private final SimpleStringProperty ratePerDay;
        private final SimpleStringProperty halfDayRate;
        private final SimpleStringProperty ratePerMinute;
        
        public SalaryReference(String id, String monthlySalary, String ratePerDay, String halfDayRate, String ratePerMinute) {
            this.id = new SimpleStringProperty(id);
            this.monthlySalary = new SimpleStringProperty(monthlySalary);
            this.ratePerDay = new SimpleStringProperty(ratePerDay);
            this.halfDayRate = new SimpleStringProperty(halfDayRate);
            this.ratePerMinute = new SimpleStringProperty(ratePerMinute);
        }
        
        public String getId() { return id.get(); }
        public String getMonthlySalary() { return monthlySalary.get(); }
        public String getRatePerDay() { return ratePerDay.get(); }
        public String getHalfDayRate() { return halfDayRate.get(); }
        public String getRatePerMinute() { return ratePerMinute.get(); }
    }
}