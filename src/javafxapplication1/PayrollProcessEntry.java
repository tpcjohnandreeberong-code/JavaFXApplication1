package javafxapplication1;

import javafx.beans.property.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PayrollProcessEntry {
    private final IntegerProperty processId;
    private final IntegerProperty employeeId;
    private final StringProperty accountNumber;
    private final StringProperty employeeName;
    private final StringProperty position;
    private final StringProperty payPeriodStart;
    private final StringProperty payPeriodEnd;
    
    // Basic Salary
    private final DoubleProperty basicSalary;
    private final DoubleProperty dailyRate;
    private final DoubleProperty hourlyRate;
    
    // Attendance
    private final IntegerProperty totalWorkDays;
    private final IntegerProperty presentDays;
    private final IntegerProperty absentDays;
    private final IntegerProperty lateOccurrences;
    private final IntegerProperty totalLateMinutes;
    
    // Hours
    private final DoubleProperty regularHours;
    private final DoubleProperty overtimeHours;
    private final DoubleProperty undertimeHours;
    
    // Earnings
    private final DoubleProperty basicPay;
    private final DoubleProperty overtimePay;
    private final DoubleProperty allowances;
    private final DoubleProperty grossPay;
    
    // Government Deductions
    private final DoubleProperty sssDeduction;
    private final DoubleProperty philhealthDeduction;
    private final DoubleProperty pagibigDeduction;
    private final DoubleProperty withholdingTax;
    
    // Other Deductions
    private final DoubleProperty lateDeduction;
    private final DoubleProperty absentDeduction;
    private final DoubleProperty otherDeductions;
    private final DoubleProperty totalDeductions;
    
    // Final
    private final DoubleProperty netPay;
    private final StringProperty status;
    
    // Default constructor
    public PayrollProcessEntry() {
        this.processId = new SimpleIntegerProperty();
        this.employeeId = new SimpleIntegerProperty();
        this.accountNumber = new SimpleStringProperty();
        this.employeeName = new SimpleStringProperty();
        this.position = new SimpleStringProperty();
        this.payPeriodStart = new SimpleStringProperty();
        this.payPeriodEnd = new SimpleStringProperty();
        
        this.basicSalary = new SimpleDoubleProperty();
        this.dailyRate = new SimpleDoubleProperty();
        this.hourlyRate = new SimpleDoubleProperty();
        
        this.totalWorkDays = new SimpleIntegerProperty();
        this.presentDays = new SimpleIntegerProperty();
        this.absentDays = new SimpleIntegerProperty();
        this.lateOccurrences = new SimpleIntegerProperty();
        this.totalLateMinutes = new SimpleIntegerProperty();
        
        this.regularHours = new SimpleDoubleProperty();
        this.overtimeHours = new SimpleDoubleProperty();
        this.undertimeHours = new SimpleDoubleProperty();
        
        this.basicPay = new SimpleDoubleProperty();
        this.overtimePay = new SimpleDoubleProperty();
        this.allowances = new SimpleDoubleProperty();
        this.grossPay = new SimpleDoubleProperty();
        
        this.sssDeduction = new SimpleDoubleProperty();
        this.philhealthDeduction = new SimpleDoubleProperty();
        this.pagibigDeduction = new SimpleDoubleProperty();
        this.withholdingTax = new SimpleDoubleProperty();
        
        this.lateDeduction = new SimpleDoubleProperty();
        this.absentDeduction = new SimpleDoubleProperty();
        this.otherDeductions = new SimpleDoubleProperty();
        this.totalDeductions = new SimpleDoubleProperty();
        
        this.netPay = new SimpleDoubleProperty();
        this.status = new SimpleStringProperty();
    }
    
    // Debug method to test deductions table
    public void debugDeductionsTable() {
        System.out.println("=== DEBUGGING DEDUCTIONS TABLE ===");
        try (Connection conn = java.sql.DriverManager.getConnection(
                DatabaseConfig.getDbUrl(), DatabaseConfig.getDbUser(), DatabaseConfig.getDbPassword());
             PreparedStatement stmt = conn.prepareStatement(
                "SELECT code, name, basis, employee_share, fixed_amount, min_salary, max_salary FROM deductions WHERE is_active = TRUE ORDER BY code")) {
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    System.out.println(String.format("Code: %s, Name: %s, Basis: %s, Employee Share: %.2f%%, Fixed: %.2f, Min: %.2f, Max: %.2f",
                        rs.getString("code"),
                        rs.getString("name"),
                        rs.getString("basis"),
                        rs.getDouble("employee_share"),
                        rs.getDouble("fixed_amount"),
                        rs.getDouble("min_salary"),
                        rs.getDouble("max_salary")
                    ));
                }
            }
        } catch (Exception e) {
            System.err.println("ERROR: Could not access deductions table: " + e.getMessage());
            e.printStackTrace();
        }
        System.out.println("=== END DEBUG ===");
    }

    // Calculate all payroll components
    public void calculatePayroll() {
        // Debug the deductions table first
        debugDeductionsTable();
        // Calculate basic pay based on present days (prorated)
        double calculatedBasicPay = (getBasicSalary() / getTotalWorkDays()) * getPresentDays();
        setBasicPay(round(calculatedBasicPay));
        
        // Calculate overtime pay (1.5x hourly rate)
        double calculatedOvertimePay = getOvertimeHours() * getHourlyRate() * 1.5;
        setOvertimePay(round(calculatedOvertimePay));
        
        // Calculate gross pay (Basic Pay + Overtime + Allowances)
        double calculatedGrossPay = getBasicPay() + getOvertimePay() + getAllowances();
        setGrossPay(round(calculatedGrossPay));
        
        // Calculate deductions based on actual earnings
        if (getPresentDays() > 0) {
            // Only calculate government deductions if employee was present and earning
            calculateGovernmentDeductions();
        } else {
            // No government deductions if not present
            setSssDeduction(0.0);
            setPhilhealthDeduction(0.0);
            setPagibigDeduction(0.0);
            setWithholdingTax(0.0);
        }
        
        // Calculate late deduction from database
        double calculatedLateDeduction = calculateLateDeductionFromDatabase();
        setLateDeduction(round(calculatedLateDeduction));
        
        // Calculate absent deduction ONLY if there are actual absences
        // If employee was not present at all, no salary deduction (since no salary was earned)
        double calculatedAbsentDeduction = 0.0;
        if (getPresentDays() > 0 && getAbsentDays() > 0) {
            // Only deduct for absences if employee had some present days
            double dailyRate = getBasicSalary() / getTotalWorkDays();
            calculatedAbsentDeduction = dailyRate * getAbsentDays();
        }
        setAbsentDeduction(round(calculatedAbsentDeduction));
        
        // STEP 1: Calculate TOTAL DEDUCTIONS first
        double calculatedTotalDeductions = getSssDeduction() + getPhilhealthDeduction() + 
                                         getPagibigDeduction() + getWithholdingTax() + 
                                         getLateDeduction() + getAbsentDeduction() + getOtherDeductions();
        setTotalDeductions(round(calculatedTotalDeductions));
        
        // STEP 2: Calculate NET PAY = GROSS PAY - TOTAL DEDUCTIONS
        // Ensure net pay is never negative (minimum 0)
        double calculatedNetPay = Math.max(0, getGrossPay() - getTotalDeductions());
        setNetPay(round(calculatedNetPay));
    }
    
    private void calculateGovernmentDeductions() {
        double monthlySalary = getBasicSalary();
        
        // Calculate deductions based on database configuration
        calculateDeductionFromDatabase("SSS", monthlySalary);
        calculateDeductionFromDatabase("PHILHEALTH", monthlySalary);
        calculateDeductionFromDatabase("PAGIBIG", monthlySalary);
        
        // Calculate withholding tax using tax brackets
        setWithholdingTax(round(calculateTaxFromDatabase(monthlySalary)));
    }
    
    private void calculateDeductionFromDatabase(String deductionCode, double salary) {
        try (Connection conn = java.sql.DriverManager.getConnection(
                DatabaseConfig.getDbUrl(), DatabaseConfig.getDbUser(), DatabaseConfig.getDbPassword());
             PreparedStatement stmt = conn.prepareStatement(
                "SELECT rate_percent, min_salary, max_salary, employee_share, fixed_amount, basis FROM deductions WHERE code = ? AND is_active = TRUE")) {
            
            stmt.setString(1, deductionCode);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String basis = rs.getString("basis");
                    double deductionAmount = 0.0;
                    
                    System.out.println("DEBUG: Calculating " + deductionCode + " for salary: " + salary);
                    System.out.println("DEBUG: Basis: " + basis);
                    
                    if ("salary".equals(basis)) {
                        double minSalary = rs.getDouble("min_salary");
                        double maxSalary = rs.getDouble("max_salary");
                        double employeeShare = rs.getDouble("employee_share");
                        
                        System.out.println("DEBUG: Min: " + minSalary + ", Max: " + maxSalary + ", Employee Share: " + employeeShare + "%");
                        
                        // Apply salary range limits
                        double applicableSalary = salary;
                        if (minSalary > 0) applicableSalary = Math.max(minSalary, applicableSalary);
                        if (maxSalary > 0) applicableSalary = Math.min(maxSalary, applicableSalary);
                        
                        // Calculate deduction based on employee share percentage
                        deductionAmount = applicableSalary * (employeeShare / 100.0);
                        
                        System.out.println("DEBUG: Applicable Salary: " + applicableSalary + ", Deduction: " + deductionAmount);
                        
                    } else if ("fixed".equals(basis)) {
                        deductionAmount = rs.getDouble("fixed_amount");
                        System.out.println("DEBUG: Fixed amount: " + deductionAmount);
                    }
                    
                    // Set the appropriate deduction based on code
                    switch (deductionCode) {
                        case "SSS" -> setSssDeduction(round(deductionAmount));
                        case "PHILHEALTH" -> setPhilhealthDeduction(round(deductionAmount));
                        case "PAGIBIG" -> setPagibigDeduction(round(deductionAmount));
                    }
                } else {
                    System.err.println("ERROR: No deduction record found for " + deductionCode);
                    // Set to zero if no record found
                    switch (deductionCode) {
                        case "SSS" -> setSssDeduction(0.0);
                        case "PHILHEALTH" -> setPhilhealthDeduction(0.0);
                        case "PAGIBIG" -> setPagibigDeduction(0.0);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error calculating " + deductionCode + " deduction: " + e.getMessage());
            e.printStackTrace();
            // Fallback to zero if database error
            switch (deductionCode) {
                case "SSS" -> setSssDeduction(0.0);
                case "PHILHEALTH" -> setPhilhealthDeduction(0.0);
                case "PAGIBIG" -> setPagibigDeduction(0.0);
            }
        }
    }
    
    private double calculateTaxFromDatabase(double monthlySalary) {
        System.out.println("DEBUG: Calculating tax for salary: " + monthlySalary);
        
        try (Connection conn = java.sql.DriverManager.getConnection(
                DatabaseConfig.getDbUrl(), DatabaseConfig.getDbUser(), DatabaseConfig.getDbPassword());
             PreparedStatement stmt = conn.prepareStatement(
                "SELECT code, rate_percent, min_salary, max_salary, base_tax, excess_over FROM deductions " +
                "WHERE code LIKE 'TAX_BRACKET_%' AND is_active = TRUE AND ? BETWEEN min_salary AND max_salary")) {
            
            stmt.setDouble(1, monthlySalary);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String taxCode = rs.getString("code");
                    double ratePercent = rs.getDouble("rate_percent");
                    double baseTax = rs.getDouble("base_tax");
                    double excessOver = rs.getDouble("excess_over");
                    
                    System.out.println("DEBUG: Found tax bracket: " + taxCode);
                    System.out.println("DEBUG: Rate: " + ratePercent + "%, Base Tax: " + baseTax + ", Excess Over: " + excessOver);
                    
                    // Progressive tax calculation: Base Tax + (Excess Amount × Tax Rate)
                    double taxAmount = baseTax;
                    if (monthlySalary > excessOver) {
                        double excess = monthlySalary - excessOver;
                        double excessTax = excess * (ratePercent / 100.0);
                        taxAmount += excessTax;
                        System.out.println("DEBUG: Excess: " + excess + ", Excess Tax: " + excessTax + ", Total Tax: " + taxAmount);
                    } else {
                        System.out.println("DEBUG: No excess, Tax: " + taxAmount);
                    }
                    
                    return taxAmount;
                } else {
                    System.err.println("ERROR: No tax bracket found for salary: " + monthlySalary);
                }
            }
        } catch (Exception e) {
            System.err.println("Error calculating tax: " + e.getMessage());
            e.printStackTrace();
        }
        
        return 0.0; // No tax if error or no bracket found
    }
    
    private double calculateLateDeductionFromDatabase() {
        try (Connection conn = java.sql.DriverManager.getConnection(
                DatabaseConfig.getDbUrl(), DatabaseConfig.getDbUser(), DatabaseConfig.getDbPassword());
             PreparedStatement stmt = conn.prepareStatement(
                "SELECT fixed_amount FROM deductions WHERE code = 'LATE' AND is_active = TRUE")) {
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    double lateAmount = rs.getDouble("fixed_amount");
                    return getLateOccurrences() * lateAmount;
                }
            }
        } catch (Exception e) {
            System.err.println("Error calculating late deduction: " + e.getMessage());
        }
        
        // Fallback to ₱50 per occurrence if database error
        return getLateOccurrences() * 50.0;
    }
    
    private double calculateWithholdingTax(double monthlySalary) {
        // TRAIN Law 2025 Tax Brackets (Monthly)
        if (monthlySalary <= 20833) {
            return 0; // 0% tax
        } else if (monthlySalary <= 33332) {
            return (monthlySalary - 20833) * 0.15; // 15% tax
        } else if (monthlySalary <= 66667) {
            return 2500 + (monthlySalary - 33332) * 0.20; // ₱2,500 + 20% of excess
        } else if (monthlySalary <= 166667) {
            return 10166.67 + (monthlySalary - 66667) * 0.25; // ₱10,166.67 + 25% of excess
        } else if (monthlySalary <= 666667) {
            return 35166.67 + (monthlySalary - 166667) * 0.30; // ₱35,166.67 + 30% of excess
        } else {
            return 185166.67 + (monthlySalary - 666667) * 0.35; // ₱185,166.67 + 35% of excess
        }
    }
    
    private double round(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
    
    // Property getters and setters
    
    // Process ID
    public IntegerProperty processIdProperty() { return processId; }
    public int getProcessId() { return processId.get(); }
    public void setProcessId(int processId) { this.processId.set(processId); }
    
    // Employee ID
    public IntegerProperty employeeIdProperty() { return employeeId; }
    public int getEmployeeId() { return employeeId.get(); }
    public void setEmployeeId(int employeeId) { this.employeeId.set(employeeId); }
    
    // Account Number
    public StringProperty accountNumberProperty() { return accountNumber; }
    public String getAccountNumber() { return accountNumber.get(); }
    public void setAccountNumber(String accountNumber) { this.accountNumber.set(accountNumber); }
    
    // Employee Name
    public StringProperty employeeNameProperty() { return employeeName; }
    public String getEmployeeName() { return employeeName.get(); }
    public void setEmployeeName(String employeeName) { this.employeeName.set(employeeName); }
    
    // Position
    public StringProperty positionProperty() { return position; }
    public String getPosition() { return position.get(); }
    public void setPosition(String position) { this.position.set(position); }
    
    // Pay Period Start
    public StringProperty payPeriodStartProperty() { return payPeriodStart; }
    public String getPayPeriodStart() { return payPeriodStart.get(); }
    public void setPayPeriodStart(String payPeriodStart) { this.payPeriodStart.set(payPeriodStart); }
    
    // Pay Period End
    public StringProperty payPeriodEndProperty() { return payPeriodEnd; }
    public String getPayPeriodEnd() { return payPeriodEnd.get(); }
    public void setPayPeriodEnd(String payPeriodEnd) { this.payPeriodEnd.set(payPeriodEnd); }
    
    // Basic Salary
    public DoubleProperty basicSalaryProperty() { return basicSalary; }
    public double getBasicSalary() { return basicSalary.get(); }
    public void setBasicSalary(double basicSalary) { this.basicSalary.set(basicSalary); }
    
    // Daily Rate
    public DoubleProperty dailyRateProperty() { return dailyRate; }
    public double getDailyRate() { return dailyRate.get(); }
    public void setDailyRate(double dailyRate) { this.dailyRate.set(dailyRate); }
    
    // Hourly Rate
    public DoubleProperty hourlyRateProperty() { return hourlyRate; }
    public double getHourlyRate() { return hourlyRate.get(); }
    public void setHourlyRate(double hourlyRate) { this.hourlyRate.set(hourlyRate); }
    
    // Total Work Days
    public IntegerProperty totalWorkDaysProperty() { return totalWorkDays; }
    public int getTotalWorkDays() { return totalWorkDays.get(); }
    public void setTotalWorkDays(int totalWorkDays) { this.totalWorkDays.set(totalWorkDays); }
    
    // Present Days
    public IntegerProperty presentDaysProperty() { return presentDays; }
    public int getPresentDays() { return presentDays.get(); }
    public void setPresentDays(int presentDays) { this.presentDays.set(presentDays); }
    
    // Absent Days
    public IntegerProperty absentDaysProperty() { return absentDays; }
    public int getAbsentDays() { return absentDays.get(); }
    public void setAbsentDays(int absentDays) { this.absentDays.set(absentDays); }
    
    // Late Occurrences
    public IntegerProperty lateOccurrencesProperty() { return lateOccurrences; }
    public int getLateOccurrences() { return lateOccurrences.get(); }
    public void setLateOccurrences(int lateOccurrences) { this.lateOccurrences.set(lateOccurrences); }
    
    // Total Late Minutes
    public IntegerProperty totalLateMinutesProperty() { return totalLateMinutes; }
    public int getTotalLateMinutes() { return totalLateMinutes.get(); }
    public void setTotalLateMinutes(int totalLateMinutes) { this.totalLateMinutes.set(totalLateMinutes); }
    
    // Regular Hours
    public DoubleProperty regularHoursProperty() { return regularHours; }
    public double getRegularHours() { return regularHours.get(); }
    public void setRegularHours(double regularHours) { this.regularHours.set(regularHours); }
    
    // Overtime Hours
    public DoubleProperty overtimeHoursProperty() { return overtimeHours; }
    public double getOvertimeHours() { return overtimeHours.get(); }
    public void setOvertimeHours(double overtimeHours) { this.overtimeHours.set(overtimeHours); }
    
    // Undertime Hours
    public DoubleProperty undertimeHoursProperty() { return undertimeHours; }
    public double getUndertimeHours() { return undertimeHours.get(); }
    public void setUndertimeHours(double undertimeHours) { this.undertimeHours.set(undertimeHours); }
    
    // Basic Pay
    public DoubleProperty basicPayProperty() { return basicPay; }
    public double getBasicPay() { return basicPay.get(); }
    public void setBasicPay(double basicPay) { this.basicPay.set(basicPay); }
    
    // Overtime Pay
    public DoubleProperty overtimePayProperty() { return overtimePay; }
    public double getOvertimePay() { return overtimePay.get(); }
    public void setOvertimePay(double overtimePay) { this.overtimePay.set(overtimePay); }
    
    // Allowances
    public DoubleProperty allowancesProperty() { return allowances; }
    public double getAllowances() { return allowances.get(); }
    public void setAllowances(double allowances) { this.allowances.set(allowances); }
    
    // Gross Pay
    public DoubleProperty grossPayProperty() { return grossPay; }
    public double getGrossPay() { return grossPay.get(); }
    public void setGrossPay(double grossPay) { this.grossPay.set(grossPay); }
    
    // SSS Deduction
    public DoubleProperty sssDeductionProperty() { return sssDeduction; }
    public double getSssDeduction() { return sssDeduction.get(); }
    public void setSssDeduction(double sssDeduction) { this.sssDeduction.set(sssDeduction); }
    
    // PhilHealth Deduction
    public DoubleProperty philhealthDeductionProperty() { return philhealthDeduction; }
    public double getPhilhealthDeduction() { return philhealthDeduction.get(); }
    public void setPhilhealthDeduction(double philhealthDeduction) { this.philhealthDeduction.set(philhealthDeduction); }
    
    // Pag-IBIG Deduction
    public DoubleProperty pagibigDeductionProperty() { return pagibigDeduction; }
    public double getPagibigDeduction() { return pagibigDeduction.get(); }
    public void setPagibigDeduction(double pagibigDeduction) { this.pagibigDeduction.set(pagibigDeduction); }
    
    // Withholding Tax
    public DoubleProperty withholdingTaxProperty() { return withholdingTax; }
    public double getWithholdingTax() { return withholdingTax.get(); }
    public void setWithholdingTax(double withholdingTax) { this.withholdingTax.set(withholdingTax); }
    
    // Late Deduction
    public DoubleProperty lateDeductionProperty() { return lateDeduction; }
    public double getLateDeduction() { return lateDeduction.get(); }
    public void setLateDeduction(double lateDeduction) { this.lateDeduction.set(lateDeduction); }
    
    // Absent Deduction
    public DoubleProperty absentDeductionProperty() { return absentDeduction; }
    public double getAbsentDeduction() { return absentDeduction.get(); }
    public void setAbsentDeduction(double absentDeduction) { this.absentDeduction.set(absentDeduction); }
    
    // Other Deductions
    public DoubleProperty otherDeductionsProperty() { return otherDeductions; }
    public double getOtherDeductions() { return otherDeductions.get(); }
    public void setOtherDeductions(double otherDeductions) { this.otherDeductions.set(otherDeductions); }
    
    // Total Deductions
    public DoubleProperty totalDeductionsProperty() { return totalDeductions; }
    public double getTotalDeductions() { return totalDeductions.get(); }
    public void setTotalDeductions(double totalDeductions) { this.totalDeductions.set(totalDeductions); }
    
    // Net Pay
    public DoubleProperty netPayProperty() { return netPay; }
    public double getNetPay() { return netPay.get(); }
    public void setNetPay(double netPay) { this.netPay.set(netPay); }
    
    // Status
    public StringProperty statusProperty() { return status; }
    public String getStatus() { return status.get(); }
    public void setStatus(String status) { this.status.set(status); }
    
    @Override
    public String toString() {
        return String.format("PayrollProcessEntry{processId=%d, employeeName='%s', netPay=%.2f, status='%s'}", 
                            getProcessId(), getEmployeeName(), getNetPay(), getStatus());
    }
}