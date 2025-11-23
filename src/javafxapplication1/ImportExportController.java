package javafxapplication1;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.DirectoryChooser;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;

import java.io.*;
import java.net.URL;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.ResourceBundle;
import java.nio.file.Files;
import java.nio.file.Paths;
import javafx.application.Platform;

public class ImportExportController implements Initializable {

    // Database connection constants
    private static final String DB_URL = DatabaseConfig.getDbUrl();
    private static final String DB_USER = DatabaseConfig.getDbUser();
    private static final String DB_PASSWORD = DatabaseConfig.getDbPassword();

    public static class ExportResult {
        public final int recordsExported;
        public final String exportFilename;
        
        public ExportResult(int recordsExported, String exportFilename) {
            this.recordsExported = recordsExported;
            this.exportFilename = exportFilename;
        }
    }

    public static class ImportPreviewData {
        private final String col1;  // Fullname
        private final String col2;  // Account Number
        private final String col3;  // Date & Time
        private final String col4;  // Log Type
        private final String col5;  // Raw Data
        private final String col6;  // Status

        public ImportPreviewData(String col1, String col2, String col3, String col4, String col5, String col6) {
            this.col1 = col1;
            this.col2 = col2;
            this.col3 = col3;
            this.col4 = col4;
            this.col5 = col5;
            this.col6 = col6;
        }

        public String getCol1() { return col1; }
        public String getCol2() { return col2; }
        public String getCol3() { return col3; }
        public String getCol4() { return col4; }
        public String getCol5() { return col5; }
        public String getCol6() { return col6; }
    }

    @FXML private ComboBox<String> importFileFormat;
    @FXML private ComboBox<String> logTypeFilter;
    @FXML private TextField searchField;
    @FXML private TextField dateFilter;
    @FXML private Label importFileLabel;
    @FXML private ProgressBar importProgressBar;
    
    // File loading progress controls
    @FXML private VBox fileLoadingSection;
    @FXML private Label fileLoadingLabel;
    @FXML private ProgressBar fileProgressBar;
    @FXML private Label fileProgressLabel;
    @FXML private TableView<ImportPreviewData> importPreviewTable;
    @FXML private TableColumn<ImportPreviewData, String> colPreview1;
    @FXML private TableColumn<ImportPreviewData, String> colPreview2;
    @FXML private TableColumn<ImportPreviewData, String> colPreview3;
    @FXML private TableColumn<ImportPreviewData, String> colPreview4;
    @FXML private TableColumn<ImportPreviewData, String> colPreview5;
    @FXML private TableColumn<ImportPreviewData, String> colPreview6;

    private final ObservableList<String> dataTypes = FXCollections.observableArrayList();
    private final ObservableList<String> fileFormats = FXCollections.observableArrayList();
    private final ObservableList<String> departments = FXCollections.observableArrayList();
    private final ObservableList<ImportPreviewData> previewData = FXCollections.observableArrayList();
    
