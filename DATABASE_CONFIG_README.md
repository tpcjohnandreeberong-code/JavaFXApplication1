# Database Configuration Management

## Overview
The TPC Payroll Management System now uses a centralized database configuration system that allows you to manage database connection settings in one place.

## Files Created
- `src/javafxapplication1/DatabaseConfig.java` - Central configuration class
- `src/database.properties` - Configuration properties file

## How to Use

### 1. Update Database Settings
Edit the `src/database.properties` file to change database connection settings:

```properties
# Database Configuration
db.url=jdbc:mysql://127.0.0.1:3306/payroll
db.user=tpc_user
db.password=tpcuser123!
```

### 2. Environment-Specific Configurations
For different environments (development, testing, production), you can:

**Option A: Multiple Properties Files**
- Create `database-dev.properties`, `database-test.properties`, `database-prod.properties`
- Modify `DatabaseConfig.java` to load different files based on environment

**Option B: Environment Variables**
Add support for environment variable overrides in `DatabaseConfig.java`:

```java
public static String getDbUrl() {
    String envUrl = System.getenv("DB_URL");
    return envUrl != null ? envUrl : properties.getProperty("db.url", DEFAULT_DB_URL);
}
```

### 3. Benefits
- ✅ **Single Point of Configuration**: Change database settings in one place
- ✅ **Environment Flexibility**: Easy to switch between dev/test/prod databases
- ✅ **Security**: Can move sensitive credentials to environment variables
- ✅ **Maintainability**: No more scattered hardcoded connection strings
- ✅ **Fallback Support**: Uses default values if configuration file is missing

### 4. Updated Files
All controller files now use `DatabaseConfig` instead of hardcoded values:
- DatabaseAuthService.java
- EmployeeController.java
- MainController.java
- PayrollGeneratorController.java
- PayrollProcessingController.java
- ImportExportController.java
- EditProfileController.java
- SecurityMaintenanceController.java
- UserAccessController.java
- GovernmentRemittancesController.java
- UserManagementController.java
- SecurityLogger.java
- PayrollProcessEntry.java

### 5. Usage in Code
Controllers now use:
```java
private static final String DB_URL = DatabaseConfig.getDbUrl();
private static final String DB_USER = DatabaseConfig.getDbUser();
private static final String DB_PASSWORD = DatabaseConfig.getDbPassword();
```

### 6. Testing Configuration
You can test the configuration by calling:
```java
DatabaseConfig.printConfiguration(); // Shows current config (password hidden)
DatabaseConfig.reloadConfiguration(); // Reloads from properties file
```

## Next Steps
Consider adding:
- Environment variable support
- Encrypted password storage
- Connection pooling configuration
- Database driver configuration