-- TPC Payroll Management System - Employee Database
-- Employee Management Database Schema

-- Create database if it doesn't exist
CREATE DATABASE IF NOT EXISTS payroll;
USE payroll;

-- =============================================
-- EMPLOYEES TABLE (Main Employee Management)
-- =============================================
DROP TABLE IF EXISTS employees;

CREATE TABLE employees (
    id INT AUTO_INCREMENT PRIMARY KEY,
    account_number VARCHAR(50) UNIQUE NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    position VARCHAR(100),
    salary DECIMAL(10,2) DEFAULT 0.00,
    status ENUM('Active', 'Inactive', 'Terminated') NOT NULL DEFAULT 'Active',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_account_number (account_number)
);

-- Insert sample employee data
INSERT INTO employees (account_number, full_name, position, salary, status) VALUES
('EMP-001', 'Juan Cruz', 'Teacher I', 25000.00, 'Active'),
('EMP-002', 'Maria Santos', 'Teacher II', 28000.00, 'Active'),
('EMP-003', 'Pedro Gonzales', 'Head Teacher', 35000.00, 'Active'),
('EMP-004', 'Ana Reyes', 'Teacher I', 25000.00, 'Active'),
('EMP-005', 'Carlos Mendoza', 'Principal', 50000.00, 'Active'),
('EMP-006', 'Rosa Garcia', 'Teacher III', 32000.00, 'Active'),
('EMP-007', 'Miguel Torres', 'Assistant Principal', 45000.00, 'Active'),
('EMP-008', 'Luz Fernandez', 'School Librarian', 22000.00, 'Active'),
('EMP-009', 'Antonio Villanueva', 'Guidance Counselor', 28000.00, 'Active'),
('EMP-010', 'Carmen Dela Cruz', 'Administrative Assistant', 20000.00, 'Active');

-- =============================================
-- USEFUL QUERIES FOR EMPLOYEE MANAGEMENT
-- =============================================

-- Query to get all active employees
-- SELECT id, account_number, full_name, position, salary, created_at FROM employees WHERE status = 'Active' ORDER BY id;

-- Query to search employees by name or account number
-- SELECT * FROM employees WHERE (full_name LIKE '%search_term%' OR account_number LIKE '%search_term%') AND status = 'Active';

-- Query to get employee by ID
-- SELECT * FROM employees WHERE id = ?;

-- Query to check if account number exists (for validation)
-- SELECT COUNT(*) FROM employees WHERE account_number = ? AND id != ?;

-- Query to add new employee
-- INSERT INTO employees (account_number, full_name, position, salary) VALUES (?, ?, ?, ?);

-- Query to update employee
-- UPDATE employees SET account_number = ?, full_name = ?, position = ?, salary = ? WHERE id = ?;

-- Query to soft delete employee (set status to Inactive)
-- UPDATE employees SET status = 'Inactive' WHERE id = ?;

-- Query to hard delete employee (if needed)
-- DELETE FROM employees WHERE id = ?;

SELECT 'Employee database setup completed successfully!' as Message;