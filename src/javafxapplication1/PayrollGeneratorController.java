package javafxapplication1;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.event.ActionEvent;
import javafx.scene.control.Alert.AlertType;
import javafx.application.Platform;
import javafx.concurrent.Task;

import java.net.URL;
import java.util.ResourceBundle;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.sql.*;
import java.util.Optional;
import javafx.scene.layout.GridPane;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PayrollGeneratorController implements Initializable {

    private static final Logger logger = Logger.getLogger(PayrollGeneratorController.class.getName());
    
    // Database connection constants
    private static final String DB_URL = DatabaseConfig.getDbUrl();
    private static final String DB_USER = DatabaseConfig.getDbUser();
    private static final String DB_PASSWORD = DatabaseConfig.getDbPassword();

    // FXML Components
    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusFilter;
    @FXML private DatePicker periodFilter;
    @FXML private TableView<PayrollEntry> payrollTable;
    @FXML private TableColumn<PayrollEntry, Integer> colPayrollId;
    @FXML private TableColumn<PayrollEntry, Integer> colEmployeeId;
    @FXML private TableColumn<PayrollEntry, String> colEmployeeName;
    @FXML private TableColumn<PayrollEntry, String> colPosition;
    @FXML private TableColumn<PayrollEntry, String> colPayPeriod;
    @FXML private TableColumn<PayrollEntry, Double> colBasicSalary;
    @FXML private TableColumn<PayrollEntry, Double> colOvertime;
    @FXML private TableColumn<PayrollEntry, Double> colAllowances;
    @FXML private TableColumn<PayrollEntry, Double> colDeductions;
    @FXML private TableColumn<PayrollEntry, Double> colNetPay;
    @FXML private TableColumn<PayrollEntry, String> colStatus;
    @FXML private TableColumn<PayrollEntry, String> colCreatedDate;
    @FXML private Button generatePayrollButton;
    @FXML private Button addPayrollButton;
    @FXML private Button editPayrollButton;
    @FXML private Button deletePayrollButton;

    // Data
    private ObservableList<PayrollEntry> payrollData = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initializeTable();
        initializeFilters();
        loadPayrollData();
        
        // Setup permission-based button visibility
        setupPermissionBasedVisibility();
    }
    
    private void setupPermissionBasedVisibility() {
        try {
            // Get current user from SessionManager
            SessionManager sessionManager = SessionManager.getInstance();
            
            if (sessionManager.isLoggedIn()) {
                String currentUser = sessionManager.getCurrentUser();
                logger.info("Setting up permission-based visibility for user: " + currentUser);
                
                // Check user permissions
                // Generate and Add both use payroll_gen.add permission
                boolean canAdd = hasUserPermission(currentUser, "payroll_gen.add");
                boolean canEdit = hasUserPermission(currentUser, "payroll_gen.edit");
                boolean canDelete = hasUserPermission(currentUser, "payroll_gen.delete");
                
                // Show/hide buttons based on permissions
                if (generatePayrollButton != null) {
                    generatePayrollButton.setVisible(canAdd);
                    generatePayrollButton.setManaged(canAdd);
                }
                if (addPayrollButton != null) {
                    addPayrollButton.setVisible(canAdd);
                    addPayrollButton.setManaged(canAdd);
                }
                if (editPayrollButton != null) {
                    editPayrollButton.setVisible(canEdit);
                    editPayrollButton.setManaged(canEdit);
                }
                if (deletePayrollButton != null) {
                    deletePayrollButton.setVisible(canDelete);
                    deletePayrollButton.setManaged(canDelete);
                }
                
                logger.info("Payroll Generator buttons visibility - Add/Generate: " + canAdd + ", Edit: " + canEdit + ", Delete: " + canDelete);
                
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
        if (generatePayrollButton != null) {
            generatePayrollButton.setVisible(false);
            generatePayrollButton.setManaged(false);
        }
        if (addPayrollButton != null) {
            addPayrollButton.setVisible(false);
            addPayrollButton.setManaged(false);
        }
        if (editPayrollButton != null) {
            editPayrollButton.setVisible(false);
            editPayrollButton.setManaged(false);
        }
        if (deletePayrollButton != null) {
            deletePayrollButton.setVisible(false);
            deletePayrollButton.setManaged(false);
        }
    }
    
    private boolean hasUserPermission(String username, String permissionName) {
        try (Connection conn = getConnection()) {
            // Get user's role from database
            String getRoleQuery = "SELECT role FROM users WHERE username = ?";
            try (PreparedStatement getRoleStmt = conn.prepareStatement(getRoleQuery)) {
                getRoleStmt.setString(1, username);
                try (ResultSet roleRs = getRoleStmt.executeQuery()) {
                    if (roleRs.next()) {
                        String userRole = roleRs.getString("role");
                        
                        // Check if role has the permission
                        String checkPermissionQuery = "SELECT COUNT(*) FROM role_permissions rp " +
                                                   "JOIN roles r ON rp.role_id = r.role_id " +
                                                   "JOIN permissions p ON rp.permission_id = p.permission_id " +
                                                   "WHERE r.role_name = ? AND p.permission_name = ? AND rp.granted = TRUE";
                        try (PreparedStatement checkPermissionStmt = conn.prepareStatement(checkPermissionQuery)) {
                            checkPermissionStmt.setString(1, userRole);
                            checkPermissionStmt.setString(2, permissionName);
                            try (ResultSet permissionRs = checkPermissionStmt.executeQuery()) {
                                if (permissionRs.next()) {
                                    boolean hasPermission = permissionRs.getInt(1) > 0;
                                    logger.info("User " + username + " (Role: " + userRole + ") has permission " + permissionName + ": " + hasPermission);
                                    return hasPermission;
                                }
                            }
                        }
                    }
                }
            }
            
            return false;
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error checking user permission", e);
            // If database error occurs, default to admin permissions for admin user
            return username != null && username.equals("admin");
        }
    }

    // Database connection method
    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }

    private void initializeTable() {
        // Set up table columns with property value factories
        colPayrollId.setCellValueFactory(new PropertyValueFactory<>("payrollId"));
        colEmployeeId.setCellValueFactory(new PropertyValueFactory<>("employeeId"));
        colEmployeeName.setCellValueFactory(new PropertyValueFactory<>("employeeName"));
        colPosition.setCellValueFactory(new PropertyValueFactory<>("position"));
        colPayPeriod.setCellValueFactory(new PropertyValueFactory<>("payPeriod"));
        colBasicSalary.setCellValueFactory(new PropertyValueFactory<>("basicSalary"));
        colOvertime.setCellValueFactory(new PropertyValueFactory<>("overtime"));
        colAllowances.setCellValueFactory(new PropertyValueFactory<>("allowances"));
        colDeductions.setCellValueFactory(new PropertyValueFactory<>("deductions"));
        colNetPay.setCellValueFactory(new PropertyValueFactory<>("netPay"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colCreatedDate.setCellValueFactory(new PropertyValueFactory<>("createdDate"));

        // Format currency columns
        formatCurrencyColumn(colBasicSalary);
        formatCurrencyColumn(colOvertime);
        formatCurrencyColumn(colAllowances);
        formatCurrencyColumn(colDeductions);
        formatCurrencyColumn(colNetPay);

        // Set the data to the table
        payrollTable.setItems(payrollData);
    }

    private void formatCurrencyColumn(TableColumn<PayrollEntry, Double> column) {
        column.setCellFactory(col -> new TableCell<PayrollEntry, Double>() {
            @Override
            protected void updateItem(Double amount, boolean empty) {
                super.updateItem(amount, empty);
                if (empty || amount == null) {
                    setText(null);
                } else {
                    setText(String.format("₱%.2f", amount));
                }
            }
        });
    }

    private void initializeFilters() {
        // Populate status filter
        statusFilter.setItems(FXCollections.observableArrayList(
            "All Status", "Draft", "Generated", "Paid", "Cancelled"
        ));
        statusFilter.setValue("All Status");
        
        // Set default period to current month
        periodFilter.setValue(LocalDate.now().withDayOfMonth(1));
        
        // Add listeners for real-time filtering
        searchField.textProperty().addListener((obs, oldVal, newVal) -> filterPayrollData());
        statusFilter.valueProperty().addListener((obs, oldVal, newVal) -> filterPayrollData());
        periodFilter.valueProperty().addListener((obs, oldVal, newVal) -> filterPayrollData());
    }

    private void loadPayrollData() {
        Task<ObservableList<PayrollEntry>> loadTask = new Task<ObservableList<PayrollEntry>>() {
            @Override
            protected ObservableList<PayrollEntry> call() throws Exception {
                ObservableList<PayrollEntry> entries = FXCollections.observableArrayList();
                
                String sql = """
                    SELECT 
                        pp.id as payroll_id,
                        pp.employee_id,
                        e.full_name as employee_name,
                        e.position,
                        CONCAT(pp.pay_period_start, ' to ', pp.pay_period_end) as pay_period,
                        pp.basic_salary,
                        pp.overtime_pay as overtime,
                        pp.allowances,
                        pp.total_deductions as deductions,
                        pp.net_pay,
                        pp.status,
                        pp.processed_at as created_date
                    FROM payroll_process pp
                    JOIN employees e ON pp.employee_id = e.id
                    ORDER BY pp.processed_at DESC
                    """;
                
                try (Connection conn = getConnection();
                     PreparedStatement stmt = conn.prepareStatement(sql);
                     ResultSet rs = stmt.executeQuery()) {
                    
                    while (rs.next()) {
                        PayrollEntry entry = new PayrollEntry();
                        entry.setPayrollId(rs.getInt("payroll_id"));
                        entry.setEmployeeId(rs.getInt("employee_id"));
                        entry.setEmployeeName(rs.getString("employee_name"));
                        entry.setPosition(rs.getString("position"));
                        entry.setPayPeriod(rs.getString("pay_period"));
                        entry.setBasicSalary(rs.getDouble("basic_salary"));
                        entry.setOvertime(rs.getDouble("overtime"));
                        entry.setAllowances(rs.getDouble("allowances"));
                        entry.setDeductions(rs.getDouble("deductions"));
                        entry.setNetPay(rs.getDouble("net_pay"));
                        entry.setStatus(rs.getString("status"));
                        entry.setCreatedDate(rs.getTimestamp("created_date").toLocalDateTime()
                                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                        
                        entries.add(entry);
                    }
                }
                
                return entries;
            }
        };
        
        loadTask.setOnSucceeded(e -> {
            payrollData.setAll(loadTask.getValue());
            filterPayrollData();
        });
        
        loadTask.setOnFailed(e -> {
            Throwable exception = loadTask.getException();
            showErrorAlert("Database Error", "Failed to load payroll data: " + exception.getMessage());
        });
        
        new Thread(loadTask).start();
    }

    private void filterPayrollData() {
        ObservableList<PayrollEntry> filteredData = FXCollections.observableArrayList();
        String searchTerm = searchField.getText().toLowerCase();
        String selectedStatus = statusFilter.getValue();
        LocalDate selectedPeriod = periodFilter.getValue();
        
        for (PayrollEntry entry : payrollData) {
            boolean matchesSearch = searchTerm.isEmpty() || 
                                  entry.getEmployeeName().toLowerCase().contains(searchTerm) ||
                                  String.valueOf(entry.getPayrollId()).contains(searchTerm) ||
                                  String.valueOf(entry.getEmployeeId()).contains(searchTerm);
            
            boolean matchesStatus = "All Status".equals(selectedStatus) || 
                                  entry.getStatus().equals(selectedStatus);
            
            boolean matchesPeriod = selectedPeriod == null ||
                                  entry.getPayPeriod().startsWith(selectedPeriod.format(DateTimeFormatter.ofPattern("yyyy-MM")));
            
            if (matchesSearch && matchesStatus && matchesPeriod) {
                filteredData.add(entry);
            }
        }
        
        payrollTable.setItems(filteredData);
    }

    @FXML
    private void onAddPayroll(ActionEvent event) {
        showAddPayrollDialog();
    }

    @FXML
    private void onGeneratePayroll(ActionEvent event) {
        showGeneratePayrollDialog();
    }

    private void showGeneratePayrollDialog() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Generate LGU Payroll");
        dialog.setHeaderText("Generate payroll using LGU calculation logic");
        
        // Set the button types
        ButtonType generateButtonType = new ButtonType("Generate All", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(generateButtonType, ButtonType.CANCEL);
        
        // Create the form
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(20, 150, 10, 10));
        
        DatePicker startDatePicker = new DatePicker();
        DatePicker endDatePicker = new DatePicker();
        
        // Set default values for current period
        LocalDate now = LocalDate.now();
        startDatePicker.setValue(now.withDayOfMonth(1));
        endDatePicker.setValue(now.withDayOfMonth(15)); // First half of month
        
        grid.add(new Label("Start Date:"), 0, 0);
        grid.add(startDatePicker, 1, 0);
        grid.add(new Label("End Date:"), 0, 1);
        grid.add(endDatePicker, 1, 1);
        
        Label infoLabel = new Label("This will generate payroll for all active employees\nusing the LGU calculation logic with:\n• Basic Pay = Monthly Salary / 2\n• Overload = Units × (Monthly Salary / 24) for instructors\n• Government deductions (PAG-IBIG, Expanded Tax, GVAT)\n• Attendance-based deductions");
        infoLabel.setStyle("-fx-text-fill: blue; -fx-font-size: 12px;");
        grid.add(infoLabel, 0, 2, 2, 1);
        
        dialog.getDialogPane().setContent(grid);
        
        // Convert the result when the generate button is clicked
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == generateButtonType) {
                LocalDate startDate = startDatePicker.getValue();
                LocalDate endDate = endDatePicker.getValue();
                
                if (startDate == null || endDate == null) {
                    showErrorAlert("Validation Error", "Please select both start and end dates.");
                    return null;
                }
                
                if (startDate.isAfter(endDate)) {
                    showErrorAlert("Validation Error", "Start date must be before end date.");
                    return null;
                }
                
                generatePayrollForAllEmployees(startDate, endDate);
            }
            return null;
        });
        
        dialog.showAndWait();
    }

    private void generatePayrollForAllEmployees(LocalDate startDate, LocalDate endDate) {
        Task<Void> generateTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                // Call the stored procedure to generate payroll
                String sql = "CALL sp_generate_payroll(?, ?, ?)";
                
                try (Connection conn = getConnection();
                     PreparedStatement stmt = conn.prepareStatement(sql)) {
                    
                    stmt.setDate(1, java.sql.Date.valueOf(startDate));
                    stmt.setDate(2, java.sql.Date.valueOf(endDate));
                    stmt.setString(3, "SYSTEM_AUTO"); // processed_by
                    
                    stmt.execute();
                }
                
                return null;
            }
        };
        
        generateTask.setOnSucceeded(e -> {
            Platform.runLater(() -> {
                showInfoAlert("Success", 
                    "Payroll generated successfully for period: " + 
                    startDate + " to " + endDate + 
                    "\n\nUsing LGU calculation logic:\n" +
                    "• Instructors: Basic pay + Overload based on units\n" +
                    "• Staff: Basic pay only\n" +
                    "• All deductions applied automatically");
                loadPayrollData(); // Refresh the table
            });
        });
        
        generateTask.setOnFailed(e -> {
            Throwable exception = generateTask.getException();
            Platform.runLater(() -> {
                showErrorAlert("Generation Error", 
                    "Failed to generate payroll: " + exception.getMessage() + 
                    "\n\nPlease check:\n" +
                    "• Database connection\n" +
                    "• Employee salary references\n" +
                    "• Processed attendance data");
                exception.printStackTrace();
            });
        });
        
        new Thread(generateTask).start();
    }

    private void showAddPayrollDialog() {
        Dialog<PayrollEntry> dialog = new Dialog<>();
        dialog.setTitle("Generate Payroll Entry");
        dialog.setHeaderText("Generate payroll from processed attendance data:");
        
        // Set the button types
        ButtonType addButtonType = new ButtonType("Generate", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);
        
        // Create the enhanced form for attendance-based generation
        GridPane grid = createAttendanceBasedPayrollForm();
        dialog.getDialogPane().setContent(grid);
        
        // Get form controls
        ComboBox<Employee> employeeCombo = (ComboBox<Employee>) grid.getChildren().get(1);
        DatePicker startDatePicker = (DatePicker) grid.getChildren().get(3);
        DatePicker endDatePicker = (DatePicker) grid.getChildren().get(5);
        Label attendanceInfoLabel = (Label) grid.getChildren().get(7);
        TextField basicSalaryField = (TextField) grid.getChildren().get(9);
        TextField overtimeField = (TextField) grid.getChildren().get(11);
        TextField allowancesField = (TextField) grid.getChildren().get(13);
        TextField deductionsField = (TextField) grid.getChildren().get(15);
        Label netPayLabel = (Label) grid.getChildren().get(17);
        ComboBox<String> statusCombo = (ComboBox<String>) grid.getChildren().get(19);
        
        // Load employees
        loadEmployeesForCombo(employeeCombo);
        
        // Set default values
        startDatePicker.setValue(LocalDate.now().withDayOfMonth(1));
        endDatePicker.setValue(LocalDate.now());
        statusCombo.setValue("Draft");
        
        // Auto-calculate when employee or dates change
        Runnable calculatePayroll = () -> {
            Employee selectedEmployee = employeeCombo.getValue();
            LocalDate startDate = startDatePicker.getValue();
            LocalDate endDate = endDatePicker.getValue();
            
            if (selectedEmployee != null && startDate != null && endDate != null) {
                calculatePayrollFromProcessedAttendance(selectedEmployee, startDate, endDate, 
                    basicSalaryField, overtimeField, deductionsField, attendanceInfoLabel, netPayLabel);
            }
        };
        
        employeeCombo.valueProperty().addListener((obs, oldVal, newVal) -> calculatePayroll.run());
        startDatePicker.valueProperty().addListener((obs, oldVal, newVal) -> calculatePayroll.run());
        endDatePicker.valueProperty().addListener((obs, oldVal, newVal) -> calculatePayroll.run());
        
        // Convert the result when the generate button is clicked
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == addButtonType) {
                try {
                    PayrollEntry entry = new PayrollEntry();
                    Employee selectedEmployee = employeeCombo.getValue();
                    if (selectedEmployee == null) {
                        showErrorAlert("Validation Error", "Please select an employee.");
                        return null;
                    }
                    
                    entry.setEmployeeId(selectedEmployee.getId());
                    entry.setEmployeeName(selectedEmployee.getFullName());
                    entry.setPosition(selectedEmployee.getPosition());
                    entry.setPayPeriod(startDatePicker.getValue() + " to " + endDatePicker.getValue());
                    entry.setBasicSalary(Double.parseDouble(basicSalaryField.getText()));
                    entry.setOvertime(Double.parseDouble(overtimeField.getText()));
                    entry.setAllowances(Double.parseDouble(allowancesField.getText()));
                    entry.setDeductions(Double.parseDouble(deductionsField.getText()));
                    entry.setStatus(statusCombo.getValue());
                    entry.calculateNetPay();
                    
                    return entry;
                } catch (NumberFormatException e) {
                    showErrorAlert("Validation Error", "Please enter valid numbers for salary fields.");
                    return null;
                }
            }
            return null;
        });
        
        Optional<PayrollEntry> result = dialog.showAndWait();
        result.ifPresent(this::savePayrollEntry);
    }

    private GridPane createPayrollForm() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(20, 150, 10, 10));
        
        // Employee selection
        grid.add(new Label("Employee:"), 0, 0);
        ComboBox<Employee> employeeCombo = new ComboBox<>();
        employeeCombo.setPromptText("Select Employee");
        employeeCombo.setPrefWidth(200);
        grid.add(employeeCombo, 1, 0);
        
        // Pay Period
        grid.add(new Label("Pay Period:"), 0, 1);
        TextField payPeriodField = new TextField();
        payPeriodField.setPromptText("YYYY-MM");
        grid.add(payPeriodField, 1, 1);
        
        // Basic Salary
        grid.add(new Label("Basic Salary:"), 0, 2);
        TextField basicSalaryField = new TextField();
        basicSalaryField.setPromptText("0.00");
        grid.add(basicSalaryField, 1, 2);
        
        // Overtime
        grid.add(new Label("Overtime:"), 0, 3);
        TextField overtimeField = new TextField();
        overtimeField.setPromptText("0.00");
        overtimeField.setText("0.00");
        grid.add(overtimeField, 1, 3);
        
        // Allowances
        grid.add(new Label("Allowances:"), 0, 4);
        TextField allowancesField = new TextField();
        allowancesField.setPromptText("0.00");
        allowancesField.setText("0.00");
        grid.add(allowancesField, 1, 4);
        
        // Deductions
        grid.add(new Label("Deductions:"), 0, 5);
        TextField deductionsField = new TextField();
        deductionsField.setPromptText("0.00");
        deductionsField.setText("0.00");
        grid.add(deductionsField, 1, 5);
        
        // Status
        grid.add(new Label("Status:"), 0, 6);
        ComboBox<String> statusCombo = new ComboBox<>();
        statusCombo.setItems(FXCollections.observableArrayList("Draft", "Generated", "Paid"));
        grid.add(statusCombo, 1, 6);
        
        return grid;
    }

    private GridPane createAttendanceBasedPayrollForm() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(20, 150, 10, 10));
        
        // Employee selection
        grid.add(new Label("Employee:"), 0, 0);
        ComboBox<Employee> employeeCombo = new ComboBox<>();
        employeeCombo.setPromptText("Select Employee");
        employeeCombo.setPrefWidth(250);
        grid.add(employeeCombo, 1, 0);
        
        // Start Date
        grid.add(new Label("Start Date:"), 0, 1);
        DatePicker startDatePicker = new DatePicker();
        grid.add(startDatePicker, 1, 1);
        
        // End Date  
        grid.add(new Label("End Date:"), 0, 2);
        DatePicker endDatePicker = new DatePicker();
        grid.add(endDatePicker, 1, 2);
        
        // Attendance Info
        grid.add(new Label("Attendance Info:"), 0, 3);
        Label attendanceInfoLabel = new Label("Select employee and dates");
        attendanceInfoLabel.setStyle("-fx-text-fill: blue; -fx-font-style: italic;");
        grid.add(attendanceInfoLabel, 1, 3);
        
        // Basic Salary
        grid.add(new Label("Basic Salary:"), 0, 4);
        TextField basicSalaryField = new TextField();
        basicSalaryField.setPromptText("0.00");
        basicSalaryField.setEditable(false);
        grid.add(basicSalaryField, 1, 4);
        
        // Overtime Pay
        grid.add(new Label("Overtime Pay:"), 0, 5);
        TextField overtimeField = new TextField();
        overtimeField.setPromptText("0.00");
        overtimeField.setEditable(false);
        grid.add(overtimeField, 1, 5);
        
        // Allowances
        grid.add(new Label("Allowances:"), 0, 6);
        TextField allowancesField = new TextField();
        allowancesField.setPromptText("0.00");
        allowancesField.setText("0.00");
        grid.add(allowancesField, 1, 6);
        
        // Total Deductions
        grid.add(new Label("Total Deductions:"), 0, 7);
        TextField deductionsField = new TextField();
        deductionsField.setPromptText("0.00");
        deductionsField.setEditable(false);
        grid.add(deductionsField, 1, 7);
        
        // Net Pay
        grid.add(new Label("Net Pay:"), 0, 8);
        Label netPayLabel = new Label("₱0.00");
        netPayLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: green;");
        grid.add(netPayLabel, 1, 8);
        
        // Status
        grid.add(new Label("Status:"), 0, 9);
        ComboBox<String> statusCombo = new ComboBox<>();
        statusCombo.setItems(FXCollections.observableArrayList("Draft", "Generated", "Paid"));
        grid.add(statusCombo, 1, 9);
        
        return grid;
    }

    private void loadEmployeesForCombo(ComboBox<Employee> combo) {
        Task<ObservableList<Employee>> loadTask = new Task<ObservableList<Employee>>() {
            @Override
            protected ObservableList<Employee> call() throws Exception {
                ObservableList<Employee> employees = FXCollections.observableArrayList();
                
                String sql = """
                    SELECT e.id, e.account_number, e.full_name, e.position, sr.monthly_salary as salary,
                           e.employment_type, e.assigned_units
                    FROM employees e 
                    JOIN salary_reference sr ON e.salary_ref_id = sr.id
                    WHERE e.status = 'Active' ORDER BY e.full_name
                    """;
                
                try (Connection conn = getConnection();
                     PreparedStatement stmt = conn.prepareStatement(sql);
                     ResultSet rs = stmt.executeQuery()) {
                    
                    while (rs.next()) {
                        Employee emp = new Employee();
                        emp.setId(rs.getInt("id"));
                        emp.setAccountNumber(rs.getString("account_number"));
                        emp.setFullName(rs.getString("full_name"));
                        emp.setPosition(rs.getString("position"));
                        emp.setSalary(rs.getDouble("salary"));
                        employees.add(emp);
                    }
                }
                
                return employees;
            }
        };
        
        loadTask.setOnSucceeded(e -> {
            combo.setItems(loadTask.getValue());
            // Auto-fill basic salary when employee is selected
            combo.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null) {
                    GridPane parent = (GridPane) combo.getParent();
                    TextField basicSalaryField = (TextField) parent.getChildren().get(5);
                    basicSalaryField.setText(String.valueOf(newVal.getSalary()));
                }
            });
        });
        
        new Thread(loadTask).start();
    }

    private void savePayrollEntry(PayrollEntry entry) {
        Task<Void> saveTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                String sql = """
                    INSERT INTO payroll_process 
                    (employee_id, account_number, pay_period_start, pay_period_end, basic_salary, 
                     overtime_pay, allowances, total_deductions, net_pay, status, processed_by)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'SYSTEM')
                    """;
                
                try (Connection conn = getConnection();
                     PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    
                    // Parse pay period
                    String[] dates = entry.getPayPeriod().split(" to ");
                    
                    stmt.setInt(1, entry.getEmployeeId());
                    stmt.setString(2, "EMP" + entry.getEmployeeId()); // account_number
                    stmt.setDate(3, java.sql.Date.valueOf(dates[0]));
                    stmt.setDate(4, java.sql.Date.valueOf(dates[1]));
                    stmt.setDouble(5, entry.getBasicSalary());
                    stmt.setDouble(6, entry.getOvertime());
                    stmt.setDouble(7, entry.getAllowances());
                    stmt.setDouble(8, entry.getDeductions());
                    stmt.setDouble(9, entry.getNetPay());
                    stmt.setString(10, entry.getStatus());
                    
                    stmt.executeUpdate();
                    
                    // Get generated payroll ID
                    try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            entry.setPayrollId(generatedKeys.getInt(1));
                        }
                    }
                }
                
                return null;
            }
        };
        
        saveTask.setOnSucceeded(e -> {
            Platform.runLater(() -> {
                showInfoAlert("Success", "Payroll entry added successfully!");
                loadPayrollData(); // Refresh the table
            });
        });
        
        saveTask.setOnFailed(e -> {
            Throwable exception = saveTask.getException();
            Platform.runLater(() -> {
                if (exception.getMessage().contains("Duplicate entry")) {
                    showErrorAlert("Duplicate Entry", "A payroll entry already exists for this employee and period.");
                } else {
                    showErrorAlert("Database Error", "Failed to save payroll entry: " + exception.getMessage());
                }
            });
        });
        
        new Thread(saveTask).start();
    }

    @FXML
    private void onSearch(ActionEvent event) {
        filterPayrollData();
    }

    @FXML
    private void onViewGenerated(ActionEvent event) {
        statusFilter.setValue("Generated");
        filterPayrollData();
    }

    @FXML
    private void onEditPayroll(ActionEvent event) {
        PayrollEntry selectedPayroll = payrollTable.getSelectionModel().getSelectedItem();
        if (selectedPayroll == null) {
            showErrorAlert("No Selection", "Please select a payroll entry to edit.");
            return;
        }
        showEditPayrollDialog(selectedPayroll);
    }

    private void showEditPayrollDialog(PayrollEntry entry) {
        Dialog<PayrollEntry> dialog = new Dialog<>();
        dialog.setTitle("Edit Payroll Entry");
        dialog.setHeaderText("Edit payroll details for: " + entry.getEmployeeName());
        
        ButtonType updateButtonType = new ButtonType("Update", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(updateButtonType, ButtonType.CANCEL);
        
        GridPane grid = createPayrollForm();
        dialog.getDialogPane().setContent(grid);
        
        // Pre-fill form with existing data
        TextField payPeriodField = (TextField) grid.getChildren().get(3);
        TextField basicSalaryField = (TextField) grid.getChildren().get(5);
        TextField overtimeField = (TextField) grid.getChildren().get(7);
        TextField allowancesField = (TextField) grid.getChildren().get(9);
        TextField deductionsField = (TextField) grid.getChildren().get(11);
        ComboBox<String> statusCombo = (ComboBox<String>) grid.getChildren().get(13);
        
        payPeriodField.setText(entry.getPayPeriod());
        basicSalaryField.setText(String.valueOf(entry.getBasicSalary()));
        overtimeField.setText(String.valueOf(entry.getOvertime()));
        allowancesField.setText(String.valueOf(entry.getAllowances()));
        deductionsField.setText(String.valueOf(entry.getDeductions()));
        statusCombo.setValue(entry.getStatus());
        
        // Disable employee selection for editing
        ComboBox<Employee> employeeCombo = (ComboBox<Employee>) grid.getChildren().get(1);
        employeeCombo.setDisable(true);
        
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == updateButtonType) {
                try {
                    entry.setPayPeriod(payPeriodField.getText());
                    entry.setBasicSalary(Double.parseDouble(basicSalaryField.getText()));
                    entry.setOvertime(Double.parseDouble(overtimeField.getText()));
                    entry.setAllowances(Double.parseDouble(allowancesField.getText()));
                    entry.setDeductions(Double.parseDouble(deductionsField.getText()));
                    entry.setStatus(statusCombo.getValue());
                    entry.calculateNetPay();
                    
                    return entry;
                } catch (NumberFormatException e) {
                    showErrorAlert("Validation Error", "Please enter valid numbers for salary fields.");
                    return null;
                }
            }
            return null;
        });
        
        Optional<PayrollEntry> result = dialog.showAndWait();
        result.ifPresent(this::updatePayrollEntry);
    }

    private void updatePayrollEntry(PayrollEntry entry) {
        Task<Void> updateTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                String sql = """
                    UPDATE payroll_history SET
                        pay_period = ?, basic_salary = ?, overtime = ?, allowances = ?, 
                        deductions = ?, net_pay = ?, status = ?, updated_at = CURRENT_TIMESTAMP
                    WHERE payroll_id = ?
                    """;
                
                try (Connection conn = getConnection();
                     PreparedStatement stmt = conn.prepareStatement(sql)) {
                    
                    stmt.setString(1, entry.getPayPeriod());
                    stmt.setDouble(2, entry.getBasicSalary());
                    stmt.setDouble(3, entry.getOvertime());
                    stmt.setDouble(4, entry.getAllowances());
                    stmt.setDouble(5, entry.getDeductions());
                    stmt.setDouble(6, entry.getNetPay());
                    stmt.setString(7, entry.getStatus());
                    stmt.setInt(8, entry.getPayrollId());
                    
                    stmt.executeUpdate();
                }
                
                return null;
            }
        };
        
        updateTask.setOnSucceeded(e -> {
            Platform.runLater(() -> {
                showInfoAlert("Success", "Payroll entry updated successfully!");
                loadPayrollData();
            });
        });
        
        updateTask.setOnFailed(e -> {
            Throwable exception = updateTask.getException();
            Platform.runLater(() -> {
                showErrorAlert("Database Error", "Failed to update payroll entry: " + exception.getMessage());
            });
        });
        
        new Thread(updateTask).start();
    }

    @FXML
    private void onDeletePayroll(ActionEvent event) {
        PayrollEntry selectedPayroll = payrollTable.getSelectionModel().getSelectedItem();
        if (selectedPayroll == null) {
            showErrorAlert("No Selection", "Please select a payroll entry to delete.");
            return;
        }
        
        Alert confirmAlert = new Alert(AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirm Delete");
        confirmAlert.setHeaderText("Delete Payroll Entry");
        confirmAlert.setContentText("Are you sure you want to delete the payroll entry for " + 
                                   selectedPayroll.getEmployeeName() + "?");
        
        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            deletePayrollEntry(selectedPayroll);
        }
    }

    private void deletePayrollEntry(PayrollEntry entry) {
        Task<Void> deleteTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                String sql = "DELETE FROM payroll_history WHERE payroll_id = ?";
                
                try (Connection conn = getConnection();
                     PreparedStatement stmt = conn.prepareStatement(sql)) {
                    
                    stmt.setInt(1, entry.getPayrollId());
                    stmt.executeUpdate();
                }
                
                return null;
            }
        };
        
        deleteTask.setOnSucceeded(e -> {
            Platform.runLater(() -> {
                showInfoAlert("Success", "Payroll entry deleted successfully!");
                loadPayrollData();
            });
        });
        
        deleteTask.setOnFailed(e -> {
            Throwable exception = deleteTask.getException();
            Platform.runLater(() -> {
                showErrorAlert("Database Error", "Failed to delete payroll entry: " + exception.getMessage());
            });
        });
        
        new Thread(deleteTask).start();
    }

    private void calculatePayrollFromProcessedAttendance(Employee employee, LocalDate startDate, LocalDate endDate,
            TextField basicSalaryField, TextField overtimeField, TextField deductionsField, 
            Label attendanceInfoLabel, Label netPayLabel) {
        
        Task<Void> calculateTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                
                // 1. Get employee basic salary
                double basicSalary = employee.getSalary();
                
                // 2. Get processed attendance data
                String attendanceSQL = """
                    SELECT 
                        SUM(hours_worked) as total_hours,
                        SUM(overtime_hours) as total_overtime,
                        SUM(late_minutes) as total_late_minutes,
                        SUM(absent_days) as total_absent_days,
                        COUNT(*) as total_days
                    FROM processed_attendance 
                    WHERE employee_id = ? AND process_date BETWEEN ? AND ?
                    """;
                
                double totalHours = 0, totalOvertime = 0, totalLateMinutes = 0, totalAbsentDays = 0;
                int totalDays = 0;
                
                try (Connection conn = getConnection();
                     PreparedStatement stmt = conn.prepareStatement(attendanceSQL)) {
                    
                    stmt.setInt(1, employee.getId());
                    stmt.setDate(2, java.sql.Date.valueOf(startDate));
                    stmt.setDate(3, java.sql.Date.valueOf(endDate));
                    
                    ResultSet rs = stmt.executeQuery();
                    if (rs.next()) {
                        totalHours = rs.getDouble("total_hours");
                        totalOvertime = rs.getDouble("total_overtime");
                        totalLateMinutes = rs.getDouble("total_late_minutes");
                        totalAbsentDays = rs.getDouble("total_absent_days");
                        totalDays = rs.getInt("total_days");
                    }
                }
                
                // Make final copies for use in lambda
                final double finalTotalHours = totalHours;
                final double finalTotalOvertime = totalOvertime;
                final double finalTotalLateMinutes = totalLateMinutes;
                final double finalTotalAbsentDays = totalAbsentDays;
                final int finalTotalDays = totalDays;
                
                // 3. Calculate overtime pay (assume 1.5x hourly rate)
                double hourlyRate = basicSalary / (22 * 8); // 22 working days, 8 hours per day
                double overtimePay = totalOvertime * hourlyRate * 1.5;
                
                // 4. Calculate deductions from deduction_types and employee_deductions
                double totalDeductions = 0;
                
                // 4a. Get mandatory deductions from deduction_types
                String deductionTypesSQL = """
                    SELECT id, name, fixed_amount, percentage 
                    FROM deduction_types 
                    WHERE name IN ('Pag-ibig', 'GVAT', 'Expanded Tax')
                    """;
                
                try (Connection conn = getConnection();
                     PreparedStatement stmt = conn.prepareStatement(deductionTypesSQL)) {
                    
                    ResultSet rs = stmt.executeQuery();
                    while (rs.next()) {
                        String name = rs.getString("name");
                        Double fixedAmount = rs.getDouble("fixed_amount");
                        Double percentage = rs.getDouble("percentage");
                        
                        if (fixedAmount != null && fixedAmount > 0) {
                            totalDeductions += fixedAmount;
                        } else if (percentage != null && percentage > 0) {
                            totalDeductions += (basicSalary * percentage / 100);
                        }
                    }
                }
                
                // 4b. Get employee-specific deductions
                String employeeDeductionsSQL = """
                    SELECT ed.amount, dt.name 
                    FROM employee_deductions ed
                    JOIN deduction_types dt ON ed.deduction_type_id = dt.id
                    WHERE ed.employee_id = ?
                    """;
                
                try (Connection conn = getConnection();
                     PreparedStatement stmt = conn.prepareStatement(employeeDeductionsSQL)) {
                    
                    stmt.setInt(1, employee.getId());
                    ResultSet rs = stmt.executeQuery();
                    while (rs.next()) {
                        totalDeductions += rs.getDouble("amount");
                    }
                }
                
                // 4c. Calculate late and absent deductions
                double lateDeduction = (totalLateMinutes / 60.0) * hourlyRate; // Deduct per hour of lateness
                double absentDeduction = totalAbsentDays * (basicSalary / 22); // Per day deduction
                totalDeductions += lateDeduction + absentDeduction;
                
                // 5. Calculate net pay
                double netPay = basicSalary + overtimePay - totalDeductions;
                
                // Make final copies for use in lambda
                final double finalTotalDeductions = totalDeductions;
                final double finalNetPay = netPay;
                final double finalOvertimePay = overtimePay;
                
                // Update UI on JavaFX thread
                Platform.runLater(() -> {
                    basicSalaryField.setText(String.format("%.2f", basicSalary));
                    overtimeField.setText(String.format("%.2f", finalOvertimePay));
                    deductionsField.setText(String.format("%.2f", finalTotalDeductions));
                    netPayLabel.setText(String.format("₱%.2f", finalNetPay));
                    
                    String attendanceInfo = String.format(
                        "Days: %d | Hours: %.2f | OT: %.2f hrs | Late: %.0f mins | Absent: %.0f days",
                        finalTotalDays, finalTotalHours, finalTotalOvertime, finalTotalLateMinutes, finalTotalAbsentDays
                    );
                    attendanceInfoLabel.setText(attendanceInfo);
                });
                
                return null;
            }
        };
        
        calculateTask.setOnFailed(e -> {
            Throwable exception = calculateTask.getException();
            Platform.runLater(() -> {
                attendanceInfoLabel.setText("Error calculating payroll: " + exception.getMessage());
                System.err.println("Payroll calculation error: " + exception.getMessage());
                exception.printStackTrace();
            });
        });
        
        new Thread(calculateTask).start();
    }

    private void showInfoAlert(String title, String message) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showErrorAlert(String title, String message) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // Employee helper class
    private static class Employee {
        private int id;
        private String accountNumber;
        private String fullName;
        private String position;
        private double salary;
        
        // Getters and setters
        public int getId() { return id; }
        public void setId(int id) { this.id = id; }
        
        public String getAccountNumber() { return accountNumber; }
        public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }
        
        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }
        
        public String getPosition() { return position; }
        public void setPosition(String position) { this.position = position; }
        
        public double getSalary() { return salary; }
        public void setSalary(double salary) { this.salary = salary; }
        
        @Override
        public String toString() {
            return fullName + " (" + accountNumber + ")";
        }
    }
}