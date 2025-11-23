package javafxapplication1;

import javafx.beans.property.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class PayrollEntry {
    private final IntegerProperty payrollId;
    private final IntegerProperty employeeId;
    private final StringProperty employeeName;
    private final StringProperty position;
    private final StringProperty payPeriod;
    private final DoubleProperty basicSalary;
    private final DoubleProperty overtime;
    private final DoubleProperty allowances;
    private final DoubleProperty deductions;
    private final DoubleProperty netPay;
    private final StringProperty status;
    private final StringProperty createdDate;
    
    // Default constructor
    public PayrollEntry() {
        this.payrollId = new SimpleIntegerProperty();
        this.employeeId = new SimpleIntegerProperty();
        this.employeeName = new SimpleStringProperty();
        this.position = new SimpleStringProperty();
        this.payPeriod = new SimpleStringProperty();
        this.basicSalary = new SimpleDoubleProperty();
        this.overtime = new SimpleDoubleProperty();
        this.allowances = new SimpleDoubleProperty();
        this.deductions = new SimpleDoubleProperty();
        this.netPay = new SimpleDoubleProperty();
        this.status = new SimpleStringProperty();
        this.createdDate = new SimpleStringProperty();
    }
    
    // Constructor with parameters
    public PayrollEntry(int payrollId, int employeeId, String employeeName, String position, 
                       String payPeriod, double basicSalary, double overtime, double allowances, 
                       double deductions, String status, LocalDateTime createdDate) {
        this();
        setPayrollId(payrollId);
        setEmployeeId(employeeId);
        setEmployeeName(employeeName);
        setPosition(position);
        setPayPeriod(payPeriod);
        setBasicSalary(basicSalary);
        setOvertime(overtime);
        setAllowances(allowances);
        setDeductions(deductions);
        setStatus(status);
        setCreatedDate(createdDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        calculateNetPay();
    }
    
    // Calculate net pay automatically
    public void calculateNetPay() {
        double gross = getBasicSalary() + getOvertime() + getAllowances();
        double net = gross - getDeductions();
        
        // Round to 2 decimal places
        BigDecimal roundedNet = BigDecimal.valueOf(net).setScale(2, RoundingMode.HALF_UP);
        setNetPay(roundedNet.doubleValue());
    }
    
    // Payroll ID
    public IntegerProperty payrollIdProperty() { return payrollId; }
    public int getPayrollId() { return payrollId.get(); }
    public void setPayrollId(int payrollId) { this.payrollId.set(payrollId); }
    
    // Employee ID
    public IntegerProperty employeeIdProperty() { return employeeId; }
    public int getEmployeeId() { return employeeId.get(); }
    public void setEmployeeId(int employeeId) { this.employeeId.set(employeeId); }
    
    // Employee Name
    public StringProperty employeeNameProperty() { return employeeName; }
    public String getEmployeeName() { return employeeName.get(); }
    public void setEmployeeName(String employeeName) { this.employeeName.set(employeeName); }
    
    // Position
    public StringProperty positionProperty() { return position; }
    public String getPosition() { return position.get(); }
    public void setPosition(String position) { this.position.set(position); }
    
    // Pay Period
    public StringProperty payPeriodProperty() { return payPeriod; }
    public String getPayPeriod() { return payPeriod.get(); }
    public void setPayPeriod(String payPeriod) { this.payPeriod.set(payPeriod); }
    
    // Basic Salary
    public DoubleProperty basicSalaryProperty() { return basicSalary; }
    public double getBasicSalary() { return basicSalary.get(); }
    public void setBasicSalary(double basicSalary) { 
        this.basicSalary.set(basicSalary); 
        calculateNetPay();
    }
    
    // Overtime
    public DoubleProperty overtimeProperty() { return overtime; }
    public double getOvertime() { return overtime.get(); }
    public void setOvertime(double overtime) { 
        this.overtime.set(overtime); 
        calculateNetPay();
    }
    
    // Allowances
    public DoubleProperty allowancesProperty() { return allowances; }
    public double getAllowances() { return allowances.get(); }
    public void setAllowances(double allowances) { 
        this.allowances.set(allowances); 
        calculateNetPay();
    }
    
    // Deductions
    public DoubleProperty deductionsProperty() { return deductions; }
    public double getDeductions() { return deductions.get(); }
    public void setDeductions(double deductions) { 
        this.deductions.set(deductions); 
        calculateNetPay();
    }
    
    // Net Pay
    public DoubleProperty netPayProperty() { return netPay; }
    public double getNetPay() { return netPay.get(); }
    public void setNetPay(double netPay) { this.netPay.set(netPay); }
    
    // Status
    public StringProperty statusProperty() { return status; }
    public String getStatus() { return status.get(); }
    public void setStatus(String status) { this.status.set(status); }
    
    // Created Date
    public StringProperty createdDateProperty() { return createdDate; }
    public String getCreatedDate() { return createdDate.get(); }
    public void setCreatedDate(String createdDate) { this.createdDate.set(createdDate); }
    
    @Override
    public String toString() {
        return String.format("PayrollEntry{payrollId=%d, employeeId=%d, employeeName='%s', netPay=%.2f}", 
                            getPayrollId(), getEmployeeId(), getEmployeeName(), getNetPay());
    }
}