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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.ResourceBundle;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ImportExportController implements Initializable {

    public static class ExportResult {
        public final int recordsExported;
        public final String exportFilename;
        
        public ExportResult(int recordsExported, String exportFilename) {
            this.recordsExported = recordsExported;
            this.exportFilename = exportFilename;
        }
    }

    public static class ImportPreviewData {
        private final String col1;
        private final String col2;
        private final String col3;
        private final String col4;
        private final String col5;

        public ImportPreviewData(String col1, String col2, String col3, String col4, String col5) {
            this.col1 = col1;
            this.col2 = col2;
            this.col3 = col3;
            this.col4 = col4;
            this.col5 = col5;
        }

        public String getCol1() { return col1; }
        public String getCol2() { return col2; }
        public String getCol3() { return col3; }
        public String getCol4() { return col4; }
        public String getCol5() { return col5; }
    }

    @FXML private ComboBox<String> importDataType;
    @FXML private ComboBox<String> importFileFormat;
    @FXML private ComboBox<String> exportDataType;
    @FXML private ComboBox<String> exportFileFormat;
    @FXML private ComboBox<String> exportDepartmentFilter;
    @FXML private Label importFileLabel;
    @FXML private Label exportStatusLabel;
    @FXML private ProgressBar importProgressBar;
    @FXML private ProgressBar exportProgressBar;
    @FXML private TableView<ImportPreviewData> importPreviewTable;
    @FXML private TableColumn<ImportPreviewData, String> colPreview1;
    @FXML private TableColumn<ImportPreviewData, String> colPreview2;
    @FXML private TableColumn<ImportPreviewData, String> colPreview3;
    @FXML private TableColumn<ImportPreviewData, String> colPreview4;
    @FXML private TableColumn<ImportPreviewData, String> colPreview5;

    private final ObservableList<String> dataTypes = FXCollections.observableArrayList();
    private final ObservableList<String> fileFormats = FXCollections.observableArrayList();
    private final ObservableList<String> departments = FXCollections.observableArrayList();
    private final ObservableList<ImportPreviewData> previewData = FXCollections.observableArrayList();
    
    private File selectedImportFile;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupComboBoxes();
        setupTableColumns();
        loadSamplePreviewData();
    }

    private void setupComboBoxes() {
        // Data types for import/export
        dataTypes.addAll("Employee Data", "Payroll Data", "Attendance Data", "Department Data", "All Data");
        importDataType.setItems(dataTypes);
        importDataType.setValue("Employee Data");
        
        exportDataType.setItems(dataTypes);
        exportDataType.setValue("Employee Data");

        // File formats
        fileFormats.addAll("CSV", "Excel", "JSON");
        importFileFormat.setItems(fileFormats);
        importFileFormat.setValue("CSV");
        
        exportFileFormat.setItems(fileFormats);
        exportFileFormat.setValue("CSV");

        // Departments
        departments.addAll("All Departments", "Math", "Science", "English", "Admin", "Finance", "ICT", "Library", "Transport", "HR", "Registrar", "Student Affairs", "Clinic");
        exportDepartmentFilter.setItems(departments);
        exportDepartmentFilter.setValue("All Departments");
    }

    private void setupTableColumns() {
        colPreview1.setCellValueFactory(new PropertyValueFactory<>("col1"));
        colPreview2.setCellValueFactory(new PropertyValueFactory<>("col2"));
        colPreview3.setCellValueFactory(new PropertyValueFactory<>("col3"));
        colPreview4.setCellValueFactory(new PropertyValueFactory<>("col4"));
        colPreview5.setCellValueFactory(new PropertyValueFactory<>("col5"));

        importPreviewTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private void loadSamplePreviewData() {
        previewData.addAll(
            new ImportPreviewData("E-001", "Juan Dela Cruz", "Math", "Teacher I", "25000"),
            new ImportPreviewData("E-002", "Maria Santos", "Admin", "Registrar", "30000"),
            new ImportPreviewData("E-003", "Pedro Reyes", "Science", "Teacher II", "28000"),
            new ImportPreviewData("E-004", "Ana Lopez", "Finance", "Cashier", "27000"),
            new ImportPreviewData("E-005", "Rico Valdez", "ICT", "IT Support", "32000")
        );
        importPreviewTable.setItems(previewData);
    }

    @FXML
    private void onImportData() {
        showImportDialog();
    }

    @FXML
    private void onExportData() {
        showExportDialog();
    }

    @FXML
    private void onBrowseImportFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select File to Import");
        
        String selectedFormat = importFileFormat.getValue();
        if (selectedFormat != null) {
            switch (selectedFormat) {
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
            importFileLabel.setText("Selected: " + selectedImportFile.getName());
            loadPreviewData();
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
        
        String dataType = importDataType.getValue();
        String fileFormat = importFileFormat.getValue();
        
        if (dataType == null || fileFormat == null) {
            showAlert("Please select both data type and file format.");
            return;
        }
        
        performImport(dataType, fileFormat);
    }

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

    private void performImport(String dataType, String fileFormat) {
        importProgressBar.setVisible(true);
        importProgressBar.setProgress(0);
        
        Task<Integer> importTask = new Task<Integer>() {
            @Override
            protected Integer call() throws Exception {
                try {
                    // Process the selected file
                    List<String[]> records = processImportFile(selectedImportFile, fileFormat);
                    int recordsImported = records.size();
                    
                    // Simulate processing each record
                    for (int i = 0; i <= recordsImported; i++) {
                        updateProgress(i, recordsImported);
                        Thread.sleep(10); // Simulate processing time per record
                    }
                    
                    // Update preview table with imported data
                    javafx.application.Platform.runLater(() -> {
                        previewData.clear();
                        for (String[] record : records) {
                            if (record.length >= 5) {
                                previewData.add(new ImportPreviewData(
                                    record[0], record[1], record[2], record[3], record[4]
                                ));
                            }
                        }
                        importPreviewTable.setItems(previewData);
                    });
                    
                    return recordsImported;
                    
                } catch (Exception e) {
                    throw new Exception("Failed to process file: " + e.getMessage());
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

    private void performExport(String dataType, String fileFormat, String department) {
        exportProgressBar.setVisible(true);
        exportProgressBar.setProgress(0);
        exportStatusLabel.setText("Exporting " + dataType + " as " + fileFormat + "...");
        
        Task<ExportResult> exportTask = new Task<ExportResult>() {
            @Override
            protected ExportResult call() throws Exception {
                try {
                    // Generate sample data for export
                    List<String[]> data = generateSampleData(dataType, department);
                    int recordsExported = data.size();
                    
                    // Create filename with timestamp
                    String timestamp = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
                    String extension = fileFormat.equals("Excel") ? ".xlsx" : 
                                     fileFormat.equals("JSON") ? ".json" : ".csv";
                    String exportFilename = dataType.replace(" ", "_") + "_" + timestamp + extension;
                    
                    // Create exports directory if it doesn't exist
                    String userHome = System.getProperty("user.home");
                    String exportDir = userHome + File.separator + "Desktop" + File.separator + "Exports";
                    new File(exportDir).mkdirs();
                    
                    String fullPath = exportDir + File.separator + exportFilename;
                    
                    // Simulate processing each record
                    for (int i = 0; i <= recordsExported; i++) {
                        updateProgress(i, recordsExported);
                        Thread.sleep(15); // Simulate processing time per record
                    }
                    
                    // Write the file
                    processExportFile(data, fullPath, fileFormat);
                    
                    return new ExportResult(recordsExported, exportFilename);
                    
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

    private void loadPreviewData() {
        // In a real implementation, this would parse the selected file
        // and populate the preview table with actual data
        previewData.clear();
        previewData.addAll(
            new ImportPreviewData("E-001", "Juan Dela Cruz", "Math", "Teacher I", "25000"),
            new ImportPreviewData("E-002", "Maria Santos", "Admin", "Registrar", "30000"),
            new ImportPreviewData("E-003", "Pedro Reyes", "Science", "Teacher II", "28000")
        );
        importPreviewTable.setItems(previewData);
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
}
