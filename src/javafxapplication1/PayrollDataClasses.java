package javafxapplication1;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

/**
 * Supporting data classes for the PayrollEngine
 */

/**
 * Employee data class
 */
class Employee {
    private int id;
    private String accountNumber;
    private String fullName;
    private String position;
    private int salaryRefId;
    
    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }
    
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    
    public String getPosition() { return position; }
    public void setPosition(String position) { this.position = position; }
    
    public int getSalaryRefId() { return salaryRefId; }
    public void setSalaryRefId(int salaryRefId) { this.salaryRefId = salaryRefId; }
}

/**
 * Salary Reference data class
 */
class SalaryReference {
    private int id;
    private BigDecimal monthlySalary;
    private BigDecimal ratePerDay;
    private BigDecimal halfDayRate;
    private BigDecimal ratePerMinute;
    
    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public BigDecimal getMonthlySalary() { return monthlySalary; }
    public void setMonthlySalary(BigDecimal monthlySalary) { this.monthlySalary = monthlySalary; }
    
    public BigDecimal getRatePerDay() { return ratePerDay; }
    public void setRatePerDay(BigDecimal ratePerDay) { this.ratePerDay = ratePerDay; }
    
    public BigDecimal getHalfDayRate() { return halfDayRate; }
    public void setHalfDayRate(BigDecimal halfDayRate) { this.halfDayRate = halfDayRate; }
    
    public BigDecimal getRatePerMinute() { return ratePerMinute; }
    public void setRatePerMinute(BigDecimal ratePerMinute) { this.ratePerMinute = ratePerMinute; }
}

/**
 * Daily attendance data class
 */
class DailyAttendance {
    private LocalDate date;
    private LocalTime timeInAM;
    private LocalTime timeOutAM;
    private LocalTime timeInPM;
    private LocalTime timeOutPM;
    
    public DailyAttendance(LocalDate date) {
        this.date = date;
    }
    
    public void addLog(String logType, LocalTime logTime) {
        switch (logType) {
            case "TIME_IN_AM":
                this.timeInAM = logTime;
                break;
            case "TIME_OUT_AM":
                this.timeOutAM = logTime;
                break;
            case "TIME_IN_PM":
                this.timeInPM = logTime;
                break;
            case "TIME_OUT_PM":
                this.timeOutPM = logTime;
                break;
        }
    }
    
    // Getters
    public LocalDate getDate() { return date; }
    public LocalTime getTimeInAM() { return timeInAM; }
    public LocalTime getTimeOutAM() { return timeOutAM; }
    public LocalTime getTimeInPM() { return timeInPM; }
    public LocalTime getTimeOutPM() { return timeOutPM; }
}

/**
 * Attendance summary for a pay period
 */
class AttendanceSummary {
    private int totalLateMinutes = 0;
    private int absentDays = 0;
    private int halfDays = 0;
    private int undertimeMinutes = 0;
    private int totalWorkingMinutes = 0;
    
    public void addLateMinutes(int minutes) {
        this.totalLateMinutes += minutes;
    }
    
    public void addAbsentDay() {
        this.absentDays++;
    }
    
    public void addHalfDay() {
        this.halfDays++;
    }
    
    public void addUndertimeMinutes(int minutes) {
        this.undertimeMinutes += minutes;
    }
    
    public void addWorkingMinutes(int minutes) {
        this.totalWorkingMinutes += minutes;
    }
    
    // Getters
    public int getTotalLateMinutes() { return totalLateMinutes; }
    public int getAbsentDays() { return absentDays; }
    public int getHalfDays() { return halfDays; }
    public int getUndertimeMinutes() { return undertimeMinutes; }
    public int getTotalWorkingMinutes() { return totalWorkingMinutes; }
}

/**
 * Individual deduction entry
 */
class DeductionEntry {
    private String typeName;
    private BigDecimal amount;
    private String details;
    
    public DeductionEntry(String typeName, BigDecimal amount, String details) {
        this.typeName = typeName;
        this.amount = amount;
        this.details = details;
    }
    
    // Getters and Setters
    public String getTypeName() { return typeName; }
    public void setTypeName(String typeName) { this.typeName = typeName; }
    
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    
    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }
}

/**
 * Complete payroll data for one employee
 */
class PayrollData {
    private Employee employee;
    private SalaryReference salaryReference;
    private List<DeductionEntry> deductions;
    private BigDecimal grossPay;
    private BigDecimal totalDeductions;
    private BigDecimal netPay;
    
    public PayrollData() {
        this.deductions = new ArrayList<>();
        this.grossPay = BigDecimal.ZERO;
        this.totalDeductions = BigDecimal.ZERO;
        this.netPay = BigDecimal.ZERO;
    }
    
    public void addDeduction(String typeName, BigDecimal amount, String details) {
        deductions.add(new DeductionEntry(typeName, amount, details));
    }
    
    public BigDecimal getTotalDeductions() {
        return deductions.stream()
                        .map(DeductionEntry::getAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    // Getters and Setters
    public Employee getEmployee() { return employee; }
    public void setEmployee(Employee employee) { this.employee = employee; }
    
    public SalaryReference getSalaryReference() { return salaryReference; }
    public void setSalaryReference(SalaryReference salaryReference) { this.salaryReference = salaryReference; }
    
    public List<DeductionEntry> getDeductions() { return deductions; }
    public void setDeductions(List<DeductionEntry> deductions) { this.deductions = deductions; }
    
    public BigDecimal getGrossPay() { return grossPay; }
    public void setGrossPay(BigDecimal grossPay) { this.grossPay = grossPay; }
    
    public void setTotalDeductions(BigDecimal totalDeductions) { this.totalDeductions = totalDeductions; }
    
    public BigDecimal getNetPay() { return netPay; }
    public void setNetPay(BigDecimal netPay) { this.netPay = netPay; }
}

/**
 * Payroll processing result
 */
class PayrollProcessResult {
    private boolean success;
    private String errorMessage;
    private Map<Integer, Integer> successfulEmployees; // employeeId -> payrollId
    private Map<Integer, String> failedEmployees; // employeeId -> error message
    
    public PayrollProcessResult() {
        this.successfulEmployees = new HashMap<>();
        this.failedEmployees = new HashMap<>();
    }
    
    public void addSuccessfulEmployee(int employeeId, int payrollId) {
        successfulEmployees.put(employeeId, payrollId);
    }
    
    public void addFailedEmployee(int employeeId, String error) {
        failedEmployees.put(employeeId, error);
    }
    
    public int getSuccessfulCount() {
        return successfulEmployees.size();
    }
    
    public int getFailedCount() {
        return failedEmployees.size();
    }
    
    // Getters and Setters
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    
    public Map<Integer, Integer> getSuccessfulEmployees() { return successfulEmployees; }
    public Map<Integer, String> getFailedEmployees() { return failedEmployees; }
}