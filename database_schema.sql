-- TPC Payroll Management System - Simplified Database Schema
-- Created for User Management and Access Control

-- Create database (uncomment if needed)
-- CREATE DATABASE tpc_payroll_system;
-- USE tpc_payroll_system;

-- =============================================
-- USERS TABLE (Simplified - matches data table)
-- =============================================
CREATE TABLE users (
    user_id INT(11) AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    role VARCHAR(50) NOT NULL,
    status ENUM('Active', 'Inactive', 'Suspended') NOT NULL DEFAULT 'Active',
    last_login VARCHAR(50) NULL,
    created_date VARCHAR(50) NOT NULL,
    
    INDEX idx_username (username),
    INDEX idx_email (email),
    INDEX idx_role (role),
    INDEX idx_status (status)
);

-- =============================================
-- ROLES TABLE (Simplified - matches data table)
-- =============================================
CREATE TABLE roles (
    role_id INT(11) AUTO_INCREMENT PRIMARY KEY,
    role_name VARCHAR(50) NOT NULL UNIQUE,
    description TEXT NULL,
    user_count INT(11) NOT NULL DEFAULT 0,
    status ENUM('Active', 'Inactive') NOT NULL DEFAULT 'Active',
    created_date VARCHAR(50) NOT NULL,
    
    INDEX idx_role_name (role_name),
    INDEX idx_status (status)
);

-- =============================================
-- PERMISSIONS TABLE
-- =============================================
CREATE TABLE permissions (
    permission_id INT(11) AUTO_INCREMENT PRIMARY KEY,
    permission_name VARCHAR(100) NOT NULL UNIQUE,
    module_name VARCHAR(50) NOT NULL,
    action_name VARCHAR(50) NOT NULL,
    description TEXT NULL,
    status ENUM('Active', 'Inactive') NOT NULL DEFAULT 'Active',
    created_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    INDEX idx_permission_name (permission_name),
    INDEX idx_module_name (module_name),
    INDEX idx_action_name (action_name),
    INDEX idx_status (status)
);

-- =============================================
-- ROLE_PERMISSIONS TABLE (Many-to-Many)
-- =============================================
CREATE TABLE role_permissions (
    role_permission_id INT(11) AUTO_INCREMENT PRIMARY KEY,
    role_id INT(11) NOT NULL,
    permission_id INT(11) NOT NULL,
    granted BOOLEAN NOT NULL DEFAULT TRUE,
    created_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    UNIQUE KEY unique_role_permission (role_id, permission_id),
    INDEX idx_role_id (role_id),
    INDEX idx_permission_id (permission_id),
    INDEX idx_granted (granted),
    
    FOREIGN KEY (role_id) REFERENCES roles(role_id) ON DELETE CASCADE,
    FOREIGN KEY (permission_id) REFERENCES permissions(permission_id) ON DELETE CASCADE
);

-- =============================================
-- SECURITY EVENTS TABLE (for Security Monitoring)
-- =============================================
CREATE TABLE security_events (
    event_id INT(11) AUTO_INCREMENT PRIMARY KEY,
    timestamp DATETIME NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    severity VARCHAR(20) NOT NULL,
    username VARCHAR(100) NULL,
    description TEXT NOT NULL,
    ip_address VARCHAR(45) NULL,
    user_agent TEXT NULL,
    event_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    INDEX idx_timestamp (timestamp),
    INDEX idx_event_type (event_type),
    INDEX idx_severity (severity),
    INDEX idx_username (username),
    INDEX idx_event_status (event_status)
);

-- =============================================
-- INSERT DEFAULT DATA
-- =============================================

-- Insert default roles
INSERT INTO roles (role_name, description, user_count, created_date) VALUES
('Admin', 'Full system access with all permissions', 3, '2024-01-01'),
('Payroll Maker', 'Can process payroll and generate reports', 5, '2024-01-01'),
('Staff', 'Basic access to view employee data and reports', 8, '2024-01-01');

-- Insert default permissions
INSERT INTO permissions (permission_name, module_name, action_name, description, created_date) VALUES
-- Dashboard permissions
('dashboard.view', 'Dashboard', 'View', 'View dashboard overview', NOW()),
('dashboard.quick_actions', 'Dashboard', 'Quick Actions', 'Access quick action buttons', NOW()),

