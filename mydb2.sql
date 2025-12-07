-- --------------------------------------------------------
-- Host:                         127.0.0.1
-- Server version:               8.0.44 - MySQL Community Server - GPL
-- Server OS:                    Win64
-- HeidiSQL Version:             12.13.0.7147
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

-- Dumping structure for table payroll.attendance
DROP TABLE IF EXISTS `attendance`;
CREATE TABLE IF NOT EXISTS `attendance` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT 'Primary key, auto-incremented',
  `account_number` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Employee account number from employees table',
  `log_datetime` datetime NOT NULL COMMENT 'Date and time of the attendance log',
  `log_type` enum('TIME_IN_AM','TIME_OUT_AM','TIME_IN_PM','TIME_OUT_PM') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Type of attendance log',
  `raw_data` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Raw biometric log data',
  `is_processed` tinyint(1) NOT NULL DEFAULT '0' COMMENT 'Whether this log has been processed for payroll',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Record creation timestamp',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Record last update timestamp',
  `imported_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'User who imported this record',
  `import_batch` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Import batch identifier',
  PRIMARY KEY (`id`),
  UNIQUE KEY `unique_attendance_log` (`account_number`,`log_datetime`,`log_type`),
  KEY `idx_account_number` (`account_number`)
) ENGINE=InnoDB AUTO_INCREMENT=69 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Employee attendance logs from biometric devices';

-- Dumping data for table payroll.attendance: ~68 rows (approximately)
INSERT INTO `attendance` (`id`, `account_number`, `log_datetime`, `log_type`, `raw_data`, `is_processed`, `created_at`, `updated_at`, `imported_by`, `import_batch`) VALUES
	(1, '124', '2025-09-01 07:56:26', 'TIME_IN_AM', '124	2025-09-01 07:56:26	1	0	1	0', 0, '2025-11-22 17:35:57', '2025-11-22 17:35:57', 'admin', 'TPC-sam-1-312025_attlog.dat'),
	(2, '124', '2025-09-01 12:00:06', 'TIME_OUT_PM', '124	2025-09-01 12:00:06	1	1	1	0', 0, '2025-11-22 17:35:57', '2025-11-22 17:35:57', 'admin', 'TPC-sam-1-312025_attlog.dat'),
	(3, '124', '2025-09-01 12:10:05', 'TIME_IN_PM', '124	2025-09-01 12:10:05	1	0	1	0', 0, '2025-11-22 17:35:57', '2025-11-22 17:35:57', 'admin', 'TPC-sam-1-312025_attlog.dat'),
	(4, '124', '2025-09-01 17:00:02', 'TIME_OUT_PM', '124	2025-09-01 17:00:02	1	1	1	0', 0, '2025-11-22 17:35:57', '2025-11-22 17:35:57', 'admin', 'TPC-sam-1-312025_attlog.dat'),
	(5, '124', '2025-09-04 08:14:55', 'TIME_IN_AM', '124	2025-09-04 08:14:55	1	0	1	0', 0, '2025-11-22 17:35:57', '2025-11-22 17:35:57', 'admin', 'TPC-sam-1-312025_attlog.dat'),
	(6, '124', '2025-09-04 12:00:26', 'TIME_OUT_PM', '124	2025-09-04 12:00:26	1	1	1	0', 0, '2025-11-22 17:35:57', '2025-11-22 17:35:57', 'admin', 'TPC-sam-1-312025_attlog.dat'),
	(7, '124', '2025-09-04 12:10:08', 'TIME_IN_PM', '124	2025-09-04 12:10:08	1	0	1	0', 0, '2025-11-22 17:35:57', '2025-11-22 17:35:57', 'admin', 'TPC-sam-1-312025_attlog.dat'),
	(8, '124', '2025-09-04 17:00:28', 'TIME_OUT_PM', '124	2025-09-04 17:00:28	1	1	1	0', 0, '2025-11-22 17:35:57', '2025-11-22 17:35:57', 'admin', 'TPC-sam-1-312025_attlog.dat'),
	(9, '124', '2025-09-05 08:14:32', 'TIME_IN_AM', '124	2025-09-05 08:14:32	1	0	1	0', 0, '2025-11-22 17:35:57', '2025-11-22 17:35:57', 'admin', 'TPC-sam-1-312025_attlog.dat'),
	(10, '124', '2025-09-05 12:00:08', 'TIME_OUT_PM', '124	2025-09-05 12:00:08	1	1	1	0', 0, '2025-11-22 17:35:57', '2025-11-22 17:35:57', 'admin', 'TPC-sam-1-312025_attlog.dat'),
	(11, '124', '2025-09-05 12:10:15', 'TIME_IN_PM', '124	2025-09-05 12:10:15	1	0	1	0', 0, '2025-11-22 17:35:57', '2025-11-22 17:35:57', 'admin', 'TPC-sam-1-312025_attlog.dat'),
	(12, '124', '2025-09-05 17:00:07', 'TIME_OUT_PM', '124	2025-09-05 17:00:07	1	1	1	0', 0, '2025-11-22 17:35:57', '2025-11-22 17:35:57', 'admin', 'TPC-sam-1-312025_attlog.dat'),
	(13, '124', '2025-09-06 07:46:32', 'TIME_IN_AM', '124	2025-09-06 07:46:32	1	0	1	0', 0, '2025-11-22 17:35:57', '2025-11-22 17:35:57', 'admin', 'TPC-sam-1-312025_attlog.dat'),
	(14, '124', '2025-09-06 12:01:09', 'TIME_OUT_PM', '124	2025-09-06 12:01:09	1	1	1	0', 0, '2025-11-22 17:35:57', '2025-11-22 17:35:57', 'admin', 'TPC-sam-1-312025_attlog.dat'),
	(15, '124', '2025-09-06 12:10:22', 'TIME_IN_PM', '124	2025-09-06 12:10:22	1	0	1	0', 0, '2025-11-22 17:35:57', '2025-11-22 17:35:57', 'admin', 'TPC-sam-1-312025_attlog.dat'),
	(16, '124', '2025-09-06 17:00:03', 'TIME_OUT_PM', '124	2025-09-06 17:00:03	1	1	1	0', 0, '2025-11-22 17:35:57', '2025-11-22 17:35:57', 'admin', 'TPC-sam-1-312025_attlog.dat'),
	(17, '124', '2025-09-07 08:03:12', 'TIME_IN_AM', '124	2025-09-07 08:03:12	1	0	1	0', 0, '2025-11-22 17:35:57', '2025-11-22 17:35:57', 'admin', 'TPC-sam-1-312025_attlog.dat'),
	(18, '124', '2025-09-07 12:00:12', 'TIME_OUT_PM', '124	2025-09-07 12:00:12	1	1	1	0', 0, '2025-11-22 17:35:57', '2025-11-22 17:35:57', 'admin', 'TPC-sam-1-312025_attlog.dat'),
	(19, '124', '2025-09-07 12:10:06', 'TIME_IN_PM', '124	2025-09-07 12:10:06	1	0	1	0', 0, '2025-11-22 17:35:57', '2025-11-22 17:35:57', 'admin', 'TPC-sam-1-312025_attlog.dat'),
	(20, '124', '2025-09-07 17:00:08', 'TIME_OUT_PM', '124	2025-09-07 17:00:08	1	1	1	0', 0, '2025-11-22 17:35:57', '2025-11-22 17:35:57', 'admin', 'TPC-sam-1-312025_attlog.dat'),
	(21, '124', '2025-09-08 08:13:25', 'TIME_IN_AM', '124	2025-09-08 08:13:25	1	0	1	0', 0, '2025-11-22 17:35:57', '2025-11-22 17:35:57', 'admin', 'TPC-sam-1-312025_attlog.dat'),
	(22, '124', '2025-09-08 12:00:34', 'TIME_OUT_PM', '124	2025-09-08 12:00:34	1	1	1	0', 0, '2025-11-22 17:35:57', '2025-11-22 17:35:57', 'admin', 'TPC-sam-1-312025_attlog.dat'),
	(23, '124', '2025-09-08 12:11:27', 'TIME_IN_PM', '124	2025-09-08 12:11:27	1	0	1	0', 0, '2025-11-22 17:35:57', '2025-11-22 17:35:57', 'admin', 'TPC-sam-1-312025_attlog.dat'),
	(24, '124', '2025-09-08 17:00:08', 'TIME_OUT_PM', '124	2025-09-08 17:00:08	1	1	1	0', 0, '2025-11-22 17:35:57', '2025-11-22 17:35:57', 'admin', 'TPC-sam-1-312025_attlog.dat'),
	(25, '124', '2025-09-11 07:59:26', 'TIME_IN_AM', '124	2025-09-11 07:59:26	1	0	1	0', 0, '2025-11-22 17:35:57', '2025-11-22 17:35:57', 'admin', 'TPC-sam-1-312025_attlog.dat'),
	(26, '124', '2025-09-11 12:34:52', 'TIME_OUT_PM', '124	2025-09-11 12:34:52	1	1	1	0', 0, '2025-11-22 17:35:57', '2025-11-22 17:35:57', 'admin', 'TPC-sam-1-312025_attlog.dat'),
	(27, '124', '2025-09-11 12:50:27', 'TIME_IN_PM', '124	2025-09-11 12:50:27	1	0	1	0', 0, '2025-11-22 17:35:57', '2025-11-22 17:35:57', 'admin', 'TPC-sam-1-312025_attlog.dat'),
	(28, '124', '2025-09-11 17:00:04', 'TIME_OUT_PM', '124	2025-09-11 17:00:04	1	1	1	0', 0, '2025-11-22 17:35:57', '2025-11-22 17:35:57', 'admin', 'TPC-sam-1-312025_attlog.dat'),
	(29, '124', '2025-09-12 08:06:40', 'TIME_IN_AM', '124	2025-09-12 08:06:40	1	0	1	0', 0, '2025-11-22 17:35:57', '2025-11-22 17:35:57', 'admin', 'TPC-sam-1-312025_attlog.dat'),
	(30, '124', '2025-09-12 12:00:06', 'TIME_OUT_PM', '124	2025-09-12 12:00:06	1	1	1	0', 0, '2025-11-22 17:35:57', '2025-11-22 17:35:57', 'admin', 'TPC-sam-1-312025_attlog.dat'),
	(31, '124', '2025-09-12 12:17:26', 'TIME_IN_PM', '124	2025-09-12 12:17:26	1	0	1	0', 0, '2025-11-22 17:35:57', '2025-11-22 17:35:57', 'admin', 'TPC-sam-1-312025_attlog.dat'),
	(32, '124', '2025-09-12 17:00:39', 'TIME_OUT_PM', '124	2025-09-12 17:00:39	1	1	1	0', 0, '2025-11-22 17:35:57', '2025-11-22 17:35:57', 'admin', 'TPC-sam-1-312025_attlog.dat'),
	(33, '124', '2025-09-13 08:08:26', 'TIME_IN_AM', '124	2025-09-13 08:08:26	1	0	1	0', 0, '2025-11-22 17:35:57', '2025-11-22 17:35:57', 'admin', 'TPC-sam-1-312025_attlog.dat'),
	(34, '124', '2025-09-13 12:00:25', 'TIME_OUT_PM', '124	2025-09-13 12:00:25	1	1	1	0', 0, '2025-11-22 17:35:57', '2025-11-22 17:35:57', 'admin', 'TPC-sam-1-312025_attlog.dat'),
	(35, '124', '2025-09-13 12:10:40', 'TIME_IN_PM', '124	2025-09-13 12:10:40	1	0	1	0', 0, '2025-11-22 17:35:57', '2025-11-22 17:35:57', 'admin', 'TPC-sam-1-312025_attlog.dat'),
	(36, '124', '2025-09-13 17:00:33', 'TIME_OUT_PM', '124	2025-09-13 17:00:33	1	1	1	0', 0, '2025-11-22 17:35:57', '2025-11-22 17:35:57', 'admin', 'TPC-sam-1-312025_attlog.dat'),
	(37, '124', '2025-09-14 08:12:28', 'TIME_IN_AM', '124	2025-09-14 08:12:28	1	0	1	0', 0, '2025-11-22 17:35:57', '2025-11-22 17:35:57', 'admin', 'TPC-sam-1-312025_attlog.dat'),
	(38, '124', '2025-09-14 12:00:25', 'TIME_OUT_PM', '124	2025-09-14 12:00:25	1	1	1	0', 0, '2025-11-22 17:35:57', '2025-11-22 17:35:57', 'admin', 'TPC-sam-1-312025_attlog.dat'),
	(39, '124', '2025-09-14 12:10:18', 'TIME_IN_PM', '124	2025-09-14 12:10:18	1	0	1	0', 0, '2025-11-22 17:35:57', '2025-11-22 17:35:57', 'admin', 'TPC-sam-1-312025_attlog.dat'),
	(40, '124', '2025-09-14 17:00:41', 'TIME_OUT_PM', '124	2025-09-14 17:00:41	1	1	1	0', 0, '2025-11-22 17:35:57', '2025-11-22 17:35:57', 'admin', 'TPC-sam-1-312025_attlog.dat'),
	(41, '124', '2025-09-18 07:47:49', 'TIME_IN_AM', '124	2025-09-18 07:47:49	1	0	1	0', 0, '2025-11-22 17:35:57', '2025-11-22 17:35:57', 'admin', 'TPC-sam-1-312025_attlog.dat'),
	(42, '124', '2025-09-18 12:00:18', 'TIME_OUT_PM', '124	2025-09-18 12:00:18	1	1	1	0', 0, '2025-11-22 17:35:57', '2025-11-22 17:35:57', 'admin', 'TPC-sam-1-312025_attlog.dat'),
	(43, '124', '2025-09-18 12:10:11', 'TIME_IN_PM', '124	2025-09-18 12:10:11	1	0	1	0', 0, '2025-11-22 17:35:57', '2025-11-22 17:35:57', 'admin', 'TPC-sam-1-312025_attlog.dat'),
	(44, '124', '2025-09-18 17:00:07', 'TIME_OUT_PM', '124	2025-09-18 17:00:07	1	1	1	0', 0, '2025-11-22 17:35:57', '2025-11-22 17:35:57', 'admin', 'TPC-sam-1-312025_attlog.dat'),
	(45, '124', '2025-09-19 07:32:24', 'TIME_IN_AM', '124	2025-09-19 07:32:24	1	0	1	0', 0, '2025-11-22 17:35:57', '2025-11-22 17:35:57', 'admin', 'TPC-sam-1-312025_attlog.dat'),
	(46, '124', '2025-09-19 12:00:11', 'TIME_OUT_PM', '124	2025-09-19 12:00:11	1	1	1	0', 0, '2025-11-22 17:35:57', '2025-11-22 17:35:57', 'admin', 'TPC-sam-1-312025_attlog.dat'),
	(47, '124', '2025-09-19 12:10:10', 'TIME_IN_PM', '124	2025-09-19 12:10:10	1	0	1	0', 0, '2025-11-22 17:35:57', '2025-11-22 17:35:57', 'admin', 'TPC-sam-1-312025_attlog.dat'),
	(48, '124', '2025-09-19 17:00:07', 'TIME_OUT_PM', '124	2025-09-19 17:00:07	1	1	1	0', 0, '2025-11-22 17:35:57', '2025-11-22 17:35:57', 'admin', 'TPC-sam-1-312025_attlog.dat'),
	(49, '124', '2025-09-20 07:24:51', 'TIME_IN_AM', '124	2025-09-20 07:24:51	1	0	1	0', 0, '2025-11-22 17:35:57', '2025-11-22 17:35:57', 'admin', 'TPC-sam-1-312025_attlog.dat'),
	(50, '124', '2025-09-20 12:00:55', 'TIME_OUT_PM', '124	2025-09-20 12:00:55	1	1	1	0', 0, '2025-11-22 17:35:57', '2025-11-22 17:35:57', 'admin', 'TPC-sam-1-312025_attlog.dat'),
	(51, '124', '2025-09-20 12:10:36', 'TIME_IN_PM', '124	2025-09-20 12:10:36	1	0	1	0', 0, '2025-11-22 17:35:57', '2025-11-22 17:35:57', 'admin', 'TPC-sam-1-312025_attlog.dat'),
	(52, '124', '2025-09-20 17:00:08', 'TIME_OUT_PM', '124	2025-09-20 17:00:08	1	1	1	0', 0, '2025-11-22 17:35:57', '2025-11-22 17:35:57', 'admin', 'TPC-sam-1-312025_attlog.dat'),
	(53, '124', '2025-09-26 07:24:50', 'TIME_IN_AM', '124	2025-09-26 07:24:50	1	0	1	0', 0, '2025-11-22 17:35:57', '2025-11-22 17:35:57', 'admin', 'TPC-sam-1-312025_attlog.dat'),
	(54, '124', '2025-09-26 12:10:44', 'TIME_OUT_PM', '124	2025-09-26 12:10:44	1	1	1	0', 0, '2025-11-22 17:35:57', '2025-11-22 17:35:57', 'admin', 'TPC-sam-1-312025_attlog.dat'),
	(55, '124', '2025-09-26 12:26:38', 'TIME_IN_PM', '124	2025-09-26 12:26:38	1	0	1	0', 0, '2025-11-22 17:35:57', '2025-11-22 17:35:57', 'admin', 'TPC-sam-1-312025_attlog.dat'),
	(56, '124', '2025-09-26 17:00:17', 'TIME_OUT_PM', '124	2025-09-26 17:00:17	1	1	1	0', 0, '2025-11-22 17:35:57', '2025-11-22 17:35:57', 'admin', 'TPC-sam-1-312025_attlog.dat'),
	(57, '124', '2025-09-27 06:50:33', 'TIME_IN_AM', '124	2025-09-27 06:50:33	1	0	1	0', 0, '2025-11-22 17:35:57', '2025-11-22 17:35:57', 'admin', 'TPC-sam-1-312025_attlog.dat'),
	(58, '124', '2025-09-27 12:01:09', 'TIME_OUT_PM', '124	2025-09-27 12:01:09	1	1	1	0', 0, '2025-11-22 17:35:57', '2025-11-22 17:35:57', 'admin', 'TPC-sam-1-312025_attlog.dat'),
	(59, '124', '2025-09-27 12:11:06', 'TIME_IN_PM', '124	2025-09-27 12:11:06	1	0	1	0', 0, '2025-11-22 17:35:57', '2025-11-22 17:35:57', 'admin', 'TPC-sam-1-312025_attlog.dat'),
	(60, '124', '2025-09-27 17:00:14', 'TIME_OUT_PM', '124	2025-09-27 17:00:14	1	1	1	0', 0, '2025-11-22 17:35:57', '2025-11-22 17:35:57', 'admin', 'TPC-sam-1-312025_attlog.dat'),
	(61, '124', '2025-09-28 06:44:36', 'TIME_IN_AM', '124	2025-09-28 06:44:36	1	0	1	0', 0, '2025-11-22 17:35:57', '2025-11-22 17:35:57', 'admin', 'TPC-sam-1-312025_attlog.dat'),
	(62, '124', '2025-09-28 12:00:28', 'TIME_OUT_PM', '124	2025-09-28 12:00:28	1	1	1	0', 0, '2025-11-22 17:35:57', '2025-11-22 17:35:57', 'admin', 'TPC-sam-1-312025_attlog.dat'),
	(63, '124', '2025-09-28 12:11:24', 'TIME_IN_PM', '124	2025-09-28 12:11:24	1	0	1	0', 0, '2025-11-22 17:35:57', '2025-11-22 17:35:57', 'admin', 'TPC-sam-1-312025_attlog.dat'),
	(64, '124', '2025-09-28 17:00:02', 'TIME_OUT_PM', '124	2025-09-28 17:00:02	1	1	1	0', 0, '2025-11-22 17:35:57', '2025-11-22 17:35:57', 'admin', 'TPC-sam-1-312025_attlog.dat'),
	(65, '124', '2025-09-29 07:00:04', 'TIME_IN_AM', '124	2025-09-29 07:00:04	1	0	1	0', 0, '2025-11-22 17:35:57', '2025-11-22 17:35:57', 'admin', 'TPC-sam-1-312025_attlog.dat'),
	(66, '124', '2025-09-29 12:00:14', 'TIME_OUT_PM', '124	2025-09-29 12:00:14	1	1	1	0', 0, '2025-11-22 17:35:57', '2025-11-22 17:35:57', 'admin', 'TPC-sam-1-312025_attlog.dat'),
	(67, '124', '2025-09-29 12:10:37', 'TIME_IN_PM', '124	2025-09-29 12:10:37	1	0	1	0', 0, '2025-11-22 17:35:57', '2025-11-22 17:35:57', 'admin', 'TPC-sam-1-312025_attlog.dat'),
	(68, '124', '2025-09-29 17:01:28', 'TIME_OUT_PM', '124	2025-09-29 17:01:28	1	1	1	0', 0, '2025-11-22 17:35:57', '2025-11-22 17:35:57', 'admin', 'TPC-sam-1-312025_attlog.dat');

-- Dumping structure for table payroll.deduction_types
DROP TABLE IF EXISTS `deduction_types`;
CREATE TABLE IF NOT EXISTS `deduction_types` (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `fixed_amount` decimal(10,2) DEFAULT NULL,
  `percentage` decimal(5,2) DEFAULT NULL,
  `formula` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Dumping data for table payroll.deduction_types: ~7 rows (approximately)
INSERT INTO `deduction_types` (`id`, `name`, `fixed_amount`, `percentage`, `formula`) VALUES
	(1, 'Pag-ibig', 200.00, NULL, NULL),
	(2, 'Late', NULL, NULL, NULL),
	(3, 'Absent', NULL, NULL, NULL),
	(4, 'Half-day', NULL, NULL, NULL),
	(5, 'GVAT', NULL, 5.00, NULL),
	(6, 'Expanded Tax', NULL, 3.00, NULL),
	(7, 'Pag-ibig Loan', NULL, NULL, NULL);

-- Dumping structure for table payroll.employee_deductions
DROP TABLE IF EXISTS `employee_deductions`;
CREATE TABLE IF NOT EXISTS `employee_deductions` (
  `id` int NOT NULL AUTO_INCREMENT,
  `employee_id` int NOT NULL,
  `deduction_type_id` int NOT NULL,
  `amount` decimal(10,2) NOT NULL,
  `details` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `employee_id` (`employee_id`),
  KEY `deduction_type_id` (`deduction_type_id`),
  CONSTRAINT `employee_deductions_ibfk_1` FOREIGN KEY (`employee_id`) REFERENCES `employees` (`id`),
  CONSTRAINT `employee_deductions_ibfk_2` FOREIGN KEY (`deduction_type_id`) REFERENCES `deduction_types` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Dumping data for table payroll.employee_deductions: ~0 rows (approximately)

-- Dumping structure for table payroll.employee_loans
DROP TABLE IF EXISTS `employee_loans`;
CREATE TABLE IF NOT EXISTS `employee_loans` (
  `id` int NOT NULL AUTO_INCREMENT,
  `employee_id` int NOT NULL,
  `loan_type_id` int NOT NULL,
  `loan_amount` decimal(10,2) NOT NULL,
  `monthly_amortization` decimal(10,2) NOT NULL,
  `balance` decimal(10,2) NOT NULL,
  `start_date` date NOT NULL,
  `end_date` date DEFAULT NULL,
  `status` enum('Active','Completed','Stopped') COLLATE utf8mb4_unicode_ci DEFAULT 'Active',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `employee_id` (`employee_id`),
  KEY `loan_type_id` (`loan_type_id`),
  CONSTRAINT `employee_loans_ibfk_1` FOREIGN KEY (`employee_id`) REFERENCES `employees` (`id`),
  CONSTRAINT `employee_loans_ibfk_2` FOREIGN KEY (`loan_type_id`) REFERENCES `loan_types` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Dumping data for table payroll.employee_loans: ~0 rows (approximately)

-- Dumping structure for table payroll.employees
DROP TABLE IF EXISTS `employees`;
CREATE TABLE IF NOT EXISTS `employees` (
  `id` int NOT NULL AUTO_INCREMENT,
  `account_number` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `full_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `position` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `salary` decimal(10,2) DEFAULT '0.00',
  `status` enum('Active','Inactive','Terminated') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'Active',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `salary_ref_id` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `account_number` (`account_number`),
  KEY `idx_account_number` (`account_number`),
  KEY `salary_ref_id` (`salary_ref_id`),
  CONSTRAINT `employees_ibfk_1` FOREIGN KEY (`salary_ref_id`) REFERENCES `salary_reference` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Dumping data for table payroll.employees: ~2 rows (approximately)
