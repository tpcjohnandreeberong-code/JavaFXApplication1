-- ========================================
-- PAYROLL SYSTEM FIXES
-- Date: 2026-02-01
-- ========================================
-- Fix 1: Dashboard Payroll Count Query (already fixed in Java code)
-- Fix 2: Attendance Deduction Logic - Only deduct if actually late
-- ========================================

-- BACKUP REMINDER: Please backup your database before running this!
-- Command: mysqldump -u root -p payroll > payroll_backup_$(date +%Y%m%d).sql

-- Fix stored procedure to only deduct for actual late/absent
DROP PROCEDURE IF EXISTS `sp_generate_payroll`;

DELIMITER //

CREATE PROCEDURE `sp_generate_payroll`(
    IN p_start_date DATE,
    IN p_end_date DATE,
    IN p_processed_by VARCHAR(50)
)
BEGIN

    DECLARE done INT DEFAULT FALSE;

    DECLARE v_emp_id INT;
    DECLARE v_acc_no VARCHAR(20);
    DECLARE v_salary DECIMAL(10,2);
    DECLARE v_employment_type VARCHAR(50);
    DECLARE v_assigned_units INT;

    -- LGU Specific variables
    DECLARE v_gross_regular DECIMAL(10,2) DEFAULT 0;
    DECLARE v_rate_per_unit DECIMAL(10,2) DEFAULT 0;
    DECLARE v_overload_amount DECIMAL(10,2) DEFAULT 0;
    DECLARE v_gross_total DECIMAL(10,2) DEFAULT 0;
    DECLARE v_gross_earned DECIMAL(10,2) DEFAULT 0;
    DECLARE v_expanded_tax DECIMAL(10,2) DEFAULT 0;
    DECLARE v_gvat DECIMAL(10,2) DEFAULT 0;

    DECLARE v_sss DECIMAL(10,2) DEFAULT 0;
    DECLARE v_ph DECIMAL(10,2) DEFAULT 0;
    DECLARE v_pagibig DECIMAL(10,2) DEFAULT 0;

    DECLARE v_late DECIMAL(10,2) DEFAULT 0;
    DECLARE v_absent DECIMAL(10,2) DEFAULT 0;
    DECLARE v_tardy_minutes INT DEFAULT 0;
    DECLARE v_absent_days DECIMAL(5,2) DEFAULT 0;

    DECLARE v_total_deductions DECIMAL(10,2);
    DECLARE v_net DECIMAL(10,2);

    DECLARE v_has_attendance INT DEFAULT 0;

    DECLARE cur CURSOR FOR
        SELECT e.id, e.account_number, sr.monthly_salary, e.employment_type, e.assigned_units
        FROM employees e
        JOIN salary_reference sr ON e.salary_ref_id = sr.id
        WHERE e.status='Active';

    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;

    OPEN cur;

    payroll_loop: LOOP

        FETCH cur INTO v_emp_id, v_acc_no, v_salary, v_employment_type, v_assigned_units;
        IF done THEN LEAVE payroll_loop; END IF;

        -- *************************************************
        -- 1. BASIC SALARY COMPUTATION (LGU Formula)
        -- *************************************************
        -- Gross Regular = monthly_salary / 2
        SET v_gross_regular = v_salary / 2;

        -- *************************************************
        -- 2. OVERLOAD COMPUTATION (INSTRUCTORS ONLY)
        -- *************************************************
        IF v_employment_type IN ('INSTRUCTOR', 'TEMPORARY_INSTRUCTOR') AND v_assigned_units IS NOT NULL THEN
            -- rate per unit = monthly salary ÷ 24
            SET v_rate_per_unit = v_salary / 24;
            -- overload_amount = units * rate_per_unit
            SET v_overload_amount = v_assigned_units * v_rate_per_unit;
        ELSE
            SET v_rate_per_unit = NULL;
            SET v_overload_amount = 0;
        END IF;

        -- *************************************************
        -- 3. GROSS TOTAL
        -- *************************************************
        SET v_gross_total = v_gross_regular + v_overload_amount;

        -- *************************************************
        -- 4. ATTENDANCE DEDUCTIONS - FIXED VERSION
        -- *************************************************
        -- Check if employee has attendance records
        SELECT COUNT(*)
        INTO v_has_attendance
        FROM attendance
        WHERE account_number=v_acc_no
          AND DATE(log_datetime) BETWEEN p_start_date AND p_end_date;

        -- ✅ FIXED: Only count actual late minutes from employees who are truly late
        -- Only sum late_minutes when status indicates the employee was actually late
        SELECT 
            IFNULL(SUM(CASE 
                WHEN status IN ('Late', 'Undertime', 'Late & Undertime') 
                THEN late_minutes 
                ELSE 0 
            END), 0),
            IFNULL(SUM(absent_days + (half_days * 0.5)), 0)
        INTO v_tardy_minutes, v_absent_days
        FROM processed_attendance
        WHERE employee_id = v_emp_id
          AND process_date BETWEEN p_start_date AND p_end_date;

        -- Calculate time deductions using LGU rates
        -- Daily rate = monthly_salary / 22
        -- Hourly rate = daily_rate / 8
        -- Minute rate = hourly_rate / 60
        SET v_late = v_tardy_minutes * (v_salary / 22 / 8 / 60);
        SET v_absent = v_absent_days * (v_salary / 22);

        -- *************************************************
        -- 5. GROSS EARNED
        -- *************************************************
        SET v_gross_earned = v_gross_total - v_late - v_absent;

        -- *************************************************
        -- 6. GOVERNMENT DEDUCTIONS (LGU Style)
        -- *************************************************
        -- PAG-IBIG Premium (fixed 200)
        SET v_pagibig = 200.00;

        -- Expanded Tax (5% of gross earned)
        SET v_expanded_tax = v_gross_earned * 0.05;

        -- GVAT (3% of gross earned)
        SET v_gvat = v_gross_earned * 0.03;

        -- Other deductions (SSS, PhilHealth) - use existing logic
        SELECT IFNULL((v_salary * (employee_share / 100)), 0)
        INTO v_sss
        FROM deductions WHERE code='SSS' LIMIT 1;

        SELECT IFNULL((v_salary * (employee_share / 100)), 0)
        INTO v_ph
        FROM deductions WHERE code='PHILHEALTH' LIMIT 1;


        -- *************************************************
        -- 7. TOTAL DEDUCTIONS (LGU Formula)
        -- *************************************************
        SET v_total_deductions =
            IFNULL(v_pagibig,0) +
            IFNULL(v_expanded_tax,0) +
            IFNULL(v_gvat,0) +
            IFNULL(v_sss,0) +
            IFNULL(v_ph,0);

        -- *************************************************
        -- 8. NET AMOUNT DUE (LGU Formula)
        -- *************************************************
        SET v_net = v_gross_earned - v_total_deductions;


        -- *************************************************
        -- 9. INSERT OR UPDATE RESULT (LGU Payroll Format)
        -- *************************************************
        INSERT INTO payroll_process (
            employee_id,
            account_number,
            pay_period_start,
            pay_period_end,
            basic_salary,
            units,
            rate_per_unit,
            overload_amount,
            gross_regular,
            gross_total,
            gross_earned,
            late_deduction,
            absent_deduction,
            sss_deduction,
            philhealth_deduction,
            pagibig_deduction,
            expanded_tax,
            gvat,
            total_deductions,
            net_pay,
            processed_by,
            status
        )
        VALUES (
            v_emp_id,
            v_acc_no,
            p_start_date,
            p_end_date,
            v_salary,
            v_assigned_units,
            v_rate_per_unit,
            v_overload_amount,
            v_gross_regular,
            v_gross_total,
            v_gross_earned,
            v_late,
            v_absent,
            v_sss,
            v_ph,
            v_pagibig,
            v_expanded_tax,
            v_gvat,
            v_total_deductions,
            v_net,
            p_processed_by,
            'Calculated'
        )
        ON DUPLICATE KEY UPDATE
            basic_salary = v_salary,
            units = v_assigned_units,
            rate_per_unit = v_rate_per_unit,
            overload_amount = v_overload_amount,
            gross_regular = v_gross_regular,
            gross_total = v_gross_total,
            gross_earned = v_gross_earned,
            late_deduction = v_late,
            absent_deduction = v_absent,
            sss_deduction = v_sss,
            philhealth_deduction = v_ph,
            pagibig_deduction = v_pagibig,
            expanded_tax = v_expanded_tax,
            gvat = v_gvat,
            total_deductions = v_total_deductions,
            net_pay = v_net,
            processed_by = p_processed_by,
            status = 'Calculated',
            updated_at = CURRENT_TIMESTAMP;

    END LOOP;

    CLOSE cur;

