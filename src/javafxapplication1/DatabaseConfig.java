package javafxapplication1;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Centralized database configuration management
 * Loads database settings from properties file
 */
public class DatabaseConfig {
    private static final String CONFIG_FILE = "/database.properties";
    private static Properties properties;
    
    // Default values as fallback
    private static final String DEFAULT_DB_URL = "jdbc:mysql://127.0.0.1:3307/payroll";
    private static final String DEFAULT_DB_USER = "root";
    private static final String DEFAULT_DB_PASSWORD = "berong123!";
    
    static {
        loadProperties();
    }
    
    private static void loadProperties() {
        properties = new Properties();
        try (InputStream input = DatabaseConfig.class.getResourceAsStream(CONFIG_FILE)) {
            if (input != null) {
                properties.load(input);
                System.out.println("Database configuration loaded successfully");
            } else {
                System.out.println("Configuration file not found, using default values");
            }
        } catch (IOException e) {
            System.err.println("Error loading database configuration: " + e.getMessage());
            System.out.println("Using default database configuration");
        }
    }
    
    public static String getDbUrl() {
        return properties.getProperty("db.url", DEFAULT_DB_URL);
    }
    
    public static String getDbUser() {
        return properties.getProperty("db.user", DEFAULT_DB_USER);
    }
    
    public static String getDbPassword() {
        return properties.getProperty("db.password", DEFAULT_DB_PASSWORD);
    }
    
    // Method to reload configuration if needed
    public static void reloadConfiguration() {
        loadProperties();
    }
    
    // Method to display current configuration (without password)
    public static void printConfiguration() {
        System.out.println("=== Database Configuration ===");
        System.out.println("URL: " + getDbUrl());
        System.out.println("User: " + getDbUser());
        System.out.println("Password: [HIDDEN]");
        System.out.println("=============================");
    }
}