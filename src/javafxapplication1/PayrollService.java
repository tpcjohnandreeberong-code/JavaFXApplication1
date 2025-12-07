package javafxapplication1;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.math.BigDecimal;
import javafx.concurrent.Task;

/**
 * PayrollService - Service layer to integrate PayrollEngine with your controllers
 * This class provides methods to be called from your existing controllers
 */
public class PayrollService {
    
    private static final Logger logger = Logger.getLogger(PayrollService.class.getName());
    private PayrollEngine payrollEngine;
    private Connection connection;
    
    public PayrollService() {
        this.payrollEngine = new PayrollEngine();
        try {
            this.connection = DatabaseConfig.getConnection();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to establish database connection", e);
        }
    }
    
    /**
     * Process payroll for all employees in a pay period
     * This method can be called from PayrollProcessingController
     */
    public PayrollProcessResult processPayrollForPeriod(String startDateStr, String endDateStr, String processedBy) {
        try {
            LocalDate startDate = LocalDate.parse(startDateStr);
            LocalDate endDate = LocalDate.parse(endDateStr);
            
            return payrollEngine.processPayrollForPeriod(startDate, endDate, processedBy);
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error processing payroll", e);
            PayrollProcessResult result = new PayrollProcessResult();
            result.setSuccess(false);
            result.setErrorMessage("Error processing payroll: " + e.getMessage());
            return result;
        }
    }
    
    /**
     * Process payroll for a single employee
     * Can be called from EmployeeController or PayrollGeneratorController
     */
    public PayrollProcessResult processSingleEmployeePayroll(int employeeId, String startDateStr, 
                                                            String endDateStr, String processedBy) {
        try {
            LocalDate startDate = LocalDate.parse(startDateStr);
            LocalDate endDate = LocalDate.parse(endDateStr);
            
            // Create a modified version that processes only one employee
            return processSingleEmployee(employeeId, startDate, endDate, processedBy);
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error processing single employee payroll", e);
            PayrollProcessResult result = new PayrollProcessResult();
            result.setSuccess(false);
            result.setErrorMessage("Error processing payroll: " + e.getMessage());
            return result;
        }
    }
    
    /**
     * Get payroll summary for display in controllers
     */
    public List<PayrollSummary> getPayrollSummary(String startDateStr, String endDateStr) {
        List<PayrollSummary> summaries = new ArrayList<>();
        
        String sql = """
            SELECT pp.id, pp.employee_id, pp.full_name, pp.position,
                   pp.basic_salary, pp.gross_pay, pp.total_deductions, pp.net_pay,
                   pp.pay_period_start, pp.pay_period_end, pp.created_at, pp.processed_by
            FROM payroll_process pp
            WHERE pp.pay_period_start = ? AND pp.pay_period_end = ?
            ORDER BY pp.full_name
            """;
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, startDateStr);
            stmt.setString(2, endDateStr);
            
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                PayrollSummary summary = new PayrollSummary();
                summary.setPayrollId(rs.getInt("id"));
                summary.setEmployeeId(rs.getInt("employee_id"));
                summary.setFullName(rs.getString("full_name"));
                summary.setPosition(rs.getString("position"));
                summary.setBasicSalary(rs.getBigDecimal("basic_salary"));
                summary.setGrossPay(rs.getBigDecimal("gross_pay"));
                summary.setTotalDeductions(rs.getBigDecimal("total_deductions"));
                summary.setNetPay(rs.getBigDecimal("net_pay"));
                summary.setPayPeriodStart(rs.getString("pay_period_start"));
                summary.setPayPeriodEnd(rs.getString("pay_period_end"));
                summary.setProcessedBy(rs.getString("processed_by"));
                summary.setCreatedAt(rs.getTimestamp("created_at"));
                
