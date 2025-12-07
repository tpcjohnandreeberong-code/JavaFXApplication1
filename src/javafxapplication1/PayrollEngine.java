package javafxapplication1;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Complete Payroll Engine - Implements the full deduction process flow
 * Based on your exact database structure: attendance → payroll_process → employee_deductions → net pay
 */
public class PayrollEngine {
    
    private static final Logger logger = Logger.getLogger(PayrollEngine.class.getName());
    private Connection connection;
    
    // Constants for time calculations
    private static final LocalTime STANDARD_TIME_IN = LocalTime.of(8, 0); // 8:00 AM
    private static final LocalTime STANDARD_TIME_OUT = LocalTime.of(17, 0); // 5:00 PM
    private static final int STANDARD_WORKING_MINUTES = 480; // 8 hours
    
    public PayrollEngine() {
        try {
            this.connection = DatabaseConfig.getConnection();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to establish database connection", e);
        }
    }
    
    /**
     * MAIN METHOD - Complete payroll processing for a pay period
     */
    public PayrollProcessResult processPayrollForPeriod(LocalDate startDate, LocalDate endDate, String processedBy) {
        PayrollProcessResult result = new PayrollProcessResult();
        
        try {
            logger.info("Starting payroll processing for period: " + startDate + " to " + endDate);
            
            // Step 1: Get all active employees
            List<Employee> employees = getActiveEmployees();
            
            for (Employee employee : employees) {
                try {
                    // Process each employee's payroll
                    PayrollData payrollData = processEmployeePayroll(employee, startDate, endDate);
                    
                    // Save to payroll_process table
                    int payrollId = savePayrollProcess(payrollData, startDate, endDate, processedBy);
                    
                    result.addSuccessfulEmployee(employee.getId(), payrollId);
                    
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Failed to process payroll for employee: " + employee.getFullName(), e);
                    result.addFailedEmployee(employee.getId(), e.getMessage());
                }
            }
            
            result.setSuccess(true);
            logger.info("Payroll processing completed. Successful: " + result.getSuccessfulCount() + 
                       ", Failed: " + result.getFailedCount());
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Payroll processing failed", e);
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
        }
        
        return result;
    }
    
    /**
     * STEP 1 & 2 - Collect attendance logs and compute daily status
     */
    private AttendanceSummary processAttendanceForEmployee(int employeeId, String accountNumber, 
                                                          LocalDate startDate, LocalDate endDate) throws SQLException {
        
        AttendanceSummary summary = new AttendanceSummary();
        
        String sql = """
            SELECT 
                DATE(log_datetime) as attendance_date,
                log_type,
                TIME(log_datetime) as log_time
            FROM attendance 
            WHERE account_number = ? 
            AND DATE(log_datetime) BETWEEN ? AND ?
            ORDER BY log_datetime
            """;
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, accountNumber);
            stmt.setDate(2, java.sql.Date.valueOf(startDate));
            stmt.setDate(3, java.sql.Date.valueOf(endDate));
            
            ResultSet rs = stmt.executeQuery();
            
            // Group logs by date
            Map<LocalDate, DailyAttendance> dailyLogs = new HashMap<>();
            
            while (rs.next()) {
                LocalDate date = rs.getDate("attendance_date").toLocalDate();
                String logType = rs.getString("log_type");
                LocalTime logTime = rs.getTime("log_time").toLocalTime();
                
                dailyLogs.computeIfAbsent(date, k -> new DailyAttendance(date))
                         .addLog(logType, logTime);
            }
            
