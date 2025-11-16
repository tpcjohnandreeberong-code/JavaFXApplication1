package javafxapplication1;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Database Authentication Service for TPC Payroll Management System
 * Handles user authentication, role checking, and permission validation
 */
public class DatabaseAuthService {
    
    private static final Logger logger = Logger.getLogger(DatabaseAuthService.class.getName());
    private Connection connection;
    
    // Database connection parameters
    private static final String DB_URL = "jdbc:mysql://127.0.0.1:3306/payroll";
    private static final String DB_USER = "tpc_user";
    private static final String DB_PASSWORD = "tpcuser123!";
    
    public DatabaseAuthService() {
        try {
            // Load MySQL JDBC driver
            Class.forName("com.mysql.cj.jdbc.Driver");
            connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
        } catch (ClassNotFoundException | SQLException e) {
            logger.log(Level.SEVERE, "Database connection failed", e);
        }
    }
    
    /**
     * Authenticate user login
     * @param username Username to authenticate
     * @param password Plain text password
     * @param ipAddress Client IP address
     * @param userAgent Client user agent
     * @return AuthenticationResult containing user info and success status
     */
    public AuthenticationResult authenticateUser(String username, String password, String ipAddress, String userAgent) {
        AuthenticationResult result = new AuthenticationResult();
        
        try {
            // Get user information - support both username and email login
            String userQuery = "SELECT user_id, username, password_hash, full_name, email, role, status " +
                              "FROM users WHERE (username = ? OR email = ?) AND status = 'Active'";
            
            PreparedStatement userStmt = connection.prepareStatement(userQuery);
            userStmt.setString(1, username);
            userStmt.setString(2, username); // Same parameter for both username and email check
            ResultSet userRs = userStmt.executeQuery();
            
            if (!userRs.next()) {
                // User not found - check if it looks like an email or username
                if (username.contains("@")) {
                    result.setSuccess(false);
                    result.setMessage("Email address not found or account is inactive.");
                } else {
                    result.setSuccess(false);
                    result.setMessage("Username not found or account is inactive.");
                }
                return result;
            }
            
            int userId = userRs.getInt("user_id");
            String storedHash = userRs.getString("password_hash");
            String fullName = userRs.getString("full_name");
            String email = userRs.getString("email");
            String role = userRs.getString("role");
            String status = userRs.getString("status");
            
            // Static admin login bypass - only for admin user
            if (username.equalsIgnoreCase("admin")) {
                // Admin login - skip password verification
                logger.info("Admin login detected - bypassing password verification");
            } else {
                // Verify password for all other users (including staff)
                logger.info("Verifying password for user: " + username);
                logger.info("Input password: " + password);
                logger.info("Stored hash from DB: " + storedHash);
                
                boolean passwordMatch = verifyPassword(password, storedHash);
                logger.info("Password verification result: " + (passwordMatch ? "SUCCESS" : "FAILED"));
                
                if (!passwordMatch) {
                    // Password incorrect
                    result.setSuccess(false);
                    result.setMessage("Incorrect password. Please try again.");
                    return result;
                }
            }
            
            // Login successful
            updateLastLogin(userId);
            
            // Get user permissions based on role
            List<String> permissions = getUserPermissions(userId);
            List<String> roles = new ArrayList<>();
            roles.add(role);
            
            result.setSuccess(true);
            result.setUserId(userId);
            result.setUsername(username);
            result.setFullName(fullName);
            result.setEmail(email);
            result.setRoles(roles);
            result.setPermissions(permissions);
            result.setMessage("Login successful");
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Authentication error", e);
            result.setSuccess(false);
            result.setMessage("System error. Please try again.");
        }
        
        return result;
    }
    
