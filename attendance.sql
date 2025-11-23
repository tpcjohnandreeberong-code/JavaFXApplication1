-- ==================================================
-- ATTENDANCE TABLE CREATION SCRIPT
-- ==================================================
-- This table stores employee attendance logs from biometric devices

CREATE TABLE IF NOT EXISTS attendance (
    id INT AUTO_INCREMENT PRIMARY KEY COMMENT 'Primary key, auto-incremented',
    
    -- Employee Information
    account_number VARCHAR(20) NOT NULL COMMENT 'Employee account number from employees table',
    
    -- Attendance Information
    log_datetime DATETIME NOT NULL COMMENT 'Date and time of the attendance log',
    log_type ENUM('TIME_IN_AM', 'TIME_OUT_AM', 'TIME_IN_PM', 'TIME_OUT_PM') NOT NULL COMMENT 'Type of attendance log',
    
    -- Raw biometric data (for reference)
    raw_data VARCHAR(100) NULL COMMENT 'Raw biometric log data',
    
    -- Status and metadata
    is_processed BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'Whether this log has been processed for payroll',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT 'Record creation timestamp',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Record last update timestamp',
    imported_by VARCHAR(50) NULL COMMENT 'User who imported this record',
    import_batch VARCHAR(100) NULL COMMENT 'Import batch identifier',
    
    -- Indexes for better performance
    INDEX idx_account_number (account_number),
    INDEX idx_log_datetime (log_datetime),
    INDEX idx_log_type (log_type),
    INDEX idx_processed (is_processed),
    INDEX idx_import_batch (import_batch),
    INDEX idx_account_date (account_number, log_datetime),
    
    -- Foreign key constraint (if employees table exists)
    -- FOREIGN KEY (account_number) REFERENCES employees(account_number) ON DELETE CASCADE
    
    UNIQUE KEY unique_attendance_log (account_number, log_datetime, log_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci 
COMMENT='Employee attendance logs from biometric devices';

-- ==================================================
-- SAMPLE DATA FOR TESTING
-- ==================================================

INSERT INTO attendance (
    account_number, log_datetime, log_type, raw_data, imported_by, import_batch
) VALUES 
-- August 1, 2025 logs
('124', '2025-08-01 07:56:26', 'TIME_IN_AM', '124	2025-08-01 07:56:26	1	0	1	0', 'system', 'TPC-AUGUST-1-312025_attlog.txt'),
('203', '2025-08-01 07:56:29', 'TIME_IN_AM', '203	2025-08-01 07:56:29	1	0	1	0', 'system', 'TPC-AUGUST-1-312025_attlog.txt'),
('124', '2025-08-01 12:00:06', 'TIME_OUT_AM', '124	2025-08-01 12:00:06	1	1	1	0', 'system', 'TPC-AUGUST-1-312025_attlog.txt'),
('6', '2025-08-01 12:00:07', 'TIME_OUT_AM', '6	2025-08-01 12:00:07	1	1	1	0', 'system', 'TPC-AUGUST-1-312025_attlog.txt'),
('124', '2025-08-01 12:10:05', 'TIME_IN_PM', '124	2025-08-01 12:10:05	1	0	1	0', 'system', 'TPC-AUGUST-1-312025_attlog.txt'),
('130', '2025-08-01 12:10:06', 'TIME_IN_PM', '130	2025-08-01 12:10:06	1	0	1	0', 'system', 'TPC-AUGUST-1-312025_attlog.txt'),
('124', '2025-08-01 17:00:02', 'TIME_OUT_PM', '124	2025-08-01 17:00:02	1	1	1	0', 'system', 'TPC-AUGUST-1-312025_attlog.txt'),
('149', '2025-08-01 17:00:04', 'TIME_OUT_PM', '149	2025-08-01 17:00:04	1	1	1	0', 'system', 'TPC-AUGUST-1-312025_attlog.txt'),
('130', '2025-08-01 17:00:10', 'TIME_OUT_PM', '130	2025-08-01 17:00:10	1	1	1	0', 'system', 'TPC-AUGUST-1-312025_attlog.txt'),

-- August 4, 2025 logs
('124', '2025-08-04 08:14:55', 'TIME_IN_AM', '124	2025-08-04 08:14:55	1	0	1	0', 'system', 'TPC-AUGUST-1-312025_attlog.txt'),
('230', '2025-08-04 08:20:39', 'TIME_IN_AM', '230	2025-08-04 08:20:39	1	0	1	0', 'system', 'TPC-AUGUST-1-312025_attlog.txt'),
('1', '2025-08-04 12:00:03', 'TIME_OUT_AM', '1	2025-08-04 12:00:03	1	1	1	0', 'system', 'TPC-AUGUST-1-312025_attlog.txt'),
('124', '2025-08-04 12:00:26', 'TIME_OUT_AM', '124	2025-08-04 12:00:26	1	1	1	0', 'system', 'TPC-AUGUST-1-312025_attlog.txt'),
('226', '2025-08-04 12:00:29', 'TIME_OUT_AM', '226	2025-08-04 12:00:29	1	1	1	0', 'system', 'TPC-AUGUST-1-312025_attlog.txt'),
('6', '2025-08-04 12:00:30', 'TIME_OUT_AM', '6	2025-08-04 12:00:30	1	1	1	0', 'system', 'TPC-AUGUST-1-312025_attlog.txt'),
('124', '2025-08-04 12:10:08', 'TIME_IN_PM', '124	2025-08-04 12:10:08	1	0	1	0', 'system', 'TPC-AUGUST-1-312025_attlog.txt'),
('156', '2025-08-04 12:10:11', 'TIME_IN_PM', '156	2025-08-04 12:10:11	1	0	1	0', 'system', 'TPC-AUGUST-1-312025_attlog.txt'),
('50', '2025-08-04 12:10:15', 'TIME_IN_PM', '50	2025-08-04 12:10:15	1	0	1	0', 'system', 'TPC-AUGUST-1-312025_attlog.txt'),
('124', '2025-08-04 17:00:28', 'TIME_OUT_PM', '124	2025-08-04 17:00:28	1	1	1	0', 'system', 'TPC-AUGUST-1-312025_attlog.txt'),
('156', '2025-08-04 17:00:30', 'TIME_OUT_PM', '156	2025-08-04 17:00:30	1	1	1	0', 'system', 'TPC-AUGUST-1-312025_attlog.txt'),
('20', '2025-08-04 17:00:37', 'TIME_OUT_PM', '20	2025-08-04 17:00:37	1	1	1	0', 'system', 'TPC-AUGUST-1-312025_attlog.txt');

-- ==================================================
-- USEFUL QUERIES FOR ATTENDANCE MANAGEMENT
-- ==================================================

-- Query to view all attendance for a specific employee
-- SELECT * FROM attendance WHERE account_number = '124' ORDER BY log_datetime;

-- Query to view attendance for a specific date
-- SELECT a.*, e.name as employee_name 
-- FROM attendance a 
-- LEFT JOIN employees e ON a.account_number = e.account_number 
-- WHERE DATE(a.log_datetime) = '2025-08-01' 
-- ORDER BY a.log_datetime;

-- Query to view attendance summary by employee
-- SELECT 
--     account_number,
--     DATE(log_datetime) as attendance_date,
--     COUNT(CASE WHEN log_type = 'TIME_IN_AM' THEN 1 END) as morning_in,
--     COUNT(CASE WHEN log_type = 'TIME_OUT_AM' THEN 1 END) as morning_out,
--     COUNT(CASE WHEN log_type = 'TIME_IN_PM' THEN 1 END) as afternoon_in,
--     COUNT(CASE WHEN log_type = 'TIME_OUT_PM' THEN 1 END) as afternoon_out
-- FROM attendance 
-- GROUP BY account_number, DATE(log_datetime)
-- ORDER BY attendance_date DESC, account_number;

-- Query to find duplicate entries
-- SELECT account_number, log_datetime, log_type, COUNT(*) as duplicate_count
-- FROM attendance 
-- GROUP BY account_number, log_datetime, log_type
-- HAVING COUNT(*) > 1;

-- Query to export attendance data in the original format
-- SELECT 
--     CONCAT(
--         LPAD(account_number, 3, ' '), '\t',
--         DATE_FORMAT(log_datetime, '%Y-%m-%d %H:%i:%s'), '\t',
--         CASE 
--             WHEN log_type IN ('TIME_IN_AM', 'TIME_IN_PM') THEN '1\t0\t1\t0'
--             ELSE '1\t1\t1\t0'
--         END
--     ) as export_line
-- FROM attendance 
-- ORDER BY log_datetime;