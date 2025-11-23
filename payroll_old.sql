-- --------------------------------------------------------
-- Host:                         127.0.0.1
-- Server version:               8.0.43 - MySQL Community Server - GPL
-- Server OS:                    Win64
-- HeidiSQL Version:             12.11.0.7065
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
DROP DATABASE IF EXISTS `payroll`;
CREATE DATABASE IF NOT EXISTS `payroll` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;
USE `payroll`;

-- Dumping structure for table payroll.permissions
DROP TABLE IF EXISTS `permissions`;
CREATE TABLE IF NOT EXISTS `permissions` (
  `permission_id` int NOT NULL AUTO_INCREMENT,
  `permission_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `module_name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `action_name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` text COLLATE utf8mb4_unicode_ci,
  `status` enum('Active','Inactive') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'Active',
  `created_date` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`permission_id`),
  UNIQUE KEY `permission_name` (`permission_name`),
  KEY `idx_permission_name` (`permission_name`),
  KEY `idx_module_name` (`module_name`),
  KEY `idx_action_name` (`action_name`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=33 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Dumping data for table payroll.permissions: ~30 rows (approximately)
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
	(17, 'import_export.view', 'Import/Export', 'View', 'View import/export', 'Active', '2025-10-17 17:41:50'),
	(18, 'import_export.import', 'Import/Export', 'Import', 'Import data', 'Active', '2025-10-17 17:41:50'),
	(19, 'import_export.export', 'Import/Export', 'Export', 'Export data', 'Active', '2025-10-17 17:41:50'),
	(20, 'history.view', 'History', 'View', 'View system history', 'Active', '2025-10-17 17:41:50'),
	(21, 'security.view', 'Security Maintenance', 'View', 'View security settings', 'Active', '2025-10-17 17:41:50'),
	(22, 'security.edit', 'Security Maintenance', 'Edit', 'Edit security settings', 'Active', '2025-10-17 17:41:50'),
	(23, 'gov_remit.view', 'Government Remittances', 'View', 'View government remittances', 'Active', '2025-10-17 17:41:50'),
	(24, 'gov_remit.edit', 'Government Remittances', 'Edit', 'Edit government remittances', 'Active', '2025-10-17 17:41:50'),
	(25, 'user_mgmt.view', 'User Management', 'View', 'View user management', 'Active', '2025-10-17 17:41:50'),
	(26, 'user_mgmt.add', 'User Management', 'Add', 'Add new user', 'Active', '2025-10-17 17:41:50'),
	(27, 'user_mgmt.edit', 'User Management', 'Edit', 'Edit user information', 'Active', '2025-10-17 17:41:50'),
	(28, 'user_mgmt.delete', 'User Management', 'Delete', 'Delete user account', 'Active', '2025-10-17 17:41:50'),
	(29, 'user_access.view', 'User Access', 'View', 'View user access control', 'Active', '2025-10-17 17:41:50'),
	(30, 'user_access.edit', 'User Access', 'Edit', 'Edit user access control', 'Active', '2025-10-17 17:41:50'),
	(31, 'user_access.add', 'User Access', 'Add', 'Add new user access control', 'Active', '2025-10-17 20:00:00'),
	(32, 'user_access.delete', 'User Access', 'Delete', 'Delete user access conrol', 'Active', '2025-10-17 20:00:57');

-- Dumping structure for table payroll.roles
DROP TABLE IF EXISTS `roles`;
CREATE TABLE IF NOT EXISTS `roles` (
  `role_id` int NOT NULL AUTO_INCREMENT,
  `role_name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` text COLLATE utf8mb4_unicode_ci,
  `user_count` int NOT NULL DEFAULT '0',
  `status` enum('Active','Inactive') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'Active',
  `created_date` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`role_id`),
  UNIQUE KEY `role_name` (`role_name`),
  KEY `idx_role_name` (`role_name`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Dumping data for table payroll.roles: ~5 rows (approximately)
INSERT INTO `roles` (`role_id`, `role_name`, `description`, `user_count`, `status`, `created_date`) VALUES
	(1, 'Admin', 'Full system access with all permissions', 3, 'Active', '2024-01-01'),
	(2, 'Payroll Maker', 'Can process payroll and generate reports', 5, 'Active', '2024-01-01'),
	(3, 'Staff', 'Basic access to view employee data and reports', 8, 'Active', '2024-01-01'),
	(4, 'Janitor', 'Sample tes', 0, 'Active', '2025-10-17'),
	(5, 'sample', 'asas', 0, 'Active', '2025-10-17');

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
) ENGINE=InnoDB AUTO_INCREMENT=265 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Dumping data for table payroll.role_permissions: ~56 rows (approximately)
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
	(103, 1, 1, 1, '2025-10-17 20:14:53'),
	(104, 1, 2, 1, '2025-10-17 20:14:53'),
	(105, 1, 3, 1, '2025-10-17 20:14:53'),
	(106, 1, 4, 1, '2025-10-17 20:14:53'),
	(107, 1, 5, 1, '2025-10-17 20:14:53'),
	(108, 1, 6, 1, '2025-10-17 20:14:53'),
	(109, 1, 7, 1, '2025-10-17 20:14:53'),
	(110, 1, 8, 1, '2025-10-17 20:14:53'),
	(111, 1, 9, 1, '2025-10-17 20:14:53'),
	(112, 1, 10, 1, '2025-10-17 20:14:53'),
	(113, 1, 11, 1, '2025-10-17 20:14:53'),
	(114, 1, 12, 1, '2025-10-17 20:14:53'),
	(115, 1, 13, 1, '2025-10-17 20:14:53'),
	(116, 1, 14, 1, '2025-10-17 20:14:53'),
	(117, 1, 15, 1, '2025-10-17 20:14:53'),
	(118, 1, 16, 1, '2025-10-17 20:14:53'),
	(119, 1, 17, 1, '2025-10-17 20:14:53'),
	(120, 1, 18, 1, '2025-10-17 20:14:53'),
	(121, 1, 19, 1, '2025-10-17 20:14:53'),
	(122, 1, 20, 1, '2025-10-17 20:14:53'),
	(123, 1, 21, 1, '2025-10-17 20:14:53'),
	(124, 1, 22, 1, '2025-10-17 20:14:53'),
	(125, 1, 23, 1, '2025-10-17 20:14:53'),
	(126, 1, 24, 1, '2025-10-17 20:14:53'),
	(127, 1, 25, 1, '2025-10-17 20:14:53'),
	(128, 1, 26, 1, '2025-10-17 20:14:53'),
	(129, 1, 27, 1, '2025-10-17 20:14:53'),
	(130, 1, 28, 1, '2025-10-17 20:14:53'),
	(131, 1, 29, 1, '2025-10-17 20:14:53'),
	(132, 1, 30, 1, '2025-10-17 20:14:53'),
	(133, 1, 31, 1, '2025-10-17 20:14:53'),
	(134, 1, 32, 1, '2025-10-17 20:14:53'),
	(234, 4, 5, 1, '2025-10-17 20:28:04'),
	(235, 4, 25, 1, '2025-10-17 20:28:04'),
	(263, 3, 1, 1, '2025-11-16 11:58:30'),
	(264, 3, 20, 1, '2025-11-16 11:58:30');

-- Dumping structure for table payroll.users
DROP TABLE IF EXISTS `users`;
CREATE TABLE IF NOT EXISTS `users` (
  `user_id` int NOT NULL AUTO_INCREMENT,
  `username` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `password_hash` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `full_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `email` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `role` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` enum('Active','Inactive','Suspended') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'Active',
  `last_login` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_date` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `username` (`username`),
  UNIQUE KEY `email` (`email`),
  KEY `idx_username` (`username`),
  KEY `idx_email` (`email`),
  KEY `idx_role` (`role`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Dumping data for table payroll.users: ~2 rows (approximately)
INSERT INTO `users` (`user_id`, `username`, `password_hash`, `full_name`, `email`, `role`, `status`, `last_login`, `created_date`) VALUES
	(1, 'admin', '6583f34aad7a18b9ff3b0a6a0edd7c37b0c6c9905367b12cd4726cdd2951b3e7', 'System Administrator', 'admin@tpc.edu.ph', 'Admin', 'Active', '2025-11-16T11:59:19.970313900', '2024-01-01'),
	(2, 'staff', '58bde48a8648abd29ec79736a008dc138bff384879cb070dba2c201d9f6a9f57', 'Tpc Staff', 'staff@gmail.com', 'Staff', 'Active', '2025-11-16T11:58:55.398264300', '2025-10-17');

/*!40103 SET TIME_ZONE=IFNULL(@OLD_TIME_ZONE, 'system') */;
/*!40101 SET SQL_MODE=IFNULL(@OLD_SQL_MODE, '') */;
/*!40014 SET FOREIGN_KEY_CHECKS=IFNULL(@OLD_FOREIGN_KEY_CHECKS, 1) */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40111 SET SQL_NOTES=IFNULL(@OLD_SQL_NOTES, 1) */;