INSERT INTO `employees` (`id`, `account_number`, `full_name`, `position`, `salary`, `status`, `created_at`, `updated_at`, `salary_ref_id`) VALUES
	(1, '124', 'Josept Jett Abela', 'Instructor', 25000.00, 'Active', '2025-11-22 13:27:14', '2025-12-05 09:59:39', 11),
	(2, '203', 'Mafe R. Autida', 'Instructor', 30000.00, 'Active', '2025-11-22 13:39:39', '2025-12-05 09:59:44', 11);

-- Dumping structure for table payroll.loan_types
DROP TABLE IF EXISTS `loan_types`;
CREATE TABLE IF NOT EXISTS `loan_types` (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Dumping data for table payroll.loan_types: ~7 rows (approximately)
INSERT INTO `loan_types` (`id`, `name`) VALUES
	(1, 'Pag-ibig Loan'),
	(2, 'Pag-IBIG Loan'),
	(3, 'SSS Loan'),
	(4, 'Company Loan'),
	(5, 'Pag-IBIG Loan'),
	(6, 'SSS Loan'),
	(7, 'Company Loan'),
	(8, 'Pag-IBIG Loan'),
	(9, 'SSS Loan'),
	(10, 'Company Loan'),
	(11, 'Pag-IBIG Loan'),
	(12, 'SSS Loan'),
	(13, 'Company Loan');

-- Dumping structure for table payroll.payroll_history
DROP TABLE IF EXISTS `payroll_history`;
CREATE TABLE IF NOT EXISTS `payroll_history` (
  `payroll_id` int NOT NULL AUTO_INCREMENT,
  `employee_id` int NOT NULL,
  `pay_period` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `basic_salary` decimal(10,2) NOT NULL DEFAULT '0.00',
  `overtime` decimal(10,2) NOT NULL DEFAULT '0.00',
  `allowances` decimal(10,2) NOT NULL DEFAULT '0.00',
  `deductions` decimal(10,2) NOT NULL DEFAULT '0.00',
  `net_pay` decimal(10,2) NOT NULL DEFAULT '0.00',
  `status` enum('Draft','Generated','Paid','Cancelled') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'Draft',
  `created_date` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`payroll_id`),
  UNIQUE KEY `unique_employee_period` (`employee_id`),
  KEY `idx_employee_id` (`employee_id`),
  CONSTRAINT `payroll_history_ibfk_1` FOREIGN KEY (`employee_id`) REFERENCES `employees` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Dumping data for table payroll.payroll_history: ~0 rows (approximately)

-- Dumping structure for table payroll.payroll_process
DROP TABLE IF EXISTS `payroll_process`;
CREATE TABLE IF NOT EXISTS `payroll_process` (
  `process_id` int NOT NULL AUTO_INCREMENT,
  `employee_id` int NOT NULL,
  `account_number` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `pay_period_start` date NOT NULL,
  `pay_period_end` date NOT NULL,
  `basic_salary` decimal(10,2) NOT NULL DEFAULT '0.00',
  `daily_rate` decimal(10,2) NOT NULL DEFAULT '0.00',
  `hourly_rate` decimal(10,2) NOT NULL DEFAULT '0.00',
  `total_work_days` int NOT NULL DEFAULT '0',
  `present_days` int NOT NULL DEFAULT '0',
  `absent_days` int NOT NULL DEFAULT '0',
  `late_occurrences` int NOT NULL DEFAULT '0',
  `total_late_minutes` int NOT NULL DEFAULT '0',
  `regular_hours` decimal(8,2) NOT NULL DEFAULT '0.00',
  `overtime_hours` decimal(8,2) NOT NULL DEFAULT '0.00',
  `undertime_hours` decimal(8,2) NOT NULL DEFAULT '0.00',
  `basic_pay` decimal(10,2) NOT NULL DEFAULT '0.00',
  `overtime_pay` decimal(10,2) NOT NULL DEFAULT '0.00',
  `allowances` decimal(10,2) NOT NULL DEFAULT '0.00',
  `gross_pay` decimal(10,2) NOT NULL DEFAULT '0.00',
  `sss_deduction` decimal(10,2) NOT NULL DEFAULT '0.00',
  `philhealth_deduction` decimal(10,2) NOT NULL DEFAULT '0.00',
  `pagibig_deduction` decimal(10,2) NOT NULL DEFAULT '0.00',
  `withholding_tax` decimal(10,2) NOT NULL DEFAULT '0.00',
  `late_deduction` decimal(10,2) NOT NULL DEFAULT '0.00',
  `absent_deduction` decimal(10,2) NOT NULL DEFAULT '0.00',
  `other_deductions` decimal(10,2) NOT NULL DEFAULT '0.00',
  `total_deductions` decimal(10,2) NOT NULL DEFAULT '0.00',
  `net_pay` decimal(10,2) NOT NULL DEFAULT '0.00',
  `calculation_details` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `status` enum('Draft','Calculated','Approved','Paid','Cancelled') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'Draft',
  `processed_date` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `processed_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `approved_date` timestamp NULL DEFAULT NULL,
  `approved_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`process_id`),
  UNIQUE KEY `unique_employee_period` (`employee_id`,`pay_period_start`,`pay_period_end`),
  KEY `idx_employee_id` (`employee_id`),
  KEY `idx_account_number` (`account_number`),
  KEY `idx_pay_period` (`pay_period_start`,`pay_period_end`),
  KEY `idx_status` (`status`),
  KEY `idx_processed_date` (`processed_date`),
  CONSTRAINT `payroll_process_ibfk_1` FOREIGN KEY (`employee_id`) REFERENCES `employees` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Payroll processing records with attendance-based calculations';

-- Dumping data for table payroll.payroll_process: ~2 rows (approximately)
INSERT INTO `payroll_process` (`process_id`, `employee_id`, `account_number`, `pay_period_start`, `pay_period_end`, `basic_salary`, `daily_rate`, `hourly_rate`, `total_work_days`, `present_days`, `absent_days`, `late_occurrences`, `total_late_minutes`, `regular_hours`, `overtime_hours`, `undertime_hours`, `basic_pay`, `overtime_pay`, `allowances`, `gross_pay`, `sss_deduction`, `philhealth_deduction`, `pagibig_deduction`, `withholding_tax`, `late_deduction`, `absent_deduction`, `other_deductions`, `total_deductions`, `net_pay`, `calculation_details`, `status`, `processed_date`, `processed_by`, `approved_date`, `approved_by`, `created_at`, `updated_at`) VALUES
	(1, 1, '124', '2025-10-01', '2025-10-31', 25000.00, 0.00, 0.00, 0, 0, 0, 0, 0, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 1250.00, 625.00, 250.00, 0.00, 0.00, 0.00, 0.00, 2125.00, 22875.00, NULL, 'Calculated', '2025-11-22 17:58:58', 'Admin', NULL, NULL, '2025-11-22 17:58:58', '2025-11-22 17:58:58'),
	(2, 2, '203', '2025-10-01', '2025-10-31', 30000.00, 0.00, 0.00, 0, 0, 0, 0, 0, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 1500.00, 750.00, 300.00, 0.00, 0.00, 0.00, 0.00, 2550.00, 27450.00, NULL, 'Calculated', '2025-11-22 17:58:58', 'Admin', NULL, NULL, '2025-11-22 17:58:58', '2025-11-22 17:58:58');

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
) ENGINE=InnoDB AUTO_INCREMENT=33 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

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
	(265, 1, 1, 1, '2025-12-05 18:45:29'),
	(266, 1, 2, 1, '2025-12-05 18:45:29'),
	(267, 1, 3, 1, '2025-12-05 18:45:29'),
	(268, 1, 4, 1, '2025-12-05 18:45:29'),
	(269, 1, 5, 1, '2025-12-05 18:45:29'),
	(270, 1, 6, 1, '2025-12-05 18:45:29'),
	(271, 1, 7, 1, '2025-12-05 18:45:29'),
	(272, 1, 8, 1, '2025-12-05 18:45:29'),
	(273, 1, 9, 1, '2025-12-05 18:45:29'),
	(274, 1, 10, 1, '2025-12-05 18:45:29'),
	(275, 1, 11, 1, '2025-12-05 18:45:29'),
	(276, 1, 12, 1, '2025-12-05 18:45:29'),
	(277, 1, 13, 1, '2025-12-05 18:45:29'),
	(278, 1, 14, 1, '2025-12-05 18:45:29'),
	(279, 1, 15, 1, '2025-12-05 18:45:29'),
	(280, 1, 16, 1, '2025-12-05 18:45:29'),
	(281, 1, 17, 1, '2025-12-05 18:45:29'),
	(282, 1, 18, 1, '2025-12-05 18:45:29'),
	(283, 1, 19, 1, '2025-12-05 18:45:29'),
	(284, 1, 20, 1, '2025-12-05 18:45:29'),
	(285, 1, 21, 1, '2025-12-05 18:45:29'),
	(286, 1, 22, 1, '2025-12-05 18:45:29'),
	(287, 1, 23, 1, '2025-12-05 18:45:29'),
	(288, 1, 24, 1, '2025-12-05 18:45:29'),
	(289, 1, 25, 1, '2025-12-05 18:45:29'),
	(290, 1, 26, 1, '2025-12-05 18:45:29'),
	(291, 1, 27, 1, '2025-12-05 18:45:29'),
	(292, 1, 28, 1, '2025-12-05 18:45:29'),
	(293, 1, 29, 1, '2025-12-05 18:45:29'),
	(294, 1, 30, 1, '2025-12-05 18:45:29'),
	(295, 1, 31, 1, '2025-12-05 18:45:29'),
	(296, 1, 32, 1, '2025-12-05 18:45:29'),
	(297, 1, 33, 1, '2025-12-05 18:45:29'),
	(298, 1, 34, 1, '2025-12-05 18:45:29'),
	(299, 1, 35, 1, '2025-12-05 18:45:29'),
	(300, 1, 36, 1, '2025-12-05 18:45:29'),
	(301, 1, 37, 1, '2025-12-05 18:45:29'),
	(302, 1, 38, 1, '2025-12-05 18:45:29'),
	(303, 1, 39, 1, '2025-12-05 18:45:29'),
	(304, 1, 40, 1, '2025-12-05 18:45:29'),
	(305, 1, 41, 1, '2025-12-05 18:45:29'),
	(306, 1, 42, 1, '2025-12-05 18:45:29'),
	(307, 1, 43, 1, '2025-12-05 18:45:29'),
	(308, 1, 44, 1, '2025-12-05 18:45:29'),
	(309, 1, 45, 1, '2025-12-05 18:45:29'),
	(310, 1, 46, 1, '2025-12-05 18:45:29'),
	(311, 1, 47, 1, '2025-12-05 18:45:29'),
	(312, 1, 48, 1, '2025-12-05 18:45:29'),
	(313, 1, 49, 1, '2025-12-05 18:45:29'),
	(314, 1, 50, 1, '2025-12-05 18:45:29'),
	(315, 1, 51, 1, '2025-12-05 18:45:29'),
	(316, 1, 52, 1, '2025-12-05 18:45:29');

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

