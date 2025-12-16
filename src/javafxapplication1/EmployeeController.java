package javafxapplication1;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.net.URL;
import java.sql.*;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.scene.layout.GridPane;

public class EmployeeController implements Initializable {

    private static final Logger logger = Logger.getLogger(EmployeeController.class.getName());
    
    // Database connection parameters
    private static final String DB_URL = DatabaseConfig.getDbUrl();
    private static final String DB_USER = DatabaseConfig.getDbUser();
    private static final String DB_PASSWORD = DatabaseConfig.getDbPassword();
    private Connection connection;

    public static class Employee {
        private final int id;
        private final String accountNumber;
        private final String name;
        private final String position;
        private final double salary;
        private final Integer salaryRefId; // Add salary_ref_id

        public Employee(int id, String accountNumber, String name, String position, double salary) {
            this.id = id;
            this.accountNumber = accountNumber;
            this.name = name;
            this.position = position;
            this.salary = salary;
            this.salaryRefId = null;
        }
        
        public Employee(int id, String accountNumber, String name, String position, double salary, Integer salaryRefId) {
            this.id = id;
            this.accountNumber = accountNumber;
            this.name = name;
            this.position = position;
            this.salary = salary;
            this.salaryRefId = salaryRefId;
        }
        
        public int getId() { return id; }
        public String getAccountNumber() { return accountNumber; }
        public String getName() { return name; }
        public String getPosition() { return position; }
        public double getSalary() { return salary; }
        public Integer getSalaryRefId() { return salaryRefId; }
    }
    
    // Inner class for Salary Reference items in ComboBox
    public static class SalaryReferenceItem {
        private final int id;
        private final double monthlySalary;
        
        public SalaryReferenceItem(int id, double monthlySalary) {
            this.id = id;
            this.monthlySalary = monthlySalary;
        }
        
        public int getId() { return id; }
        public double getMonthlySalary() { return monthlySalary; }
        
        @Override
        public String toString() {
            return String.format("₱%,.2f", monthlySalary);
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            SalaryReferenceItem that = (SalaryReferenceItem) obj;
            return id == that.id;
        }
    }

    @FXML private TextField searchField;
    @FXML private TableView<Employee> table;
    @FXML private TableColumn<Employee, Integer> colId;
    @FXML private TableColumn<Employee, String> colAccountNumber;
    @FXML private TableColumn<Employee, String> colName;
    @FXML private TableColumn<Employee, String> colPosition;
    @FXML private TableColumn<Employee, Double> colSalary;
    @FXML private Button addEmployeeButton;
    @FXML private Button editEmployeeButton;
    @FXML private Button deleteEmployeeButton;

    private final ObservableList<Employee> data = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        initializeDatabase();
        setupTableColumns();
        loadEmployeesFromDatabase();
        
        // Setup permission-based button visibility
        setupPermissionBasedVisibility();
        