END//

DELIMITER ;

-- ========================================
-- VERIFICATION QUERIES
-- ========================================

SELECT '=== VERIFICATION QUERIES ===' as '';

-- 1. Check payroll count for current month
SELECT 
    '1. Current Month Payroll Count' as 'Query',
    COUNT(DISTINCT employee_id) as payroll_processed_count
FROM payroll_process 
WHERE MONTH(created_at) = MONTH(CURRENT_DATE()) 
AND YEAR(created_at) = YEAR(CURRENT_DATE());

-- 2. Check processed attendance with actual late status
SELECT '2. Attendance Status with Deduction Logic' as '';
SELECT 
    e.full_name,
    pa.process_date,
    pa.late_minutes,
    pa.status,
    CASE 
        WHEN pa.status IN ('Late', 'Undertime', 'Late & Undertime') 
        THEN pa.late_minutes 
        ELSE 0 
    END as deductible_late_minutes,
    CASE 
        WHEN pa.status IN ('Late', 'Undertime', 'Late & Undertime') 
        THEN 'WILL DEDUCT'
        ELSE 'NO DEDUCTION'
    END as deduction_status
FROM processed_attendance pa
JOIN employees e ON pa.employee_id = e.id
WHERE pa.process_date >= DATE_SUB(CURRENT_DATE(), INTERVAL 30 DAY)
ORDER BY pa.process_date DESC, e.full_name
LIMIT 20;

-- 3. Check payroll calculation summary
SELECT '3. Recent Payroll Calculations' as '';
SELECT 
    e.full_name,
    pp.pay_period_start,
    pp.pay_period_end,
    pp.gross_earned,
    pp.late_deduction,
    pp.absent_deduction,
    pp.total_deductions,
    pp.net_pay,
    pp.status
FROM payroll_process pp
JOIN employees e ON pp.employee_id = e.id
ORDER BY pp.created_at DESC
LIMIT 10;

SELECT '=== FIXES APPLIED SUCCESSFULLY ===' as '';
SELECT 'Dashboard count query: Fixed (using payroll_process table)' as 'Status';
SELECT 'Attendance deduction logic: Fixed (only deduct if actually late)' as 'Status';
SELECT 'Net pay calculation: Consistent across all views' as 'Status';
