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

public class PayrollProcessingController implements Initializable {

    // Database connection constants
    private static final String DB_URL = DatabaseConfig.getDbUrl();
    private static final String DB_USER = DatabaseConfig.getDbUser();
    private static final String DB_PASSWORD = DatabaseConfig.getDbPassword();


    // FXML Components
    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> monthFilter;
    @FXML private ComboBox<String> yearFilter;
    @FXML private TableView<PayrollProcessEntry> payrollTable;
    @FXML private TableColumn<PayrollProcessEntry, Integer> colEmployeeId;
    @FXML private TableColumn<PayrollProcessEntry, String> colEmployeeName;
    @FXML private TableColumn<PayrollProcessEntry, String> colPosition;
    @FXML private TableColumn<PayrollProcessEntry, Double> colBasicSalary;
    @FXML private TableColumn<PayrollProcessEntry, Double> colOvertime;
    @FXML private TableColumn<PayrollProcessEntry, Double> colAllowances;
    @FXML private TableColumn<PayrollProcessEntry, Double> colDeductions;
    @FXML private TableColumn<PayrollProcessEntry, Double> colNetPay;
    @FXML private TableColumn<PayrollProcessEntry, String> colStatus;

    // Data
    private ObservableList<PayrollProcessEntry> payrollData = FXCollections.observableArrayList();

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
        // Set up table columns
        colEmployeeId.setCellValueFactory(new PropertyValueFactory<>("employeeId"));
        colEmployeeName.setCellValueFactory(new PropertyValueFactory<>("employeeName"));
        colPosition.setCellValueFactory(new PropertyValueFactory<>("position"));
        colBasicSalary.setCellValueFactory(new PropertyValueFactory<>("basicSalary"));
        colOvertime.setCellValueFactory(new PropertyValueFactory<>("overtimePay"));
        colAllowances.setCellValueFactory(new PropertyValueFactory<>("allowances"));
        colDeductions.setCellValueFactory(new PropertyValueFactory<>("totalDeductions"));
        colNetPay.setCellValueFactory(new PropertyValueFactory<>("netPay"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        // Format currency columns
        formatCurrencyColumn(colBasicSalary);
        formatCurrencyColumn(colOvertime);
        formatCurrencyColumn(colAllowances);
        formatCurrencyColumn(colDeductions);
        formatCurrencyColumn(colNetPay);

        // Set the data to the table
        payrollTable.setItems(payrollData);
    }

    private void formatCurrencyColumn(TableColumn<PayrollProcessEntry, Double> column) {
        column.setCellFactory(col -> new TableCell<PayrollProcessEntry, Double>() {
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
        // Set default date range to current month
        LocalDate now = LocalDate.now();
        startDatePicker.setValue(now.withDayOfMonth(1));
        endDatePicker.setValue(now.withDayOfMonth(now.lengthOfMonth()));
        
        // Initialize month filter
        monthFilter.setItems(FXCollections.observableArrayList(
            "All Months", "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
        ));
        // Set current month as default
        monthFilter.setValue(now.getMonth().toString());
        
        // Initialize year filter (current year and previous/next few years)
        ObservableList<String> yearList = FXCollections.observableArrayList();
        yearList.add("All Years");
        int currentYear = now.getYear();
        for (int year = currentYear - 3; year <= currentYear + 2; year++) {
            yearList.add(String.valueOf(year));
        }
        yearFilter.setItems(yearList);
        yearFilter.setValue(String.valueOf(currentYear)); // Set current year as default
        
        // Add listeners for real-time filtering
        searchField.textProperty().addListener((obs, oldVal, newVal) -> filterPayrollData());
        monthFilter.valueProperty().addListener((obs, oldVal, newVal) -> filterPayrollData());
        yearFilter.valueProperty().addListener((obs, oldVal, newVal) -> filterPayrollData());
        
        // Add listener to date pickers to update month/year filters accordingly
        startDatePicker.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                updateDateFilters(newVal);
            }
        });
    }

