-- =====================================================
-- LGU PAYROLL SYSTEM - DATABASE MIGRATION
-- Based on exact payroll sheet specifications
-- =====================================================

USE payroll;

-- =====================================================
-- 1. CREATE payroll_periods TABLE
-- =====================================================
CREATE TABLE IF NOT EXISTS `payroll_periods` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `period_start` DATE NOT NULL,
  `period_end` DATE NOT NULL,
  `description` VARCHAR(100) NULL,   
  `sheet_type` ENUM('INSTRUCTOR','STAFF_NON_TEACHING') NOT NULL,
  `status` ENUM('Open','Locked','Cancelled') NOT NULL DEFAULT 'Open',
  `created_at` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_period` (`period_start`, `period_end`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- 2. CREATE payroll_sheet_items TABLE (MAIN TABLE)
-- =====================================================
CREATE TABLE IF NOT EXISTS `payroll_sheet_items` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `payroll_period_id` INT NOT NULL,
  `employee_id` INT NOT NULL,
  `sheet_type` ENUM('INSTRUCTOR','STAFF_NON_TEACHING') NOT NULL,

  -- EXACT COLUMNS FROM PAYROLL SHEET
  `monthly_salary` DECIMAL(10,2) NOT NULL COMMENT 'Salaries & Wages Regular',
  `gross_regular` DECIMAL(10,2) NOT NULL COMMENT 'Gross Amount (half month)',

  `units` INT NULL COMMENT 'No. of Units (Instructor)',
  `rate_per_unit` DECIMAL(10,2) NULL COMMENT 'monthly_salary/24',
  `overload_amount` DECIMAL(10,2) NULL COMMENT 'units * rate_per_unit',

  `gross_total` DECIMAL(10,2) NOT NULL COMMENT 'gross_regular + overload',

  `absent_days` DECIMAL(5,2) DEFAULT 0.00 COMMENT '1, 0.5 (half day)',
  `tardy_minutes` INT DEFAULT 0,
  `tardy_amount` DECIMAL(10,2) DEFAULT 0.00,

  `gross_earned` DECIMAL(10,2) NOT NULL,

  `pagibig_premium` DECIMAL(10,2) DEFAULT 0.00 COMMENT 'Fixed 200.00',
  `pagibig_mpl` DECIMAL(10,2) DEFAULT 0.00 COMMENT 'MPL Loan',
  `calamity_loan` DECIMAL(10,2) DEFAULT 0.00 COMMENT 'Calamity Loan',
  `expanded_tax` DECIMAL(10,2) DEFAULT 0.00 COMMENT '5% of gross_earned',
  `gvat` DECIMAL(10,2) DEFAULT 0.00 COMMENT '3% of gross_earned',

  `total_deductions` DECIMAL(10,2) DEFAULT 0.00,

  `net_amount_due` DECIMAL(10,2) NOT NULL,

  `remarks` VARCHAR(255) NULL,
  `created_at` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  PRIMARY KEY (`id`),
  KEY `idx_period` (`payroll_period_id`),
  KEY `idx_employee` (`employee_id`),
  UNIQUE KEY `unique_employee_period` (`employee_id`, `payroll_period_id`),

  FOREIGN KEY (`payroll_period_id`)
    REFERENCES `payroll_periods`(`id`)
    ON DELETE CASCADE,

  FOREIGN KEY (`employee_id`)
    REFERENCES `employees`(`id`)
    ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- 3. UPDATE salary_reference TABLE
-- =====================================================
-- Add rate_per_unit column if it doesn't exist
ALTER TABLE `salary_reference`
  ADD COLUMN IF NOT EXISTS `rate_per_unit` DECIMAL(10,2) NULL COMMENT 'For instructors: monthly_salary / 24' AFTER `rate_per_minute`;

-- Auto-fill rate_per_unit for existing records
UPDATE `salary_reference`
SET `rate_per_unit` = `monthly_salary` / 24
WHERE `rate_per_unit` IS NULL;

-- =====================================================
-- 4. UPDATE employees TABLE
-- =====================================================
-- Add employment_type column if it doesn't exist
ALTER TABLE `employees`
  ADD COLUMN IF NOT EXISTS `employment_type` ENUM('INSTRUCTOR','TEMPORARY_INSTRUCTOR','STAFF_NON_TEACHING')
    NULL COMMENT 'Type of employment for payroll calculation' AFTER `position`;

-- Add assigned_units column if it doesn't exist
ALTER TABLE `employees`
  ADD COLUMN IF NOT EXISTS `assigned_units` INT NULL COMMENT 'Number of units assigned (for instructors)' AFTER `employment_type`;

-- =====================================================
-- 5. UPDATE loan_types to include MPL and Calamity Loan types
-- =====================================================
-- Ensure MPL Loan type exists
INSERT IGNORE INTO `loan_types` (`name`) VALUES ('MPL Loan');
INSERT IGNORE INTO `loan_types` (`name`) VALUES ('Calamity Loan');

-- =====================================================
-- END OF MIGRATION
-- =====================================================