            // Process each day and compute status
            LocalDate currentDate = startDate;
            while (!currentDate.isAfter(endDate)) {
                DailyAttendance dayData = dailyLogs.get(currentDate);
                
                if (dayData == null) {
                    // Absent
                    summary.addAbsentDay();
                } else {
                    // Analyze attendance
                    analyzeDailyAttendance(dayData, summary);
                }
                
                currentDate = currentDate.plusDays(1);
            }
        }
        
        return summary;
    }
    
    /**
     * Analyze daily attendance and compute late, undertime, half-day
     */
    private void analyzeDailyAttendance(DailyAttendance dayData, AttendanceSummary summary) {
        
        LocalTime timeInAM = dayData.getTimeInAM();
        LocalTime timeOutAM = dayData.getTimeOutAM();
        LocalTime timeInPM = dayData.getTimeInPM();
        LocalTime timeOutPM = dayData.getTimeOutPM();
        
        // Check for half day (missing AM or PM)
        boolean hasAM = (timeInAM != null && timeOutAM != null);
        boolean hasPM = (timeInPM != null && timeOutPM != null);
        
        if (!hasAM || !hasPM) {
            summary.addHalfDay();
            return;
        }
        
        // Check for late
        if (timeInAM.isAfter(STANDARD_TIME_IN)) {
            int lateMinutes = (int) java.time.Duration.between(STANDARD_TIME_IN, timeInAM).toMinutes();
            summary.addLateMinutes(lateMinutes);
        }
        
        // Check for undertime
        if (timeOutPM.isBefore(STANDARD_TIME_OUT)) {
            int undertimeMinutes = (int) java.time.Duration.between(timeOutPM, STANDARD_TIME_OUT).toMinutes();
            summary.addUndertimeMinutes(undertimeMinutes);
        }
        
        // Calculate total working minutes
        int amMinutes = (int) java.time.Duration.between(timeInAM, timeOutAM).toMinutes();
        int pmMinutes = (int) java.time.Duration.between(timeInPM, timeOutPM).toMinutes();
        int totalMinutes = amMinutes + pmMinutes - 60; // Minus 1 hour lunch break
        
        summary.addWorkingMinutes(totalMinutes);
    }
    
    /**
     * STEP 3 & 4 - Match employee to salary reference and compute deductions
     */
    private PayrollData processEmployeePayroll(Employee employee, LocalDate startDate, LocalDate endDate) throws SQLException {
        PayrollData payrollData = new PayrollData();
        payrollData.setEmployee(employee);
        
        // Get employee salary reference
        SalaryReference salaryRef = getSalaryReference(employee.getSalaryRefId());
        payrollData.setSalaryReference(salaryRef);
        
        // Process attendance and compute attendance-based deductions
        AttendanceSummary attendance = processAttendanceForEmployee(employee.getId(), 
                                                                   employee.getAccountNumber(), 
                                                                   startDate, endDate);
        
        // Calculate attendance deductions
        BigDecimal lateDeduction = calculateLateDeduction(attendance.getTotalLateMinutes(), salaryRef);
        BigDecimal absentDeduction = calculateAbsentDeduction(attendance.getAbsentDays(), salaryRef);
        BigDecimal halfDayDeduction = calculateHalfDayDeduction(attendance.getHalfDays(), salaryRef);
        BigDecimal undertimeDeduction = calculateUndertimeDeduction(attendance.getUndertimeMinutes(), salaryRef);
        
        // Add attendance deductions
        if (lateDeduction.compareTo(BigDecimal.ZERO) > 0) {
            payrollData.addDeduction("Late", lateDeduction, "Late for " + attendance.getTotalLateMinutes() + " minutes");
        }
        if (absentDeduction.compareTo(BigDecimal.ZERO) > 0) {
            payrollData.addDeduction("Absent", absentDeduction, "Absent for " + attendance.getAbsentDays() + " days");
        }
        if (halfDayDeduction.compareTo(BigDecimal.ZERO) > 0) {
            payrollData.addDeduction("Half-day", halfDayDeduction, "Half-day for " + attendance.getHalfDays() + " days");
        }
        if (undertimeDeduction.compareTo(BigDecimal.ZERO) > 0) {
            payrollData.addDeduction("Undertime", undertimeDeduction, "Undertime for " + attendance.getUndertimeMinutes() + " minutes");
        }
        
        // STEP 5 & 6 - Process loan deductions
        processLoanDeductions(employee.getId(), payrollData);
        
        // STEP 7 - Process government contributions
        processGovernmentContributions(employee.getId(), salaryRef, payrollData);
        
        // Calculate totals
        BigDecimal totalDeductions = payrollData.getTotalDeductions();
        BigDecimal netPay = salaryRef.getMonthlySalary().subtract(totalDeductions);
        
        payrollData.setGrossPay(salaryRef.getMonthlySalary());
        payrollData.setTotalDeductions(totalDeductions);
        payrollData.setNetPay(netPay);
        
        return payrollData;
    }
    
    /**
     * STEP 6 - Process loan deductions
     */
    private void processLoanDeductions(int employeeId, PayrollData payrollData) throws SQLException {
        String sql = """
            SELECT el.id, el.loan_type_id, lt.name as loan_type_name, 
                   el.monthly_amortization, el.balance, el.status
            FROM employee_loans el
            JOIN loan_types lt ON el.loan_type_id = lt.id
            WHERE el.employee_id = ? AND el.status = 'Active' AND el.balance > 0
            """;
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, employeeId);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                int loanId = rs.getInt("id");
                String loanTypeName = rs.getString("loan_type_name");
                BigDecimal amortization = rs.getBigDecimal("monthly_amortization");
                BigDecimal balance = rs.getBigDecimal("balance");
                
                // Deduct monthly amortization
                BigDecimal deductionAmount = (amortization.compareTo(balance) <= 0) ? amortization : balance;
                
                payrollData.addDeduction(loanTypeName + " Loan", deductionAmount, 
                                       "Monthly amortization for " + loanTypeName);
                
                // Update loan balance
                updateLoanBalance(loanId, deductionAmount);
            }
        }
    }
    
    /**
     * STEP 7 - Process government contributions
     */
    private void processGovernmentContributions(int employeeId, SalaryReference salaryRef, PayrollData payrollData) throws SQLException {
        String sql = """
            SELECT dt.name, dt.rate_percent, dt.fixed_amount, dt.basis
            FROM deduction_types dt
            WHERE dt.is_government = 1 AND dt.is_active = 1
            """;
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                String name = rs.getString("name");
                BigDecimal ratePercent = rs.getBigDecimal("rate_percent");
                BigDecimal fixedAmount = rs.getBigDecimal("fixed_amount");
                String basis = rs.getString("basis");
                
                BigDecimal deductionAmount = BigDecimal.ZERO;
                
                if ("fixed".equals(basis) && fixedAmount != null) {
                    deductionAmount = fixedAmount;
                } else if ("percentage".equals(basis) && ratePercent != null) {
                    deductionAmount = salaryRef.getMonthlySalary()
                                                .multiply(ratePercent.divide(BigDecimal.valueOf(100)))
                                                .setScale(2, RoundingMode.HALF_UP);
                }
                
                if (deductionAmount.compareTo(BigDecimal.ZERO) > 0) {
                    payrollData.addDeduction(name, deductionAmount, "Government contribution");
                }
            }
        }
    }
    
    // Helper calculation methods
    private BigDecimal calculateLateDeduction(int lateMinutes, SalaryReference salaryRef) {
        if (lateMinutes <= 0) return BigDecimal.ZERO;
        return salaryRef.getRatePerMinute().multiply(BigDecimal.valueOf(lateMinutes))
                                           .setScale(2, RoundingMode.HALF_UP);
    }
    
    private BigDecimal calculateAbsentDeduction(int absentDays, SalaryReference salaryRef) {
        if (absentDays <= 0) return BigDecimal.ZERO;
        return salaryRef.getRatePerDay().multiply(BigDecimal.valueOf(absentDays))
                                        .setScale(2, RoundingMode.HALF_UP);
    }
    
    private BigDecimal calculateHalfDayDeduction(int halfDays, SalaryReference salaryRef) {
        if (halfDays <= 0) return BigDecimal.ZERO;
        return salaryRef.getHalfDayRate().multiply(BigDecimal.valueOf(halfDays))
                                         .setScale(2, RoundingMode.HALF_UP);
    }
    
    private BigDecimal calculateUndertimeDeduction(int undertimeMinutes, SalaryReference salaryRef) {
        if (undertimeMinutes <= 0) return BigDecimal.ZERO;
        return salaryRef.getRatePerMinute().multiply(BigDecimal.valueOf(undertimeMinutes))
                                           .setScale(2, RoundingMode.HALF_UP);
    }
    
    /**
     * STEP 8 & 9 - Save payroll process and employee deductions
     */
    private int savePayrollProcess(PayrollData payrollData, LocalDate startDate, LocalDate endDate, String processedBy) throws SQLException {
        // Save main payroll record
        String insertPayrollSQL = """
            INSERT INTO payroll_process (employee_id, full_name, position, basic_salary, 
                                       gross_pay, total_deductions, net_pay, 
                                       pay_period_start, pay_period_end, processed_by)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        
        try (PreparedStatement stmt = connection.prepareStatement(insertPayrollSQL, Statement.RETURN_GENERATED_KEYS)) {
            Employee emp = payrollData.getEmployee();
            
            stmt.setInt(1, emp.getId());
            stmt.setString(2, emp.getFullName());
            stmt.setString(3, emp.getPosition());
            stmt.setBigDecimal(4, payrollData.getSalaryReference().getMonthlySalary());
            stmt.setBigDecimal(5, payrollData.getGrossPay());
            stmt.setBigDecimal(6, payrollData.getTotalDeductions());
            stmt.setBigDecimal(7, payrollData.getNetPay());
            stmt.setDate(8, java.sql.Date.valueOf(startDate));
            stmt.setDate(9, java.sql.Date.valueOf(endDate));
            stmt.setString(10, processedBy);
            
            stmt.executeUpdate();
            
            ResultSet keys = stmt.getGeneratedKeys();
            int payrollId = 0;
            if (keys.next()) {
                payrollId = keys.getInt(1);
            }
            
            // Save individual deductions
            saveEmployeeDeductions(payrollData.getEmployee().getId(), payrollData.getDeductions(), payrollId);
            
            return payrollId;
        }
    }
    
    private void saveEmployeeDeductions(int employeeId, List<DeductionEntry> deductions, int payrollId) throws SQLException {
        String insertSQL = """
            INSERT INTO employee_deductions (employee_id, deduction_type_id, amount, details, payroll_id)
            VALUES (?, ?, ?, ?, ?)
            """;
        
        try (PreparedStatement stmt = connection.prepareStatement(insertSQL)) {
            for (DeductionEntry deduction : deductions) {
                int deductionTypeId = getDeductionTypeId(deduction.getTypeName());
                
                stmt.setInt(1, employeeId);
                stmt.setInt(2, deductionTypeId);
                stmt.setBigDecimal(3, deduction.getAmount());
                stmt.setString(4, deduction.getDetails());
                stmt.setInt(5, payrollId);
                stmt.addBatch();
            }
            stmt.executeBatch();
        }
    }
    
    // Database helper methods
    private List<Employee> getActiveEmployees() throws SQLException {
        List<Employee> employees = new ArrayList<>();
        String sql = "SELECT id, account_number, full_name, position, salary_ref_id FROM employees WHERE status = 'Active'";
        
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                Employee emp = new Employee();
                emp.setId(rs.getInt("id"));
                emp.setAccountNumber(rs.getString("account_number"));
                emp.setFullName(rs.getString("full_name"));
                emp.setPosition(rs.getString("position"));
                emp.setSalaryRefId(rs.getInt("salary_ref_id"));
                employees.add(emp);
            }
        }
        return employees;
    }
    
    private SalaryReference getSalaryReference(int salaryRefId) throws SQLException {
        String sql = "SELECT monthly_salary, rate_per_day, half_day_rate, rate_per_minute FROM salary_reference WHERE id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, salaryRefId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                SalaryReference ref = new SalaryReference();
                ref.setId(salaryRefId);
                ref.setMonthlySalary(rs.getBigDecimal("monthly_salary"));
                ref.setRatePerDay(rs.getBigDecimal("rate_per_day"));
                ref.setHalfDayRate(rs.getBigDecimal("half_day_rate"));
                ref.setRatePerMinute(rs.getBigDecimal("rate_per_minute"));
                return ref;
            }
        }
        throw new SQLException("Salary reference not found for ID: " + salaryRefId);
    }
    
    private void updateLoanBalance(int loanId, BigDecimal paymentAmount) throws SQLException {
        String updateSQL = """
            UPDATE employee_loans 
            SET balance = balance - ?,
                status = CASE WHEN (balance - ?) <= 0 THEN 'Completed' ELSE status END
            WHERE id = ?
            """;
        
        try (PreparedStatement stmt = connection.prepareStatement(updateSQL)) {
            stmt.setBigDecimal(1, paymentAmount);
            stmt.setBigDecimal(2, paymentAmount);
            stmt.setInt(3, loanId);
            stmt.executeUpdate();
        }
    }
    
    private int getDeductionTypeId(String typeName) throws SQLException {
        String sql = "SELECT id FROM deduction_types WHERE name = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, typeName);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("id");
            }
        }
        
        // If not found, create new deduction type
        String insertSQL = "INSERT INTO deduction_types (name, is_active) VALUES (?, 1)";
        try (PreparedStatement stmt = connection.prepareStatement(insertSQL, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, typeName);
            stmt.executeUpdate();
            ResultSet keys = stmt.getGeneratedKeys();
            if (keys.next()) {
                return keys.getInt(1);
            }
        }
        
        return 1; // Default fallback
    }
}
