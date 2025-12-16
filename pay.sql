-- --------------------------------------------------------
-- Host:                         localhost
-- Server version:               8.0.41 - MySQL Community Server - GPL
-- Server OS:                    Win64
-- HeidiSQL Version:             12.8.0.6908
-- --------------------------------------------------------

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET NAMES utf8 */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;


-- Dumping database structure for payroll
CREATE DATABASE IF NOT EXISTS `payroll` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;
USE `payroll`;

-- Dumping structure for table payroll.permissions
DROP TABLE IF EXISTS `permissions`;
CREATE TABLE IF NOT EXISTS `permissions` (
  `permission_id` int NOT NULL AUTO_INCREMENT,
  `permission_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `module_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `action_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `status` enum('Active','Inactive') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'Active',
  `created_date` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`permission_id`),
  UNIQUE KEY `permission_name` (`permission_name`),
  KEY `idx_permission_name` (`permission_name`),
  KEY `idx_module_name` (`module_name`),
  KEY `idx_action_name` (`action_name`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=53 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Dumping data for table payroll.permissions: ~52 rows (approximately)
INSERT INTO `permissions` (`permission_id`, `permission_name`, `module_name`, `action_name`, `description`, `status`, `created_date`) VALUES
	(1, 'dashboard.view', 'Dashboard', 'View', 'View dashboard overview', 'Active', '2025-10-17 17:41:50'),
	(2, 'dashboard.quick_actions', 'Dashboard', 'Quick Actions', 'Access quick action buttons', 'Active', '2025-10-17 17:41:50'),
	(3, 'employee.view', 'Employee Management', 'View', 'View employee list and details', 'Active', '2025-10-17 17:41:50'),
	(4, 'employee.add', 'Employee Management', 'Add', 'Add new employee', 'Active', '2025-10-17 17:41:50'),
	(5, 'employee.edit', 'Employee Management', 'Edit', 'Edit employee information', 'Active', '2025-10-17 17:41:50'),
	(6, 'employee.delete', 'Employee Management', 'Delete', 'Delete employee record', 'Active', '2025-10-17 17:41:50'),
	(7, 'payroll.view', 'Payroll Processing', 'View', 'View payroll processing', 'Active', '2025-10-17 17:41:50'),
	(8, 'payroll.generate', 'Payroll Processing', 'Generate', 'Generate payroll', 'Active', '2025-10-17 17:41:50'),
	(9, 'payroll.export', 'Payroll Processing', 'Export', 'Export payroll data', 'Active', '2025-10-17 17:41:50'),
	(10, 'payroll_gen.view', 'Payroll Generator', 'View', 'View payroll generator', 'Active', '2025-10-17 17:41:50'),
	(11, 'payroll_gen.add', 'Payroll Generator', 'Add', 'Add payroll entry', 'Active', '2025-10-17 17:41:50'),
	(12, 'payroll_gen.edit', 'Payroll Generator', 'Edit', 'Edit payroll entry', 'Active', '2025-10-17 17:41:50'),
	(13, 'payroll_gen.delete', 'Payroll Generator', 'Delete', 'Delete payroll record', 'Active', '2025-10-17 17:41:50'),
	(14, 'reports.view', 'Reports', 'View', 'View reports', 'Active', '2025-10-17 17:41:50'),
	(15, 'reports.generate', 'Reports', 'Generate', 'Generate reports', 'Active', '2025-10-17 17:41:50'),
	(16, 'reports.export', 'Reports', 'Export', 'Export reports', 'Active', '2025-10-17 17:41:50'),
	(17, 'import_export.view', 'Attendance log', 'View', 'View import/export', 'Active', '2025-10-17 17:41:50'),
	(18, 'import_export.import', 'Attendance log', 'Import', 'Import data', 'Active', '2025-10-17 17:41:50'),
	(19, 'import_export.export', 'Attendance log', 'Export', 'Export data', 'Active', '2025-10-17 17:41:50'),
	(20, 'history.view', 'History', 'View', 'View system history', 'Active', '2025-10-17 17:41:50'),
	(21, 'security.view', 'Security Maintenance', 'View', 'View security settings', 'Active', '2025-10-17 17:41:50'),
	(22, 'security.edit', 'Security Maintenance', 'Edit', 'Edit security settings', 'Active', '2025-10-17 17:41:50'),
	(23, 'gov_remit.view', 'Dedcutions', 'View', 'View Dedcutions', 'Active', '2025-10-17 17:41:50'),
	(24, 'gov_remit.edit', 'Dedcutions', 'Edit', 'Edit Dedcutions', 'Active', '2025-10-17 17:41:50'),
	(25, 'user_mgmt.view', 'User Management', 'View', 'View user management', 'Active', '2025-10-17 17:41:50'),
	(26, 'user_mgmt.add', 'User Management', 'Add', 'Add new user', 'Active', '2025-10-17 17:41:50'),
	(27, 'user_mgmt.edit', 'User Management', 'Edit', 'Edit user information', 'Active', '2025-10-17 17:41:50'),
	(28, 'user_mgmt.delete', 'User Management', 'Delete', 'Delete user account', 'Active', '2025-10-17 17:41:50'),
	(29, 'user_access.view', 'User Access', 'View', 'View user access control', 'Active', '2025-10-17 17:41:50'),
	(30, 'user_access.edit', 'User Access', 'Edit', 'Edit user access control', 'Active', '2025-10-17 17:41:50'),
	(31, 'user_access.add', 'User Access', 'Add', 'Add new user access control', 'Active', '2025-10-17 20:00:00'),
	(32, 'user_access.delete', 'User Access', 'Delete', 'Delete user access conrol', 'Active', '2025-10-17 20:00:57'),
	(33, 'salary_ref.view', 'Salary Reference', 'View', 'View salary reference tables', 'Active', '2025-12-05 18:45:29'),
	(34, 'salary_ref.edit', 'Salary Reference', 'Edit', 'Edit salary reference data', 'Active', '2025-12-05 18:45:29'),
	(35, 'salary_ref.add', 'Salary Reference', 'Add', 'Add salary reference data', 'Active', '2025-12-05 18:45:29'),
	(36, 'salary_ref.delete', 'Salary Reference', 'Delete', 'Delete salary reference data', 'Active', '2025-12-05 18:45:29'),
	(37, 'attendance_process.view', 'Attendance Processing', 'View', 'View attendance processing', 'Active', '2025-12-05 18:45:29'),
	(38, 'attendance_process.edit', 'Attendance Processing', 'Edit', 'Process attendance data', 'Active', '2025-12-05 18:45:29'),
	(39, 'attendance_process.add', 'Attendance Processing', 'Add', 'Add attendance processing', 'Active', '2025-12-05 18:45:29'),
	(40, 'attendance_process.delete', 'Attendance Processing', 'Delete', 'Delete attendance processing', 'Active', '2025-12-05 18:45:29'),
	(41, 'loan_mgmt.view', 'Loan Management', 'View', 'View loan management', 'Active', '2025-12-05 18:45:29'),
	(42, 'loan_mgmt.add', 'Loan Management', 'Add', 'Add new loans', 'Active', '2025-12-05 18:45:29'),
	(43, 'loan_mgmt.edit', 'Loan Management', 'Edit', 'Edit loan information', 'Active', '2025-12-05 18:45:29'),
	(44, 'loan_mgmt.delete', 'Loan Management', 'Delete', 'Delete loan records', 'Active', '2025-12-05 18:45:29'),
	(45, 'gov_contrib.view', 'Government Contributions', 'View', 'View government contributions', 'Active', '2025-12-05 18:45:29'),
	(46, 'gov_contrib.edit', 'Government Contributions', 'Edit', 'Edit government contributions', 'Active', '2025-12-05 18:45:29'),
	(47, 'gov_contrib.add', 'Government Contributions', 'Add', 'Add government contributions', 'Active', '2025-12-05 18:45:29'),
	(48, 'gov_contrib.delete', 'Government Contributions', 'Delete', 'Delete government contributions', 'Active', '2025-12-05 18:45:29'),
	(49, 'system_settings.view', 'System Settings', 'View', 'View system settings', 'Active', '2025-12-05 18:45:29'),
	(50, 'system_settings.edit', 'System Settings', 'Edit', 'Edit system settings', 'Active', '2025-12-05 18:45:29'),
	(51, 'system_settings.add', 'System Settings', 'Add', 'Add system settings', 'Active', '2025-12-05 18:45:29'),
	(52, 'system_settings.delete', 'System Settings', 'Delete', 'Delete system settings', 'Active', '2025-12-05 18:45:29');

-- Dumping structure for table payroll.roles
DROP TABLE IF EXISTS `roles`;
CREATE TABLE IF NOT EXISTS `roles` (
  `role_id` int NOT NULL AUTO_INCREMENT,
  `role_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `user_count` int NOT NULL DEFAULT '0',
  `status` enum('Active','Inactive') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'Active',
  `created_date` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`role_id`),
  UNIQUE KEY `role_name` (`role_name`),
  KEY `idx_role_name` (`role_name`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Dumping data for table payroll.roles: ~3 rows (approximately)
INSERT INTO `roles` (`role_id`, `role_name`, `description`, `user_count`, `status`, `created_date`) VALUES
	(1, 'Admin', 'Full system access with all permissions', 3, 'Active', '2024-01-01'),
	(2, 'Payroll Maker', 'Can process payroll and generate reports', 5, 'Active', '2024-01-01'),
	(3, 'Staff', 'Basic access to view employee data and reports', 8, 'Active', '2024-01-01');

-- Dumping structure for table payroll.role_permissions
DROP TABLE IF EXISTS `role_permissions`;
CREATE TABLE IF NOT EXISTS `role_permissions` (
  `role_permission_id` int NOT NULL AUTO_INCREMENT,
  `role_id` int NOT NULL,
  `permission_id` int NOT NULL,
  `granted` tinyint(1) NOT NULL DEFAULT '1',
  `created_date` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`role_permission_id`),
  UNIQUE KEY `unique_role_permission` (`role_id`,`permission_id`),
  KEY `idx_role_id` (`role_id`),
  KEY `idx_permission_id` (`permission_id`),
  KEY `idx_granted` (`granted`),
  CONSTRAINT `role_permissions_ibfk_1` FOREIGN KEY (`role_id`) REFERENCES `roles` (`role_id`) ON DELETE CASCADE,
  CONSTRAINT `role_permissions_ibfk_2` FOREIGN KEY (`permission_id`) REFERENCES `permissions` (`permission_id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=452 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Dumping data for table payroll.role_permissions: ~74 rows (approximately)
INSERT INTO `role_permissions` (`role_permission_id`, `role_id`, `permission_id`, `granted`, `created_date`) VALUES
	(32, 2, 1, 1, '2025-10-17 17:41:50'),
	(33, 2, 2, 1, '2025-10-17 17:41:50'),
	(34, 2, 3, 1, '2025-10-17 17:41:50'),
	(35, 2, 4, 1, '2025-10-17 17:41:50'),
	(36, 2, 5, 1, '2025-10-17 17:41:50'),
	(37, 2, 6, 1, '2025-10-17 17:41:50'),
	(38, 2, 20, 1, '2025-10-17 17:41:50'),
	(39, 2, 17, 1, '2025-10-17 17:41:50'),
	(40, 2, 18, 1, '2025-10-17 17:41:50'),
	(41, 2, 19, 1, '2025-10-17 17:41:50'),
	(42, 2, 10, 1, '2025-10-17 17:41:50'),
	(43, 2, 11, 1, '2025-10-17 17:41:50'),
	(44, 2, 12, 1, '2025-10-17 17:41:50'),
	(45, 2, 13, 1, '2025-10-17 17:41:50'),
	(46, 2, 7, 1, '2025-10-17 17:41:50'),
	(47, 2, 8, 1, '2025-10-17 17:41:50'),
	(48, 2, 9, 1, '2025-10-17 17:41:50'),
	(49, 2, 14, 1, '2025-10-17 17:41:50'),
	(50, 2, 15, 1, '2025-10-17 17:41:50'),
	(51, 2, 16, 1, '2025-10-17 17:41:50'),
	(263, 3, 1, 1, '2025-11-16 11:58:30'),
	(264, 3, 20, 1, '2025-11-16 11:58:30'),
	(409, 1, 19, 1, '2025-12-16 12:16:31'),
	(410, 1, 18, 1, '2025-12-16 12:16:31'),
	(411, 1, 17, 1, '2025-12-16 12:16:31'),
	(412, 1, 39, 1, '2025-12-16 12:16:31'),
	(413, 1, 40, 1, '2025-12-16 12:16:31'),
	(414, 1, 38, 1, '2025-12-16 12:16:31'),
	(415, 1, 37, 1, '2025-12-16 12:16:31'),
	(416, 1, 2, 1, '2025-12-16 12:16:31'),
	(417, 1, 1, 1, '2025-12-16 12:16:31'),
	(418, 1, 24, 1, '2025-12-16 12:16:31'),
	(419, 1, 23, 1, '2025-12-16 12:16:31'),
	(420, 1, 4, 1, '2025-12-16 12:16:31'),
	(421, 1, 6, 1, '2025-12-16 12:16:31'),
	(422, 1, 5, 1, '2025-12-16 12:16:31'),
	(423, 1, 3, 1, '2025-12-16 12:16:31'),
	(424, 1, 47, 1, '2025-12-16 12:16:31'),
	(425, 1, 48, 1, '2025-12-16 12:16:31'),
	(426, 1, 46, 1, '2025-12-16 12:16:31'),
	(427, 1, 45, 1, '2025-12-16 12:16:31'),
	(428, 1, 42, 1, '2025-12-16 12:16:31'),
	(429, 1, 44, 1, '2025-12-16 12:16:31'),
	(430, 1, 43, 1, '2025-12-16 12:16:31'),
	(431, 1, 41, 1, '2025-12-16 12:16:31'),
	(432, 1, 9, 1, '2025-12-16 12:16:31'),
	(433, 1, 8, 1, '2025-12-16 12:16:31'),
	(434, 1, 7, 1, '2025-12-16 12:16:31'),
	(435, 1, 16, 1, '2025-12-16 12:16:31'),
	(436, 1, 15, 1, '2025-12-16 12:16:31'),
	(437, 1, 14, 1, '2025-12-16 12:16:31'),
	(438, 1, 35, 1, '2025-12-16 12:16:31'),
	(439, 1, 36, 1, '2025-12-16 12:16:31'),
	(440, 1, 34, 1, '2025-12-16 12:16:31'),
	(441, 1, 33, 1, '2025-12-16 12:16:31'),
	(442, 1, 22, 1, '2025-12-16 12:16:31'),
	(443, 1, 21, 1, '2025-12-16 12:16:31'),
	(444, 1, 31, 1, '2025-12-16 12:16:31'),
	(445, 1, 32, 1, '2025-12-16 12:16:31'),
	(446, 1, 30, 1, '2025-12-16 12:16:31'),
	(447, 1, 29, 1, '2025-12-16 12:16:31'),
	(448, 1, 26, 1, '2025-12-16 12:16:31'),
	(449, 1, 28, 1, '2025-12-16 12:16:31'),
	(450, 1, 27, 1, '2025-12-16 12:16:31'),
	(451, 1, 25, 1, '2025-12-16 12:16:31');

/*!40103 SET TIME_ZONE=IFNULL(@OLD_TIME_ZONE, 'system') */;
/*!40101 SET SQL_MODE=IFNULL(@OLD_SQL_MODE, '') */;
/*!40014 SET FOREIGN_KEY_CHECKS=IFNULL(@OLD_FOREIGN_KEY_CHECKS, 1) */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40111 SET SQL_NOTES=IFNULL(@OLD_SQL_NOTES, 1) */;
