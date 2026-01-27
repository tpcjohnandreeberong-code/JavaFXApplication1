package javafxapplication1;

import java.io.File;
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
import javafx.stage.Stage;
import javafx.stage.Modality;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.BorderPane;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.print.PrinterJob;
import javafx.print.Printer;
import javafx.print.PageLayout;
import javafx.print.PageOrientation;

import java.net.URL;
import java.util.ResourceBundle;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.sql.*;
import java.util.List;
import java.util.Optional;
import java.io.FileOutputStream;
import java.io.IOException;
import javafx.stage.FileChooser;

// CSV export imports (no external library needed)
import java.io.PrintWriter;
import javafx.print.Paper;
import javafx.scene.transform.Scale;

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
        
        // Log module access
        String currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser != null) {
            SecurityLogger.logSecurityEvent(
                "PAYROLL_PROCESSING_MODULE_ACCESS",
                "LOW",
                currentUser,
                "Accessed Payroll Processing module"
            );
        }
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
        // Set current month as default - convert to title case
        String currentMonth = now.getMonth().toString();
        String titleCaseMonth = currentMonth.substring(0, 1).toUpperCase() + 
                               currentMonth.substring(1).toLowerCase();
        monthFilter.setValue(titleCaseMonth);
        
        // Initialize year filter (current year and previous/next few years)
        ObservableList<String> yearList = FXCollections.observableArrayList();
        yearList.add("All Years");
        int currentYear = now.getYear();
        for (int year = currentYear - 3; year <= currentYear + 2; year++) {
            yearList.add(String.valueOf(year));
        }
        yearFilter.setItems(yearList);
        yearFilter.setValue(String.valueOf(currentYear)); // Set current year as default
        
        // Add listeners for real-time filtering (with logging for significant changes)
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            // Only log if there's actual text change (not just clearing)
            if (newVal != null && !newVal.trim().isEmpty() && (oldVal == null || oldVal.trim().isEmpty())) {
                String currentUser = SessionManager.getInstance().getCurrentUser();
                if (currentUser != null) {
                    SecurityLogger.logSecurityEvent(
                        "PAYROLL_PROCESSING_SEARCH",
                        "LOW",
                        currentUser,
                        "Searched payroll records with keyword: '" + newVal.trim() + "'"
                    );
                }
            }
            filterPayrollData();
        });
        monthFilter.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.equals(oldVal)) {
                String currentUser = SessionManager.getInstance().getCurrentUser();
                if (currentUser != null) {
                    SecurityLogger.logSecurityEvent(
                        "PAYROLL_FILTER_MONTH",
                        "LOW",
                        currentUser,
                        "Filtered payroll records by month: " + newVal
                    );
                }
            }
            filterPayrollData();
        });
        yearFilter.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.equals(oldVal)) {
                String currentUser = SessionManager.getInstance().getCurrentUser();
                if (currentUser != null) {
                    SecurityLogger.logSecurityEvent(
                        "PAYROLL_FILTER_YEAR",
                        "LOW",
                        currentUser,
                        "Filtered payroll records by year: " + newVal
                    );
                }
            }
            filterPayrollData();
        });
        
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
        String titleCaseMonth = monthName.substring(0, 1).toUpperCase() + 
                               monthName.substring(1).toLowerCase();
        String year = String.valueOf(selectedDate.getYear());
        
        // Update filters without triggering listeners temporarily
        monthFilter.setValue(titleCaseMonth);
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
                
                // Calculate individual deductions for detailed view
                calculateIndividualDeductions(entry);
                
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
                String entryMonth = startDate.getMonth().toString();
                String titleCaseEntryMonth = entryMonth.substring(0, 1).toUpperCase() + 
                                           entryMonth.substring(1).toLowerCase();
                matchesMonth = titleCaseEntryMonth.equals(selectedMonth);
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
        
        // Log generate payroll click
        String currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser != null) {
            SecurityLogger.logSecurityEvent(
                "PAYROLL_GENERATE_CLICK",
                "MEDIUM",
                currentUser,
                "Clicked Generate Payroll button - Period: " + 
                (startDate != null ? startDate.toString() : "N/A") + " to " + 
                (endDate != null ? endDate.toString() : "N/A")
            );
        }
        
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
                int generatedCount = 0;
                int skippedCount = 0;
                
                try (Connection conn = getConnection()) {
                    // Get all active employees with their salary information
                    String employeeSQL = """
                        SELECT e.id, e.account_number, e.full_name, e.position, 
                               sr.monthly_salary, sr.rate_per_day, sr.half_day_rate, sr.rate_per_minute
                        FROM employees e
                        LEFT JOIN salary_reference sr ON e.salary_ref_id = sr.id
                        WHERE e.status = 'Active'
                        ORDER BY e.full_name
                        """;
                    
                    try (PreparedStatement employeeStmt = conn.prepareStatement(employeeSQL)) {
                        ResultSet employeeRs = employeeStmt.executeQuery();
                        
                        while (employeeRs.next()) {
                            int employeeId = employeeRs.getInt("id");
                            String accountNumber = employeeRs.getString("account_number");
                            String fullName = employeeRs.getString("full_name");
                            String position = employeeRs.getString("position");
                            double basicSalary = employeeRs.getDouble("monthly_salary");
                            
                            // Check if employee has processed attendance data for this period
                            String checkAttendanceSQL = """
                                SELECT COUNT(*) 
                                FROM processed_attendance 
                                WHERE employee_id = ? AND process_date BETWEEN ? AND ?
                                """;
                            
                            boolean hasAttendanceData = false;
                            try (PreparedStatement checkStmt = conn.prepareStatement(checkAttendanceSQL)) {
                                checkStmt.setInt(1, employeeId);
                                checkStmt.setDate(2, Date.valueOf(startDate));
                                checkStmt.setDate(3, Date.valueOf(endDate));
                                ResultSet checkRs = checkStmt.executeQuery();
                                if (checkRs.next()) {
                                    hasAttendanceData = checkRs.getInt(1) > 0;
                                }
                            }
                            
                            if (!hasAttendanceData) {
                                skippedCount++;
                                continue; // Skip employees without processed attendance
                            }
                            
                            // Check if payroll already exists for this employee and period
                            String checkPayrollSQL = """
                                SELECT COUNT(*) 
                                FROM payroll_process 
                                WHERE employee_id = ? 
                                AND pay_period_start = ? 
                                AND pay_period_end = ?
                                """;
                            
                            try (PreparedStatement checkStmt = conn.prepareStatement(checkPayrollSQL)) {
                                checkStmt.setInt(1, employeeId);
                                checkStmt.setDate(2, Date.valueOf(startDate));
                                checkStmt.setDate(3, Date.valueOf(endDate));
                                ResultSet checkRs = checkStmt.executeQuery();
                                if (checkRs.next() && checkRs.getInt(1) > 0) {
                                    skippedCount++;
                                    continue; // Skip if already exists
                                }
                            }
                            
                            // Calculate payroll from processed attendance
                            PayrollCalculation calc = calculatePayrollForEmployee(conn, employeeId, basicSalary, startDate, endDate);
                            
                            // Insert into payroll_process
                            String insertSQL = """
                                INSERT INTO payroll_process 
                                (employee_id, account_number, pay_period_start, pay_period_end, basic_salary, 
                                 present_days, absent_days, late_occurrences, overtime_hours,
                                 basic_pay, overtime_pay, allowances, total_deductions, net_pay, 
                                 status, processed_date, processed_by)
                                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'Calculated', CURRENT_TIMESTAMP, 'Admin')
                                """;
                            
                            try (PreparedStatement insertStmt = conn.prepareStatement(insertSQL)) {
                                insertStmt.setInt(1, employeeId);
                                insertStmt.setString(2, accountNumber);
                                insertStmt.setDate(3, Date.valueOf(startDate));
                                insertStmt.setDate(4, Date.valueOf(endDate));
                                insertStmt.setDouble(5, calc.basicSalary);
                                insertStmt.setInt(6, calc.presentDays);
                                insertStmt.setInt(7, calc.absentDays);
                                insertStmt.setInt(8, calc.lateOccurrences);
                                insertStmt.setDouble(9, calc.overtimeHours);
                                insertStmt.setDouble(10, calc.basicPay);
                                insertStmt.setDouble(11, calc.overtimePay);
                                insertStmt.setDouble(12, calc.allowances);
                                insertStmt.setDouble(13, calc.totalDeductions);
                                insertStmt.setDouble(14, calc.netPay);
                                
                                insertStmt.executeUpdate();
                                
                                // Update loan balances after payroll is saved
                                double totalLoanDeduction = getAllLoanDeductions(conn, employeeId);
                                if (totalLoanDeduction > 0) {
                                    updateLoanBalanceAfterPayroll(conn, employeeId, totalLoanDeduction);
                                }
                                
                                generatedCount++;
                            }
                        }
                    }
                }
                
                return String.format("Payroll generation completed!\n\n" +
                    "✅ Generated: %d employees\n" +
                    "⏭️ Skipped: %d employees (no attendance data or already processed)\n" +
                    "📅 Period: %s to %s", 
                    generatedCount, skippedCount, startDate, endDate);
            }
        };
        
        generateTask.setOnSucceeded(e -> {
            Platform.runLater(() -> {
                String result = generateTask.getValue();
                
                // Log successful payroll generation
                String currentUser = SessionManager.getInstance().getCurrentUser();
                if (currentUser != null) {
                    SecurityLogger.logSecurityEvent(
                        "PAYROLL_GENERATE_SUCCESS",
                        "HIGH",
                        currentUser,
                        "Successfully generated payroll - Period: " + startDate + " to " + endDate + " - " + result
                    );
                }
                
                showInfoAlert("Payroll Generation Complete", result);
                loadPayrollData(); // Refresh the table
            });
        });
        
        generateTask.setOnFailed(e -> {
            Throwable exception = generateTask.getException();
            Platform.runLater(() -> {
                // Log failed payroll generation
                String currentUser = SessionManager.getInstance().getCurrentUser();
                if (currentUser != null) {
                    SecurityLogger.logSecurityEvent(
                        "PAYROLL_GENERATE_FAILED",
                        "HIGH",
                        currentUser,
                        "Failed to generate payroll - Period: " + startDate + " to " + endDate + 
                        " - Error: " + exception.getMessage()
                    );
                }
                
                showErrorAlert("Payroll Generation Error", 
                             "Failed to generate payroll: " + exception.getMessage());
                exception.printStackTrace(); // For debugging
            });
        });
        
        new Thread(generateTask).start();
    }
    
    private PayrollCalculation calculatePayrollForEmployee(Connection conn, int employeeId, double basicSalary, 
            LocalDate startDate, LocalDate endDate) throws SQLException {
        
        PayrollCalculation calc = new PayrollCalculation();
        calc.basicSalary = basicSalary;
        calc.allowances = 0.0; // Can be enhanced later
        
        // Get employee employment type and assigned units
        String employeeInfoSQL = """
            SELECT employment_type, assigned_units 
            FROM employees 
            WHERE id = ?
            """;
        
        String employmentType = null;
        Integer assignedUnits = null;
        try (PreparedStatement stmt = conn.prepareStatement(employeeInfoSQL)) {
            stmt.setInt(1, employeeId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                employmentType = rs.getString("employment_type");
                if (rs.wasNull()) {
                    employmentType = null;
                }
                assignedUnits = rs.getInt("assigned_units");
                if (rs.wasNull()) {
                    assignedUnits = null;
                }
            }
        }
        
        // Determine if employee is instructor
        boolean isInstructor = employmentType != null && 
                              (employmentType.equals("INSTRUCTOR") || employmentType.equals("TEMPORARY_INSTRUCTOR"));
        
        // Get processed attendance data
        String attendanceSQL = """
            SELECT 
                SUM(hours_worked) as total_hours,
                SUM(overtime_hours) as total_overtime,
                SUM(late_minutes) as total_late_minutes,
                SUM(absent_days) as total_absent_days,
                SUM(half_days) as total_half_days,
                COUNT(*) as present_days
            FROM processed_attendance 
            WHERE employee_id = ? AND process_date BETWEEN ? AND ?
            """;
        
        int totalLateMinutes = 0;
        int totalAbsentDays = 0;
        int totalHalfDays = 0;
        
        try (PreparedStatement stmt = conn.prepareStatement(attendanceSQL)) {
            stmt.setInt(1, employeeId);
            stmt.setDate(2, Date.valueOf(startDate));
            stmt.setDate(3, Date.valueOf(endDate));
            
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                calc.overtimeHours = rs.getDouble("total_overtime");
                totalLateMinutes = rs.getInt("total_late_minutes");
                totalAbsentDays = rs.getInt("total_absent_days");
                totalHalfDays = rs.getInt("total_half_days");
                calc.presentDays = rs.getInt("present_days");
            }
        }
        
        calc.totalLateMinutes = totalLateMinutes;
        calc.absentDays = totalAbsentDays;
        calc.lateOccurrences = totalLateMinutes > 0 ? 1 : 0;
        
        // =====================================================
        // LGU PAYROLL CALCULATION FORMULA
        // =====================================================
        
        double monthlySalary = basicSalary;
        
        // 1. GROSS REGULAR - Depende sa period
        // Determine period type based on dates
        int startDay = startDate.getDayOfMonth();
        int endDay = endDate.getDayOfMonth();
        int daysInPeriod = (int) java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1;
        
        double grossRegular;
        // If period is 1-15 or 16-30/31 (half month) = monthly_salary / 2
        // If period is 1-30/31 (full month) = monthly_salary
        if ((startDay == 1 && endDay == 15) || (startDay == 16 && endDay >= 28 && endDay <= 31)) {
            // Half month period
            grossRegular = monthlySalary / 2.0;
        } else if (startDay == 1 && (endDay == 30 || endDay == 31)) {
            // Full month period
            grossRegular = monthlySalary;
        } else {
            // Prorated based on number of days
            // Calculate based on working days in period vs working days in month
            int workingDaysInPeriod = daysInPeriod; // Simplified - can be enhanced
            int workingDaysInMonth = 22; // Standard working days
            grossRegular = (monthlySalary / workingDaysInMonth) * workingDaysInPeriod;
        }
        calc.basicPay = grossRegular;
        
        // 2. OVERLOAD COMPUTATION (Instructors only)
        double overloadAmount = 0.0;
        if (isInstructor && assignedUnits != null && assignedUnits > 0) {
            // rate per unit = monthly salary ÷ 24
            double ratePerUnit = monthlySalary / 24.0;
            // overload_amount = units * rate_per_unit
            double monthlyOverload = assignedUnits * ratePerUnit;
            
            // Prorate overload based on period (same logic as grossRegular)
            if ((startDay == 1 && endDay == 15) || (startDay == 16 && endDay >= 28 && endDay <= 31)) {
                // Half month period
                overloadAmount = monthlyOverload / 2.0;
            } else if (startDay == 1 && (endDay == 30 || endDay == 31)) {
                // Full month period
                overloadAmount = monthlyOverload;
            } else {
                // Prorated based on number of days
                int workingDaysInPeriod = daysInPeriod;
                int workingDaysInMonth = 22;
                overloadAmount = (monthlyOverload / workingDaysInMonth) * workingDaysInPeriod;
            }
        }
        calc.overtimePay = overloadAmount; // Store overload in overtimePay field for now
        
        // 3. GROSS TOTAL
        double grossTotal = grossRegular + overloadAmount;
        
        // 4. ATTENDANCE DEDUCTIONS
        // Daily rate = monthly_salary / 22
        double dailyRate = monthlySalary / 22.0;
        // Hourly rate = daily_rate / 8
        double hourlyRate = dailyRate / 8.0;
        // Minute rate = hourly_rate / 60
        double minuteRate = hourlyRate / 60.0;
        
        // Tardiness deduction
        double tardinessDeduction = minuteRate * totalLateMinutes;
        
        // Absence deduction (full days + half days)
        double absenceDeduction = (totalAbsentDays * dailyRate) + (totalHalfDays * (dailyRate / 2.0));
        
        // 5. GROSS EARNED
        double grossEarned = grossTotal - tardinessDeduction - absenceDeduction;
        
        // 6. DEDUCTIONS
        double totalDeductions = 0.0;
        
        // 6.1 PAG-IBIG Premium (Fixed ₱200.00)
        double pagibigPremium = 200.0;
        totalDeductions += pagibigPremium;
        
        // 6.2 All Active Loans (MPL Loan, Calamity Loan, etc.)
        double totalLoanDeduction = getAllLoanDeductions(conn, employeeId);
        totalDeductions += totalLoanDeduction;
        
        // 6.3 Tax Deductions (Only if gross_earned >= 20,000)
        double expandedTax = 0.0;
        double gvat = 0.0;
        if (grossEarned >= 20000.0) {
            // 6.4 Expanded Tax (5% of gross_earned)
            expandedTax = grossEarned * 0.05;
            totalDeductions += expandedTax;
            
            // 6.5 GVAT (3% of gross_earned)
            gvat = grossEarned * 0.03;
            totalDeductions += gvat;
        }
        
        calc.totalDeductions = totalDeductions;
        
        // 7. NET AMOUNT DUE
        calc.netPay = grossEarned - totalDeductions;
        
        return calc;
    }
    
    /**
     * Get loan amount for employee (MPL or Calamity Loan)
     */
    private double getLoanAmount(Connection conn, int employeeId, String loanTypeName) throws SQLException {
        String sql = """
            SELECT el.monthly_amortization, el.balance, el.id as loan_id
            FROM employee_loans el
            JOIN loan_types lt ON el.loan_type_id = lt.id
            WHERE el.employee_id = ? 
            AND lt.name = ?
            AND el.status = 'Active' 
            AND el.balance > 0
            LIMIT 1
            """;
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, employeeId);
            stmt.setString(2, loanTypeName);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                double amortization = rs.getDouble("monthly_amortization");
                double balance = rs.getDouble("balance");
                // Return the minimum of amortization and balance
                return Math.min(amortization, balance);
            }
        }
        return 0.0;
    }
    
    /**
     * Get all active loans for an employee and return total deduction amount
     */
    private double getAllLoanDeductions(Connection conn, int employeeId) throws SQLException {
        String sql = """
            SELECT el.id, el.monthly_amortization, el.balance, lt.name as loan_type
            FROM employee_loans el
            JOIN loan_types lt ON el.loan_type_id = lt.id
            WHERE el.employee_id = ? 
            AND el.status = 'Active' 
            AND el.balance > 0
            """;
        
        double totalLoanDeduction = 0.0;
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, employeeId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                double amortization = rs.getDouble("monthly_amortization");
                double balance = rs.getDouble("balance");
                // Add the minimum of amortization and balance
                totalLoanDeduction += Math.min(amortization, balance);
            }
        }
        return totalLoanDeduction;
    }
    
    /**
     * Update loan balance after payroll deduction
     */
    private void updateLoanBalanceAfterPayroll(Connection conn, int employeeId, double totalLoanDeduction) throws SQLException {
        if (totalLoanDeduction <= 0) {
            return; // No loan deduction, nothing to update
        }
        
        // Get all active loans
        String sql = """
            SELECT el.id, el.monthly_amortization, el.balance
            FROM employee_loans el
            WHERE el.employee_id = ? 
            AND el.status = 'Active' 
            AND el.balance > 0
            ORDER BY el.id
            """;
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, employeeId);
            ResultSet rs = stmt.executeQuery();
            
            double remainingDeduction = totalLoanDeduction;
            
            while (rs.next() && remainingDeduction > 0) {
                int loanId = rs.getInt("id");
                double amortization = rs.getDouble("monthly_amortization");
                double currentBalance = rs.getDouble("balance");
                
                // Calculate deduction for this loan
                double loanDeduction = Math.min(amortization, Math.min(currentBalance, remainingDeduction));
                
                if (loanDeduction > 0) {
                    // Update balance
                    double newBalance = Math.max(0, currentBalance - loanDeduction);
                    String updateSQL = """
                        UPDATE employee_loans 
                        SET balance = ?, 
                            status = CASE WHEN ? <= 0 THEN 'Completed' ELSE status END,
                            end_date = CASE WHEN ? <= 0 THEN CURDATE() ELSE end_date END
                        WHERE id = ?
                        """;
                    
                    try (PreparedStatement updateStmt = conn.prepareStatement(updateSQL)) {
                        updateStmt.setDouble(1, newBalance);
                        updateStmt.setDouble(2, newBalance);
                        updateStmt.setDouble(3, newBalance);
                        updateStmt.setInt(4, loanId);
                        updateStmt.executeUpdate();
                    }
                    
                    remainingDeduction -= loanDeduction;
                }
            }
        }
    }
    
    private static class PayrollCalculation {
        double basicSalary;
        double basicPay;
        double overtimePay;
        double allowances;
        double totalDeductions;
        double netPay;
        double overtimeHours;
        int totalLateMinutes;
        int absentDays;
        int presentDays;
        int lateOccurrences;
    }


    @FXML
    private void onSearch(ActionEvent event) {
        // Log search activity
        String currentUser = SessionManager.getInstance().getCurrentUser();
        String searchText = searchField.getText() != null ? searchField.getText().trim() : "";
        String monthFilterValue = monthFilter.getValue() != null ? monthFilter.getValue() : "";
        String yearFilterValue = yearFilter.getValue() != null ? yearFilter.getValue() : "";
        
        if (currentUser != null) {
            SecurityLogger.logSecurityEvent(
                "PAYROLL_PROCESSING_SEARCH",
                "LOW",
                currentUser,
                "Searched payroll records - Keyword: '" + (searchText.isEmpty() ? "(none)" : searchText) + 
                "', Month: " + monthFilterValue + ", Year: " + yearFilterValue
            );
        }
        
        filterPayrollData();
    }

    private void calculateIndividualDeductions(PayrollProcessEntry entry) {
        try (Connection conn = getConnection()) {
            double monthlySalary = entry.getBasicSalary();
            
            // Get employee employment type and assigned units
            String employeeInfoSQL = """
                SELECT employment_type, assigned_units 
                FROM employees 
                WHERE id = ?
                """;
            
            String employmentType = null;
            Integer assignedUnits = null;
            try (PreparedStatement stmt = conn.prepareStatement(employeeInfoSQL)) {
                stmt.setInt(1, entry.getEmployeeId());
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    employmentType = rs.getString("employment_type");
                    if (rs.wasNull()) {
                        employmentType = null;
                    }
                    assignedUnits = rs.getInt("assigned_units");
                    if (rs.wasNull()) {
                        assignedUnits = null;
                    }
                }
            }
            
            // Determine if employee is instructor
            boolean isInstructor = employmentType != null && 
                                  (employmentType.equals("INSTRUCTOR") || employmentType.equals("TEMPORARY_INSTRUCTOR"));
            
            // =====================================================
            // LGU PAYROLL CALCULATION FORMULA
            // =====================================================
            
            // 1. GROSS REGULAR (Half-month salary)
            double grossRegular = monthlySalary / 2.0;
            
            // 2. OVERLOAD COMPUTATION (Instructors only)
            double overloadAmount = 0.0;
            if (isInstructor && assignedUnits != null && assignedUnits > 0) {
                double ratePerUnit = monthlySalary / 24.0;
                overloadAmount = assignedUnits * ratePerUnit;
            }
            
            // 3. GROSS TOTAL
            double grossTotal = grossRegular + overloadAmount;
            
            // 4. ATTENDANCE DEDUCTIONS (LGU Formula)
            double dailyRate = monthlySalary / 22.0;
            double hourlyRate = dailyRate / 8.0;
            double minuteRate = hourlyRate / 60.0;
            
            // Get half days from processed_attendance
            int totalHalfDays = 0;
            String halfDaysSQL = """
                SELECT SUM(half_days) as total_half_days
                FROM processed_attendance 
                WHERE employee_id = ? 
                AND process_date BETWEEN ? AND ?
                """;
            try (PreparedStatement stmt = conn.prepareStatement(halfDaysSQL)) {
                stmt.setInt(1, entry.getEmployeeId());
                stmt.setDate(2, Date.valueOf(LocalDate.parse(entry.getPayPeriodStart())));
                stmt.setDate(3, Date.valueOf(LocalDate.parse(entry.getPayPeriodEnd())));
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    totalHalfDays = rs.getInt("total_half_days");
                }
            }
            
            // Tardiness deduction
            double tardinessDeduction = minuteRate * entry.getTotalLateMinutes();
            entry.setLateDeduction(tardinessDeduction);
            
            // Absence deduction (full days + half days)
            double absenceDeduction = (entry.getAbsentDays() * dailyRate) + (totalHalfDays * (dailyRate / 2.0));
            entry.setAbsentDeduction(absenceDeduction);
            
            // 5. GROSS EARNED
            double grossEarned = grossTotal - tardinessDeduction - absenceDeduction;
            entry.setGrossPay(grossEarned);
            
            // 6. DEDUCTIONS (LGU Formula)
            // 6.1 PAG-IBIG Premium (Fixed ₱200.00)
            entry.setPagibigDeduction(200.0);
            
            // 6.2 All Active Loans
            double totalLoanDeduction = getAllLoanDeductions(conn, entry.getEmployeeId());
            
            // 6.3 Tax Deductions (Only if gross_earned >= 20,000)
            double expandedTax = 0.0;
            double gvat = 0.0;
            if (grossEarned >= 20000.0) {
                // 6.4 Expanded Tax (5% of gross_earned)
                expandedTax = grossEarned * 0.05;
                
                // 6.5 GVAT (3% of gross_earned)
                gvat = grossEarned * 0.03;
            }
            
            // Set withholding tax as sum of Expanded Tax and GVAT for display
            entry.setWithholdingTax(expandedTax + gvat);
            
            // Set other deductions (all loans)
            entry.setOtherDeductions(totalLoanDeduction);
            
            // Clear unused deductions
            entry.setSssDeduction(0.0);
            entry.setPhilhealthDeduction(0.0);
            
        } catch (SQLException e) {
            System.err.println("Error calculating individual deductions: " + e.getMessage());
        }
    }

    @FXML
    private void onViewComputation(ActionEvent event) {
        PayrollProcessEntry selectedEntry = payrollTable.getSelectionModel().getSelectedItem();
        if (selectedEntry == null) {
            showErrorAlert("No Selection", "Please select a payroll entry to view computation details.");
            
            // Log failed view attempt
            String currentUser = SessionManager.getInstance().getCurrentUser();
            if (currentUser != null) {
                SecurityLogger.logSecurityEvent(
                    "PAYROLL_VIEW_COMPUTATION_FAILED",
                    "LOW",
                    currentUser,
                    "Attempted to view payroll computation but no entry was selected"
                );
            }
            return;
        }
        
        // Log view computation click
        String currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser != null) {
            SecurityLogger.logSecurityEvent(
                "PAYROLL_VIEW_COMPUTATION",
                "MEDIUM",
                currentUser,
                "Viewed payroll computation details for: " + selectedEntry.getEmployeeName() + 
                " (ID: " + selectedEntry.getEmployeeId() + ", Period: " + 
                selectedEntry.getPayPeriodStart() + " to " + selectedEntry.getPayPeriodEnd() + ")"
            );
        }
        
        showComputationDetails(selectedEntry);
    }

    private void showComputationDetails(PayrollProcessEntry entry) {
        try (Connection conn = getConnection()) {
            // Get company information - Always use Talibon Polytechnic College
            String companyName = "Talibon Polytechnic College";
            
            // Get employee details including employment type and assigned units
            String employmentType = "";
            Integer assignedUnits = null;
            try (PreparedStatement stmt = conn.prepareStatement(
                "SELECT employment_type, assigned_units FROM employees WHERE id = ?")) {
                stmt.setInt(1, entry.getEmployeeId());
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    employmentType = rs.getString("employment_type");
                    assignedUnits = rs.getInt("assigned_units");
                    if (rs.wasNull()) assignedUnits = null;
                }
            }
            
            // Get all loan deductions
            double totalLoanDeduction = getAllLoanDeductions(conn, entry.getEmployeeId());
            
            // Get individual loan amounts for display (MPL and Calamity)
            double mplLoan = getLoanAmount(conn, entry.getEmployeeId(), "MPL Loan");
            double calamityLoan = getLoanAmount(conn, entry.getEmployeeId(), "Calamity Loan");
            
            // Get other loans (not MPL or Calamity)
            double otherLoans = totalLoanDeduction - mplLoan - calamityLoan;
            
            // Recalculate using LGU formula to ensure accuracy
            double monthlySalary = entry.getBasicSalary();
            
            // Determine period type from entry dates
            LocalDate startDate = LocalDate.parse(entry.getPayPeriodStart());
            LocalDate endDate = LocalDate.parse(entry.getPayPeriodEnd());
            int startDay = startDate.getDayOfMonth();
            int endDay = endDate.getDayOfMonth();
            int daysInPeriod = (int) java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1;
            
            // 1. GROSS REGULAR - Depende sa period
            double grossRegular;
            // If period is 1-15 or 16-30/31 (half month) = monthly_salary / 2
            // If period is 1-30/31 (full month) = monthly_salary
            if ((startDay == 1 && endDay == 15) || (startDay == 16 && endDay >= 28 && endDay <= 31)) {
                // Half month period
                grossRegular = monthlySalary / 2.0;
            } else if (startDay == 1 && (endDay == 30 || endDay == 31)) {
                // Full month period
                grossRegular = monthlySalary;
            } else {
                // Prorated based on number of days
                int workingDaysInPeriod = daysInPeriod; // Simplified
                int workingDaysInMonth = 22; // Standard working days
                grossRegular = (monthlySalary / workingDaysInMonth) * workingDaysInPeriod;
            }
            
            // 2. OVERLOAD (if instructor with units)
            double overloadAmount = 0.0;
            if (assignedUnits != null && assignedUnits > 0 && 
                (employmentType != null && (employmentType.equals("INSTRUCTOR") || employmentType.equals("TEMPORARY_INSTRUCTOR")))) {
                double ratePerUnit = monthlySalary / 24.0;
                double monthlyOverload = assignedUnits * ratePerUnit;
                
                // Prorate overload based on period (same logic as grossRegular)
                if ((startDay == 1 && endDay == 15) || (startDay == 16 && endDay >= 28 && endDay <= 31)) {
                    // Half month period
                    overloadAmount = monthlyOverload / 2.0;
                } else if (startDay == 1 && (endDay == 30 || endDay == 31)) {
                    // Full month period
                    overloadAmount = monthlyOverload;
                } else {
                    // Prorated based on number of days
                    int workingDaysInPeriod = daysInPeriod;
                    int workingDaysInMonth = 22;
                    overloadAmount = (monthlyOverload / workingDaysInMonth) * workingDaysInPeriod;
                }
            }
            
            // 3. GROSS TOTAL
            double grossTotal = grossRegular + overloadAmount;
            
            // 4. Calculate time deductions to get grossEarned
            double dailyRate = monthlySalary / 22.0;
            double hourlyRate = dailyRate / 8.0;
            double minuteRate = hourlyRate / 60.0;
            
            // Get half days from processed_attendance
            int totalHalfDays = 0;
            String halfDaysSQL = """
                SELECT SUM(half_days) as total_half_days
                FROM processed_attendance 
                WHERE employee_id = ? 
                AND process_date BETWEEN ? AND ?
                """;
            try (PreparedStatement stmt = conn.prepareStatement(halfDaysSQL)) {
                stmt.setInt(1, entry.getEmployeeId());
                stmt.setDate(2, Date.valueOf(startDate));
                stmt.setDate(3, Date.valueOf(endDate));
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    totalHalfDays = rs.getInt("total_half_days");
                }
            }
            
            // Tardiness deduction
            double tardinessDeduction = minuteRate * entry.getTotalLateMinutes();
            
            // Absence deduction (full days + half days)
            double absenceDeduction = (entry.getAbsentDays() * dailyRate) + (totalHalfDays * (dailyRate / 2.0));
            
            // 5. GROSS EARNED (after time deductions)
            double grossEarned = grossTotal - tardinessDeduction - absenceDeduction;
            
            // Calculate deductions based on grossEarned
            double pagibigPremium = 200.0; // Fixed per month, not prorated
            
            // Tax Deductions (Only if gross_earned >= 20,000)
            double expandedTax = 0.0;
            double gvat = 0.0;
            if (grossEarned >= 20000.0) {
                expandedTax = grossEarned * 0.05;
                gvat = grossEarned * 0.03;
            }
            
            // Create payroll slip window
            Stage payrollStage = new Stage();
            payrollStage.setTitle("Pay Advice - " + entry.getEmployeeName());
            payrollStage.initModality(Modality.WINDOW_MODAL);
            payrollStage.initOwner(payrollTable.getScene().getWindow());
            
            VBox root = new VBox(15);
            root.setPadding(new Insets(30, 40, 30, 40));
            root.setStyle("-fx-background-color: white;");
            
            // Header - Company Name (centered for display and print)
            Label companyLabel = new Label(companyName);
            companyLabel.setFont(Font.font("Arial", FontWeight.BOLD, 18));
            companyLabel.setMaxWidth(Double.MAX_VALUE);
            companyLabel.setAlignment(Pos.CENTER);
            companyLabel.setStyle("-fx-alignment: center;");
            HBox headerBox = new HBox(companyLabel);
            headerBox.setAlignment(Pos.CENTER);
            headerBox.setPrefWidth(Double.MAX_VALUE);
            root.getChildren().add(headerBox);
            
            // Employee Information Grid
            GridPane infoGrid = new GridPane();
            infoGrid.setHgap(15);
            infoGrid.setVgap(8);
            infoGrid.setPadding(new Insets(20, 0, 20, 0));
            
            int row = 0;
            infoGrid.add(createBoldLabel("Code:"), 0, row);
            infoGrid.add(new Label(entry.getAccountNumber()), 1, row);
            infoGrid.add(createBoldLabel("Name:"), 2, row);
            infoGrid.add(new Label(entry.getEmployeeName().toUpperCase()), 3, row);
            
            row++;
            infoGrid.add(createBoldLabel("Position:"), 0, row);
            infoGrid.add(new Label(entry.getPosition()), 1, row);
            infoGrid.add(createBoldLabel("Employment Type:"), 2, row);
            String empTypeDisplay = employmentType != null ? employmentType.replace("_", " ") : "Regular";
            infoGrid.add(new Label(empTypeDisplay), 3, row);
            
            row++;
            infoGrid.add(createBoldLabel("Timesheet Period:"), 0, row);
            infoGrid.add(new Label(entry.getPayPeriodStart() + " - " + entry.getPayPeriodEnd()), 1, row);
            
            if (assignedUnits != null && assignedUnits > 0) {
                row++;
                infoGrid.add(createBoldLabel("Assigned Units:"), 0, row);
                infoGrid.add(new Label(String.valueOf(assignedUnits)), 1, row);
            }
            
            root.getChildren().add(infoGrid);
            
            // Separator
            root.getChildren().add(createSeparator());
            
            // Earnings Section
            VBox earningsBox = new VBox(5);
            Label earningsTitle = new Label("EARNINGS");
            earningsTitle.setFont(Font.font("Arial", FontWeight.BOLD, 12));
            earningsBox.getChildren().add(earningsTitle);
            
            // Use recalculated grossRegular instead of entry.getBasicPay()
            HBox regBasicRow = createDataRow("REG BASIC", grossRegular);
            earningsBox.getChildren().add(regBasicRow);
            
            if (overloadAmount > 0) {
                HBox overloadRow = createDataRow("OVERLOAD", overloadAmount);
                earningsBox.getChildren().add(overloadRow);
            }
            
            // Total Earnings should be grossTotal (before deductions)
            HBox totalEarningsRow = createDataRow("Total Earnings", grossTotal, true);
            earningsBox.getChildren().add(totalEarningsRow);
            
            root.getChildren().add(earningsBox);
            
            // Separator
            root.getChildren().add(createSeparator());
            
            // Deductions Section
            VBox deductionsBox = new VBox(5);
            Label deductionsTitle = new Label("DEDUCTIONS");
            deductionsTitle.setFont(Font.font("Arial", FontWeight.BOLD, 12));
            deductionsBox.getChildren().add(deductionsTitle);
            
            deductionsBox.getChildren().add(createDataRow("PAG-IBIG", pagibigPremium));
            
            // Only show tax deductions if grossEarned >= 20,000
            if (grossEarned >= 20000.0) {
                deductionsBox.getChildren().add(createDataRow("EXPANDED TAX (5%)", expandedTax));
                deductionsBox.getChildren().add(createDataRow("GVAT (3%)", gvat));
            }
            
            if (mplLoan > 0) {
                deductionsBox.getChildren().add(createDataRow("MPL LOAN", mplLoan));
            }
            if (calamityLoan > 0) {
                deductionsBox.getChildren().add(createDataRow("CALAMITY LOAN", calamityLoan));
            }
            if (otherLoans > 0) {
                deductionsBox.getChildren().add(createDataRow("OTHER LOANS", otherLoans));
            }
            if (tardinessDeduction > 0) {
                deductionsBox.getChildren().add(createDataRow("LATE DEDUCTION", tardinessDeduction));
            }
            if (absenceDeduction > 0) {
                deductionsBox.getChildren().add(createDataRow("ABSENT DEDUCTION", absenceDeduction));
            }
            
            // Recalculate total deductions (use totalLoanDeduction instead of individual loans)
            double totalDeductions = pagibigPremium + expandedTax + gvat + totalLoanDeduction + 
                                    tardinessDeduction + absenceDeduction;
            HBox totalDeductionsRow = createDataRow("Total Deductions", totalDeductions, true);
            deductionsBox.getChildren().add(totalDeductionsRow);
            
            root.getChildren().add(deductionsBox);
            
            // Separator
            root.getChildren().add(createSeparator());
            
            
            
            // Loan Balances Section
            VBox loanBox = new VBox(5);
            Label loanTitle = new Label("LOAN BALANCES");
            loanTitle.setFont(Font.font("Arial", FontWeight.BOLD, 12));
            loanBox.getChildren().add(loanTitle);
            
            
            
            // Get all loan balances from database
            double mplBalance = getLoanBalance(conn, entry.getEmployeeId(), "MPL Loan");
            double calamityBalance = getLoanBalance(conn, entry.getEmployeeId(), "Calamity Loan");
            
            // Get total of all loan balances
            double totalLoanBalance = getAllLoanBalances(conn, entry.getEmployeeId());
            double totalPaidThisPeriod = totalLoanDeduction;
            
            loanBox.getChildren().add(createDataRow("Principal", totalLoanBalance));
            loanBox.getChildren().add(createDataRow("Total Paid", totalPaidThisPeriod));
            double remainingBalance = Math.max(0, totalLoanBalance - totalPaidThisPeriod);
            loanBox.getChildren().add(createDataRow("Balance", remainingBalance, true));
            
            root.getChildren().add(loanBox);
            
            // Separator
            root.getChildren().add(createSeparator());
            
            // Net Pay Section - Recalculate based on grossEarned and totalDeductions
            double netPay = grossEarned - totalDeductions;
            HBox netPayBox = new HBox(10);
            netPayBox.setAlignment(Pos.CENTER_RIGHT);
            Label netPayLabel = new Label("NET PAY");
            netPayLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
            Label netPayValue = new Label(String.format("₱%.2f", netPay));
            netPayValue.setFont(Font.font("Arial", FontWeight.BOLD, 16));
            netPayValue.setStyle("-fx-text-fill: #1b5e20;");
            netPayBox.getChildren().addAll(netPayLabel, netPayValue);
            root.getChildren().add(netPayBox);
            
          
            HBox buttonBox = new HBox(10);
            buttonBox.setAlignment(Pos.CENTER);
            buttonBox.setPadding(new Insets(20, 0, 0, 0));
            buttonBox.setId("buttonBox"); 
            
            Button printButton = new Button("Print");
            printButton.setStyle("-fx-background-color: #2e7d32; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 20;");
            printButton.setOnAction(e -> printPayrollSlip(root, payrollStage, buttonBox));
            
            Button closeButton = new Button("Close");
            closeButton.setStyle("-fx-background-color: #757575; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 20;");
            closeButton.setOnAction(e -> payrollStage.close());
            
            buttonBox.getChildren().addAll(printButton, closeButton);
            root.getChildren().add(buttonBox);
            
            Scene scene = new Scene(root, 700, 800);
            payrollStage.setScene(scene);
            payrollStage.setResizable(true);
            payrollStage.show();
            
        } catch (SQLException e) {
            showErrorAlert("Database Error", "Failed to load payroll details: " + e.getMessage());
        }
    }
    
    private Label createBoldLabel(String text) {
        Label label = new Label(text);
        label.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        return label;
    }
    
    private HBox createSeparator() {
        HBox separator = new HBox();
        separator.setPrefHeight(1);
        separator.setStyle("-fx-background-color: #000000;");
        return separator;
    }
    
    private HBox createDataRow(String label, double value) {
        return createDataRow(label, value, false);
    }
    
    private HBox createDataRow(String label, double value, boolean underline) {
        HBox row = new HBox(10);
        Label labelLbl = new Label(label + ":");
        labelLbl.setPrefWidth(200);
        Label valueLbl = new Label(String.format("₱%.2f", value));
        valueLbl.setPrefWidth(150);
        valueLbl.setAlignment(Pos.CENTER_RIGHT);
        if (underline) {
            valueLbl.setFont(Font.font("Arial", FontWeight.BOLD, 14));
            valueLbl.setStyle("-fx-underline: true;");
        }
        row.getChildren().addAll(labelLbl, valueLbl);
        return row;
    }
    
    private double getLoanBalance(Connection conn, int employeeId, String loanTypeName) throws SQLException {
        String sql = """
            SELECT el.balance
            FROM employee_loans el
            JOIN loan_types lt ON el.loan_type_id = lt.id
            WHERE el.employee_id = ? 
            AND lt.name = ?
            AND el.status = 'Active' 
            AND el.balance > 0
            LIMIT 1
            """;
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, employeeId);
            stmt.setString(2, loanTypeName);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getDouble("balance");
            }
        }
        return 0.0;
    }
    
    /**
     * Get total balance of all active loans for an employee
     */
    private double getAllLoanBalances(Connection conn, int employeeId) throws SQLException {
        String sql = """
            SELECT SUM(el.balance) as total_balance
            FROM employee_loans el
            WHERE el.employee_id = ? 
            AND el.status = 'Active' 
            AND el.balance > 0
            """;
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, employeeId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                double total = rs.getDouble("total_balance");
                return rs.wasNull() ? 0.0 : total;
            }
        }
        return 0.0;
    }
    
    private void printPayrollSlip(VBox content, Stage stage, HBox buttonBox) {
       // Log print attempt
        String currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser != null) {
            SecurityLogger.logSecurityEvent(
                "PAYROLL_PRINT_CLICK",
                "MEDIUM",
                currentUser,
                "Attempted to print payroll slip"
            );
        }

        PrinterJob job = PrinterJob.createPrinterJob();
        if (job == null) {
            showErrorAlert("Print Error", "No printer available.");
            return;
        }

        boolean showDialog = job.showPrintDialog(stage.getOwner());
        if (!showDialog) {
            if (currentUser != null) {
                SecurityLogger.logSecurityEvent(
                    "PAYROLL_PRINT_CANCELLED",
                    "LOW",
                    currentUser,
                    "Cancelled printing payroll slip"
                );
            }
            return;
        }

        // ===============================
        // SET PAPER SIZE (SHORT BOND)
        // ===============================
        Printer printer = job.getPrinter();
        PageLayout pageLayout = printer.createPageLayout(
                Paper.NA_LETTER,                 // Short bond (8.5 x 11)
                PageOrientation.PORTRAIT,
                Printer.MarginType.DEFAULT
        );

        // Hide buttons before printing
        buttonBox.setVisible(false);
        buttonBox.setManaged(false);

        // ===============================
        // AUTO SCALE CONTENT TO FIT PAGE
        // ===============================
        double printableWidth = pageLayout.getPrintableWidth();
        double printableHeight = pageLayout.getPrintableHeight();

        double nodeWidth = content.getBoundsInParent().getWidth();
        double nodeHeight = content.getBoundsInParent().getHeight();

        double scaleX = printableWidth / nodeWidth;
        double scaleY = printableHeight / nodeHeight;
        double scale = Math.min(scaleX, scaleY);

        Scale scaleTransform = new Scale(scale, scale);
        content.getTransforms().add(scaleTransform);

        boolean success = job.printPage(pageLayout, content);

        // Restore UI
        content.getTransforms().remove(scaleTransform);
        buttonBox.setVisible(true);
        buttonBox.setManaged(true);

        if (success) {
            job.endJob();

            if (currentUser != null) {
                SecurityLogger.logSecurityEvent(
                    "PAYROLL_PRINT_SUCCESS",
                    "MEDIUM",
                    currentUser,
                    "Successfully printed payroll slip"
                );
            }

            showInfoAlert("Print", "Payroll slip printed successfully!");
        } else {
            if (currentUser != null) {
                SecurityLogger.logSecurityEvent(
                    "PAYROLL_PRINT_FAILED",
                    "MEDIUM",
                    currentUser,
                    "Failed to print payroll slip"
                );
            }

            showErrorAlert("Print Error", "Failed to print payroll slip.");
        }
    }

    @FXML
    private void onExportPayroll(ActionEvent event) throws SQLException {
        String currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser != null) {
            SecurityLogger.logSecurityEvent(
                    "PAYROLL_EXPORT_CLICK",
                    "MEDIUM",
                    currentUser,
                    "Clicked Export Payroll button"
            );
        }

        // Get filtered payroll data from the table (only visible records)
        ObservableList<PayrollProcessEntry> payrollList = payrollTable.getItems();
        
        if (payrollList == null || payrollList.isEmpty()) {
            showErrorAlert("No Data", "No payroll records to export. Please apply filters to show data.");
            return;
        }

        // Open a file chooser to save the CSV file (can be opened in Excel)
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Payroll File");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("CSV Files (Excel Compatible)", "*.csv"),
            new FileChooser.ExtensionFilter("All Files", "*.*")
        );
        File file = fileChooser.showSaveDialog(null);
        
        if (file != null) {
            try (PrintWriter writer = new PrintWriter(file)) {
                // Determine date range from filtered data
                String dateRangeHeader = "";
                DateTimeFormatter displayFormatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");
                
                // Get the selected month and year filters
                String selectedMonth = monthFilter.getValue();
                String selectedYear = yearFilter.getValue();
                
                // Try to get date range from date pickers first
                LocalDate startDate = startDatePicker.getValue();
                LocalDate endDate = endDatePicker.getValue();
                
                if (startDate != null && endDate != null) {
                    // Use date pickers
                    if (startDate.equals(endDate)) {
                        // Single date
                        dateRangeHeader = "Payroll Report - " + startDate.format(displayFormatter);
                    } else {
                        // Date range
                        dateRangeHeader = "Payroll Report - " + startDate.format(displayFormatter) + " to " + endDate.format(displayFormatter);
                    }
                } else if (selectedMonth != null && !selectedMonth.equals("All Months") && 
                          selectedYear != null && !selectedYear.equals("All Years")) {
                    // Use month and year filters
                    dateRangeHeader = "Payroll Report - " + selectedMonth + " " + selectedYear;
                } else if (selectedYear != null && !selectedYear.equals("All Years")) {
                    // Use year filter only
                    dateRangeHeader = "Payroll Report - Year " + selectedYear;
                } else if (selectedMonth != null && !selectedMonth.equals("All Months")) {
                    // Use month filter only
                    dateRangeHeader = "Payroll Report - " + selectedMonth;
                } else {
                    // No specific filter, use current date
                    dateRangeHeader = "Payroll Report - " + LocalDate.now().format(displayFormatter);
                }
                
                // Write date header
                writer.println(dateRangeHeader);
                writer.println(); // Empty line for separation
                
                // Write column header row
                writer.println("Employee ID,Account Number,Employee Name,Position,Pay Period Start,Pay Period End,Basic Salary,Present Days,Absent Days,Late Occurrences,Overtime Hours,Basic Pay,Overtime Pay,Allowances,Total Deductions,Net Pay,Status");
                
                // Write payroll data from table
                for (PayrollProcessEntry entry : payrollList) {
                    writer.printf("%d,\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",%.2f,%d,%d,%d,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,\"%s\"%n",
                            entry.getEmployeeId(),
                            entry.getAccountNumber(),
                            entry.getEmployeeName(),
                            entry.getPosition(),
                            entry.getPayPeriodStart(),
                            entry.getPayPeriodEnd(),
                            entry.getBasicSalary(),
                            entry.getPresentDays(),
                            entry.getAbsentDays(),
                            entry.getLateOccurrences(),
                            entry.getOvertimeHours(),
                            entry.getBasicPay(),
                            entry.getOvertimePay(),
                            entry.getAllowances(),
                            entry.getTotalDeductions(),
                            entry.getNetPay(),
                            entry.getStatus());
                }
                
                // Log successful export
                if (currentUser != null) {
                    SecurityLogger.logSecurityEvent(
                            "PAYROLL_EXPORT_SUCCESS",
                            "MEDIUM",
                            currentUser,
                            "Successfully exported " + payrollList.size() + " payroll records to CSV - Filter: " + dateRangeHeader
                    );
                }

                showInfoAlert("Export Successful", "Payroll exported successfully!\n" +
                        "Records exported: " + payrollList.size() + "\n" +
                        "Filter: " + dateRangeHeader + "\n\n" +
                        "Note: CSV file can be opened in Microsoft Excel");
            } catch (IOException e) {
                e.printStackTrace();
                
                // Log failed export
                if (currentUser != null) {
                    SecurityLogger.logSecurityEvent(
                            "PAYROLL_EXPORT_FAILED",
                            "MEDIUM",
                            currentUser,
                            "Failed to export payroll: " + e.getMessage()
                    );
                }
                
                showErrorAlert("Export Failed", "Error while exporting payroll: " + e.getMessage());
            }
        }
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