    /**
     * Get user permissions based on their role
     * @param userId User ID
     * @return List of permission names
     */
    public List<String> getUserPermissions(int userId) {
        List<String> permissions = new ArrayList<>();
        
        try {
            String query = "SELECT DISTINCT p.permission_name " +
                          "FROM users u " +
                          "JOIN roles r ON u.role = r.role_name " +
                          "JOIN role_permissions rp ON r.role_id = rp.role_id " +
                          "JOIN permissions p ON rp.permission_id = p.permission_id " +
                          "WHERE u.user_id = ? AND r.status = 'Active' " +
                          "AND rp.granted = TRUE AND p.status = 'Active'";
            
            PreparedStatement stmt = connection.prepareStatement(query);
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                permissions.add(rs.getString("permission_name"));
            }
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error getting user permissions", e);
        }
        
        return permissions;
    }
    
    /**
     * Get user role
     * @param userId User ID
     * @return Role name
     */
    public String getUserRole(int userId) {
        try {
            String query = "SELECT role FROM users WHERE user_id = ?";
            
            PreparedStatement stmt = connection.prepareStatement(query);
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getString("role");
            }
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error getting user role", e);
        }
        
        return null;
    }
    
    /**
     * Check if user has specific permission
     * @param userId User ID
     * @param permission Permission name to check
     * @return True if user has permission
     */
    public boolean hasPermission(int userId, String permission) {
        try {
            String query = "SELECT COUNT(*) as count " +
                          "FROM users u " +
                          "JOIN roles r ON u.role = r.role_name " +
                          "JOIN role_permissions rp ON r.role_id = rp.role_id " +
                          "JOIN permissions p ON rp.permission_id = p.permission_id " +
                          "WHERE u.user_id = ? AND p.permission_name = ? " +
                          "AND r.status = 'Active' " +
                          "AND rp.granted = TRUE AND p.status = 'Active'";
            
            PreparedStatement stmt = connection.prepareStatement(query);
            stmt.setInt(1, userId);
            stmt.setString(2, permission);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt("count") > 0;
            }
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error checking permission", e);
        }
        
        return false;
    }
    
    /**
     * Update user's last login time
     */
    private void updateLastLogin(int userId) {
        try {
            String query = "UPDATE users SET last_login = ? WHERE user_id = ?";
            PreparedStatement stmt = connection.prepareStatement(query);
            stmt.setString(1, java.time.LocalDateTime.now().toString());
            stmt.setInt(2, userId);
            stmt.executeUpdate();
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error updating last login", e);
        }
    }
    
    /**
     * Verify password using SHA-256 with salt
     */
    private boolean verifyPassword(String password, String hash) {
        try {
            String salt = "TPC_PAYROLL_SALT_2024"; // Same salt as UserManagement
            String passwordWithSalt = password + salt;
            
            logger.info("Password verification details:");
            logger.info("  Input password: " + password);
            logger.info("  Salt: " + salt);
            logger.info("  Password + Salt: " + passwordWithSalt);
            logger.info("  Stored hash: " + hash);
            
            // Use SHA-256 hashing
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(passwordWithSalt.getBytes("UTF-8"));
            
            // Convert to hexadecimal string
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            String generatedHash = hexString.toString();
            logger.info("  Generated hash: " + generatedHash);
            logger.info("  Hash comparison: " + generatedHash.equals(hash));
            
            return generatedHash.equals(hash);
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error verifying password", e);
            return false;
        }
    }
    
    /**
     * Close database connection
     */
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error closing database connection", e);
        }
    }
    
    /**
     * Authentication result class
     */
    public static class AuthenticationResult {
        private boolean success;
        private String message;
        private int userId;
        private String username;
        private String fullName;
        private String email;
        private List<String> roles;
        private List<String> permissions;
        
        // Getters and Setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        public int getUserId() { return userId; }
        public void setUserId(int userId) { this.userId = userId; }
        
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        
        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }
        
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        
        public List<String> getRoles() { return roles; }
        public void setRoles(List<String> roles) { this.roles = roles; }
        
        public List<String> getPermissions() { return permissions; }
        public void setPermissions(List<String> permissions) { this.permissions = permissions; }
    }
}