-- Employee Management permissions
('employee.view', 'Employee Management', 'View', 'View employee list and details', NOW()),
('employee.add', 'Employee Management', 'Add', 'Add new employee', NOW()),
('employee.edit', 'Employee Management', 'Edit', 'Edit employee information', NOW()),
('employee.delete', 'Employee Management', 'Delete', 'Delete employee record', NOW()),

-- Payroll Processing permissions
('payroll.view', 'Payroll Processing', 'View', 'View payroll processing', NOW()),
('payroll.generate', 'Payroll Processing', 'Generate', 'Generate payroll', NOW()),
('payroll.export', 'Payroll Processing', 'Export', 'Export payroll data', NOW()),

-- Payroll Generator permissions
('payroll_gen.view', 'Payroll Generator', 'View', 'View payroll generator', NOW()),
('payroll_gen.add', 'Payroll Generator', 'Add', 'Add payroll entry', NOW()),
('payroll_gen.edit', 'Payroll Generator', 'Edit', 'Edit payroll entry', NOW()),
('payroll_gen.delete', 'Payroll Generator', 'Delete', 'Delete payroll record', NOW()),

-- Reports permissions
('reports.view', 'Reports', 'View', 'View reports', NOW()),
('reports.generate', 'Reports', 'Generate', 'Generate reports', NOW()),
('reports.export', 'Reports', 'Export', 'Export reports', NOW()),

-- Import/Export permissions
('import_export.view', 'Import/Export', 'View', 'View import/export', NOW()),
('import_export.import', 'Import/Export', 'Import', 'Import data', NOW()),
('import_export.export', 'Import/Export', 'Export', 'Export data', NOW()),

-- History permissions
('history.view', 'History', 'View', 'View system history', NOW()),

-- Security Maintenance permissions
('security.view', 'Security Maintenance', 'View', 'View security settings', NOW()),
('security.edit', 'Security Maintenance', 'Edit', 'Edit security settings', NOW()),

-- Government Remittances permissions
('gov_remit.view', 'Government Remittances', 'View', 'View government remittances', NOW()),
('gov_remit.edit', 'Government Remittances', 'Edit', 'Edit government remittances', NOW()),

-- User Management permissions
('user_mgmt.view', 'User Management', 'View', 'View user management', NOW()),
('user_mgmt.add', 'User Management', 'Add', 'Add new user', NOW()),
('user_mgmt.edit', 'User Management', 'Edit', 'Edit user information', NOW()),
('user_mgmt.delete', 'User Management', 'Delete', 'Delete user account', NOW()),

-- User Access permissions
('user_access.view', 'User Access', 'View', 'View user access control', NOW()),
('user_access.edit', 'User Access', 'Edit', 'Edit user access control', NOW());

