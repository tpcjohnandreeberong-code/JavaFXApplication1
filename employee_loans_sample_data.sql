-- =====================================================
-- Sample Data for employee_loans table
-- =====================================================
-- This file contains sample data for testing the Loan Management module
-- Make sure you have at least one employee in the employees table before running this

-- Sample Employee Loan Record
-- Assuming employee_id = 1 exists (Josept Jett Abela)
-- You may need to adjust the employee_id and loan_type_id based on your actual data

INSERT INTO `employee_loans` 
(`employee_id`, `loan_type_id`, `loan_amount`, `monthly_amortization`, `balance`, `start_date`, `status`) 
VALUES 
(
    1,  -- employee_id (adjust based on your employees table)
    2,  -- loan_type_id (adjust based on your loan_types table - e.g., 'Pag-IBIG Loan')
    50000.00,  -- loan_amount
    2000.00,   -- monthly_amortization
    50000.00,  -- initial balance (same as loan_amount)
    '2025-01-15',  -- start_date
    'Active'  -- status
);

-- =====================================================
-- Notes:
-- =====================================================
-- 1. Make sure the employee_id exists in the employees table
-- 2. Make sure the loan_type_id exists in the loan_types table
-- 3. The balance should initially equal the loan_amount
-- 4. Status can be: 'Active', 'Completed', or 'Stopped'
-- 5. end_date will be NULL for active loans and set when loan is completed
-- =====================================================

