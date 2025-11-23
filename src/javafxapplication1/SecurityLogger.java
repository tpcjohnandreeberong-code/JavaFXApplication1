package javafxapplication1;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.net.*;
import java.util.Enumeration;
import java.io.*;

/**
 * Static utility class for security event logging
 */
public class SecurityLogger {
    
    private static final Logger logger = Logger.getLogger(SecurityLogger.class.getName());
    
    // Database connection parameters
    private static final String DB_URL = DatabaseConfig.getDbUrl();
    private static final String DB_USER = DatabaseConfig.getDbUser();
    private static final String DB_PASSWORD = DatabaseConfig.getDbPassword();
    
    private static Connection getConnection() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL driver not found", e);
        }
    }
    
    /**
     * Log a security event to the database
     */
    public static void logSecurityEvent(String eventType, String severity, String username, 
                                      String description, String ipAddress) {
        try (Connection connection = getConnection()) {
            
            // Create table if it doesn't exist
            createTableIfNotExists(connection);
            
            String insertQuery = """
                INSERT INTO security_events (timestamp, event_type, severity, username, 
                                           description, ip_address, event_status) 
                VALUES (?, ?, ?, ?, ?, ?, 'ACTIVE')
            """;
            
            try (PreparedStatement stmt = connection.prepareStatement(insertQuery)) {
                stmt.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
                stmt.setString(2, eventType);
                stmt.setString(3, severity);
                stmt.setString(4, username);
                stmt.setString(5, description);
                stmt.setString(6, ipAddress != null ? ipAddress : "127.0.0.1");
                
                stmt.executeUpdate();
                logger.info("Security event logged: " + eventType + " - " + description);
            }
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to log security event", e);
            // Don't throw exception - logging should be non-blocking
        }
    }
    
    /**
     * Create security_events table if it doesn't exist
     */
    private static void createTableIfNotExists(Connection connection) throws SQLException {
        String createTableSQL = """
            CREATE TABLE IF NOT EXISTS security_events (
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
            )
        """;
        
        try (PreparedStatement stmt = connection.prepareStatement(createTableSQL)) {
            stmt.executeUpdate();
        }
    }
    
    /**
     * Convenience method with automatic IP detection
     */
    public static void logSecurityEvent(String eventType, String severity, String username, 
                                      String description) {
        logSecurityEvent(eventType, severity, username, description, getClientIP());
    }
    
    /**
     * Get the actual client IP address
     */
    public static String getClientIP() {
        try {
            // First try to get external IP
            String externalIP = getExternalIP();
            if (externalIP != null && !externalIP.equals("127.0.0.1")) {
                return externalIP;
            }
            
            // If external IP fails, get local network IP
            String localIP = getLocalNetworkIP();
            if (localIP != null && !localIP.equals("127.0.0.1")) {
                return localIP;
            }
            
            // Fallback to localhost
            return "127.0.0.1";
            
        } catch (Exception e) {
            logger.log(Level.WARNING, "Could not determine client IP", e);
            return "127.0.0.1";
        }
    }
    
    /**
     * Get external IP address by checking with online service
     */
    private static String getExternalIP() {
        try {
            // Try multiple services for reliability
            String[] ipServices = {
                "http://checkip.amazonaws.com/",
                "http://ipinfo.io/ip",
                "http://icanhazip.com/"
            };
            
            for (String service : ipServices) {
                try {
                    URL url = new URL(service);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setConnectTimeout(5000); // 5 second timeout
                    connection.setReadTimeout(5000);
                    connection.setRequestMethod("GET");
                    
                    if (connection.getResponseCode() == 200) {
                        try (java.io.BufferedReader reader = new java.io.BufferedReader(
                                new java.io.InputStreamReader(connection.getInputStream()))) {
                            String ip = reader.readLine().trim();
                            if (isValidIP(ip)) {
                                logger.info("External IP detected: " + ip);
                                return ip;
                            }
                        }
                    }
                } catch (Exception e) {
                    // Try next service
                    continue;
                }
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Could not get external IP", e);
        }
        return null;
    }
    
    /**
     * Get local network IP address
     */
    private static String getLocalNetworkIP() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                
                // Skip loopback and inactive interfaces
                if (networkInterface.isLoopback() || !networkInterface.isUp()) {
                    continue;
                }
                
                Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress address = addresses.nextElement();
                    
                    // We want IPv4 addresses that are not loopback
                    if (address instanceof Inet4Address && !address.isLoopbackAddress()) {
                        String ip = address.getHostAddress();
                        
                        // Prefer private network addresses (192.168.x.x, 10.x.x.x, 172.16-31.x.x)
                        if (isPrivateIP(ip)) {
                            logger.info("Local network IP detected: " + ip);
                            return ip;
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Could not get local network IP", e);
        }
        return null;
    }
    
    /**
     * Check if IP address is a valid IPv4 address
     */
    private static boolean isValidIP(String ip) {
        if (ip == null || ip.trim().isEmpty()) {
            return false;
        }
        
        String[] parts = ip.split("\\.");
        if (parts.length != 4) {
            return false;
        }
        
        try {
            for (String part : parts) {
                int num = Integer.parseInt(part);
                if (num < 0 || num > 255) {
                    return false;
                }
            }
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    /**
     * Check if IP is a private network address
     */
    private static boolean isPrivateIP(String ip) {
        if (ip == null) return false;
        
        return ip.startsWith("192.168.") ||   // 192.168.0.0/16
               ip.startsWith("10.") ||        // 10.0.0.0/8
               (ip.startsWith("172.") &&      // 172.16.0.0/12
                ip.split("\\.").length >= 2 &&
                Integer.parseInt(ip.split("\\.")[1]) >= 16 &&
                Integer.parseInt(ip.split("\\.")[1]) <= 31);
    }
}