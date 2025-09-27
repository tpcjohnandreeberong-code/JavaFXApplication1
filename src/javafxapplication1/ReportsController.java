package javafxapplication1;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.control.ToggleGroup;

import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.ResourceBundle;

public class ReportsController implements Initializable {

    @FXML private ComboBox<String> reportTypeFilter;
    @FXML private ComboBox<String> departmentFilter;
    @FXML private ComboBox<String> statusFilter;
    @FXML private RadioButton payrollReportRadio;
    @FXML private RadioButton employeeReportRadio;
    @FXML private RadioButton departmentReportRadio;
    @FXML private RadioButton summaryReportRadio;
    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;
    @FXML private RadioButton pdfFormatRadio;
    @FXML private RadioButton excelFormatRadio;
    @FXML private RadioButton csvFormatRadio;
    @FXML private Label reportsGeneratedLabel;
    @FXML private Label thisMonthLabel;
    @FXML private Label totalDownloadsLabel;
    @FXML private Label storageUsedLabel;

    private ToggleGroup reportTypeGroup = new ToggleGroup();
    private ToggleGroup formatGroup = new ToggleGroup();
    private final ObservableList<String> departments = FXCollections.observableArrayList();
    private final ObservableList<String> statusOptions = FXCollections.observableArrayList();
    private final ObservableList<String> reportTypes = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupToggleGroups();
        setupReportTypeFilter();
        setupDepartmentFilter();
        setupStatusFilter();
        setupDateDefaults();
        updateStatistics();
    }

    private void setupToggleGroups() {
        payrollReportRadio.setToggleGroup(reportTypeGroup);
        employeeReportRadio.setToggleGroup(reportTypeGroup);
        departmentReportRadio.setToggleGroup(reportTypeGroup);
        summaryReportRadio.setToggleGroup(reportTypeGroup);
        
        pdfFormatRadio.setToggleGroup(formatGroup);
        excelFormatRadio.setToggleGroup(formatGroup);
        csvFormatRadio.setToggleGroup(formatGroup);
    }

    private void setupReportTypeFilter() {
        reportTypes.addAll("All Report Types", "Payroll Report", "Employee Report", "Department Report", "Summary Report", "Tax Report", "Attendance Report");
        reportTypeFilter.setItems(reportTypes);
        reportTypeFilter.setValue("All Report Types");
    }

    private void setupDepartmentFilter() {
        departments.addAll("All Departments", "Math", "Science", "English", "Admin", "Finance", "ICT", "Library", "Transport", "HR", "Registrar", "Student Affairs", "Clinic");
        departmentFilter.setItems(departments);
        departmentFilter.setValue("All Departments");
    }

    private void setupStatusFilter() {
        statusOptions.addAll("All Status", "Draft", "Pending", "Approved", "Processed", "Cancelled");
        statusFilter.setItems(statusOptions);
        statusFilter.setValue("All Status");
    }

    private void setupDateDefaults() {
        LocalDate now = LocalDate.now();
        startDatePicker.setValue(now.withDayOfMonth(1));
        endDatePicker.setValue(now.withDayOfMonth(now.lengthOfMonth()));
    }

    @FXML
    private void onGenerateReport() {
        String reportType = getSelectedReportType();
        String format = getSelectedFormat();
        
        if (startDatePicker.getValue() == null || endDatePicker.getValue() == null) {
            showAlert("Please select both start and end dates.");
            return;
        }
        
        if (startDatePicker.getValue().isAfter(endDatePicker.getValue())) {
            showAlert("Start date cannot be after end date.");
            return;
        }

        // Simulate report generation
        showReportGenerationDialog(reportType, format);
    }

    @FXML
    private void onSearch() {
        String reportType = reportTypeFilter.getValue();
        String department = departmentFilter.getValue();
        String status = statusFilter.getValue();
        showAlert("Searching for " + reportType + " in " + department + " with status " + status + "...");
    }

    @FXML
    private void onPreviewReport() {
        String reportType = getSelectedReportType();
        showAlert("Preview feature for " + reportType + " will be implemented in the next version.");
    }

    @FXML
    private void onExportReport() {
        String reportType = getSelectedReportType();
        String format = getSelectedFormat();
        showAlert("Exporting " + reportType + " as " + format + "...");
    }

    @FXML
    private void onQuickReports() {
        showAlert("Quick Reports menu will be implemented in the next version.");
    }

    @FXML
    private void onQuickMonthlyPayroll() {
        generateQuickReport("Monthly Payroll Report", "PDF");
    }

    @FXML
    private void onQuickEmployeeList() {
        generateQuickReport("Employee List Report", "Excel");
    }

    @FXML
    private void onQuickDepartmentSummary() {
        generateQuickReport("Department Summary Report", "PDF");
    }

    @FXML
    private void onQuickTaxReport() {
        generateQuickReport("Tax Report", "PDF");
    }

    @FXML
    private void onDownloadReport() {
        showAlert("Report downloaded successfully to your Downloads folder!");
    }

    @FXML
    private void onPrintReport() {
        showAlert("Print dialog will open with the selected report.");
    }

    private String getSelectedReportType() {
        if (payrollReportRadio.isSelected()) return "Payroll Report";
        if (employeeReportRadio.isSelected()) return "Employee Report";
        if (departmentReportRadio.isSelected()) return "Department Report";
        if (summaryReportRadio.isSelected()) return "Summary Report";
        return "Payroll Report";
    }

    private String getSelectedFormat() {
        if (pdfFormatRadio.isSelected()) return "PDF";
        if (excelFormatRadio.isSelected()) return "Excel";
        if (csvFormatRadio.isSelected()) return "CSV";
        return "PDF";
    }

    private void generateQuickReport(String reportName, String format) {
        Alert alert = new Alert(AlertType.CONFIRMATION);
        alert.setTitle("Generate Quick Report");
        alert.setHeaderText("Generate " + reportName + "?");
        alert.setContentText("This will create a " + format + " report with current data.");
        
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            showReportGenerationDialog(reportName, format);
        }
    }

    private void showReportGenerationDialog(String reportType, String format) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Generating Report");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.OK);
        
        VBox content = new VBox(15);
        content.setPadding(new javafx.geometry.Insets(20));

        Label title = new Label("Report Generation Progress");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #1b5e20;");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);

        grid.add(new Label("Report Type:"), 0, 0);
        grid.add(new Label(reportType), 1, 0);
        grid.add(new Label("Format:"), 0, 1);
        grid.add(new Label(format), 1, 1);
        grid.add(new Label("Date Range:"), 0, 2);
        grid.add(new Label(startDatePicker.getValue().format(DateTimeFormatter.ofPattern("MMM dd, yyyy")) + 
                          " to " + endDatePicker.getValue().format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))), 1, 2);
        grid.add(new Label("Department:"), 0, 3);
        grid.add(new Label(departmentFilter.getValue()), 1, 3);
        grid.add(new Label("Status:"), 0, 4);
        grid.add(new Label(statusFilter.getValue()), 1, 4);
        grid.add(new Label("Status:"), 0, 5);
        grid.add(new Label("✅ Report generated successfully!"), 1, 5);
        grid.add(new Label("File Size:"), 0, 6);
        grid.add(new Label("2.3 MB"), 1, 6);
        grid.add(new Label("Location:"), 0, 7);
        grid.add(new Label("Desktop/Reports/"), 1, 7);

        content.getChildren().addAll(title, grid);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setStyle("-fx-background-color: #f5f5f5;");
        
        // Style the OK button
        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.setStyle("-fx-background-color: linear-gradient(to right, #2e7d32, #43a047); -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 6; -fx-padding: 8 16;");
        
        dialog.showAndWait();
        updateStatistics();
    }

    private void updateStatistics() {
        reportsGeneratedLabel.setText("24");
        thisMonthLabel.setText("8");
        totalDownloadsLabel.setText("156");
        storageUsedLabel.setText("45.2 MB");
    }

    private void showAlert(String message) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("Reports");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.getDialogPane().setStyle("-fx-background-color: #f5f5f5;");
        alert.showAndWait();
    }
}
