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

public class PayrollGeneratorController implements Initializable {

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

    // Data
    private ObservableList<PayrollEntry> payrollData = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initializeTable();
        initializeFilters();
        loadPayrollData();
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
                        ph.payroll_id,
                        ph.employee_id,
                        e.full_name as employee_name,
                        e.position,
                        ph.pay_period,
                        ph.basic_salary,
                        ph.overtime,
                        ph.allowances,
                        ph.deductions,
                        ph.net_pay,
                        ph.status,
                        ph.created_date
                    FROM payroll_history ph
                    JOIN employees e ON ph.employee_id = e.id
                    ORDER BY ph.created_date DESC
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

    private void showAddPayrollDialog() {
        Dialog<PayrollEntry> dialog = new Dialog<>();
        dialog.setTitle("Add New Payroll Entry");
        dialog.setHeaderText("Enter payroll details:");
        
        // Set the button types
        ButtonType addButtonType = new ButtonType("Add", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);
        
        // Create the form
        GridPane grid = createPayrollForm();
        dialog.getDialogPane().setContent(grid);
        
        // Get form controls
        ComboBox<Employee> employeeCombo = (ComboBox<Employee>) grid.getChildren().get(1);
        TextField payPeriodField = (TextField) grid.getChildren().get(3);
        TextField basicSalaryField = (TextField) grid.getChildren().get(5);
        TextField overtimeField = (TextField) grid.getChildren().get(7);
        TextField allowancesField = (TextField) grid.getChildren().get(9);
        TextField deductionsField = (TextField) grid.getChildren().get(11);
        ComboBox<String> statusCombo = (ComboBox<String>) grid.getChildren().get(13);
        
        // Load employees
        loadEmployeesForCombo(employeeCombo);
        
        // Set default values
        payPeriodField.setText(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM")));
        statusCombo.setValue("Draft");
        
        // Convert the result when the add button is clicked
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

    private void loadEmployeesForCombo(ComboBox<Employee> combo) {
        Task<ObservableList<Employee>> loadTask = new Task<ObservableList<Employee>>() {
            @Override
            protected ObservableList<Employee> call() throws Exception {
                ObservableList<Employee> employees = FXCollections.observableArrayList();
                
                String sql = "SELECT id, account_number, full_name, position, salary FROM employees WHERE status = 'Active' ORDER BY full_name";
                
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
                    INSERT INTO payroll_history 
                    (employee_id, pay_period, basic_salary, overtime, allowances, deductions, net_pay, status)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    """;
                
                try (Connection conn = getConnection();
                     PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    
                    stmt.setInt(1, entry.getEmployeeId());
                    stmt.setString(2, entry.getPayPeriod());
                    stmt.setDouble(3, entry.getBasicSalary());
                    stmt.setDouble(4, entry.getOvertime());
                    stmt.setDouble(5, entry.getAllowances());
                    stmt.setDouble(6, entry.getDeductions());
                    stmt.setDouble(7, entry.getNetPay());
                    stmt.setString(8, entry.getStatus());
                    
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