-- Dumping structure for table payroll.salary_reference
DROP TABLE IF EXISTS `salary_reference`;
CREATE TABLE IF NOT EXISTS `salary_reference` (
  `id` int NOT NULL AUTO_INCREMENT,
  `monthly_salary` decimal(10,2) NOT NULL,
  `rate_per_day` decimal(10,2) NOT NULL,
  `half_day_rate` decimal(10,2) NOT NULL,
  `rate_per_minute` decimal(10,2) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=14 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Dumping data for table payroll.salary_reference: ~13 rows (approximately)
INSERT INTO `salary_reference` (`id`, `monthly_salary`, `rate_per_day`, `half_day_rate`, `rate_per_minute`) VALUES
	(1, 11000.00, 500.00, 250.00, 1.04),
	(2, 11246.00, 511.18, 255.59, 1.06),
	(3, 11700.00, 531.82, 265.91, 1.11),
	(4, 12000.00, 545.45, 272.73, 1.14),
	(5, 12713.00, 577.86, 288.93, 1.20),
	(6, 13000.00, 590.91, 295.46, 1.23),
	(7, 15000.00, 681.82, 340.91, 1.42),
	(8, 17770.00, 807.73, 403.87, 1.68),
	(9, 20179.00, 917.23, 458.62, 1.91),
	(10, 24847.00, 1129.41, 564.71, 2.35),
	(11, 26749.00, 1215.86, 607.93, 2.53),
	(12, 29254.00, 1329.73, 664.87, 2.77),
	(13, 51000.00, 2318.18, 1159.09, 4.83);

-- Dumping structure for table payroll.security_events
DROP TABLE IF EXISTS `security_events`;
CREATE TABLE IF NOT EXISTS `security_events` (
  `event_id` int NOT NULL AUTO_INCREMENT,
  `timestamp` datetime NOT NULL,
  `event_type` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `severity` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `username` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `ip_address` varchar(45) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `user_agent` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `event_status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT 'ACTIVE',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`event_id`)
) ENGINE=InnoDB AUTO_INCREMENT=52 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Dumping data for table payroll.security_events: ~56 rows (approximately)
INSERT INTO `security_events` (`event_id`, `timestamp`, `event_type`, `severity`, `username`, `description`, `ip_address`, `user_agent`, `event_status`, `created_at`) VALUES
	(1, '2025-11-22 20:11:56', 'LOGIN_FAILED', 'MEDIUM', 'admin', 'Failed login attempt #1: Incorrect password. Please try again.', '127.0.0.1', NULL, 'ACTIVE', '2025-11-22 12:11:55'),
	(2, '2025-11-22 20:12:09', 'LOGIN_SUCCESS', 'LOW', 'admin', 'User logged in successfully from 127.0.0.1', '127.0.0.1', NULL, 'ACTIVE', '2025-11-22 12:12:08'),
	(3, '2025-11-22 20:14:07', 'PASSWORD_RESET', 'MEDIUM', 'admin', 'Password reset for user: staff (Tpc Staff 1) - Temporary password generated', '127.0.0.1', NULL, 'ACTIVE', '2025-11-22 12:14:07'),
	(4, '2025-11-22 20:15:04', 'LOGIN_FAILED', 'MEDIUM', 'staff', 'Failed login attempt #1: Incorrect password. Please try again.', '127.0.0.1', NULL, 'ACTIVE', '2025-11-22 12:15:03'),
	(5, '2025-11-22 20:15:12', 'LOGIN_FAILED', 'MEDIUM', 'staff', 'Failed login attempt #2: Incorrect password. Please try again.', '127.0.0.1', NULL, 'ACTIVE', '2025-11-22 12:15:11'),
	(6, '2025-11-22 20:15:13', 'LOGIN_FAILED', 'MEDIUM', 'staff', 'Failed login attempt #3: Incorrect password. Please try again.', '127.0.0.1', NULL, 'ACTIVE', '2025-11-22 12:15:13'),
	(7, '2025-11-22 20:15:34', 'LOGIN_SUCCESS', 'LOW', 'staff', 'User logged in successfully from 127.0.0.1', '127.0.0.1', NULL, 'ACTIVE', '2025-11-22 12:15:34'),
	(8, '2025-11-22 20:19:31', 'LOGIN_FAILED', 'MEDIUM', 'admin', 'Failed login attempt #1 from 110.54.182.16: Incorrect password. Please try again.', '110.54.182.16', NULL, 'ACTIVE', '2025-11-22 12:19:31'),
	(9, '2025-11-22 20:19:47', 'LOGIN_SUCCESS', 'LOW', 'admin', 'User logged in successfully from 110.54.182.16', '110.54.182.16', NULL, 'ACTIVE', '2025-11-22 12:19:47'),
	(10, '2025-11-22 20:27:28', 'LOGIN_SUCCESS', 'LOW', 'admin', 'User logged in successfully from 110.54.182.16', '110.54.182.16', NULL, 'ACTIVE', '2025-11-22 12:27:27'),
	(11, '2025-11-22 20:39:27', 'LOGIN_SUCCESS', 'LOW', 'admin', 'User logged in successfully from 110.54.182.16', '110.54.182.16', NULL, 'ACTIVE', '2025-11-22 12:39:27'),
	(12, '2025-11-22 20:42:22', 'LOGIN_SUCCESS', 'LOW', 'admin', 'User logged in successfully from 110.54.182.16', '110.54.182.16', NULL, 'ACTIVE', '2025-11-22 12:42:21'),
	(13, '2025-11-22 21:28:36', 'LOGIN_SUCCESS', 'LOW', 'admin', 'User logged in successfully from 110.54.182.16', '110.54.182.16', NULL, 'ACTIVE', '2025-11-22 13:28:36'),
	(14, '2025-11-22 21:32:45', 'LOGIN_SUCCESS', 'LOW', 'admin', 'User logged in successfully from 110.54.182.16', '110.54.182.16', NULL, 'ACTIVE', '2025-11-22 13:32:45'),
	(15, '2025-11-22 21:36:48', 'LOGIN_SUCCESS', 'LOW', 'admin', 'User logged in successfully from 110.54.182.16', '110.54.182.16', NULL, 'ACTIVE', '2025-11-22 13:36:48'),
	(16, '2025-11-22 21:50:28', 'LOGIN_SUCCESS', 'LOW', 'admin', 'User logged in successfully from 110.54.182.16', '110.54.182.16', NULL, 'ACTIVE', '2025-11-22 13:50:28'),
	(17, '2025-11-22 21:53:54', 'LOGIN_SUCCESS', 'LOW', 'admin', 'User logged in successfully from 110.54.182.16', '110.54.182.16', NULL, 'ACTIVE', '2025-11-22 13:53:54'),
	(18, '2025-11-22 21:57:17', 'LOGIN_SUCCESS', 'LOW', 'admin', 'User logged in successfully from 110.54.182.16', '110.54.182.16', NULL, 'ACTIVE', '2025-11-22 13:57:16'),
	(19, '2025-11-22 21:59:12', 'LOGIN_SUCCESS', 'LOW', 'admin', 'User logged in successfully from 110.54.182.16', '110.54.182.16', NULL, 'ACTIVE', '2025-11-22 13:59:12'),
	(20, '2025-11-22 22:01:56', 'LOGIN_SUCCESS', 'LOW', 'admin', 'User logged in successfully from 110.54.182.16', '110.54.182.16', NULL, 'ACTIVE', '2025-11-22 14:01:55'),
	(21, '2025-11-22 22:03:18', 'LOGIN_SUCCESS', 'LOW', 'admin', 'User logged in successfully from 110.54.182.16', '110.54.182.16', NULL, 'ACTIVE', '2025-11-22 14:03:17'),
	(22, '2025-11-22 22:10:40', 'LOGIN_SUCCESS', 'LOW', 'admin', 'User logged in successfully from 110.54.182.16', '110.54.182.16', NULL, 'ACTIVE', '2025-11-22 14:10:39'),
	(23, '2025-11-22 22:16:04', 'LOGIN_SUCCESS', 'LOW', 'admin', 'User logged in successfully from 110.54.182.16', '110.54.182.16', NULL, 'ACTIVE', '2025-11-22 14:16:04'),
	(24, '2025-11-22 22:21:55', 'LOGIN_SUCCESS', 'LOW', 'admin', 'User logged in successfully from 110.54.182.16', '110.54.182.16', NULL, 'ACTIVE', '2025-11-22 14:21:54'),
	(25, '2025-11-22 22:57:10', 'LOGIN_SUCCESS', 'LOW', 'admin', 'User logged in successfully from 112.198.98.3', '112.198.98.3', NULL, 'ACTIVE', '2025-11-22 14:57:10'),
	(26, '2025-11-22 23:00:03', 'LOGIN_SUCCESS', 'LOW', 'admin', 'User logged in successfully from 112.198.98.3', '112.198.98.3', NULL, 'ACTIVE', '2025-11-22 15:00:03'),
	(27, '2025-11-22 23:11:14', 'LOGIN_SUCCESS', 'LOW', 'admin', 'User logged in successfully from 112.198.98.3', '112.198.98.3', NULL, 'ACTIVE', '2025-11-22 15:11:14'),
	(28, '2025-11-22 23:16:47', 'LOGIN_SUCCESS', 'LOW', 'admin', 'User logged in successfully from 112.198.98.3', '112.198.98.3', NULL, 'ACTIVE', '2025-11-22 15:16:47'),
	(29, '2025-11-22 23:18:04', 'LOGIN_SUCCESS', 'LOW', 'admin', 'User logged in successfully from 112.198.98.3', '112.198.98.3', NULL, 'ACTIVE', '2025-11-22 15:18:04'),
	(30, '2025-11-22 23:29:14', 'LOGIN_SUCCESS', 'LOW', 'admin', 'User logged in successfully from 112.198.98.3', '112.198.98.3', NULL, 'ACTIVE', '2025-11-22 15:29:14'),
	(31, '2025-11-22 23:31:35', 'LOGIN_SUCCESS', 'LOW', 'admin', 'User logged in successfully from 112.198.98.3', '112.198.98.3', NULL, 'ACTIVE', '2025-11-22 15:31:35'),
	(32, '2025-11-22 23:35:31', 'LOGIN_SUCCESS', 'LOW', 'admin', 'User logged in successfully from 112.198.98.3', '112.198.98.3', NULL, 'ACTIVE', '2025-11-22 15:35:30'),
	(33, '2025-11-22 23:44:18', 'LOGIN_SUCCESS', 'LOW', 'admin', 'User logged in successfully from 112.198.98.3', '112.198.98.3', NULL, 'ACTIVE', '2025-11-22 15:44:18'),
	(34, '2025-11-22 23:48:15', 'LOGIN_SUCCESS', 'LOW', 'admin', 'User logged in successfully from 112.198.98.3', '112.198.98.3', NULL, 'ACTIVE', '2025-11-22 15:48:14'),
	(35, '2025-11-22 23:53:22', 'LOGIN_SUCCESS', 'LOW', 'admin', 'User logged in successfully from 112.198.98.3', '112.198.98.3', NULL, 'ACTIVE', '2025-11-22 15:53:22'),
	(36, '2025-11-22 23:59:06', 'LOGIN_SUCCESS', 'LOW', 'admin', 'User logged in successfully from 112.198.98.3', '112.198.98.3', NULL, 'ACTIVE', '2025-11-22 15:59:05'),
	(37, '2025-11-23 00:13:16', 'LOGIN_SUCCESS', 'LOW', 'admin', 'User logged in successfully from 112.198.98.3', '112.198.98.3', NULL, 'ACTIVE', '2025-11-22 16:13:15'),
	(38, '2025-11-23 00:20:13', 'LOGIN_SUCCESS', 'LOW', 'admin', 'User logged in successfully from 112.198.98.3', '112.198.98.3', NULL, 'ACTIVE', '2025-11-22 16:20:13'),
	(39, '2025-11-23 00:22:37', 'LOGIN_SUCCESS', 'LOW', 'admin', 'User logged in successfully from 112.198.98.3', '112.198.98.3', NULL, 'ACTIVE', '2025-11-22 16:22:37'),
	(40, '2025-11-23 00:29:36', 'LOGIN_SUCCESS', 'LOW', 'admin', 'User logged in successfully from 112.198.98.3', '112.198.98.3', NULL, 'ACTIVE', '2025-11-22 16:29:36'),
	(41, '2025-11-23 00:48:21', 'LOGIN_SUCCESS', 'LOW', 'admin', 'User logged in successfully from 112.198.98.3', '112.198.98.3', NULL, 'ACTIVE', '2025-11-22 16:48:21'),
	(42, '2025-11-23 01:09:33', 'LOGIN_SUCCESS', 'LOW', 'admin', 'User logged in successfully from 112.198.98.3', '112.198.98.3', NULL, 'ACTIVE', '2025-11-22 17:09:33'),
	(43, '2025-11-23 01:18:32', 'LOGIN_SUCCESS', 'LOW', 'admin', 'User logged in successfully from 112.198.98.3', '112.198.98.3', NULL, 'ACTIVE', '2025-11-22 17:18:31'),
	(44, '2025-11-23 01:21:48', 'LOGIN_SUCCESS', 'LOW', 'admin', 'User logged in successfully from 112.198.98.3', '112.198.98.3', NULL, 'ACTIVE', '2025-11-22 17:21:48'),
	(45, '2025-11-23 01:25:12', 'LOGIN_SUCCESS', 'LOW', 'admin', 'User logged in successfully from 112.198.98.3', '112.198.98.3', NULL, 'ACTIVE', '2025-11-22 17:25:12'),
	(46, '2025-11-23 01:30:18', 'LOGIN_SUCCESS', 'LOW', 'admin', 'User logged in successfully from 112.198.98.3', '112.198.98.3', NULL, 'ACTIVE', '2025-11-22 17:30:17'),
	(47, '2025-11-23 01:35:38', 'LOGIN_SUCCESS', 'LOW', 'admin', 'User logged in successfully from 112.198.98.3', '112.198.98.3', NULL, 'ACTIVE', '2025-11-22 17:35:38'),
	(48, '2025-11-23 01:58:32', 'LOGIN_SUCCESS', 'LOW', 'admin', 'User logged in successfully from 112.198.98.3', '112.198.98.3', NULL, 'ACTIVE', '2025-11-22 17:58:32'),
	(49, '2025-12-05 16:53:59', 'LOGIN_SUCCESS', 'LOW', 'admin', 'User logged in successfully from 180.190.127.59', '180.190.127.59', NULL, 'ACTIVE', '2025-12-05 08:53:59'),
	(50, '2025-12-05 18:20:54', 'LOGIN_SUCCESS', 'LOW', 'admin', 'User logged in successfully from 180.190.127.59', '180.190.127.59', NULL, 'ACTIVE', '2025-12-05 10:20:53'),
	(51, '2025-12-05 18:35:56', 'LOGIN_SUCCESS', 'LOW', 'admin', 'User logged in successfully from 180.190.127.59', '180.190.127.59', NULL, 'ACTIVE', '2025-12-05 10:35:55'),
	(52, '2025-12-05 18:46:06', 'LOGIN_SUCCESS', 'LOW', 'admin', 'User logged in successfully from 180.190.127.59', '180.190.127.59', NULL, 'ACTIVE', '2025-12-05 10:46:05'),
	(53, '2025-12-05 18:48:50', 'LOGIN_SUCCESS', 'LOW', 'admin', 'User logged in successfully from 180.190.127.59', '180.190.127.59', NULL, 'ACTIVE', '2025-12-05 10:48:50'),
	(54, '2025-12-05 18:51:58', 'LOGIN_SUCCESS', 'LOW', 'admin', 'User logged in successfully from 180.190.127.59', '180.190.127.59', NULL, 'ACTIVE', '2025-12-05 10:51:57'),
	(55, '2025-12-05 18:55:29', 'LOGIN_FAILED', 'MEDIUM', 'admin', 'Failed login attempt #1 from 180.190.127.59: Incorrect password. Please try again.', '180.190.127.59', NULL, 'ACTIVE', '2025-12-05 10:55:28'),
	(56, '2025-12-05 18:55:33', 'LOGIN_SUCCESS', 'LOW', 'admin', 'User logged in successfully from 180.190.127.59', '180.190.127.59', NULL, 'ACTIVE', '2025-12-05 10:55:33'),
	(57, '2025-12-05 19:11:59', 'LOGIN_SUCCESS', 'LOW', 'admin', 'User logged in successfully from 180.190.127.59', '180.190.127.59', NULL, 'ACTIVE', '2025-12-05 11:11:59'),
	(58, '2025-12-05 19:14:17', 'LOGIN_SUCCESS', 'LOW', 'admin', 'User logged in successfully from 180.190.127.59', '180.190.127.59', NULL, 'ACTIVE', '2025-12-05 11:14:16'),
	(59, '2025-12-05 19:16:32', 'LOGIN_SUCCESS', 'LOW', 'admin', 'User logged in successfully from 180.190.127.59', '180.190.127.59', NULL, 'ACTIVE', '2025-12-05 11:16:31');

-- Dumping structure for procedure payroll.sp_generate_payroll
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

    DECLARE v_sss DECIMAL(10,2) DEFAULT 0;
    DECLARE v_ph DECIMAL(10,2) DEFAULT 0;
    DECLARE v_pagibig DECIMAL(10,2) DEFAULT 0;

    DECLARE v_late DECIMAL(10,2) DEFAULT 0;
    DECLARE v_absent DECIMAL(10,2) DEFAULT 0;

    DECLARE v_total_deductions DECIMAL(10,2);
    DECLARE v_net DECIMAL(10,2);

    DECLARE v_has_attendance INT DEFAULT 0;

    DECLARE cur CURSOR FOR
        SELECT id, account_number, salary
        FROM employees
        WHERE status='Active';

    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;

    OPEN cur;

    payroll_loop: LOOP

        FETCH cur INTO v_emp_id, v_acc_no, v_salary;
        IF done THEN LEAVE payroll_loop; END IF;

        -- *************************************************
        -- 0. CHECK IF EMPLOYEE HAS ANY ATTENDANCE
        -- *************************************************
        SELECT COUNT(*)
        INTO v_has_attendance
        FROM attendance
        WHERE account_number=v_acc_no
          AND DATE(log_datetime) BETWEEN p_start_date AND p_end_date;

        -- *************************************************
        -- 1. GOVERNMENT DEDUCTIONS (ALWAYS APPLIED)
        -- *************************************************
        SELECT (v_salary * (employee_share / 100))
        INTO v_sss
        FROM deductions WHERE code='SSS' LIMIT 1;

        SELECT (v_salary * (employee_share / 100))
        INTO v_ph
        FROM deductions WHERE code='PHILHEALTH' LIMIT 1;

        SELECT (v_salary * (employee_share / 100))
        INTO v_pagibig
        FROM deductions WHERE code='PAGIBIG' LIMIT 1;


        -- *************************************************
        -- 2. IF NO ATTENDANCE AT ALL â†’ NO LATE, NO ABSENT
        -- *************************************************
        IF v_has_attendance = 0 THEN

            SET v_late = 0;
            SET v_absent = 0;

        ELSE
            -- *************************************************
            -- 3. COUNT LATE DAYS
            -- *************************************************
            SELECT fixed_amount INTO @late_fixed
            FROM deductions WHERE code='LATE' LIMIT 1;

            SELECT COUNT(*)
            INTO @late_days
            FROM attendance
            WHERE account_number=v_acc_no
              AND log_type='TIME_IN_AM'
              AND TIME(log_datetime) > '08:00:00'
              AND DATE(log_datetime) BETWEEN p_start_date AND p_end_date;

            SET v_late = @late_days * @late_fixed;

            -- *************************************************
            -- 4. ABSENTS BASED ON THEIR OWN DAYS ONLY
            -- *************************************************
            SELECT COUNT(DISTINCT DATE(log_datetime))
            INTO @emp_days
            FROM attendance
            WHERE account_number=v_acc_no
              AND DATE(log_datetime) BETWEEN p_start_date AND p_end_date;

            SELECT COUNT(*)
            INTO @present_days
            FROM (
                SELECT DATE(log_datetime)
                FROM attendance
                WHERE account_number=v_acc_no
                  AND DATE(log_datetime) BETWEEN p_start_date AND p_end_date
                GROUP BY DATE(log_datetime)
                HAVING SUM(log_type='TIME_IN_AM') > 0
                   AND SUM(log_type='TIME_OUT_PM') > 0
            ) AS x;

            SET @absents = @emp_days - @present_days;

            IF @absents < 0 THEN SET @absents = 0; END IF;

            SET v_absent = @absents * (v_salary / 26);

        END IF;


        -- *************************************************
        -- 5. TOTAL DEDUCTIONS
        -- *************************************************
        SET v_total_deductions =
            IFNULL(v_sss,0) +
            IFNULL(v_ph,0) +
            IFNULL(v_pagibig,0) +
            IFNULL(v_late,0) +
            IFNULL(v_absent,0);

        -- *************************************************
        -- 6. NET PAY = BASIC SALARY - TOTAL DEDUCTIONS
        -- *************************************************
        SET v_net = v_salary - v_total_deductions;


        -- *************************************************
        -- 7. INSERT RESULT
        -- *************************************************
        INSERT INTO payroll_process (
            employee_id,
            account_number,
            pay_period_start,
            pay_period_end,
            basic_salary,
            sss_deduction,
            philhealth_deduction,
            pagibig_deduction,
            late_deduction,
            absent_deduction,
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
            v_sss,
            v_ph,
            v_pagibig,
            v_late,
            v_absent,
            v_total_deductions,
            v_net,
            p_processed_by,
            'Calculated'
        );

    END LOOP;

    CLOSE cur;

END//
DELIMITER ;

-- Dumping structure for table payroll.users
DROP TABLE IF EXISTS `users`;
CREATE TABLE IF NOT EXISTS `users` (
  `user_id` int NOT NULL AUTO_INCREMENT,
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `password_hash` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `full_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `role` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` enum('Active','Inactive','Suspended') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'Active',
  `last_login` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_date` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
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
	(1, 'admin', '6583f34aad7a18b9ff3b0a6a0edd7c37b0c6c9905367b12cd4726cdd2951b3e7', 'System Administrator', 'admin@tpc.edu.ph', 'Admin', 'Active', '2025-12-05T19:16:31.668836400', '2024-01-01'),
	(2, 'staff', '58bde48a8648abd29ec79736a008dc138bff384879cb070dba2c201d9f6a9f57', 'Tpc Staff 1', 'staff@gmail.com', 'Staff', 'Active', '2025-11-22T20:15:33.977315900', '2025-10-17');

/*!40103 SET TIME_ZONE=IFNULL(@OLD_TIME_ZONE, 'system') */;
/*!40101 SET SQL_MODE=IFNULL(@OLD_SQL_MODE, '') */;
/*!40014 SET FOREIGN_KEY_CHECKS=IFNULL(@OLD_FOREIGN_KEY_CHECKS, 1) */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40111 SET SQL_NOTES=IFNULL(@OLD_SQL_NOTES, 1) */;