        // Log module access
        String currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser != null) {
            SecurityLogger.logSecurityEvent(
                "EMPLOYEE_MODULE_ACCESS",
                "LOW",
                currentUser,
                "Accessed Employee Management module"
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
                boolean canAdd = hasUserPermission(currentUser, "employee.add");
                boolean canEdit = hasUserPermission(currentUser, "employee.edit");
                boolean canDelete = hasUserPermission(currentUser, "employee.delete");
                
                // Show/hide buttons based on permissions
                if (addEmployeeButton != null) {
                    addEmployeeButton.setVisible(canAdd);
                    addEmployeeButton.setManaged(canAdd);
                }
                if (editEmployeeButton != null) {
                    editEmployeeButton.setVisible(canEdit);
                    editEmployeeButton.setManaged(canEdit);
                }
                if (deleteEmployeeButton != null) {
                    deleteEmployeeButton.setVisible(canDelete);
                    deleteEmployeeButton.setManaged(canDelete);
                }
                
                logger.info("Employee buttons visibility - Add: " + canAdd + ", Edit: " + canEdit + ", Delete: " + canDelete);
                
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
        if (addEmployeeButton != null) {
            addEmployeeButton.setVisible(false);
            addEmployeeButton.setManaged(false);
        }
        if (editEmployeeButton != null) {
            editEmployeeButton.setVisible(false);
            editEmployeeButton.setManaged(false);
        }
        if (deleteEmployeeButton != null) {
            deleteEmployeeButton.setVisible(false);
            deleteEmployeeButton.setManaged(false);
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
            logger.info("Database connection established for Employee management");
            createEmployeeTable();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Database connection failed", e);
            showErrorAlert("Database Error", "Cannot connect to database. Using offline mode.");
        }
    }
    
    private void createEmployeeTable() {
        try {
            String createTableSQL = """
                CREATE TABLE IF NOT EXISTS employees (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    account_number VARCHAR(50) UNIQUE NOT NULL,
                    full_name VARCHAR(100) NOT NULL,
                    position VARCHAR(100),
                    salary DECIMAL(10,2),
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
                )
            """;
            
            PreparedStatement stmt = connection.prepareStatement(createTableSQL);
            stmt.executeUpdate();
            logger.info("Employee table created/verified successfully");
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error creating employee table", e);
        }
    }

    private void setupTableColumns() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colAccountNumber.setCellValueFactory(new PropertyValueFactory<>("accountNumber"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colPosition.setCellValueFactory(new PropertyValueFactory<>("position"));
        colSalary.setCellValueFactory(new PropertyValueFactory<>("salary"));

        // Set column resize policy to fill available space
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Format salary column
        colSalary.setCellFactory(column -> new TableCell<Employee, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.format("₱%,.2f", item));
                }
            }
        });
    }

    private void loadEmployeesFromDatabase() {
        if (connection == null) {
            return;
        }
        
        try {
            String query = "SELECT \n" +
            "    e.id,\n" +
            "    e.account_number,\n" +
            "    e.full_name,\n" +
            "    e.position,\n" +
            "    e.salary_ref_id,\n" +
            "    sr.monthly_salary AS salary,\n" +
            "    sr.rate_per_day,\n" +
            "    sr.half_day_rate,\n" +
            "    sr.rate_per_minute\n" +
            "\n" +
            "FROM employees e\n" +
            "LEFT JOIN salary_reference sr\n" +
            "    ON e.salary_ref_id = sr.id\n" +
            "ORDER BY e.id;";
            PreparedStatement stmt = connection.prepareStatement(query);
            ResultSet rs = stmt.executeQuery();
            
            data.clear();
            while (rs.next()) {
                Integer salaryRefId = rs.getInt("salary_ref_id");
                if (rs.wasNull()) {
                    salaryRefId = null;
                }
                double salary = rs.getDouble("salary");
                if (rs.wasNull()) {
                    salary = 0.0;
                }
                
                Employee employee = new Employee(
                    rs.getInt("id"),
                    rs.getString("account_number"),
                    rs.getString("full_name"),
                    rs.getString("position"),
                    salary,
                    salaryRefId
                );
                data.add(employee);
            }
            
            table.setItems(data);
            logger.info("Loaded " + data.size() + " employees from database");
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error loading employees from database", e);
            showErrorAlert("Database Error", "Failed to load employees: " + e.getMessage());
        }
    }

    @FXML
    private void onSearch() {
        String searchText = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        
        // Log search activity
        String currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser != null) {
            SecurityLogger.logSecurityEvent(
                "EMPLOYEE_SEARCH",
                "LOW",
                currentUser,
                "Searched for employees with keyword: '" + (searchText.isEmpty() ? "(all)" : searchText) + "'"
            );
        }
        
        if (searchText.isEmpty()) {
            table.setItems(data);
            return;
        }

        ObservableList<Employee> filtered = FXCollections.observableArrayList();
        for (Employee employee : data) {
            boolean matchesSearch = searchText.isEmpty() || 
                                  String.valueOf(employee.getId()).contains(searchText) || 
                                  employee.getAccountNumber().toLowerCase().contains(searchText) ||
                                  employee.getName().toLowerCase().contains(searchText) ||
                                  employee.getPosition().toLowerCase().contains(searchText);
            
            if (matchesSearch) {
                filtered.add(employee);
            }
        }
        table.setItems(filtered);
    }

    @FXML
    private void onAdd() {
        // Log add button click
        String currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser != null) {
            SecurityLogger.logSecurityEvent(
                "EMPLOYEE_ADD_CLICK",
                "LOW",
                currentUser,
                "Clicked Add Employee button"
            );
        }
        
        Dialog<Employee> dialog = buildEmployeeDialog(null);
        Optional<Employee> result = dialog.showAndWait();
        result.ifPresent(employee -> {
            if (addEmployeeToDatabase(employee)) {
                // Log successful add
                if (currentUser != null) {
                    SecurityLogger.logSecurityEvent(
                        "EMPLOYEE_ADD_SUCCESS",
                        "MEDIUM",
                        currentUser,
                        "Added new employee: " + employee.getName() + " (Account: " + employee.getAccountNumber() + ", Position: " + employee.getPosition() + ")"
                    );
                }
                loadEmployeesFromDatabase(); // Refresh the table
                showSuccessAlert("Employee added successfully!");
            } else {
                // Log failed add
                if (currentUser != null) {
                    SecurityLogger.logSecurityEvent(
                        "EMPLOYEE_ADD_FAILED",
                        "MEDIUM",
                        currentUser,
                        "Failed to add employee: " + employee.getName() + " (Account: " + employee.getAccountNumber() + ")"
                    );
                }
            }
        });
    }

    @FXML
    private void onEdit() {
        Employee selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) { 
            showInfo("Please select a row to edit."); 
            return; 
        }
        
        // Log edit button click
        String currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser != null) {
            SecurityLogger.logSecurityEvent(
                "EMPLOYEE_EDIT_CLICK",
                "LOW",
                currentUser,
                "Clicked Edit Employee button for: " + selected.getName() + " (ID: " + selected.getId() + ")"
            );
        }
        
        Dialog<Employee> dialog = buildEmployeeDialog(selected);
        Optional<Employee> result = dialog.showAndWait();
        result.ifPresent(updated -> {
            if (updateEmployeeInDatabase(updated)) {
                // Log successful edit
                if (currentUser != null) {
                    SecurityLogger.logSecurityEvent(
                        "EMPLOYEE_EDIT_SUCCESS",
                        "MEDIUM",
                        currentUser,
                        "Updated employee: " + updated.getName() + " (ID: " + updated.getId() + ", Account: " + updated.getAccountNumber() + ", Position: " + updated.getPosition() + ")"
                    );
                }
                loadEmployeesFromDatabase(); // Refresh the table
                showSuccessAlert("Employee updated successfully!");
            } else {
                // Log failed edit
                if (currentUser != null) {
                    SecurityLogger.logSecurityEvent(
                        "EMPLOYEE_EDIT_FAILED",
                        "MEDIUM",
                        currentUser,
                        "Failed to update employee: " + updated.getName() + " (ID: " + updated.getId() + ")"
                    );
                }
            }
        });
    }

    @FXML
    private void onDelete() {
        Employee selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) { 
            showInfo("Please select a row to delete."); 
            return; 
        }
        
        // Log delete button click
        String currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser != null) {
            SecurityLogger.logSecurityEvent(
                "EMPLOYEE_DELETE_CLICK",
                "MEDIUM",
                currentUser,
                "Clicked Delete Employee button for: " + selected.getName() + " (ID: " + selected.getId() + ", Account: " + selected.getAccountNumber() + ")"
            );
        }
        
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Employee");
        confirm.setHeaderText(null);
        confirm.setContentText("Delete employee " + selected.getName() + "?");
        Optional<ButtonType> res = confirm.showAndWait();
        if (res.isPresent() && res.get() == ButtonType.OK) {
            if (deleteEmployeeFromDatabase(selected)) {
                // Log successful delete
                if (currentUser != null) {
                    SecurityLogger.logSecurityEvent(
                        "EMPLOYEE_DELETE_SUCCESS",
                        "HIGH",
                        currentUser,
                        "Deleted employee: " + selected.getName() + " (ID: " + selected.getId() + ", Account: " + selected.getAccountNumber() + ", Position: " + selected.getPosition() + ")"
                    );
                }
                loadEmployeesFromDatabase(); // Refresh the table
                showSuccessAlert("Employee deleted successfully!");
            } else {
                // Log failed delete
                if (currentUser != null) {
                    SecurityLogger.logSecurityEvent(
                        "EMPLOYEE_DELETE_FAILED",
                        "MEDIUM",
                        currentUser,
                        "Failed to delete employee: " + selected.getName() + " (ID: " + selected.getId() + ")"
                    );
                }
            }
        } else {
            // Log delete cancellation
            if (currentUser != null) {
                SecurityLogger.logSecurityEvent(
                    "EMPLOYEE_DELETE_CANCELLED",
                    "LOW",
                    currentUser,
                    "Cancelled deletion of employee: " + selected.getName() + " (ID: " + selected.getId() + ")"
                );
            }
        }
    }
    
    private boolean addEmployeeToDatabase(Employee employee) {
        if (connection == null) {
            showErrorAlert("Database Error", "No database connection available.");
            return false;
        }
        
        try {
            String query = "INSERT INTO employees (account_number, full_name, position, salary_ref_id) VALUES (?, ?, ?, ?)";
            PreparedStatement stmt = connection.prepareStatement(query);
            stmt.setString(1, employee.getAccountNumber());
            stmt.setString(2, employee.getName());
            stmt.setString(3, employee.getPosition());
            if (employee.getSalaryRefId() != null) {
                stmt.setInt(4, employee.getSalaryRefId());
            } else {
                stmt.setNull(4, Types.INTEGER);
            }
            
            int rowsAffected = stmt.executeUpdate();
            logger.info("Employee added to database: " + employee.getName());
            return rowsAffected > 0;
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error adding employee to database", e);
            if (e.getErrorCode() == 1062) { // MySQL duplicate entry error
                showErrorAlert("Validation Error", "Account number already exists. Please use a unique account number.");
            } else {
                showErrorAlert("Database Error", "Failed to add employee: " + e.getMessage());
            }
            return false;
        }
    }
    
    private boolean updateEmployeeInDatabase(Employee employee) {
        if (connection == null) {
            showErrorAlert("Database Error", "No database connection available.");
            return false;
        }
        
        try {
            String query = "UPDATE employees SET account_number = ?, full_name = ?, position = ?, salary_ref_id = ? WHERE id = ?";
            PreparedStatement stmt = connection.prepareStatement(query);
            stmt.setString(1, employee.getAccountNumber());
            stmt.setString(2, employee.getName());
            stmt.setString(3, employee.getPosition());
            if (employee.getSalaryRefId() != null) {
                stmt.setInt(4, employee.getSalaryRefId());
            } else {
                stmt.setNull(4, Types.INTEGER);
            }
            stmt.setInt(5, employee.getId());
            
            int rowsAffected = stmt.executeUpdate();
            logger.info("Employee updated in database: " + employee.getName());
            return rowsAffected > 0;
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error updating employee in database", e);
            if (e.getErrorCode() == 1062) { // MySQL duplicate entry error
                showErrorAlert("Validation Error", "Account number already exists. Please use a unique account number.");
            } else {
                showErrorAlert("Database Error", "Failed to update employee: " + e.getMessage());
            }
            return false;
        }
    }
    
    private boolean deleteEmployeeFromDatabase(Employee employee) {
        if (connection == null) {
            showErrorAlert("Database Error", "No database connection available.");
            return false;
        }
        
        try {
            String query = "DELETE FROM employees WHERE id = ?";
            PreparedStatement stmt = connection.prepareStatement(query);
            stmt.setInt(1, employee.getId());
            
            int rowsAffected = stmt.executeUpdate();
            logger.info("Employee deleted from database: " + employee.getName());
            return rowsAffected > 0;
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error deleting employee from database", e);
            showErrorAlert("Database Error", "Failed to delete employee: " + e.getMessage());
            return false;
        }
    }

    private Dialog<Employee> buildEmployeeDialog(Employee existing) {
        Dialog<Employee> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "Add Employee" : "Edit Employee");
        
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
        Label titleLabel = new Label(existing == null ? "Add New Employee" : "Edit Employee");
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

        // Account Number field
        javafx.scene.layout.VBox accountNumberContainer = new javafx.scene.layout.VBox(8);
        Label accountNumberLabel = new Label("Account Number");
        accountNumberLabel.setStyle(labelStyle);
        TextField accountNumberField = new TextField();
        accountNumberField.setPromptText("Enter account number (e.g., EMP-001)");
        accountNumberField.setStyle(fieldStyle);
        accountNumberContainer.getChildren().addAll(accountNumberLabel, accountNumberField);
        
        // Full Name field
        javafx.scene.layout.VBox nameContainer = new javafx.scene.layout.VBox(8);
        Label nameLabel = new Label("Full Name");
        nameLabel.setStyle(labelStyle);
        TextField nameField = new TextField();
        nameField.setPromptText("Enter full name");
        nameField.setStyle(fieldStyle);
        nameContainer.getChildren().addAll(nameLabel, nameField);
        
        // Position field
        javafx.scene.layout.VBox positionContainer = new javafx.scene.layout.VBox(8);
        Label positionLabel = new Label("Position");
        positionLabel.setStyle(labelStyle);
        TextField positionField = new TextField();
        positionField.setPromptText("Enter position (e.g., Teacher I)");
        positionField.setStyle(fieldStyle);
        positionContainer.getChildren().addAll(positionLabel, positionField);
        
        // Salary field - Changed to ComboBox
        javafx.scene.layout.VBox salaryContainer = new javafx.scene.layout.VBox(8);
        Label salaryLabel = new Label("Salary");
        salaryLabel.setStyle(labelStyle);
        ComboBox<SalaryReferenceItem> salaryCombo = new ComboBox<>();
        salaryCombo.setPromptText("Select salary");
        salaryCombo.setStyle(fieldStyle);
        
        // Load salary references from database
        ObservableList<SalaryReferenceItem> salaryReferences = FXCollections.observableArrayList();
        try {
            String sql = "SELECT id, monthly_salary FROM salary_reference ORDER BY monthly_salary";
            PreparedStatement stmt = connection.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                salaryReferences.add(new SalaryReferenceItem(
                    rs.getInt("id"),
                    rs.getDouble("monthly_salary")
                ));
            }
            salaryCombo.setItems(salaryReferences);
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error loading salary references", e);
            showErrorAlert("Database Error", "Failed to load salary references: " + e.getMessage());
        }
        
        salaryContainer.getChildren().addAll(salaryLabel, salaryCombo);

        // Populate fields if editing employee
        if (existing != null) {
            accountNumberField.setText(existing.getAccountNumber());
            nameField.setText(existing.getName());
            positionField.setText(existing.getPosition());
            
            // Set selected salary reference in combo box
            if (existing.getSalaryRefId() != null) {
                for (SalaryReferenceItem item : salaryReferences) {
                    if (item.getId() == existing.getSalaryRefId()) {
                        salaryCombo.setValue(item);
                        break;
                    }
                }
            } else {
                // If no salary_ref_id, try to find by salary amount
                for (SalaryReferenceItem item : salaryReferences) {
                    if (Math.abs(item.getMonthlySalary() - existing.getSalary()) < 0.01) {
                        salaryCombo.setValue(item);
                        break;
                    }
                }
            }
        }

        // Add all form elements to form container
        formContainer.getChildren().addAll(
            accountNumberContainer,
            nameContainer, 
            positionContainer,
            salaryContainer
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
        dialog.getDialogPane().setPrefHeight(450);

        // Add validation
        dialog.getDialogPane().lookupButton(saveButtonType).addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            if (accountNumberField.getText().trim().isEmpty()) {
                showErrorAlert("Validation Error", "Account number is required.");
                event.consume();
                return;
            }
            if (nameField.getText().trim().isEmpty()) {
                showErrorAlert("Validation Error", "Full name is required.");
                event.consume();
                return;
            }
            if (positionField.getText().trim().isEmpty()) {
                showErrorAlert("Validation Error", "Position is required.");
                event.consume();
                return;
            }
            if (salaryCombo.getValue() == null) {
                showErrorAlert("Validation Error", "Please select a salary from the dropdown.");
                event.consume();
                return;
            }
        });

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                SalaryReferenceItem selectedSalary = salaryCombo.getValue();
                if (selectedSalary == null) {
                    return null;
                }
                
                // For existing employee, preserve the ID, for new employee, ID will be auto-generated
                int employeeId = (existing != null) ? existing.getId() : 0;
                
                return new Employee(
                    employeeId, 
                    accountNumberField.getText().trim(),
                    nameField.getText().trim(), 
                    positionField.getText().trim(), 
                    selectedSalary.getMonthlySalary(),
                    selectedSalary.getId()
                );
            }
            return null;
        });

        return dialog;
    }

    private void showInfo(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    private void showSuccessAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.getDialogPane().setStyle("-fx-background-color: #f5f5f5;");
        alert.showAndWait();
    }
    
    private void showErrorAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.getDialogPane().setStyle("-fx-background-color: #f5f5f5;");
        alert.showAndWait();
    }
}


