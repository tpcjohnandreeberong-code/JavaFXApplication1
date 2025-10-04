package javafxapplication1;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.control.Label;

import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.ResourceBundle;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;

public class PayrollProcessingController implements Initializable {

    public static class PayrollRecord {
        private final String employeeId;
        private final String employeeName;
        private final String department;
        private final String position;
        private final double basicSalary;
        private final double overtime;
        private final double allowances;
        private final double deductions;
        private final double netPay;
        private final String status;

        public PayrollRecord(String employeeId, String employeeName, String department, String position,
                           double basicSalary, double overtime, double allowances, double deductions, 
                           double netPay, String status) {
            this.employeeId = employeeId;
            this.employeeName = employeeName;
            this.department = department;
            this.position = position;
            this.basicSalary = basicSalary;
            this.overtime = overtime;
            this.allowances = allowances;
            this.deductions = deductions;
            this.netPay = netPay;
            this.status = status;
        }

        // Getters
        public String getEmployeeId() { return employeeId; }
        public String getEmployeeName() { return employeeName; }
        public String getDepartment() { return department; }
        public String getPosition() { return position; }
        public double getBasicSalary() { return basicSalary; }
        public double getOvertime() { return overtime; }
        public double getAllowances() { return allowances; }
        public double getDeductions() { return deductions; }
        public double getNetPay() { return netPay; }
        public String getStatus() { return status; }
    }

    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> departmentFilter;
    @FXML private TableView<PayrollRecord> payrollTable;
    @FXML private TableColumn<PayrollRecord, String> colEmployeeId;
    @FXML private TableColumn<PayrollRecord, String> colEmployeeName;
    @FXML private TableColumn<PayrollRecord, String> colDepartment;
    @FXML private TableColumn<PayrollRecord, String> colPosition;
    @FXML private TableColumn<PayrollRecord, Double> colBasicSalary;
    @FXML private TableColumn<PayrollRecord, Double> colOvertime;
    @FXML private TableColumn<PayrollRecord, Double> colAllowances;
    @FXML private TableColumn<PayrollRecord, Double> colDeductions;
    @FXML private TableColumn<PayrollRecord, Double> colNetPay;
    @FXML private TableColumn<PayrollRecord, String> colStatus;

    private final ObservableList<PayrollRecord> payrollData = FXCollections.observableArrayList();
    private final ObservableList<String> departments = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupTableColumns();
        setupDepartmentFilter();
        loadSampleData();
        
