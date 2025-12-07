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
                showInfoAlert("Payroll Generation Complete", result);
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
    
    private PayrollCalculation calculatePayrollForEmployee(Connection conn, int employeeId, double basicSalary, 
            LocalDate startDate, LocalDate endDate) throws SQLException {
        
        PayrollCalculation calc = new PayrollCalculation();
        calc.basicSalary = basicSalary;
        calc.allowances = 0.0; // Can be enhanced later
        
        // Get processed attendance data
        String attendanceSQL = """
            SELECT 
                SUM(hours_worked) as total_hours,
                SUM(overtime_hours) as total_overtime,
                SUM(late_minutes) as total_late_minutes,
                SUM(absent_days) as total_absent_days,
                COUNT(*) as present_days
            FROM processed_attendance 
            WHERE employee_id = ? AND process_date BETWEEN ? AND ?
            """;
        
        try (PreparedStatement stmt = conn.prepareStatement(attendanceSQL)) {
            stmt.setInt(1, employeeId);
            stmt.setDate(2, Date.valueOf(startDate));
            stmt.setDate(3, Date.valueOf(endDate));
            
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                calc.overtimeHours = rs.getDouble("total_overtime");
                calc.totalLateMinutes = rs.getInt("total_late_minutes");
                calc.absentDays = rs.getInt("total_absent_days");
                calc.presentDays = rs.getInt("present_days");
            }
        }
        
        // Calculate working days in period
        int totalWorkingDays = (int) startDate.datesUntil(endDate.plusDays(1))
            .filter(date -> date.getDayOfWeek().getValue() <= 5) // Monday to Friday
            .count();
        
        calc.lateOccurrences = calc.totalLateMinutes > 0 ? 1 : 0; // Simplified
        
        // Calculate basic pay (prorated)
        double dailyRate = calc.basicSalary / 22; // Assuming 22 working days per month
        calc.basicPay = calc.presentDays * dailyRate;
        
        // Calculate overtime pay
        double hourlyRate = calc.basicSalary / (22 * 8); // 22 working days, 8 hours per day
        calc.overtimePay = calc.overtimeHours * hourlyRate * 1.5;
        
        // Calculate deductions
        calc.totalDeductions = 0;
        
        // Get employee type to determine which deductions to apply
        String employeeTypeSQL = """
            SELECT position FROM employees WHERE id = ?
            """;
        
        boolean isContractor = false;
        try (PreparedStatement typeStmt = conn.prepareStatement(employeeTypeSQL)) {
            typeStmt.setInt(1, employeeId);
            ResultSet typeRs = typeStmt.executeQuery();
            if (typeRs.next()) {
                String position = typeRs.getString("position");
                // Check if employee is contractor/instructor
                isContractor = position != null && 
                              (position.toLowerCase().contains("instructor") || 
                               position.toLowerCase().contains("contractor"));
            }
        }
        
        // Apply different deductions based on employee type
        String deductionTypesSQL;
        if (isContractor) {
            // For contractors/instructors: EVAT 5% + Expanded 3%
            deductionTypesSQL = """
                SELECT name, fixed_amount, percentage 
                FROM deduction_types 
                WHERE name IN ('GVAT', 'Expanded Tax')
                """;
        } else {
            // For regular employees: standard government deductions
            deductionTypesSQL = """
                SELECT name, fixed_amount, percentage 
                FROM deduction_types 
                WHERE name IN ('PhilHealth', 'Withholding Tax', 'Pag-ibig', 'SSS')
                """;
        }
        
        try (PreparedStatement stmt = conn.prepareStatement(deductionTypesSQL)) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Double fixedAmount = rs.getDouble("fixed_amount");
                Double percentage = rs.getDouble("percentage");
                
                if (fixedAmount != null && fixedAmount > 0) {
                    calc.totalDeductions += fixedAmount;
                } else if (percentage != null && percentage > 0) {
                    calc.totalDeductions += (calc.basicSalary * percentage / 100);
                }
            }
        }
        
        // Employee-specific deductions
        String employeeDeductionsSQL = """
            SELECT ed.amount 
            FROM employee_deductions ed
            WHERE ed.employee_id = ?
            """;
        
        try (PreparedStatement stmt = conn.prepareStatement(employeeDeductionsSQL)) {
            stmt.setInt(1, employeeId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                calc.totalDeductions += rs.getDouble("amount");
            }
        }
        
        // Late and absent deductions
        double lateDeduction = (calc.totalLateMinutes / 60.0) * hourlyRate;
        double absentDeduction = calc.absentDays * dailyRate;
        calc.totalDeductions += lateDeduction + absentDeduction;
        
        // Calculate net pay
        calc.netPay = calc.basicPay + calc.overtimePay + calc.allowances - calc.totalDeductions;
        
        return calc;
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
        filterPayrollData();
    }

    private void calculateIndividualDeductions(PayrollProcessEntry entry) {
        try (Connection conn = getConnection()) {
            double basicSalary = entry.getBasicSalary();
            
            // Determine employee type based on position
            String employeeTypeSQL = """
                SELECT position FROM employees WHERE id = ?
                """;
            
            boolean isContractor = false;
            try (PreparedStatement typeStmt = conn.prepareStatement(employeeTypeSQL)) {
                typeStmt.setInt(1, entry.getEmployeeId());
                ResultSet typeRs = typeStmt.executeQuery();
                if (typeRs.next()) {
                    String position = typeRs.getString("position");
                    isContractor = position != null && 
                                  (position.toLowerCase().contains("instructor") || 
                                   position.toLowerCase().contains("contractor"));
                }
            }
            
            // Apply different deductions based on employee type
            String deductionTypesSQL;
            if (isContractor) {
                // For contractors/instructors: EVAT 5% + Expanded 3%
                deductionTypesSQL = """
                    SELECT name, fixed_amount, percentage 
                    FROM deduction_types 
                    WHERE name IN ('GVAT', 'Expanded Tax')
                    """;
            } else {
                // For regular employees: standard government deductions
                deductionTypesSQL = """
                    SELECT name, fixed_amount, percentage 
                    FROM deduction_types 
                    WHERE name IN ('PhilHealth', 'Withholding Tax', 'Pag-ibig', 'SSS')
                    """;
            }
            
            try (PreparedStatement stmt = conn.prepareStatement(deductionTypesSQL)) {
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    String deductionName = rs.getString("name");
                    Double fixedAmount = rs.getDouble("fixed_amount");
                    Double percentage = rs.getDouble("percentage");
                    
                    double deductionAmount = 0;
                    if (fixedAmount != null && fixedAmount > 0) {
                        deductionAmount = fixedAmount;
                    } else if (percentage != null && percentage > 0) {
                        deductionAmount = (basicSalary * percentage / 100);
                    }
                    
                    if (isContractor) {
                        // For contractors: EVAT and Expanded Tax go to withholding tax
                        switch (deductionName.toLowerCase()) {
                            case "gvat" -> {
                                // EVAT 5% - treat as withholding tax for display
                                entry.setWithholdingTax(entry.getWithholdingTax() + deductionAmount);
                            }
                            case "expanded tax" -> {
                                // Expanded 3% - add to withholding tax
                                entry.setWithholdingTax(entry.getWithholdingTax() + deductionAmount);
                            }
                        }
                    } else {
                        // For regular employees: standard deductions
                        switch (deductionName.toLowerCase()) {
                            case "philhealth" -> entry.setPhilhealthDeduction(deductionAmount);
                            case "withholding tax" -> entry.setWithholdingTax(deductionAmount);
                            case "pag-ibig" -> entry.setPagibigDeduction(deductionAmount);
                            case "sss" -> entry.setSssDeduction(deductionAmount);
                        }
                    }
                }
            }
            
            // Calculate late deduction
            double hourlyRate = basicSalary / (22 * 8); // 22 working days, 8 hours per day
            double lateMinutes = entry.getTotalLateMinutes();
            double lateDeduction = (lateMinutes / 60.0) * hourlyRate;
            entry.setLateDeduction(lateDeduction);
            
            // Calculate absent deduction
            double dailyRate = basicSalary / 22;
            double absentDeduction = entry.getAbsentDays() * dailyRate;
            entry.setAbsentDeduction(absentDeduction);
            
            // Calculate gross pay
            double grossPay = entry.getBasicPay() + entry.getOvertimePay() + entry.getAllowances();
            entry.setGrossPay(grossPay);
            
            // Calculate other deductions (total - known deductions)
            double knownDeductions = entry.getPhilhealthDeduction() + entry.getWithholdingTax() + 
                                   entry.getPagibigDeduction() + entry.getSssDeduction() + 
                                   entry.getLateDeduction() + entry.getAbsentDeduction();
            double otherDeductions = Math.max(0, entry.getTotalDeductions() - knownDeductions);
            entry.setOtherDeductions(otherDeductions);
            
        } catch (SQLException e) {
            System.err.println("Error calculating individual deductions: " + e.getMessage());
        }
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
        
        // Determine employee type
        boolean isContractor = entry.getPosition() != null && 
                              (entry.getPosition().toLowerCase().contains("instructor") || 
                               entry.getPosition().toLowerCase().contains("contractor"));
        
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
        
        if (isContractor) {
            // For contractors/instructors: show withholding taxes
            details.append(String.format("• Withholding Tax (EVAT 5%% + Expanded 3%%): ₱%.2f\n", entry.getWithholdingTax()));
            details.append("  - EVAT 5%% (VAT Withholding Tax)\n");
            details.append("  - Expanded 3%% (Income Tax Withholding)\n");
        } else {
            // For regular employees: show standard government deductions
            details.append(String.format("• SSS: ₱%.2f\n", entry.getSssDeduction()));
            details.append(String.format("• PhilHealth: ₱%.2f\n", entry.getPhilhealthDeduction()));
            details.append(String.format("• Pag-IBIG: ₱%.2f\n", entry.getPagibigDeduction()));
            details.append(String.format("• Withholding Tax: ₱%.2f\n", entry.getWithholdingTax()));
        }
        
        details.append(String.format("• Late Deduction: ₱%.2f\n", entry.getLateDeduction()));
        details.append(String.format("• Absent Deduction: ₱%.2f\n", entry.getAbsentDeduction()));
        details.append(String.format("• Other Deductions: ₱%.2f\n", entry.getOtherDeductions()));
        details.append(String.format("• Total Deductions: ₱%.2f\n\n", entry.getTotalDeductions()));
        
        details.append(String.format("NET PAY: ₱%.2f\n", entry.getNetPay()));
        
        // Add employee type information
        details.append("\n=====================================\n");
        if (isContractor) {
            details.append("Employee Type: CONTRACTOR/INSTRUCTOR\n");
            details.append("Tax Scheme: EVAT 5% + Expanded 3% = 8% Total Withholding\n");
        } else {
            details.append("Employee Type: REGULAR EMPLOYEE\n");
            details.append("Deductions: Standard Government Contributions + Withholding Tax\n");
        }
        
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