-- Insert default admin user (password: admin123)
INSERT INTO users (username, password_hash, full_name, email, role, status, last_login, created_date) VALUES
('admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'System Administrator', 'admin@tpc.edu.ph', 'Admin', 'Active', '2024-01-15 09:30', '2024-01-01');

-- Assign all permissions to Admin role
INSERT INTO role_permissions (role_id, permission_id, granted, created_date)
SELECT 1, permission_id, TRUE, NOW() FROM permissions;

-- Assign basic permissions to Payroll Maker role
INSERT INTO role_permissions (role_id, permission_id, granted, created_date)
SELECT 2, permission_id, TRUE, NOW() FROM permissions 
WHERE module_name IN ('Dashboard', 'Employee Management', 'Payroll Processing', 'Payroll Generator', 'Reports', 'Import/Export', 'History');

-- Assign view-only permissions to Staff role
INSERT INTO role_permissions (role_id, permission_id, granted, created_date)
SELECT 3, permission_id, TRUE, NOW() FROM permissions 
WHERE action_name = 'View' AND module_name IN ('Dashboard', 'Employee Management', 'Reports', 'History');

-- =============================================
-- EMPLOYEES TABLE (Main Employee Management)
-- =============================================
CREATE TABLE employees (
    id INT AUTO_INCREMENT PRIMARY KEY,
    account_number VARCHAR(50) UNIQUE NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    position VARCHAR(100),
    salary DECIMAL(10,2) DEFAULT 0.00,
    status ENUM('Active', 'Inactive', 'Terminated') NOT NULL DEFAULT 'Active',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_account_number (account_number),
    INDEX idx_full_name (full_name),
    INDEX idx_position (position),
    INDEX idx_status (status)
);

-- Insert sample employee data
INSERT INTO employees (account_number, full_name, position, salary, status) VALUES
('EMP-001', 'Juan Cruz', 'Teacher I', 25000.00, 'Active'),
('EMP-002', 'Maria Santos', 'Teacher II', 28000.00, 'Active'),
('EMP-003', 'Pedro Gonzales', 'Head Teacher', 35000.00, 'Active'),
('EMP-004', 'Ana Reyes', 'Teacher I', 25000.00, 'Active'),
('EMP-005', 'Carlos Mendoza', 'Principal', 50000.00, 'Active');

-- Insert sample security events
INSERT INTO security_events (timestamp, event_type, severity, username, description, ip_address, event_status) VALUES
(NOW() - INTERVAL 1 HOUR, 'LOGIN_SUCCESS', 'LOW', 'admin', 'Administrator logged in successfully', '192.168.1.100', 'ACTIVE'),
(NOW() - INTERVAL 2 HOUR, 'LOGIN_FAILED', 'MEDIUM', 'user1', 'Failed login attempt - incorrect password', '192.168.1.101', 'ACTIVE'),
(NOW() - INTERVAL 3 HOUR, 'PASSWORD_CHANGE', 'LOW', 'admin', 'Password changed successfully', '192.168.1.100', 'ACTIVE'),
(NOW() - INTERVAL 4 HOUR, 'LOGIN_FAILED', 'MEDIUM', 'user2', 'Failed login attempt - account not found', '192.168.1.102', 'ACTIVE'),
(NOW() - INTERVAL 5 HOUR, 'LOGIN_FAILED', 'HIGH', 'user2', 'Multiple failed login attempts detected', '192.168.1.102', 'ACTIVE'),
(NOW() - INTERVAL 6 HOUR, 'PASSWORD_RESET', 'MEDIUM', 'admin', 'Password reset for user: user1', '192.168.1.100', 'ACTIVE'),
(NOW() - INTERVAL 7 HOUR, 'LOGIN_SUCCESS', 'LOW', 'user1', 'User logged in successfully after password reset', '192.168.1.101', 'ACTIVE'),
(NOW() - INTERVAL 8 HOUR, 'INVALID_ACCESS', 'HIGH', 'guest', 'Attempt to access restricted area without authentication', '192.168.1.200', 'ACTIVE'),
(NOW() - INTERVAL 9 HOUR, 'USER_CREATED', 'LOW', 'admin', 'New user account created: user3', '192.168.1.100', 'ACTIVE'),
(NOW() - INTERVAL 10 HOUR, 'PASSWORD_POLICY_UPDATE', 'MEDIUM', 'admin', 'Password policy settings updated', '192.168.1.100', 'ACTIVE');

-- =============================================
-- USEFUL QUERIES FOR LOGIN AUTHENTICATION
-- =============================================

-- Query to authenticate user login
-- SELECT u.user_id, u.username, u.password_hash, u.full_name, u.email, u.role, u.status
-- FROM users u 
-- WHERE u.username = ? AND u.status = 'Active';

-- Query to get user permissions based on role
-- SELECT DISTINCT p.permission_name, p.module_name, p.action_name
-- FROM users u
-- JOIN roles r ON u.role = r.role_name
-- JOIN role_permissions rp ON r.role_id = rp.role_id
-- JOIN permissions p ON rp.permission_id = p.permission_id
-- WHERE u.user_id = ? AND r.status = 'Active' AND rp.granted = TRUE AND p.status = 'Active';

-- Query to update user last login
-- UPDATE users SET last_login = ? WHERE user_id = ?;

-- Query to check if user has specific permission
-- SELECT COUNT(*) as has_permission
-- FROM users u
-- JOIN roles r ON u.role = r.role_name
-- JOIN role_permissions rp ON r.role_id = rp.role_id
-- JOIN permissions p ON rp.permission_id = p.permission_id
-- WHERE u.user_id = ? AND p.permission_name = ? AND r.status = 'Active' AND rp.granted = TRUE AND p.status = 'Active';
