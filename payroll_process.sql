-- TPC Payroll Management System - Payroll Process Table
-- Payroll Process Database Schema for attendance-based calculations

USE payroll;

-- =============================================
-- PAYROLL PROCESS TABLE
-- =============================================
DROP TABLE IF EXISTS payroll_process;

CREATE TABLE payroll_process (
    process_id INT AUTO_INCREMENT PRIMARY KEY,
    employee_id INT NOT NULL,
    account_number VARCHAR(20) NOT NULL,
    pay_period_start DATE NOT NULL,
    pay_period_end DATE NOT NULL,
    
    -- Basic Salary Information
    basic_salary DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    daily_rate DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    hourly_rate DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    
    -- Attendance Summary
    total_work_days INT NOT NULL DEFAULT 0,
    present_days INT NOT NULL DEFAULT 0,
    absent_days INT NOT NULL DEFAULT 0,
    late_occurrences INT NOT NULL DEFAULT 0,
    total_late_minutes INT NOT NULL DEFAULT 0,
    
    -- Hours Worked
    regular_hours DECIMAL(8,2) NOT NULL DEFAULT 0.00,
    overtime_hours DECIMAL(8,2) NOT NULL DEFAULT 0.00,
    undertime_hours DECIMAL(8,2) NOT NULL DEFAULT 0.00,
    
    -- Earnings
    basic_pay DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    overtime_pay DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    allowances DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    gross_pay DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    
    -- Government Deductions
    sss_deduction DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    philhealth_deduction DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    pagibig_deduction DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    withholding_tax DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    
    -- Other Deductions
    late_deduction DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    absent_deduction DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    other_deductions DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    total_deductions DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    
    -- Final Calculation
    net_pay DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    
    -- Processing Information
    calculation_details TEXT NULL,
    status ENUM('Draft', 'Calculated', 'Approved', 'Paid', 'Cancelled') NOT NULL DEFAULT 'Draft',
    processed_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    processed_by VARCHAR(50) NULL,
    approved_date TIMESTAMP NULL,
    approved_by VARCHAR(50) NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    -- Foreign key constraints
    FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE,
    
    -- Indexes for better performance
    INDEX idx_employee_id (employee_id),
    INDEX idx_account_number (account_number),
    INDEX idx_pay_period (pay_period_start, pay_period_end),
    INDEX idx_status (status),
    INDEX idx_processed_date (processed_date),
    
    -- Unique constraint to prevent duplicate processing for same employee and period
    UNIQUE KEY unique_employee_period (employee_id, pay_period_start, pay_period_end)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Payroll processing records with attendance-based calculations';

-- =============================================
-- USEFUL QUERIES FOR PAYROLL PROCESSING
-- =============================================

-- Query to get payroll process records with employee details
-- SELECT 
--     pp.process_id,
--     pp.employee_id,
--     e.account_number,
--     e.full_name as employee_name,
--     e.position,
--     pp.pay_period_start,
--     pp.pay_period_end,
--     pp.present_days,
--     pp.absent_days,
--     pp.late_occurrences,
--     pp.total_late_minutes,
--     pp.basic_pay,
--     pp.overtime_pay,
--     pp.gross_pay,
--     pp.total_deductions,
--     pp.net_pay,
--     pp.status
-- FROM payroll_process pp
-- JOIN employees e ON pp.employee_id = e.id
-- ORDER BY pp.processed_date DESC;

-- Query to get payroll summary for specific period
-- SELECT 
--     pay_period_start,
--     pay_period_end,
--     COUNT(*) as total_employees,
--     SUM(present_days) as total_present_days,
--     SUM(absent_days) as total_absent_days,
--     SUM(late_occurrences) as total_late_occurrences,
--     SUM(gross_pay) as total_gross_pay,
--     SUM(total_deductions) as total_deductions,
--     SUM(net_pay) as total_net_pay
-- FROM payroll_process 
-- WHERE pay_period_start = ? AND pay_period_end = ?
-- GROUP BY pay_period_start, pay_period_end;

-- Query to get attendance-based payroll calculation details
-- SELECT 
--     pp.*,
--     e.full_name,
--     e.position,
--     ROUND((pp.present_days / pp.total_work_days) * 100, 2) as attendance_percentage,
--     ROUND(pp.total_late_minutes / 60.0, 2) as total_late_hours
-- FROM payroll_process pp
-- JOIN employees e ON pp.employee_id = e.id
-- WHERE pp.employee_id = ?
-- ORDER BY pp.pay_period_start DESC;

SELECT 'Payroll Process table created successfully!' as Message;