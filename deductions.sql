-- ==================================================
-- DEDUCTIONS TABLE CREATION SCRIPT
-- ==================================================
-- This table stores government deduction types and configurations
-- for payroll processing (SSS, PhilHealth, PAG-IBIG, Tax, etc.)

CREATE TABLE IF NOT EXISTS deductions (
    id INT AUTO_INCREMENT PRIMARY KEY COMMENT 'Primary key, auto-incremented',
    
    -- Basic Information (Required Fields)
    code VARCHAR(20) NOT NULL COMMENT 'Deduction code (e.g., SSS, PHILHEALTH, PAGIBIG, TAX)',
    name VARCHAR(100) NOT NULL COMMENT 'Deduction name/title',
    description TEXT NULL COMMENT 'Detailed description of the deduction',
    
    -- Calculation Basis (Required Field)
    basis ENUM('salary', 'range', 'fixed') NOT NULL DEFAULT 'salary' COMMENT 'Calculation basis for the deduction',
    
    -- Rate and Amount Configuration (Nullable Fields)
    rate_percent DECIMAL(5,2) NULL COMMENT 'Percentage rate for calculation (e.g., 15.00 for 15%)',
    fixed_amount DECIMAL(10,2) NULL COMMENT 'Fixed deduction amount (if applicable)',
    
    -- Salary Range Configuration (Nullable Fields)
    min_salary DECIMAL(12,2) NULL DEFAULT 0.00 COMMENT 'Minimum salary threshold',
    max_salary DECIMAL(12,2) NULL DEFAULT 0.00 COMMENT 'Maximum salary threshold',
    
    -- Share Distribution (Nullable Fields)
    employee_share DECIMAL(5,2) NULL DEFAULT 0.00 COMMENT 'Employee contribution percentage',
    employer_share DECIMAL(5,2) NULL DEFAULT 0.00 COMMENT 'Employer contribution percentage',
    
    -- Tax Specific Fields (Nullable Fields)
    base_tax DECIMAL(10,2) NULL COMMENT 'Base tax amount for progressive tax calculation',
    excess_over DECIMAL(10,2) NULL COMMENT 'Excess over amount for progressive tax calculation',
    
    -- Effective Rate Calculations (Nullable Fields)
    effective_employee_percent DECIMAL(5,2) NULL DEFAULT 0.00 COMMENT 'Calculated effective employee percentage',
    effective_employer_percent DECIMAL(5,2) NULL DEFAULT 0.00 COMMENT 'Calculated effective employer percentage',
    effective_total_percent DECIMAL(5,2) NULL DEFAULT 0.00 COMMENT 'Total effective percentage (employee + employer)',
    
    -- Status and Metadata
    is_active BOOLEAN NOT NULL DEFAULT TRUE COMMENT 'Whether this deduction type is active',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT 'Record creation timestamp',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Record last update timestamp',
    created_by VARCHAR(50) NULL COMMENT 'User who created this record',
    updated_by VARCHAR(50) NULL COMMENT 'User who last updated this record',
    
    -- Indexes for better performance
    INDEX idx_code (code),
    INDEX idx_basis (basis),
    INDEX idx_active (is_active),
    INDEX idx_created_at (created_at),
    
    -- Unique constraint
    UNIQUE KEY unique_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci 
COMMENT='Government deduction types and configurations for payroll processing';

-- ==================================================
-- SAMPLE DATA INSERTION
-- ==================================================

INSERT INTO deductions (
    code, name, description, basis, rate_percent, fixed_amount,
    min_salary, max_salary, employee_share, employer_share,
    base_tax, excess_over, effective_employee_percent, 
    effective_employer_percent, effective_total_percent,
    is_active, created_by
) VALUES 

-- SSS (Social Security System)
('SSS', 'Social Security System', 'Social security contribution for employees', 
 'salary', 15.0, NULL, 5000.0, 35000.0, 5.0, 10.0, 
 NULL, NULL, 5.0, 10.0, 15.0, TRUE, 'system'),

-- PhilHealth
('PHILHEALTH', 'PhilHealth Insurance', 'Health insurance contribution', 
 'salary', 5.0, NULL, 10000.0, 100000.0, 2.5, 2.5, 
 NULL, NULL, 2.5, 2.5, 5.0, TRUE, 'system'),

-- PAG-IBIG
('PAGIBIG', 'Pag-IBIG Fund', 'Home Development Mutual Fund contribution', 
 'salary', 2.0, NULL, 0.0, 10000.0, 1.0, 1.0, 
 NULL, NULL, 1.0, 1.0, 2.0, TRUE, 'system'),

-- Income Tax - Bracket 1 (0% rate)
('TAX_BRACKET_1', 'Income Tax Bracket 1', 'Income tax for salary 0 - 20,833 (TRAIN Law 2025)', 
 'range', 0.0, 0.0, 0.0, 20833.0, 0.0, 0.0, 
 0.0, 0.0, 0.0, 0.0, 0.0, TRUE, 'system'),

-- Income Tax - Bracket 2 (15% rate)
('TAX_BRACKET_2', 'Income Tax Bracket 2', 'Income tax for salary 20,833.01 - 33,332 (TRAIN Law 2025)', 
 'range', 15.0, NULL, 20833.01, 33332.0, 15.0, 0.0, 
 0.0, 20833.0, 15.0, 0.0, 15.0, TRUE, 'system'),

-- Income Tax - Bracket 3 (20% rate)
('TAX_BRACKET_3', 'Income Tax Bracket 3', 'Income tax for salary 33,332.01 - 66,667 (TRAIN Law 2025)', 
 'range', 20.0, NULL, 33332.01, 66667.0, 20.0, 0.0, 
 2500.0, 33332.0, 20.0, 0.0, 20.0, TRUE, 'system'),

-- Income Tax - Bracket 4 (25% rate)
('TAX_BRACKET_4', 'Income Tax Bracket 4', 'Income tax for salary 66,667.01 - 166,667 (TRAIN Law 2025)', 
 'range', 25.0, NULL, 66667.01, 166667.0, 25.0, 0.0, 
 10166.67, 66667.0, 25.0, 0.0, 25.0, TRUE, 'system'),

-- Income Tax - Bracket 5 (30% rate)
('TAX_BRACKET_5', 'Income Tax Bracket 5', 'Income tax for salary 166,667.01 - 666,667 (TRAIN Law 2025)', 
 'range', 30.0, NULL, 166667.01, 666667.0, 30.0, 0.0, 
 35166.67, 166667.0, 30.0, 0.0, 30.0, TRUE, 'system'),

-- Income Tax - Bracket 6 (35% rate)
('TAX_BRACKET_6', 'Income Tax Bracket 6', 'Income tax for salary above 666,667 (TRAIN Law 2025)', 
 'range', 35.0, NULL, 666667.01, 999999999.0, 35.0, 0.0, 
 185166.67, 666667.0, 35.0, 0.0, 35.0, TRUE, 'system'),

-- Late Deduction (Fixed Amount Example)
('LATE', 'Late Deduction', 'Deduction for tardiness', 
 'fixed', NULL, 50.0, NULL, NULL, NULL, NULL, 
 NULL, NULL, NULL, NULL, NULL, TRUE, 'system');

-- ==================================================
-- VERIFICATION QUERIES
-- ==================================================

-- Query to verify the table structure
-- DESCRIBE deductions;

-- Query to view all deduction types
-- SELECT * FROM deductions ORDER BY code;

-- Query to view only active deductions
-- SELECT code, name, description, basis, rate_percent, fixed_amount 
-- FROM deductions 
-- WHERE is_active = TRUE 
-- ORDER BY code;

-- ==================================================
-- SAMPLE UPDATE AND DELETE OPERATIONS
-- ==================================================

-- Example: Update a deduction rate
-- UPDATE deductions 
-- SET rate_percent = 16.0, 
--     updated_by = 'admin', 
--     updated_at = CURRENT_TIMESTAMP 
-- WHERE code = 'SSS';

-- Example: Deactivate a deduction instead of deleting
-- UPDATE deductions 
-- SET is_active = FALSE, 
--     updated_by = 'admin', 
--     updated_at = CURRENT_TIMESTAMP 
-- WHERE code = 'OLD_DEDUCTION';

-- Example: Add a new custom deduction
-- INSERT INTO deductions (code, name, description, basis, fixed_amount, created_by)
-- VALUES ('MEAL', 'Meal Allowance Deduction', 'Monthly meal allowance deduction', 'fixed', 300.0, 'admin');