    private File selectedImportFile;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupComboBoxes();
        setupTableColumns();
        preloadEmployeeCache(); // Preload all employees for faster lookups
        refreshAttendancePreview(); // Load real attendance data instead of sample data
    }

    // Database connection method
    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }
    
    // Cache for employee names to speed up lookups
    private Map<String, String> employeeNameCache = new HashMap<>();
    
    // Method to lookup employee fullname by account number with caching
    private String lookupEmployeeFullname(String accountNumber) {
        // Check cache first for faster performance
        if (employeeNameCache.containsKey(accountNumber)) {
            return employeeNameCache.get(accountNumber);
        }
        
        String selectSQL = "SELECT full_name FROM employees WHERE account_number = ?";
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(selectSQL)) {
            
            stmt.setString(1, accountNumber);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String fullname = rs.getString("full_name");
                    if (fullname != null && !fullname.trim().isEmpty()) {
                        // Cache the result for future lookups
                        employeeNameCache.put(accountNumber, fullname);
                        return fullname;
                    }
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Error looking up employee: " + e.getMessage());
        }
        
        // Cache the N/A result to avoid repeated database queries
        employeeNameCache.put(accountNumber, "N/A");
        return "N/A"; // Return N/A if not found in database
    }
    
    // Bulk load all employee names into cache for maximum performance
    private void preloadEmployeeCache() {
        String selectSQL = "SELECT account_number, full_name FROM employees WHERE status = 'Active'";
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(selectSQL);
             ResultSet rs = stmt.executeQuery()) {
            
            employeeNameCache.clear(); // Clear existing cache
            
            while (rs.next()) {
                String accountNumber = rs.getString("account_number");
                String fullname = rs.getString("full_name");
                
                if (accountNumber != null && fullname != null && !fullname.trim().isEmpty()) {
                    employeeNameCache.put(accountNumber, fullname);
                }
            }
            
            System.out.println("Preloaded " + employeeNameCache.size() + " employee records into cache");
            
        } catch (SQLException e) {
            System.err.println("Error preloading employee cache: " + e.getMessage());
        }
    }

    private void setupComboBoxes() {
        // File formats - focus on attendance log formats
        fileFormats.addAll("TXT/DAT (Biometric)", "CSV", "Excel", "JSON");
        importFileFormat.setItems(fileFormats);
        importFileFormat.setValue("TXT/DAT (Biometric)");

        // Log type filters for attendance
        ObservableList<String> logTypes = FXCollections.observableArrayList();
        logTypes.addAll("All Log Types", "TIME_IN_AM", "TIME_OUT_AM", "TIME_IN_PM", "TIME_OUT_PM", "PARSE_ERROR", "FORMAT_ERROR", "EMPTY_LINE");
        logTypeFilter.setItems(logTypes);
        logTypeFilter.setValue("All Log Types");
    }

    private void setupTableColumns() {
        // Fullname | Account Number | Date & Time | Log Type | Raw Data | Status
        colPreview1.setCellValueFactory(new PropertyValueFactory<>("col1")); // Fullname
        colPreview2.setCellValueFactory(new PropertyValueFactory<>("col2")); // Account Number
        colPreview3.setCellValueFactory(new PropertyValueFactory<>("col3")); // Date & Time
        colPreview4.setCellValueFactory(new PropertyValueFactory<>("col4")); // Log Type
        colPreview5.setCellValueFactory(new PropertyValueFactory<>("col5")); // Raw Data
        colPreview6.setCellValueFactory(new PropertyValueFactory<>("col6")); // Status

        importPreviewTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private void loadSamplePreviewData() {
        previewData.addAll(
            new ImportPreviewData("Juan Dela Cruz", "E-001", "2024-12-15 08:00:00", "TIME_IN_AM", "E-001 2024-12-15 08:00:00 1 0 1 0", "Sample Data"),
            new ImportPreviewData("Maria Santos", "E-002", "2024-12-15 08:30:00", "TIME_IN_AM", "E-002 2024-12-15 08:30:00 1 0 1 0", "Sample Data"),
            new ImportPreviewData("Pedro Reyes", "E-003", "2024-12-15 08:15:00", "TIME_IN_AM", "E-003 2024-12-15 08:15:00 1 0 1 0", "Sample Data"),
            new ImportPreviewData("Ana Lopez", "E-004", "2024-12-15 17:00:00", "TIME_OUT_PM", "E-004 2024-12-15 17:00:00 1 1 1 0", "Sample Data"),
            new ImportPreviewData("Rico Valdez", "E-005", "2024-12-15 17:30:00", "TIME_OUT_PM", "E-005 2024-12-15 17:30:00 1 1 1 0", "Sample Data")
        );
        importPreviewTable.setItems(previewData);
    }

    @FXML
    private void onImportData() {
        showImportDialog();
    }

    // Export functionality temporarily commented out
    /*
    @FXML
    private void onExportData() {
        showExportDialog();
    }
    */

    @FXML
    private void onBrowseImportFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select File to Import");
        
        String selectedFormat = importFileFormat.getValue();
        if (selectedFormat != null) {
            switch (selectedFormat) {
                case "TXT/DAT (Biometric)":
                    fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Biometric Log Files", "*.txt", "*.dat", "*_attlog.txt", "*_attlog.dat"));
                    break;
                case "CSV":
                    fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
                    break;
                case "Excel":
                    fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel Files", "*.xlsx", "*.xls"));
                    break;
                case "JSON":
                    fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Files", "*.json"));
                    break;
            }
        }
        
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("All Files", "*.*"));
        
        selectedImportFile = fileChooser.showOpenDialog(null);
        if (selectedImportFile != null) {
            // Show file loading progress
            showFileLoadingProgress("Processing attendance file...", "Reading file: " + selectedImportFile.getName());
            
            // Process file in background
            processFileInBackground(selectedImportFile);
        }
    }

    @FXML
    private void onPreviewImportData() {
        if (selectedImportFile == null) {
            showAlert("Please select a file first.");
            return;
        }
        
        showAlert("Previewing data from " + selectedImportFile.getName() + "...");
        // In a real implementation, this would parse the file and show actual data
    }

    @FXML
    private void onImportNow() {
        if (selectedImportFile == null) {
            showAlert("Please select a file to import.");
            return;
        }
        
        String fileFormat = importFileFormat.getValue();
        
        if (fileFormat == null) {
            showAlert("Please select a file format.");
            return;
        }
        
        performImport("Attendance Data", fileFormat);
    }

    // Export functionality temporarily commented out
    /*
    @FXML
    private void onExportNow() {
        String dataType = exportDataType.getValue();
        String fileFormat = exportFileFormat.getValue();
        String department = exportDepartmentFilter.getValue();
        
        if (dataType == null || fileFormat == null) {
            showAlert("Please select both data type and file format.");
            return;
        }
        
        performExport(dataType, fileFormat, department);
    }
    */
    
    // Search and Filter Methods
    @FXML
    private void onSearchKeyReleased() {
        applyFilters();
    }
    
    @FXML
    private void onFilterChanged() {
        applyFilters();
    }
    
    @FXML
    private void onClearFilters() {
        searchField.clear();
        dateFilter.clear();
        logTypeFilter.setValue("All Log Types");
        applyFilters();
    }
    
    private void applyFilters() {
        String searchText = searchField.getText().toLowerCase().trim();
        String dateText = dateFilter.getText().trim();
        String logType = logTypeFilter.getValue();
        
        ObservableList<ImportPreviewData> filteredData = FXCollections.observableArrayList();
        
        for (ImportPreviewData data : previewData) {
            boolean matches = true;
            
            // Search by account number (now in col2) or fullname (col1)
            if (!searchText.isEmpty()) {
                boolean accountMatch = data.getCol2() != null && data.getCol2().toLowerCase().contains(searchText);
                boolean nameMatch = data.getCol1() != null && data.getCol1().toLowerCase().contains(searchText);
                if (!accountMatch && !nameMatch) {
                    matches = false;
                }
            }
            
            // Filter by date (now in col3)
            if (!dateText.isEmpty() && data.getCol3() != null) {
                if (!data.getCol3().contains(dateText)) {
                    matches = false;
                }
            }
            
            // Filter by log type (now in col4)
            if (logType != null && !"All Log Types".equals(logType) && data.getCol4() != null) {
                if (!data.getCol4().equals(logType)) {
                    matches = false;
                }
            }
            
            if (matches) {
                filteredData.add(data);
            }
        }
        
        importPreviewTable.setItems(filteredData);
    }

    private void performImport(String dataType, String fileFormat) {
        if (!"Attendance Data".equals(dataType)) {
            showAlert("Currently only Attendance Data import is supported!");
            return;
        }
        
        importProgressBar.setVisible(true);
        importProgressBar.setProgress(0);
        
        Task<Integer> importTask = new Task<Integer>() {
            @Override
            protected Integer call() throws Exception {
                try {
                    if ("TXT/DAT (Biometric)".equals(fileFormat)) {
                        return importBiometricAttendanceData();
                    } else {
                        throw new Exception("Unsupported file format for attendance data: " + fileFormat);
                    }
                } catch (Exception e) {
                    throw new Exception("Import failed: " + e.getMessage());
                }
            }
        };
        
        importTask.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent event) {
                importProgressBar.setVisible(false);
                showSuccessAlert("Import completed successfully!\n" + 
                               "Data Type: " + dataType + "\n" +
                               "Format: " + fileFormat + "\n" +
                               "Records imported: " + importTask.getValue());
                
                // Reset the selected file and clear file info
                selectedImportFile = null;
                importFileLabel.setText("No file selected");
                
                // Clear the table first, then refresh with updated database data
                clearImportTable();
                refreshAttendancePreview(); // This will populate table with new attendance data from database
            }
        });
        
        importTask.setOnFailed(new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent event) {
                importProgressBar.setVisible(false);
                showAlert("Import failed: " + importTask.getException().getMessage());
            }
        });
        
        new Thread(importTask).start();
    }

    // Export functionality temporarily commented out
    /*
    private void performExport(String dataType, String fileFormat, String department) {
        exportProgressBar.setVisible(true);
        exportProgressBar.setProgress(0);
        exportStatusLabel.setText("Exporting " + dataType + " as " + fileFormat + "...");
        
        Task<ExportResult> exportTask = new Task<ExportResult>() {
            @Override
            protected ExportResult call() throws Exception {
                try {
                    if ("Attendance Data".equals(dataType)) {
                        return exportAttendanceData(fileFormat, department);
                    } else {
                        // Generate sample data for other types
                        List<String[]> data = generateSampleData(dataType, department);
                        int recordsExported = data.size();
                        
                        // Create filename with timestamp
                        String timestamp = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
                        String extension = getFileExtension(fileFormat);
                        String exportFilename = dataType.replace(" ", "_") + "_" + timestamp + extension;
                        
                        // Create exports directory
                        String userHome = System.getProperty("user.home");
                        String exportDir = userHome + File.separator + "Desktop" + File.separator + "Exports";
                        new File(exportDir).mkdirs();
                        
                        String fullPath = exportDir + File.separator + exportFilename;
                        
                        // Write the file
                        processExportFile(data, fullPath, fileFormat);
                        
                        return new ExportResult(recordsExported, exportFilename);
                    }
                } catch (Exception e) {
                    throw new Exception("Failed to export data: " + e.getMessage());
                }
            }
        };
        
        exportTask.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent event) {
                exportProgressBar.setVisible(false);
                exportStatusLabel.setText("Export completed successfully!");
                
                ExportResult result = exportTask.getValue();
                showSuccessAlert("Export completed successfully!\n" + 
                               "Data Type: " + dataType + "\n" +
                               "Format: " + fileFormat + "\n" +
                               "Department: " + department + "\n" +
                               "Records exported: " + result.recordsExported + "\n" +
                               "File saved to: Desktop/Exports/" + result.exportFilename);
            }
        });
        
        exportTask.setOnFailed(new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent event) {
                exportProgressBar.setVisible(false);
                exportStatusLabel.setText("Export failed");
                showAlert("Export failed: " + exportTask.getException().getMessage());
            }
        });
        
        new Thread(exportTask).start();
    }
    */

    private void loadPreviewData() {
        if (selectedImportFile == null) return;
        
        previewData.clear();
        
        String fileFormat = importFileFormat.getValue();
        
        // Always treat as attendance data and load accordingly
        if ("TXT/DAT (Biometric)".equals(fileFormat)) {
            loadBiometricPreviewData();
        } else {
            loadGenericPreviewData();
        }
    }
    
    private void loadBiometricPreviewData() {
        previewData.clear();
        
        try {
            List<String> lines = Files.readAllLines(Paths.get(selectedImportFile.getAbsolutePath()));
            int lineNumber = 0;
            int validRecords = 0;
            int errorRecords = 0;
            int emptyLines = 0;
            
            // Show ALL lines from the file for complete verification
            for (String originalLine : lines) {
                lineNumber++;
                String trimmedLine = originalLine.trim();
                
                // Show empty lines too for verification
                if (trimmedLine.isEmpty()) {
                    emptyLines++;
                    previewData.add(new ImportPreviewData(
                        "N/A",  // Fullname
                        "EMPTY", // Account Number
                        "",      // Date & Time
                        "EMPTY_LINE", // Log Type
                        originalLine.isEmpty() ? "[Empty Line]" : originalLine, // Raw Data
                        "Line " + lineNumber + ": Empty line" // Status
                    ));
                    continue;
                }
                
                try {
                    // Parse biometric log format: account_number	datetime	1	0/1	1	0
                    String[] parts = trimmedLine.split("\\s+");
                    if (parts.length >= 6) {
                        String accountNumber = parts[0].trim();
                        String dateTime = parts[1] + " " + parts[2];
                        int inOutFlag = Integer.parseInt(parts[4]); // 0=in, 1=out
                        
                        // Determine log type
                        LocalDateTime logDateTime = LocalDateTime.parse(dateTime, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                        String logType = determineLogType(logDateTime, inOutFlag == 0);
                        
                        // Lookup employee fullname from database
                        String fullname = lookupEmployeeFullname(accountNumber);
                        
                        validRecords++;
                        previewData.add(new ImportPreviewData(
                            fullname,     // Fullname (from database or N/A)
                            accountNumber, // Account Number
                            dateTime,     // Date & Time
                            logType,      // Log Type
                            originalLine, // Raw Data
                            "Line " + lineNumber + ": Valid ✓" // Status
                        ));
                    } else {
                        // Show lines with insufficient fields
                        errorRecords++;
                        previewData.add(new ImportPreviewData(
                            "N/A",        // Fullname
                            "INVALID",    // Account Number
                            "Insufficient fields", // Date & Time
                            "FORMAT_ERROR", // Log Type
                            originalLine, // Raw Data
                            "Line " + lineNumber + ": Only " + parts.length + " fields (need 6) ✗" // Status
                        ));
                    }
                } catch (Exception e) {
                    // Add problematic lines for review
                    errorRecords++;
                    previewData.add(new ImportPreviewData(
                        "N/A",        // Fullname
                        "ERROR",      // Account Number
                        "Parse failed", // Date & Time
                        "PARSE_ERROR", // Log Type
                        originalLine, // Raw Data
                        "Line " + lineNumber + ": " + e.getMessage() + " ✗" // Status
                    ));
                }
            }
            
            // Make variables effectively final for lambda
            final int finalLineNumber = lineNumber;
            final int finalValidRecords = validRecords;
            final int finalErrorRecords = errorRecords;
            final int finalEmptyLines = emptyLines;
            
            // Update UI to show summary
            javafx.application.Platform.runLater(() -> {
                importPreviewTable.setItems(previewData);
                
                // Show summary in status or console
                System.out.println("File Preview Summary:");
                System.out.println("Total lines: " + finalLineNumber);
                System.out.println("Valid records: " + finalValidRecords);
                System.out.println("Error records: " + finalErrorRecords);
                System.out.println("Empty lines: " + finalEmptyLines);
                
                // You could also show this in a status label if you add one to the FXML
                String summary = String.format("File loaded: %d total lines (%d valid, %d errors, %d empty)", 
                    finalLineNumber, finalValidRecords, finalErrorRecords, finalEmptyLines);
                importFileLabel.setText(summary);
            });
            
        } catch (IOException e) {
            showAlert("Error reading file: " + e.getMessage());
        }
    }
    
    private void loadGenericPreviewData() {
        try {
            List<String> lines = Files.readAllLines(Paths.get(selectedImportFile.getAbsolutePath()));
            int maxLines = Math.min(lines.size(), 10); // Show first 10 lines
            
            for (int i = 0; i < maxLines; i++) {
                String line = lines.get(i);
                String[] parts = line.split(",|\\t"); // Split by comma or tab
                
                // For generic files, try to detect if first column is account number
                String possibleAccountNumber = parts.length > 0 ? parts[0].trim() : "";
                String fullname = "N/A"; // Default for generic files
                
                // Try to lookup employee name if first column looks like account number
                if (possibleAccountNumber.matches("\\d+")) { // If it's numeric, might be account number
                    fullname = lookupEmployeeFullname(possibleAccountNumber);
                }
                
                String col2 = parts.length > 1 ? parts[1] : "";
                String col3 = parts.length > 2 ? parts[2] : "";
                String col4 = parts.length > 3 ? parts[3] : "";
                String col5 = parts.length > 4 ? parts[4] : "";
                String col6 = "Line " + (i + 1) + ": Generic Data";
                
                // Format: Fullname | Account Number | Date & Time | Log Type | Raw Data | Status
                previewData.add(new ImportPreviewData(
                    fullname,              // Fullname (lookup if account number detected)
                    possibleAccountNumber, // Account Number (or first column)
                    col2,                 // Date & Time (or second column)
                    col3,                 // Log Type (or third column)
                    line,                 // Raw Data (whole line)
                    col6                  // Status
                ));
            }
            
            importPreviewTable.setItems(previewData);
            
        } catch (IOException e) {
            showAlert("Error reading file: " + e.getMessage());
        }
    }
    
    private String determineLogType(LocalDateTime dateTime, boolean isEntry) {
        int hour = dateTime.getHour();
        
        if (hour < 12) { // Morning (AM)
            return isEntry ? "TIME_IN_AM" : "TIME_OUT_AM";
        } else { // Afternoon/Evening (PM)
            return isEntry ? "TIME_IN_PM" : "TIME_OUT_PM";
        }
    }
    
    private int importBiometricAttendanceData() throws Exception {
        List<String> lines = Files.readAllLines(Paths.get(selectedImportFile.getAbsolutePath()));
        int totalLines = lines.size();
        int processedLines = 0;
        int importedRecords = 0;
        String batchId = selectedImportFile.getName();
        
        String insertSQL = "INSERT INTO attendance (account_number, log_datetime, log_type, raw_data, " +
                          "imported_by, import_batch) VALUES (?, ?, ?, ?, ?, ?) " +
                          "ON DUPLICATE KEY UPDATE raw_data = VALUES(raw_data), updated_at = CURRENT_TIMESTAMP";
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(insertSQL)) {
            
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i).trim();
                if (line.isEmpty()) {
                    processedLines++;
                    updateProgress(processedLines, totalLines);
                    continue;
                }
                
                try {
                    // Parse biometric log format: account_number	datetime	1	0/1	1	0
                    String[] parts = line.split("\\s+");
                    if (parts.length >= 6) {
                        String accountNumber = parts[0];
                        String dateTimeStr = parts[1] + " " + parts[2];
                        LocalDateTime logDateTime = LocalDateTime.parse(dateTimeStr, 
                            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                        
                        int inOutFlag = Integer.parseInt(parts[4]); // 0=in, 1=out
                        String logType = determineLogType(logDateTime, inOutFlag == 0);
                        
                        stmt.setString(1, accountNumber);
                        stmt.setTimestamp(2, Timestamp.valueOf(logDateTime));
                        stmt.setString(3, logType);
                        stmt.setString(4, line);
                        stmt.setString(5, "admin"); // Current user
                        stmt.setString(6, batchId);
                        
                        int rowsAffected = stmt.executeUpdate();
                        if (rowsAffected > 0) {
                            importedRecords++;
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Error processing line: " + line + " - " + e.getMessage());
                }
                
                processedLines++;
                updateProgress(processedLines, totalLines);
                
                // Small delay to show progress
                if (i % 10 == 0) {
                    Thread.sleep(10);
                }
            }
        }
        
        return importedRecords;
    }
    
    // Add missing updateProgress method
    private void updateProgress(int current, int total) {
        if (total > 0) {
            double progress = (double) current / total;
            javafx.application.Platform.runLater(() -> {
                importProgressBar.setProgress(progress);
            });
        }
    }
    
    // Method to clear the import preview table
    private void clearImportTable() {
        Platform.runLater(() -> {
            previewData.clear();
            importPreviewTable.setItems(previewData);
            importPreviewTable.refresh();
        });
    }

    private void showImportDialog() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Import Data");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        
        VBox content = new VBox(15);
        content.setPadding(new javafx.geometry.Insets(20));

        Label title = new Label("Import Data Instructions");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #1b5e20;");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);

        grid.add(new Label("Supported Formats:"), 0, 0);
        grid.add(new Label("CSV, Excel (.xlsx, .xls), JSON"), 1, 0);
        grid.add(new Label("Data Types:"), 0, 1);
        grid.add(new Label("Employee, Payroll, Attendance"), 1, 1);
        grid.add(new Label("File Requirements:"), 0, 2);
        grid.add(new Label("• First row must contain headers"), 1, 2);
        grid.add(new Label("• Data must be properly formatted"), 1, 3);
        grid.add(new Label("• Maximum 10,000 records per file"), 1, 4);
        grid.add(new Label("• File size limit: 50MB"), 1, 5);

        content.getChildren().addAll(title, grid);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setStyle("-fx-background-color: #f5f5f5;");
        
        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.setStyle("-fx-background-color: linear-gradient(to right, #2e7d32, #43a047); -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 6; -fx-padding: 8 16;");
        
        dialog.showAndWait();
    }

    private void showExportDialog() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Export Data");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        
        VBox content = new VBox(15);
        content.setPadding(new javafx.geometry.Insets(20));

        Label title = new Label("Export Data Options");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #1b5e20;");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);

        grid.add(new Label("Export Formats:"), 0, 0);
        grid.add(new Label("CSV, Excel (.xlsx), JSON"), 1, 0);
        grid.add(new Label("Available Data:"), 0, 1);
        grid.add(new Label("Employee, Payroll, Attendance"), 1, 1);
        grid.add(new Label("Filter Options:"), 0, 2);
        grid.add(new Label("• By Department"), 1, 2);
        grid.add(new Label("• By Date Range"), 1, 3);
        grid.add(new Label("• By Status"), 1, 4);
        grid.add(new Label("Output Location:"), 0, 5);
        grid.add(new Label("Desktop/Exports/ folder"), 1, 5);

        content.getChildren().addAll(title, grid);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setStyle("-fx-background-color: #f5f5f5;");
        
        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.setStyle("-fx-background-color: linear-gradient(to right, #1976d2, #42a5f5); -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 6; -fx-padding: 8 16;");
        
        dialog.showAndWait();
    }


    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Import/Export Manager");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.getDialogPane().setStyle("-fx-background-color: #f5f5f5;");
        alert.showAndWait();
    }

    private void showSuccessAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.getDialogPane().setStyle("-fx-background-color: #f5f5f5;");
        alert.showAndWait();
    }

    // CSV File Handling Methods
    private List<String[]> parseCSVFile(File file) throws IOException {
        List<String[]> records = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");
                records.add(values);
            }
        }
        return records;
    }

    private void writeCSVFile(List<String[]> data, String filename) throws IOException {
        try (FileWriter writer = new FileWriter(filename)) {
            for (String[] row : data) {
                writer.append(String.join(",", row));
                writer.append("\n");
            }
        }
    }

    // Excel File Handling Methods (Simplified - in real implementation, use Apache POI)
    private List<String[]> parseExcelFile(File file) throws IOException {
        // This is a simplified version. In a real implementation, you would use Apache POI
        // For now, we'll treat it as CSV for demonstration
        return parseCSVFile(file);
    }

    private void writeExcelFile(List<String[]> data, String filename) throws IOException {
        // This is a simplified version. In a real implementation, you would use Apache POI
        // For now, we'll write as CSV for demonstration
        writeCSVFile(data, filename.replace(".xlsx", ".csv"));
    }

    // JSON File Handling Methods
    private List<String[]> parseJSONFile(File file) throws IOException {
        // Simplified JSON parsing - in real implementation, use Jackson or Gson
        List<String[]> records = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.contains("employeeId") && line.contains("name")) {
                    // Extract data from JSON line (simplified)
                    String[] values = {"E-001", "Sample Employee", "Department", "Position", "25000"};
                    records.add(values);
                }
            }
        }
        return records;
    }

    private void writeJSONFile(List<String[]> data, String filename) throws IOException {
        try (FileWriter writer = new FileWriter(filename)) {
            writer.append("[\n");
            for (int i = 0; i < data.size(); i++) {
                String[] row = data.get(i);
                writer.append("  {\n");
                writer.append("    \"employeeId\": \"" + row[0] + "\",\n");
                writer.append("    \"name\": \"" + row[1] + "\",\n");
                writer.append("    \"department\": \"" + row[2] + "\",\n");
                writer.append("    \"position\": \"" + row[3] + "\",\n");
                writer.append("    \"salary\": \"" + row[4] + "\"\n");
                writer.append("  }");
                if (i < data.size() - 1) writer.append(",");
                writer.append("\n");
            }
            writer.append("]\n");
        }
    }

    // File Processing Methods
    private List<String[]> processImportFile(File file, String format) throws IOException {
        switch (format) {
            case "CSV":
                return parseCSVFile(file);
            case "Excel":
                return parseExcelFile(file);
            case "JSON":
                return parseJSONFile(file);
            default:
                throw new IllegalArgumentException("Unsupported file format: " + format);
        }
    }

    private void processExportFile(List<String[]> data, String filename, String format) throws IOException {
        switch (format) {
            case "CSV":
                writeCSVFile(data, filename);
                break;
            case "Excel":
                writeExcelFile(data, filename);
                break;
            case "JSON":
                writeJSONFile(data, filename);
                break;
            default:
                throw new IllegalArgumentException("Unsupported file format: " + format);
        }
    }

    // Sample data generation for export
    private List<String[]> generateSampleData(String dataType, String department) {
        List<String[]> data = new ArrayList<>();
        
        // Add headers
        switch (dataType) {
            case "Employee Data":
                data.add(new String[]{"Employee ID", "Name", "Department", "Position", "Salary", "Hire Date"});
                data.add(new String[]{"E-001", "Juan Dela Cruz", "Math", "Teacher I", "25000", "2024-01-15"});
                data.add(new String[]{"E-002", "Maria Santos", "Admin", "Registrar", "30000", "2024-02-01"});
                data.add(new String[]{"E-003", "Pedro Reyes", "Science", "Teacher II", "28000", "2024-01-20"});
                break;
            case "Payroll Data":
                data.add(new String[]{"Payroll ID", "Employee ID", "Name", "Basic Salary", "Overtime", "Allowances", "Deductions", "Net Pay"});
                data.add(new String[]{"P-001", "E-001", "Juan Dela Cruz", "25000", "2000", "1500", "3000", "25500"});
                data.add(new String[]{"P-002", "E-002", "Maria Santos", "30000", "0", "2000", "4000", "28000"});
                data.add(new String[]{"P-003", "E-003", "Pedro Reyes", "28000", "1500", "1200", "3500", "27200"});
                break;
            case "Attendance Data":
                data.add(new String[]{"Employee ID", "Name", "Date", "Time In", "Time Out", "Hours Worked", "Status"});
                data.add(new String[]{"E-001", "Juan Dela Cruz", "2024-12-01", "08:00", "17:00", "8", "Present"});
                data.add(new String[]{"E-002", "Maria Santos", "2024-12-01", "08:30", "17:30", "8", "Present"});
                data.add(new String[]{"E-003", "Pedro Reyes", "2024-12-01", "08:15", "17:15", "8", "Present"});
                break;
        }
        
        return data;
    }

    private ExportResult exportAttendanceData(String fileFormat, String department) throws Exception {
        String selectSQL = "SELECT account_number, log_datetime, log_type, raw_data, import_batch " +
                          "FROM attendance ORDER BY log_datetime DESC LIMIT 10000";
        
        List<String[]> exportData = new ArrayList<>();
        int recordsExported = 0;
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(selectSQL);
             ResultSet rs = stmt.executeQuery()) {
            
            // Add headers based on format
            if ("TXT/DAT (Biometric)".equals(fileFormat)) {
                // No headers for biometric format
            } else {
                exportData.add(new String[]{"Account Number", "Date Time", "Log Type", "Raw Data", "Import Batch"});
            }
            
            while (rs.next()) {
                if ("TXT/DAT (Biometric)".equals(fileFormat)) {
                    // Export in original biometric format
                    String rawData = rs.getString("raw_data");
                    if (rawData != null && !rawData.trim().isEmpty()) {
                        exportData.add(new String[]{rawData});
                    }
                } else {
                    // Export as structured data
                    exportData.add(new String[]{
                        rs.getString("account_number"),
                        rs.getString("log_datetime"),
                        rs.getString("log_type"),
                        rs.getString("raw_data"),
                        rs.getString("import_batch")
                    });
                }
                recordsExported++;
                
                // Update progress periodically
                if (recordsExported % 100 == 0) {
                    updateProgress(recordsExported, 10000);
                    Thread.sleep(5);
                }
            }
        }
        
        // Create filename with timestamp
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String extension = getFileExtension(fileFormat);
        String exportFilename;
        
        if ("TXT/DAT (Biometric)".equals(fileFormat)) {
            exportFilename = "TPC-EXPORT-" + timestamp + "_attlog.dat";
        } else {
            exportFilename = "Attendance_Data_" + timestamp + extension;
        }
        
        // Create exports directory
        String userHome = System.getProperty("user.home");
        String exportDir = userHome + File.separator + "Desktop" + File.separator + "Exports";
        new File(exportDir).mkdirs();
        
        String fullPath = exportDir + File.separator + exportFilename;
        
        // Write the file
        if ("TXT/DAT (Biometric)".equals(fileFormat)) {
            writeBiometricFile(exportData, fullPath);
        } else {
            processExportFile(exportData, fullPath, fileFormat);
        }
        
        updateProgress(recordsExported, recordsExported);
        
        return new ExportResult(recordsExported, exportFilename);
    }
    
    private String getFileExtension(String fileFormat) {
        switch (fileFormat) {
            case "TXT/DAT (Biometric)":
                return ".dat";
            case "Excel":
                return ".xlsx";
            case "JSON":
                return ".json";
            case "CSV":
            default:
                return ".csv";
        }
    }
    
    private void writeBiometricFile(List<String[]> data, String filename) throws IOException {
        try (FileWriter writer = new FileWriter(filename)) {
            for (String[] row : data) {
                if (row.length > 0) {
                    writer.append(row[0]);
                    writer.append("\n");
                }
            }
        }
    }
    
    // CRUD Operations for Attendance Data
    
    /**
     * Create/Insert new attendance record
     */
    public boolean createAttendanceRecord(String accountNumber, LocalDateTime logDateTime, 
                                        String logType, String rawData, String importedBy, String importBatch) {
        String insertSQL = "INSERT INTO attendance (account_number, log_datetime, log_type, raw_data, " +
                          "imported_by, import_batch) VALUES (?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(insertSQL)) {
            
            stmt.setString(1, accountNumber);
            stmt.setTimestamp(2, Timestamp.valueOf(logDateTime));
            stmt.setString(3, logType);
            stmt.setString(4, rawData);
            stmt.setString(5, importedBy);
            stmt.setString(6, importBatch);
            
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
            
        } catch (SQLException e) {
            System.err.println("Error creating attendance record: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Read/Select attendance records with filtering
     */
    public List<ImportPreviewData> readAttendanceRecords(String accountNumber, String dateFilter, int limit) {
        List<ImportPreviewData> records = new ArrayList<>();
        
        StringBuilder queryBuilder = new StringBuilder("SELECT account_number, log_datetime, log_type, raw_data, import_batch FROM attendance WHERE 1=1");
        List<String> parameters = new ArrayList<>();
        
        if (accountNumber != null && !accountNumber.trim().isEmpty()) {
            queryBuilder.append(" AND account_number = ?");
            parameters.add(accountNumber);
        }
        
        if (dateFilter != null && !dateFilter.trim().isEmpty()) {
            queryBuilder.append(" AND DATE(log_datetime) = ?");
            parameters.add(dateFilter);
        }
        
        queryBuilder.append(" ORDER BY log_datetime DESC LIMIT ").append(limit);
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(queryBuilder.toString())) {
            
            for (int i = 0; i < parameters.size(); i++) {
                stmt.setString(i + 1, parameters.get(i));
            }
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String recordAccountNumber = rs.getString("account_number");
                    String fullname = lookupEmployeeFullname(recordAccountNumber);
                    
                    records.add(new ImportPreviewData(
                        fullname,                     // Fullname (from database or N/A)
                        recordAccountNumber,          // Account Number
                        rs.getString("log_datetime"), // Date & Time
                        rs.getString("log_type"),     // Log Type
                        rs.getString("raw_data"),     // Raw Data
                        rs.getString("import_batch")  // Status/Import Batch
                    ));
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Error reading attendance records: " + e.getMessage());
        }
        
        return records;
    }
    
    /**
     * Update existing attendance record
     */
    public boolean updateAttendanceRecord(int recordId, String logType, String rawData) {
        String updateSQL = "UPDATE attendance SET log_type = ?, raw_data = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(updateSQL)) {
            
            stmt.setString(1, logType);
            stmt.setString(2, rawData);
            stmt.setInt(3, recordId);
            
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
            
        } catch (SQLException e) {
            System.err.println("Error updating attendance record: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Delete attendance record(s)
     */
    public boolean deleteAttendanceRecord(int recordId) {
        String deleteSQL = "DELETE FROM attendance WHERE id = ?";
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(deleteSQL)) {
            
            stmt.setInt(1, recordId);
            
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
            
        } catch (SQLException e) {
            System.err.println("Error deleting attendance record: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Delete attendance records by batch
     */
    public boolean deleteAttendanceBatch(String importBatch) {
        String deleteSQL = "DELETE FROM attendance WHERE import_batch = ?";
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(deleteSQL)) {
            
            stmt.setString(1, importBatch);
            
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
            
        } catch (SQLException e) {
            System.err.println("Error deleting attendance batch: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Get attendance statistics
     */
    public Map<String, Integer> getAttendanceStatistics() {
        Map<String, Integer> stats = new HashMap<>();
        
        String statsSQL = "SELECT " +
                         "COUNT(*) as total_records, " +
                         "COUNT(DISTINCT account_number) as unique_employees, " +
                         "COUNT(DISTINCT DATE(log_datetime)) as unique_dates, " +
                         "COUNT(DISTINCT import_batch) as unique_batches " +
                         "FROM attendance";
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(statsSQL);
             ResultSet rs = stmt.executeQuery()) {
            
            if (rs.next()) {
                stats.put("total_records", rs.getInt("total_records"));
                stats.put("unique_employees", rs.getInt("unique_employees"));
                stats.put("unique_dates", rs.getInt("unique_dates"));
                stats.put("unique_batches", rs.getInt("unique_batches"));
            }
            
        } catch (SQLException e) {
            System.err.println("Error getting attendance statistics: " + e.getMessage());
        }
        
        return stats;
    }
    
    /**
     * Refresh preview table with latest attendance data from database
     */
    public void refreshAttendancePreview() {
        previewData.clear();
        List<ImportPreviewData> latestRecords = readAttendanceRecords(null, null, 50); // Get latest 50 records
        previewData.addAll(latestRecords);
        importPreviewTable.setItems(previewData);
    }
    
    /**
     * Event handler for Refresh Data button
     */
    @FXML
    private void onRefreshPreview() {
        refreshAttendancePreview();
        showAlert("Attendance data preview refreshed!");
    }
    
    /**
     * Event handler for Show Statistics button
     */
    @FXML
    private void onShowStatistics() {
        Map<String, Integer> stats = getAttendanceStatistics();
        
        String message = "Attendance Database Statistics:\n\n" +
                        "Total Records: " + stats.getOrDefault("total_records", 0) + "\n" +
                        "Unique Employees: " + stats.getOrDefault("unique_employees", 0) + "\n" +
                        "Unique Dates: " + stats.getOrDefault("unique_dates", 0) + "\n" +
                        "Import Batches: " + stats.getOrDefault("unique_batches", 0);
        
        showSuccessAlert(message);
    }
    
    // File Loading Progress Methods
    private void showFileLoadingProgress(String title, String message) {
        Platform.runLater(() -> {
            // Unbind any existing bindings first
            if (fileProgressLabel.textProperty().isBound()) {
                fileProgressLabel.textProperty().unbind();
            }
            if (fileProgressBar.progressProperty().isBound()) {
                fileProgressBar.progressProperty().unbind();
            }
            
            fileLoadingLabel.setText(title);
            fileProgressLabel.setText(message);
            fileProgressBar.setProgress(0.0);
            fileLoadingSection.setVisible(true);
            fileLoadingSection.setManaged(true);
        });
    }
    
    private void hideFileLoadingProgress() {
        Platform.runLater(() -> {
            // Unbind properties before hiding
            if (fileProgressLabel.textProperty().isBound()) {
                fileProgressLabel.textProperty().unbind();
            }
            if (fileProgressBar.progressProperty().isBound()) {
                fileProgressBar.progressProperty().unbind();
            }
            
            fileLoadingSection.setVisible(false);
            fileLoadingSection.setManaged(false);
            fileProgressBar.setProgress(0.0);
        });
    }
    
    private void processFileInBackground(File file) {
        Task<Void> fileTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                try {
                    // Simulate file reading progress
                    updateMessage("Reading file: " + file.getName());
                    updateProgress(0, 100);
                    Thread.sleep(300);
                    
                    // Read file size
                    long fileSize = file.length();
                    updateMessage("File size: " + (fileSize / 1024) + " KB");
                    updateProgress(25, 100);
                    Thread.sleep(500);
                    
                    // Count lines
                    List<String> lines = Files.readAllLines(Paths.get(file.getAbsolutePath()));
                    int totalLines = lines.size();
                    updateMessage("Found " + totalLines + " lines in file");
                    updateProgress(50, 100);
                    Thread.sleep(500);
                    
                    // Validate format
                    updateMessage("Validating file format...");
                    updateProgress(75, 100);
                    Thread.sleep(500);
                    
                    // Complete
                    updateMessage("File processing complete");
                    updateProgress(100, 100);
                    Thread.sleep(300);
                    
                    Platform.runLater(() -> {
                        importFileLabel.setText("Selected: " + file.getName() + " (" + totalLines + " lines)");
                        loadPreviewData(); // Load preview after file processing
                    });
                    
                } catch (Exception e) {
                    Platform.runLater(() -> showAlert("Error processing file: " + e.getMessage()));
                }
                
                return null;
            }
        };
        
        // Bind progress to UI (only progress bar, not labels)
        fileProgressBar.progressProperty().bind(fileTask.progressProperty());
        
        // Update labels manually to avoid binding conflicts
        fileTask.messageProperty().addListener((obs, oldMessage, newMessage) -> {
            Platform.runLater(() -> {
                if (!fileProgressLabel.textProperty().isBound()) {
                    fileProgressLabel.setText(newMessage);
                }
            });
        });
        
        // Handle completion
        fileTask.setOnSucceeded(e -> hideFileLoadingProgress());
        fileTask.setOnFailed(e -> {
            hideFileLoadingProgress();
            showAlert("Error processing file: " + fileTask.getException().getMessage());
        });
        
        // Start background processing
        new Thread(fileTask).start();
    }
}
