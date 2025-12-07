-- Test script to verify the payroll system integration
-- This tests the flow: attendance -> processed_attendance -> payroll generation

-- 1. Check if we have employees
SELECT 'EMPLOYEES:' as section;
SELECT id, account_number, full_name, salary, status FROM employees WHERE status = 'Active';

-- 2. Check if we have attendance data
SELECT 'RAW ATTENDANCE:' as section;
SELECT COUNT(*) as total_attendance_records, 
       MIN(DATE(log_datetime)) as earliest_date,
       MAX(DATE(log_datetime)) as latest_date
FROM attendance;

-- 3. Check processed attendance data
SELECT 'PROCESSED ATTENDANCE:' as section;
SELECT COUNT(*) as total_processed_records,
       MIN(process_date) as earliest_processed,
       MAX(process_date) as latest_processed
FROM processed_attendance;

-- 4. Check deduction types
SELECT 'DEDUCTION TYPES:' as section;
SELECT id, name, fixed_amount, percentage FROM deduction_types;

-- 5. Check employee deductions
SELECT 'EMPLOYEE DEDUCTIONS:' as section;
SELECT ed.id, e.full_name, dt.name, ed.amount 
FROM employee_deductions ed
JOIN employees e ON ed.employee_id = e.id
JOIN deduction_types dt ON ed.deduction_type_id = dt.id;

-- 6. Check existing payroll records
SELECT 'PAYROLL HISTORY:' as section;
SELECT COUNT(*) as total_payroll_records FROM payroll_history;

SELECT 'PAYROLL PROCESS:' as section;
SELECT COUNT(*) as total_process_records FROM payroll_process;

-- 7. Sample processed attendance data for verification
SELECT 'SAMPLE PROCESSED ATTENDANCE:' as section;
SELECT pa.*, e.full_name
FROM processed_attendance pa
JOIN employees e ON pa.employee_id = e.id
LIMIT 5;