        // Set default date range (current month)
        LocalDate now = LocalDate.now();
        startDatePicker.setValue(now.withDayOfMonth(1));
        endDatePicker.setValue(now.withDayOfMonth(now.lengthOfMonth()));
    }

    private void setupTableColumns() {
        colEmployeeId.setCellValueFactory(new PropertyValueFactory<>("employeeId"));
        colEmployeeName.setCellValueFactory(new PropertyValueFactory<>("employeeName"));
        colDepartment.setCellValueFactory(new PropertyValueFactory<>("department"));
        colPosition.setCellValueFactory(new PropertyValueFactory<>("position"));
        colBasicSalary.setCellValueFactory(new PropertyValueFactory<>("basicSalary"));
        colOvertime.setCellValueFactory(new PropertyValueFactory<>("overtime"));
        colAllowances.setCellValueFactory(new PropertyValueFactory<>("allowances"));
        colDeductions.setCellValueFactory(new PropertyValueFactory<>("deductions"));
        colNetPay.setCellValueFactory(new PropertyValueFactory<>("netPay"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        // Format currency columns
        colBasicSalary.setCellFactory(column -> new TableCell<PayrollRecord, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.format("₱%,.2f", item));
                }
            }
        });

        colOvertime.setCellFactory(column -> new TableCell<PayrollRecord, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.format("₱%,.2f", item));
                }
            }
        });

        colAllowances.setCellFactory(column -> new TableCell<PayrollRecord, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.format("₱%,.2f", item));
                }
            }
        });

        colDeductions.setCellFactory(column -> new TableCell<PayrollRecord, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.format("₱%,.2f", item));
                }
            }
        });

        colNetPay.setCellFactory(column -> new TableCell<PayrollRecord, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.format("₱%,.2f", item));
                }
            }
        });
    }

    private void setupDepartmentFilter() {
        departments.addAll("All Departments", "Math", "Science", "English", "Admin", "Finance", "ICT", "Library", "Transport", "HR", "Registrar", "Student Affairs", "Clinic");
        departmentFilter.setItems(departments);
        departmentFilter.setValue("All Departments");
    }

    private void loadSampleData() {
        payrollData.addAll(
            new PayrollRecord("E-001", "Juan Dela Cruz", "Math", "Teacher I", 25000, 2000, 1500, 3000, 25500, "Processed"),
            new PayrollRecord("E-002", "Maria Santos", "Admin", "Registrar", 30000, 0, 2000, 4000, 28000, "Processed"),
            new PayrollRecord("E-003", "Pedro Reyes", "Science", "Teacher II", 28000, 1500, 1200, 3500, 27200, "Processed")
        );
        payrollTable.setItems(payrollData);
    }

    @FXML
    private void onGeneratePayroll() {
        if (startDatePicker.getValue() == null || endDatePicker.getValue() == null) {
            showAlert("Please select both start and end dates.");
            return;
        }
        
        if (startDatePicker.getValue().isAfter(endDatePicker.getValue())) {
            showAlert("Start date cannot be after end date.");
            return;
        }

        showAlert("Payroll generated successfully for period: " + 
                 startDatePicker.getValue().format(DateTimeFormatter.ofPattern("MMM dd, yyyy")) + 
                 " to " + 
                 endDatePicker.getValue().format(DateTimeFormatter.ofPattern("MMM dd, yyyy")));
    }

    @FXML
    private void onSearch() {
        String searchText = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        String selectedDept = departmentFilter.getValue();
        
        if (searchText.isEmpty() && (selectedDept == null || selectedDept.equals("All Departments"))) {
            payrollTable.setItems(payrollData);
            return;
        }

        ObservableList<PayrollRecord> filtered = FXCollections.observableArrayList();
        for (PayrollRecord record : payrollData) {
            boolean matchesSearch = searchText.isEmpty() || 
                                  record.getEmployeeId().toLowerCase().contains(searchText) || 
                                  record.getEmployeeName().toLowerCase().contains(searchText);
            boolean matchesDept = selectedDept == null || selectedDept.equals("All Departments") || 
                                record.getDepartment().equals(selectedDept);
            
            if (matchesSearch && matchesDept) {
                filtered.add(record);
            }
        }
        payrollTable.setItems(filtered);
    }

    @FXML
    private void onViewComputation() {
        PayrollRecord selected = payrollTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Please select an employee to view computation details.");
            return;
        }

        showComputationDialog(selected);
    }

    @FXML
    private void onExportPayroll() {
        try {
            exportToExcel();
            showAlert("Payroll data exported successfully to Excel format!");
        } catch (Exception e) {
            showAlert("Error exporting payroll: " + e.getMessage());
        }
    }

    private void exportToExcel() throws IOException {
        // Create CSV file with beautiful formatting
        String fileName = "Payroll_Report_" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".csv";
        File file = new File(System.getProperty("user.home") + "/Desktop/" + fileName);
        
        try (FileWriter writer = new FileWriter(file)) {
            // Write header information
            writer.append("TPC PAYROLL MANAGEMENT SYSTEM\n");
            writer.append("Payroll Processing Report\n");
            writer.append("Report Period: " + 
                (startDatePicker.getValue() != null ? startDatePicker.getValue().format(DateTimeFormatter.ofPattern("MMM dd, yyyy")) : "N/A") + 
                " to " + 
                (endDatePicker.getValue() != null ? endDatePicker.getValue().format(DateTimeFormatter.ofPattern("MMM dd, yyyy")) : "N/A") + "\n");
            writer.append("Generated on: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' hh:mm a")) + "\n");
            writer.append("\n");
            
            // Write headers
            writer.append("Employee ID,Employee Name,Department,Position,Basic Salary,Overtime,Allowances,Deductions,Net Pay,Status\n");
            
            // Write data rows
            double totalGross = 0;
            double totalDeductions = 0;
            double totalNet = 0;
            
            for (PayrollRecord record : payrollTable.getItems()) {
                writer.append(record.getEmployeeId()).append(",");
                writer.append("\"").append(record.getEmployeeName()).append("\",");
                writer.append(record.getDepartment()).append(",");
                writer.append(record.getPosition()).append(",");
                writer.append(String.format("₱%,.2f", record.getBasicSalary())).append(",");
                writer.append(String.format("₱%,.2f", record.getOvertime())).append(",");
                writer.append(String.format("₱%,.2f", record.getAllowances())).append(",");
                writer.append(String.format("₱%,.2f", record.getDeductions())).append(",");
                writer.append(String.format("₱%,.2f", record.getNetPay())).append(",");
                writer.append(record.getStatus()).append("\n");
                
                // Calculate totals
                totalGross += record.getBasicSalary() + record.getOvertime() + record.getAllowances();
                totalDeductions += record.getDeductions();
                totalNet += record.getNetPay();
            }
            
            // Write summary section
            writer.append("\n");
            writer.append("PAYROLL SUMMARY\n");
            writer.append("Total Gross Pay,").append(String.format("₱%,.2f", totalGross)).append("\n");
            writer.append("Total Deductions,").append(String.format("₱%,.2f", totalDeductions)).append("\n");
            writer.append("Total Net Pay,").append(String.format("₱%,.2f", totalNet)).append("\n");
            writer.append("Total Employees,").append(String.valueOf(payrollTable.getItems().size())).append("\n");
        }
    }

    private void showComputationDialog(PayrollRecord record) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Payroll Computation Details");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        VBox content = new VBox(10);
        content.setPadding(new javafx.geometry.Insets(20));

        Label title = new Label("Payroll Computation for " + record.getEmployeeName());
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #1b5e20;");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);

        // Earnings
        grid.add(new Label("EARNINGS:"), 0, 0);
        grid.add(new Label("Basic Salary:"), 0, 1);
        grid.add(new Label(String.format("₱%,.2f", record.getBasicSalary())), 1, 1);
        grid.add(new Label("Overtime:"), 0, 2);
        grid.add(new Label(String.format("₱%,.2f", record.getOvertime())), 1, 2);
        grid.add(new Label("Allowances:"), 0, 3);
        grid.add(new Label(String.format("₱%,.2f", record.getAllowances())), 1, 3);
        
        double grossPay = record.getBasicSalary() + record.getOvertime() + record.getAllowances();
        grid.add(new Label("TOTAL GROSS:"), 0, 4);
        grid.add(new Label(String.format("₱%,.2f", grossPay)), 1, 4);

        // Deductions
        grid.add(new Label("DEDUCTIONS:"), 0, 5);
        grid.add(new Label("SSS:"), 0, 6);
        grid.add(new Label(String.format("₱%,.2f", record.getDeductions() * 0.3)), 1, 6);
        grid.add(new Label("PhilHealth:"), 0, 7);
        grid.add(new Label(String.format("₱%,.2f", record.getDeductions() * 0.2)), 1, 7);
        grid.add(new Label("Pag-IBIG:"), 0, 8);
        grid.add(new Label(String.format("₱%,.2f", record.getDeductions() * 0.1)), 1, 8);
        grid.add(new Label("Tax:"), 0, 9);
        grid.add(new Label(String.format("₱%,.2f", record.getDeductions() * 0.4)), 1, 9);
        grid.add(new Label("TOTAL DEDUCTIONS:"), 0, 10);
        grid.add(new Label(String.format("₱%,.2f", record.getDeductions())), 1, 10);

        // Net Pay
        grid.add(new Label("NET PAY:"), 0, 11);
        grid.add(new Label(String.format("₱%,.2f", record.getNetPay())), 1, 11);
        grid.add(new Label("(Bold)"), 1, 11);
        grid.getChildren().get(grid.getChildren().size() - 1).setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #2e7d32;");

        content.getChildren().addAll(title, grid);
        dialog.getDialogPane().setContent(content);
        dialog.showAndWait();
    }


    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Payroll Processing");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