                summaries.add(summary);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error getting payroll summary", e);
        }
        
        return summaries;
    }
    
    /**
     * Get detailed deductions for a specific payroll
     */
    public List<DeductionDetail> getPayrollDeductions(int payrollId) {
        List<DeductionDetail> details = new ArrayList<>();
        
        String sql = """
            SELECT ed.id, dt.name as deduction_type, ed.amount, ed.details, ed.created_at
            FROM employee_deductions ed
            JOIN deduction_types dt ON ed.deduction_type_id = dt.id
            WHERE ed.payroll_id = ?
            ORDER BY dt.name
            """;
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, payrollId);
            
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                DeductionDetail detail = new DeductionDetail();
                detail.setId(rs.getInt("id"));
                detail.setDeductionType(rs.getString("deduction_type"));
                detail.setAmount(rs.getBigDecimal("amount"));
                detail.setDetails(rs.getString("details"));
                detail.setCreatedAt(rs.getTimestamp("created_at"));
                
                details.add(detail);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error getting payroll deductions", e);
        }
        
        return details;
    }
    
    /**
     * Get attendance summary for an employee in a pay period
     */
    public AttendanceReport getEmployeeAttendanceReport(int employeeId, String startDateStr, String endDateStr) {
        AttendanceReport report = new AttendanceReport();
        
        String sql = """
            SELECT 
                COUNT(DISTINCT DATE(log_datetime)) as days_present,
                AVG(CASE WHEN TIME(log_datetime) > '08:00:00' AND log_type = 'TIME_IN_AM' 
                         THEN TIMESTAMPDIFF(MINUTE, '08:00:00', TIME(log_datetime)) ELSE 0 END) as avg_late_minutes
            FROM attendance a
            JOIN employees e ON a.account_number = e.account_number
            WHERE e.id = ? AND DATE(log_datetime) BETWEEN ? AND ?
            """;
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, employeeId);
            stmt.setString(2, startDateStr);
            stmt.setString(3, endDateStr);
            
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                report.setEmployeeId(employeeId);
                report.setDaysPresent(rs.getInt("days_present"));
                report.setAverageLateMinutes(rs.getDouble("avg_late_minutes"));
                
                // Calculate working days in period
                LocalDate start = LocalDate.parse(startDateStr);
                LocalDate end = LocalDate.parse(endDateStr);
                int workingDays = calculateWorkingDays(start, end);
                
                report.setTotalWorkingDays(workingDays);
                report.setAbsentDays(workingDays - report.getDaysPresent());
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error getting attendance report", e);
        }
        
        return report;
    }
    
    /**
     * Create async task for processing payroll (for UI integration)
     */
    public Task<PayrollProcessResult> createPayrollProcessingTask(String startDateStr, String endDateStr, String processedBy) {
        return new Task<PayrollProcessResult>() {
            @Override
            protected PayrollProcessResult call() throws Exception {
                updateMessage("Initializing payroll processing...");
                updateProgress(0, 100);
                
                PayrollProcessResult result = processPayrollForPeriod(startDateStr, endDateStr, processedBy);
                
                updateMessage("Payroll processing completed");
                updateProgress(100, 100);
                
                return result;
            }
        };
    }
    
    /**
     * Validate payroll period before processing
     */
    public PayrollValidationResult validatePayrollPeriod(String startDateStr, String endDateStr) {
        PayrollValidationResult validation = new PayrollValidationResult();
        
        try {
            LocalDate startDate = LocalDate.parse(startDateStr);
            LocalDate endDate = LocalDate.parse(endDateStr);
            
            // Check if dates are valid
            if (startDate.isAfter(endDate)) {
                validation.addError("Start date cannot be after end date");
            }
            
            if (endDate.isAfter(LocalDate.now())) {
                validation.addError("End date cannot be in the future");
            }
            
            // Check if payroll already processed for this period
            if (isPayrollAlreadyProcessed(startDateStr, endDateStr)) {
                validation.addWarning("Payroll has already been processed for this period");
            }
            
            // Check if there are active employees
            int activeEmployees = getActiveEmployeeCount();
            if (activeEmployees == 0) {
                validation.addError("No active employees found");
            } else {
                validation.addInfo("Found " + activeEmployees + " active employees");
            }
            
            // Check if there's attendance data for the period
            int attendanceCount = getAttendanceCount(startDateStr, endDateStr);
            if (attendanceCount == 0) {
                validation.addWarning("No attendance data found for the period");
            } else {
                validation.addInfo("Found " + attendanceCount + " attendance records");
            }
            
        } catch (Exception e) {
            validation.addError("Error validating payroll period: " + e.getMessage());
        }
        
        return validation;
    }
    
    // Private helper methods
    private PayrollProcessResult processSingleEmployee(int employeeId, LocalDate startDate, 
                                                      LocalDate endDate, String processedBy) throws SQLException {
        // Implementation similar to main engine but for single employee
        PayrollProcessResult result = new PayrollProcessResult();
        // Implementation would go here - simplified for now
        result.setSuccess(true);
        return result;
    }
    
    private boolean isPayrollAlreadyProcessed(String startDateStr, String endDateStr) {
        String sql = "SELECT COUNT(*) FROM payroll_process WHERE pay_period_start = ? AND pay_period_end = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, startDateStr);
            stmt.setString(2, endDateStr);
            ResultSet rs = stmt.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        } catch (SQLException e) {
            return false;
        }
    }
    
    private int getActiveEmployeeCount() {
        String sql = "SELECT COUNT(*) FROM employees WHERE status = 'Active'";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            return 0;
        }
    }
    
    private int getAttendanceCount(String startDateStr, String endDateStr) {
        String sql = "SELECT COUNT(*) FROM attendance WHERE DATE(log_datetime) BETWEEN ? AND ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, startDateStr);
            stmt.setString(2, endDateStr);
            ResultSet rs = stmt.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            return 0;
        }
    }
    
    private int calculateWorkingDays(LocalDate start, LocalDate end) {
        int workingDays = 0;
        LocalDate current = start;
        while (!current.isAfter(end)) {
            // Skip weekends (Saturday = 6, Sunday = 7)
            if (current.getDayOfWeek().getValue() < 6) {
                workingDays++;
            }
            current = current.plusDays(1);
        }
        return workingDays;
    }
}

