-- TPC Payroll Management System - Payroll History Table
-- Payroll History Database Schema

USE payroll;

-- =============================================
-- PAYROLL HISTORY TABLE
-- =============================================
DROP TABLE IF EXISTS payroll_history;

CREATE TABLE payroll_history (
    payroll_id INT AUTO_INCREMENT PRIMARY KEY,
    employee_id INT NOT NULL,
    pay_period VARCHAR(50) NOT NULL,
    basic_salary DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    overtime DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    allowances DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    deductions DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    net_pay DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    status ENUM('Draft', 'Generated', 'Paid', 'Cancelled') NOT NULL DEFAULT 'Draft',
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    -- Foreign key constraint
    FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE,
    
    -- Indexes for better performance
    INDEX idx_employee_id (employee_id),
    INDEX idx_pay_period (pay_period),
    INDEX idx_status (status),
    INDEX idx_created_date (created_date),
    
    -- Unique constraint to prevent duplicate payroll entries for same employee and period
    UNIQUE KEY unique_employee_period (employee_id, pay_period)
);

-- =============================================
-- USEFUL QUERIES FOR PAYROLL HISTORY
-- =============================================

-- Query to get payroll history with employee details
-- SELECT 
--     ph.payroll_id,
--     ph.employee_id,
--     e.full_name as employee_name,
--     e.position,
--     ph.pay_period,
--     ph.basic_salary,
--     ph.overtime,
--     ph.allowances,
--     ph.deductions,
--     ph.net_pay,
--     ph.status,
--     ph.created_date
-- FROM payroll_history ph
-- JOIN employees e ON ph.employee_id = e.id
-- ORDER BY ph.created_date DESC;

-- Query to get payroll for specific employee
-- SELECT * FROM payroll_history WHERE employee_id = ? ORDER BY created_date DESC;

-- Query to get payroll for specific period
-- SELECT ph.*, e.full_name, e.position 
-- FROM payroll_history ph 
-- JOIN employees e ON ph.employee_id = e.id 
-- WHERE ph.pay_period = ? 
-- ORDER BY e.full_name;

-- Query to calculate total payroll cost for period
-- SELECT 
--     pay_period,
--     COUNT(*) as total_employees,
--     SUM(basic_salary) as total_basic,
--     SUM(overtime) as total_overtime,
--     SUM(allowances) as total_allowances,
--     SUM(deductions) as total_deductions,
--     SUM(net_pay) as total_net_pay
-- FROM payroll_history 
-- WHERE pay_period = ?
-- GROUP BY pay_period;

SELECT 'Payroll History table created successfully!' as Message;