    private void loadPayrollData() {
        Task<ObservableList<PayrollProcessEntry>> loadTask = new Task<ObservableList<PayrollProcessEntry>>() {
            @Override
            protected ObservableList<PayrollProcessEntry> call() throws Exception {
                ObservableList<PayrollProcessEntry> entries = FXCollections.observableArrayList();
                
                String sql = """
                    SELECT 
                        pp.process_id,
                        pp.employee_id,
                        e.account_number,
                        e.full_name as employee_name,
                        e.position,
                        pp.pay_period_start,
                        pp.pay_period_end,
                        pp.basic_salary,
                        pp.present_days,
                        pp.absent_days,
                        pp.late_occurrences,
                        pp.overtime_hours,
                        pp.basic_pay,
                        pp.overtime_pay,
                        pp.allowances,
                        pp.total_deductions,
                        pp.net_pay,
                        pp.status
                    FROM payroll_process pp
                    JOIN employees e ON pp.employee_id = e.id
                    ORDER BY pp.processed_date DESC
                    """;
                
                try (Connection conn = getConnection();
                     PreparedStatement stmt = conn.prepareStatement(sql);
                     ResultSet rs = stmt.executeQuery()) {
                    
                    while (rs.next()) {
                        PayrollProcessEntry entry = new PayrollProcessEntry();
                        entry.setProcessId(rs.getInt("process_id"));
                        entry.setEmployeeId(rs.getInt("employee_id"));
                        entry.setAccountNumber(rs.getString("account_number"));
                        entry.setEmployeeName(rs.getString("employee_name"));
                        entry.setPosition(rs.getString("position"));
                        entry.setPayPeriodStart(rs.getDate("pay_period_start").toLocalDate().toString());
                        entry.setPayPeriodEnd(rs.getDate("pay_period_end").toLocalDate().toString());
                        entry.setBasicSalary(rs.getDouble("basic_salary"));
                        entry.setPresentDays(rs.getInt("present_days"));
                        entry.setAbsentDays(rs.getInt("absent_days"));
                        entry.setLateOccurrences(rs.getInt("late_occurrences"));
                        entry.setOvertimeHours(rs.getDouble("overtime_hours"));
                        entry.setBasicPay(rs.getDouble("basic_pay"));
                        entry.setOvertimePay(rs.getDouble("overtime_pay"));
                        entry.setAllowances(rs.getDouble("allowances"));
                        entry.setTotalDeductions(rs.getDouble("total_deductions"));
                        entry.setNetPay(rs.getDouble("net_pay"));
                        entry.setStatus(rs.getString("status"));
                        
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

    private void updateDateFilters(LocalDate selectedDate) {
        // Update month and year filters when date picker changes
        String monthName = selectedDate.getMonth().toString();
        String year = String.valueOf(selectedDate.getYear());
        
        // Update filters without triggering listeners temporarily
        monthFilter.setValue(monthName);
        yearFilter.setValue(year);
        
        // Update end date picker if start date changes
        if (selectedDate.equals(startDatePicker.getValue())) {
            endDatePicker.setValue(selectedDate.withDayOfMonth(selectedDate.lengthOfMonth()));
        }
    }

    private void filterPayrollData() {
        ObservableList<PayrollProcessEntry> allData = FXCollections.observableArrayList();
        
        // Get all data first
        String sql = """
            SELECT 
                pp.process_id,
                pp.employee_id,
                e.account_number,
                e.full_name as employee_name,
                e.position,
                pp.pay_period_start,
                pp.pay_period_end,
                pp.basic_salary,
                pp.present_days,
                pp.absent_days,
                pp.late_occurrences,
                pp.overtime_hours,
                pp.basic_pay,
                pp.overtime_pay,
                pp.allowances,
                pp.total_deductions,
                pp.net_pay,
                pp.status
            FROM payroll_process pp
            JOIN employees e ON pp.employee_id = e.id
            ORDER BY pp.processed_date DESC
            """;
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                PayrollProcessEntry entry = new PayrollProcessEntry();
                entry.setProcessId(rs.getInt("process_id"));
                entry.setEmployeeId(rs.getInt("employee_id"));
                entry.setAccountNumber(rs.getString("account_number"));
                entry.setEmployeeName(rs.getString("employee_name"));
                entry.setPosition(rs.getString("position"));
                entry.setPayPeriodStart(rs.getDate("pay_period_start").toLocalDate().toString());
                entry.setPayPeriodEnd(rs.getDate("pay_period_end").toLocalDate().toString());
                entry.setBasicSalary(rs.getDouble("basic_salary"));
                entry.setPresentDays(rs.getInt("present_days"));
                entry.setAbsentDays(rs.getInt("absent_days"));
                entry.setLateOccurrences(rs.getInt("late_occurrences"));
                entry.setOvertimeHours(rs.getDouble("overtime_hours"));
                entry.setBasicPay(rs.getDouble("basic_pay"));
                entry.setOvertimePay(rs.getDouble("overtime_pay"));
                entry.setAllowances(rs.getDouble("allowances"));
                entry.setTotalDeductions(rs.getDouble("total_deductions"));
                entry.setNetPay(rs.getDouble("net_pay"));
                entry.setStatus(rs.getString("status"));
                
                allData.add(entry);
            }
        } catch (SQLException e) {
            System.err.println("Error loading payroll data for filtering: " + e.getMessage());
            return;
        }
        
        // Apply filters
        ObservableList<PayrollProcessEntry> filteredData = FXCollections.observableArrayList();
        String searchTerm = searchField.getText() != null ? searchField.getText().toLowerCase() : "";
        String selectedMonth = monthFilter.getValue();
        String selectedYear = yearFilter.getValue();
        
        for (PayrollProcessEntry entry : allData) {
            boolean matchesSearch = searchTerm.isEmpty() || 
                                  entry.getEmployeeName().toLowerCase().contains(searchTerm) ||
                                  entry.getAccountNumber().toLowerCase().contains(searchTerm) ||
                                  String.valueOf(entry.getEmployeeId()).contains(searchTerm);
            
            // Check month filter
            boolean matchesMonth = "All Months".equals(selectedMonth);
            if (!matchesMonth && entry.getPayPeriodStart() != null) {
                LocalDate startDate = LocalDate.parse(entry.getPayPeriodStart());
                matchesMonth = startDate.getMonth().toString().equals(selectedMonth);
            }
            
            // Check year filter
            boolean matchesYear = "All Years".equals(selectedYear);
            if (!matchesYear && entry.getPayPeriodStart() != null) {
                LocalDate startDate = LocalDate.parse(entry.getPayPeriodStart());
                matchesYear = String.valueOf(startDate.getYear()).equals(selectedYear);
            }
            
            if (matchesSearch && matchesMonth && matchesYear) {
                filteredData.add(entry);
            }
        }
        
        // Update table with filtered data
        Platform.runLater(() -> {
            payrollTable.setItems(filteredData);
            payrollTable.refresh();
        });
    }

    @FXML
    private void onGeneratePayroll(ActionEvent event) {
        LocalDate startDate = startDatePicker.getValue();
        LocalDate endDate = endDatePicker.getValue();
        
        if (startDate == null || endDate == null) {
            showErrorAlert("Invalid Date Range", "Please select both start and end dates for the pay period.");
            return;
        }
        
        if (startDate.isAfter(endDate)) {
            showErrorAlert("Invalid Date Range", "Start date must be before or equal to end date.");
            return;
        }
        
        generatePayrollForPeriod(startDate, endDate);
    }

    private void generatePayrollForPeriod(LocalDate startDate, LocalDate endDate) {
        Task<String> generateTask = new Task<String>() {
            @Override
            protected String call() throws Exception {
                String result = "No result";
                
                // Call the stored procedure sp_generate_payroll
                String sql = "{CALL sp_generate_payroll(?, ?, ?)}";
                
                try (Connection conn = getConnection();
                     CallableStatement stmt = conn.prepareCall(sql)) {
                    
                    stmt.setDate(1, Date.valueOf(startDate));
                    stmt.setDate(2, Date.valueOf(endDate));
                    stmt.setString(3, "Admin"); // processed_by parameter
                    
                    boolean hasResults = stmt.execute();
                    
                    // Check if stored procedure returns result sets
                    if (hasResults) {
                        try (ResultSet rs = stmt.getResultSet()) {
                            if (rs.next()) {
                                result = rs.getString(1); // Get the result message
                            }
                        }
                    }
                    
                    return result;
                    
                } catch (SQLException e) {
                    throw new Exception("Stored procedure execution failed: " + e.getMessage(), e);
                }
            }
        };
        
        generateTask.setOnSucceeded(e -> {
            Platform.runLater(() -> {
                String result = generateTask.getValue();
                showInfoAlert("Payroll Generation Complete", 
                            String.format("Payroll generated successfully!\n\n" +
                                        "Period: %s to %s\n" +
                                        "Result: %s", startDate, endDate, result));
                loadPayrollData(); // Refresh the table
            });
        });
        
        generateTask.setOnFailed(e -> {
            Throwable exception = generateTask.getException();
            Platform.runLater(() -> {
                showErrorAlert("Payroll Generation Error", 
                             "Failed to generate payroll: " + exception.getMessage());
                exception.printStackTrace(); // For debugging
            });
        });
        
        new Thread(generateTask).start();
    }


    @FXML
    private void onSearch(ActionEvent event) {
        filterPayrollData();
    }

    @FXML
    private void onViewComputation(ActionEvent event) {
        PayrollProcessEntry selectedEntry = payrollTable.getSelectionModel().getSelectedItem();
        if (selectedEntry == null) {
            showErrorAlert("No Selection", "Please select a payroll entry to view computation details.");
            return;
        }
        showComputationDetails(selectedEntry);
    }

    private void showComputationDetails(PayrollProcessEntry entry) {
        StringBuilder details = new StringBuilder();
        details.append("PAYROLL COMPUTATION DETAILS\n");
        details.append("=====================================\n\n");
        details.append(String.format("Employee: %s (%s)\n", entry.getEmployeeName(), entry.getAccountNumber()));
        details.append(String.format("Position: %s\n", entry.getPosition()));
        details.append(String.format("Period: %s to %s\n\n", entry.getPayPeriodStart(), entry.getPayPeriodEnd()));
        
        details.append("ATTENDANCE SUMMARY:\n");
        details.append(String.format("• Total Work Days: %d\n", entry.getTotalWorkDays()));
        details.append(String.format("• Present Days: %d\n", entry.getPresentDays()));
        details.append(String.format("• Absent Days: %d\n", entry.getAbsentDays()));
        details.append(String.format("• Late Occurrences: %d\n", entry.getLateOccurrences()));
        details.append(String.format("• Total Late Minutes: %d\n\n", entry.getTotalLateMinutes()));
        
        details.append("EARNINGS:\n");
        details.append(String.format("• Basic Salary: ₱%.2f\n", entry.getBasicSalary()));
        details.append(String.format("• Basic Pay (Prorated): ₱%.2f\n", entry.getBasicPay()));
        details.append(String.format("• Overtime Pay: ₱%.2f\n", entry.getOvertimePay()));
        details.append(String.format("• Allowances: ₱%.2f\n", entry.getAllowances()));
        details.append(String.format("• Gross Pay: ₱%.2f\n\n", entry.getGrossPay()));
        
        details.append("DEDUCTIONS:\n");
        details.append(String.format("• SSS: ₱%.2f\n", entry.getSssDeduction()));
        details.append(String.format("• PhilHealth: ₱%.2f\n", entry.getPhilhealthDeduction()));
        details.append(String.format("• Pag-IBIG: ₱%.2f\n", entry.getPagibigDeduction()));
        details.append(String.format("• Withholding Tax: ₱%.2f\n", entry.getWithholdingTax()));
        details.append(String.format("• Late Deduction: ₱%.2f\n", entry.getLateDeduction()));
        details.append(String.format("• Absent Deduction: ₱%.2f\n", entry.getAbsentDeduction()));
        details.append(String.format("• Other Deductions: ₱%.2f\n", entry.getOtherDeductions()));
        details.append(String.format("• Total Deductions: ₱%.2f\n\n", entry.getTotalDeductions()));
        
        details.append(String.format("NET PAY: ₱%.2f\n", entry.getNetPay()));
        
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("Payroll Computation Details");
        alert.setHeaderText(entry.getEmployeeName() + " - Payroll Breakdown");
        alert.setContentText(details.toString());
        alert.getDialogPane().setPrefWidth(600);
        alert.showAndWait();
    }

    @FXML
    private void onExportPayroll(ActionEvent event) {
        showInfoAlert("Export Feature", "Export payroll functionality will be implemented in future updates.");
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
}