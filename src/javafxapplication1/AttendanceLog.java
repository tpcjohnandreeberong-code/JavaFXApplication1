package javafxapplication1;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * AttendanceLog class represents an employee attendance record from biometric devices
 */
public class AttendanceLog {
    
    // Enum for log types based on biometric data patterns
    public enum LogType {
        TIME_IN_AM("TIME_IN_AM", "Time In (AM)"),
        TIME_OUT_AM("TIME_OUT_AM", "Time Out (AM)"), 
        TIME_IN_PM("TIME_IN_PM", "Time In (PM)"),
        TIME_OUT_PM("TIME_OUT_PM", "Time Out (PM)");
        
        private final String code;
        private final String description;
        
        LogType(String code, String description) {
            this.code = code;
            this.description = description;
        }
        
        public String getCode() { return code; }
        public String getDescription() { return description; }
        
        @Override
        public String toString() { return description; }
    }
    
    private int id;
    private String accountNumber;
    private LocalDateTime logDateTime;
    private LogType logType;
    private String rawData;
    private boolean isProcessed;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String importedBy;
    private String importBatch;
    
    // Constructors
    public AttendanceLog() {}
    
    public AttendanceLog(String accountNumber, LocalDateTime logDateTime, LogType logType) {
        this.accountNumber = accountNumber;
        this.logDateTime = logDateTime;
        this.logType = logType;
        this.isProcessed = false;
    }
    
    public AttendanceLog(int id, String accountNumber, LocalDateTime logDateTime, LogType logType, 
                        String rawData, boolean isProcessed, LocalDateTime createdAt, 
                        LocalDateTime updatedAt, String importedBy, String importBatch) {
        this.id = id;
        this.accountNumber = accountNumber;
        this.logDateTime = logDateTime;
        this.logType = logType;
        this.rawData = rawData;
        this.isProcessed = isProcessed;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.importedBy = importedBy;
        this.importBatch = importBatch;
    }
    
    // Static method to parse biometric log format
    // Format: account_number	datetime	1	0/1	1	0
    // Where third field is always 1, fourth field: 0=in, 1=out
    public static AttendanceLog parseFromBiometricLog(String logLine) {
        try {
            String[] parts = logLine.trim().split("\\s+");
            if (parts.length != 6) {
                throw new IllegalArgumentException("Invalid log format: expected 6 fields");
            }
            
            String accountNumber = parts[0].trim();
            LocalDateTime dateTime = LocalDateTime.parse(parts[1] + " " + parts[2], 
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            
            // Parse the biometric flags
            // parts[3] is always 1
            int inOutFlag = Integer.parseInt(parts[4]); // 0=in, 1=out
            // parts[5] is always 1
            // parts[6] is always 0
            
            // Determine log type based on time and in/out flag
            LogType logType = determineLogType(dateTime, inOutFlag == 0);
            
            AttendanceLog log = new AttendanceLog(accountNumber, dateTime, logType);
            log.setRawData(logLine);
            
            return log;
            
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse biometric log: " + logLine, e);
        }
    }
    
    // Determine log type based on time and whether it's an entry (true) or exit (false)
    private static LogType determineLogType(LocalDateTime dateTime, boolean isEntry) {
        int hour = dateTime.getHour();
        
        if (hour < 12) { // Morning (AM)
            return isEntry ? LogType.TIME_IN_AM : LogType.TIME_OUT_AM;
        } else { // Afternoon/Evening (PM)
            return isEntry ? LogType.TIME_IN_PM : LogType.TIME_OUT_PM;
        }
    }
    
    // Method to export back to biometric format
    public String toBiometricFormat() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        
        // Determine the in/out flag based on log type
        int inOutFlag = (logType == LogType.TIME_IN_AM || logType == LogType.TIME_IN_PM) ? 0 : 1;
        
        return String.format("%s\t%s\t1\t%d\t1\t0", 
            accountNumber, 
            logDateTime.format(formatter),
            inOutFlag);
    }
    
    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }
    
    public LocalDateTime getLogDateTime() { return logDateTime; }
    public void setLogDateTime(LocalDateTime logDateTime) { this.logDateTime = logDateTime; }
    
    public LogType getLogType() { return logType; }
    public void setLogType(LogType logType) { this.logType = logType; }
    
    public String getRawData() { return rawData; }
    public void setRawData(String rawData) { this.rawData = rawData; }
    
    public boolean isProcessed() { return isProcessed; }
    public void setProcessed(boolean processed) { isProcessed = processed; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public String getImportedBy() { return importedBy; }
    public void setImportedBy(String importedBy) { this.importedBy = importedBy; }
    
    public String getImportBatch() { return importBatch; }
    public void setImportBatch(String importBatch) { this.importBatch = importBatch; }
    
    // Formatted getters for display
    public String getFormattedDateTime() {
        return logDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
    
    public String getFormattedDate() {
        return logDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }
    
    public String getFormattedTime() {
        return logDateTime.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    }
    
    @Override
    public String toString() {
        return String.format("AttendanceLog{id=%d, accountNumber='%s', dateTime=%s, logType=%s}", 
            id, accountNumber, getFormattedDateTime(), logType);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AttendanceLog)) return false;
        AttendanceLog that = (AttendanceLog) o;
        return accountNumber.equals(that.accountNumber) && 
               logDateTime.equals(that.logDateTime) && 
               logType == that.logType;
    }
    
    @Override
    public int hashCode() {
        return java.util.Objects.hash(accountNumber, logDateTime, logType);
    }
}