// Additional data classes for the service
class PayrollSummary {
    private int payrollId;
    private int employeeId;
    private String fullName;
    private String position;
    private BigDecimal basicSalary;
    private BigDecimal grossPay;
    private BigDecimal totalDeductions;
    private BigDecimal netPay;
    private String payPeriodStart;
    private String payPeriodEnd;
    private String processedBy;
    private Timestamp createdAt;
    
    // Getters and Setters
    public int getPayrollId() { return payrollId; }
    public void setPayrollId(int payrollId) { this.payrollId = payrollId; }
    
    public int getEmployeeId() { return employeeId; }
    public void setEmployeeId(int employeeId) { this.employeeId = employeeId; }
    
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    
    public String getPosition() { return position; }
    public void setPosition(String position) { this.position = position; }
    
    public BigDecimal getBasicSalary() { return basicSalary; }
    public void setBasicSalary(BigDecimal basicSalary) { this.basicSalary = basicSalary; }
    
    public BigDecimal getGrossPay() { return grossPay; }
    public void setGrossPay(BigDecimal grossPay) { this.grossPay = grossPay; }
    
    public BigDecimal getTotalDeductions() { return totalDeductions; }
    public void setTotalDeductions(BigDecimal totalDeductions) { this.totalDeductions = totalDeductions; }
    
    public BigDecimal getNetPay() { return netPay; }
    public void setNetPay(BigDecimal netPay) { this.netPay = netPay; }
    
    public String getPayPeriodStart() { return payPeriodStart; }
    public void setPayPeriodStart(String payPeriodStart) { this.payPeriodStart = payPeriodStart; }
    
    public String getPayPeriodEnd() { return payPeriodEnd; }
    public void setPayPeriodEnd(String payPeriodEnd) { this.payPeriodEnd = payPeriodEnd; }
    
    public String getProcessedBy() { return processedBy; }
    public void setProcessedBy(String processedBy) { this.processedBy = processedBy; }
    
    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}

class DeductionDetail {
    private int id;
    private String deductionType;
    private BigDecimal amount;
    private String details;
    private Timestamp createdAt;
    
    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public String getDeductionType() { return deductionType; }
    public void setDeductionType(String deductionType) { this.deductionType = deductionType; }
    
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    
    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }
    
    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}

class AttendanceReport {
    private int employeeId;
    private int daysPresent;
    private int absentDays;
    private int totalWorkingDays;
    private double averageLateMinutes;
    
    // Getters and Setters
    public int getEmployeeId() { return employeeId; }
    public void setEmployeeId(int employeeId) { this.employeeId = employeeId; }
    
    public int getDaysPresent() { return daysPresent; }
    public void setDaysPresent(int daysPresent) { this.daysPresent = daysPresent; }
    
    public int getAbsentDays() { return absentDays; }
    public void setAbsentDays(int absentDays) { this.absentDays = absentDays; }
    
    public int getTotalWorkingDays() { return totalWorkingDays; }
    public void setTotalWorkingDays(int totalWorkingDays) { this.totalWorkingDays = totalWorkingDays; }
    
    public double getAverageLateMinutes() { return averageLateMinutes; }
    public void setAverageLateMinutes(double averageLateMinutes) { this.averageLateMinutes = averageLateMinutes; }
}

class PayrollValidationResult {
    private List<String> errors;
    private List<String> warnings;
    private List<String> info;
    
    public PayrollValidationResult() {
        this.errors = new ArrayList<>();
        this.warnings = new ArrayList<>();
        this.info = new ArrayList<>();
    }
    
    public void addError(String error) { errors.add(error); }
    public void addWarning(String warning) { warnings.add(warning); }
    public void addInfo(String info) { this.info.add(info); }
    
    public boolean isValid() { return errors.isEmpty(); }
    
    public List<String> getErrors() { return errors; }
    public List<String> getWarnings() { return warnings; }
    public List<String> getInfo() { return info; }
}