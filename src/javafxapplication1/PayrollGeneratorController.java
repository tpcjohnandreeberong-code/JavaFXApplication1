package javafxapplication1;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.ResourceBundle;

public class PayrollGeneratorController implements Initializable {

    public static class PayrollEntry {
        private final String payrollId;
        private final String employeeId;
        private final String employeeName;
        private final String department;
        private final String position;
        private final String payPeriod;
        private final double basicSalary;
        private final double overtime;
        private final double allowances;
        private final double deductions;
        private final double netPay;
        private final String status;
        private final String createdDate;

        public PayrollEntry(String payrollId, String employeeId, String employeeName, String department, 
                          String position, String payPeriod, double basicSalary, double overtime, 
                          double allowances, double deductions, double netPay, String status, String createdDate) {
            this.payrollId = payrollId;
            this.employeeId = employeeId;
            this.employeeName = employeeName;
            this.department = department;
            this.position = position;
            this.payPeriod = payPeriod;
            this.basicSalary = basicSalary;
            this.overtime = overtime;
            this.allowances = allowances;
            this.deductions = deductions;
            this.netPay = netPay;
            this.status = status;
            this.createdDate = createdDate;
        }

        // Getters
        public String getPayrollId() { return payrollId; }
        public String getEmployeeId() { return employeeId; }
        public String getEmployeeName() { return employeeName; }
        public String getDepartment() { return department; }
        public String getPosition() { return position; }
        public String getPayPeriod() { return payPeriod; }
        public double getBasicSalary() { return basicSalary; }
        public double getOvertime() { return overtime; }
        public double getAllowances() { return allowances; }
        public double getDeductions() { return deductions; }
        public double getNetPay() { return netPay; }
        public String getStatus() { return status; }
        public String getCreatedDate() { return createdDate; }
    }

    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusFilter;
    @FXML private DatePicker periodFilter;
    @FXML private TableView<PayrollEntry> payrollTable;
    @FXML private TableColumn<PayrollEntry, String> colPayrollId;
    @FXML private TableColumn<PayrollEntry, String> colEmployeeId;
    @FXML private TableColumn<PayrollEntry, String> colEmployeeName;
    @FXML private TableColumn<PayrollEntry, String> colDepartment;
    @FXML private TableColumn<PayrollEntry, String> colPosition;
    @FXML private TableColumn<PayrollEntry, String> colPayPeriod;
    @FXML private TableColumn<PayrollEntry, Double> colBasicSalary;
    @FXML private TableColumn<PayrollEntry, Double> colOvertime;
    @FXML private TableColumn<PayrollEntry, Double> colAllowances;
    @FXML private TableColumn<PayrollEntry, Double> colDeductions;
    @FXML private TableColumn<PayrollEntry, Double> colNetPay;
    @FXML private TableColumn<PayrollEntry, String> colStatus;
    @FXML private TableColumn<PayrollEntry, String> colCreatedDate;

    private final ObservableList<PayrollEntry> payrollData = FXCollections.observableArrayList();
    private final ObservableList<String> statusOptions = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupTableColumns();
        setupStatusFilter();
        loadSampleData();
    }

    private void setupTableColumns() {
        colPayrollId.setCellValueFactory(new PropertyValueFactory<>("payrollId"));
        colEmployeeId.setCellValueFactory(new PropertyValueFactory<>("employeeId"));
        colEmployeeName.setCellValueFactory(new PropertyValueFactory<>("employeeName"));
        colDepartment.setCellValueFactory(new PropertyValueFactory<>("department"));
        colPosition.setCellValueFactory(new PropertyValueFactory<>("position"));
        colPayPeriod.setCellValueFactory(new PropertyValueFactory<>("payPeriod"));
        colBasicSalary.setCellValueFactory(new PropertyValueFactory<>("basicSalary"));
        colOvertime.setCellValueFactory(new PropertyValueFactory<>("overtime"));
        colAllowances.setCellValueFactory(new PropertyValueFactory<>("allowances"));
        colDeductions.setCellValueFactory(new PropertyValueFactory<>("deductions"));
        colNetPay.setCellValueFactory(new PropertyValueFactory<>("netPay"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colCreatedDate.setCellValueFactory(new PropertyValueFactory<>("createdDate"));

        // Set column resize policy to fill available space
        payrollTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Format currency columns
        colBasicSalary.setCellFactory(column -> new TableCell<PayrollEntry, Double>() {
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

        colOvertime.setCellFactory(column -> new TableCell<PayrollEntry, Double>() {
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

        colAllowances.setCellFactory(column -> new TableCell<PayrollEntry, Double>() {
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

        colDeductions.setCellFactory(column -> new TableCell<PayrollEntry, Double>() {
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

        colNetPay.setCellFactory(column -> new TableCell<PayrollEntry, Double>() {
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

    private void setupStatusFilter() {
        statusOptions.addAll("All Status", "Draft", "Pending", "Approved", "Processed", "Cancelled");
        statusFilter.setItems(statusOptions);
        statusFilter.setValue("All Status");
    }

    private void loadSampleData() {
        payrollData.addAll(
            new PayrollEntry("P-001", "E-001", "Juan Dela Cruz", "Math", "Teacher I", "Dec 2024", 25000, 2000, 1500, 3000, 25500, "Approved", "2024-12-01"),
            new PayrollEntry("P-002", "E-002", "Maria Santos", "Admin", "Registrar", "Dec 2024", 30000, 0, 2000, 4000, 28000, "Processed", "2024-12-01"),
            new PayrollEntry("P-003", "E-003", "Pedro Reyes", "Science", "Teacher II", "Dec 2024", 28000, 1500, 1200, 3500, 27200, "Draft", "2024-12-02"),
            new PayrollEntry("P-004", "E-004", "Ana Lopez", "Finance", "Cashier", "Dec 2024", 27000, 1000, 1000, 2500, 26500, "Pending", "2024-12-02"),
            new PayrollEntry("P-005", "E-005", "Rico Valdez", "ICT", "IT Support", "Dec 2024", 32000, 3000, 2500, 5000, 32500, "Approved", "2024-12-03")
        );
        payrollTable.setItems(payrollData);
    }

    @FXML
    private void onSearch() {
        String searchText = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        String selectedStatus = statusFilter.getValue();
        LocalDate selectedPeriod = periodFilter.getValue();
        
        if (searchText.isEmpty() && (selectedStatus == null || selectedStatus.equals("All Status")) && selectedPeriod == null) {
            payrollTable.setItems(payrollData);
            return;
        }

        ObservableList<PayrollEntry> filtered = FXCollections.observableArrayList();
        for (PayrollEntry entry : payrollData) {
            boolean matchesSearch = searchText.isEmpty() || 
                                  entry.getPayrollId().toLowerCase().contains(searchText) || 
                                  entry.getEmployeeId().toLowerCase().contains(searchText) ||
                                  entry.getEmployeeName().toLowerCase().contains(searchText);
            boolean matchesStatus = selectedStatus == null || selectedStatus.equals("All Status") || 
                                  entry.getStatus().equals(selectedStatus);
            boolean matchesPeriod = selectedPeriod == null || 
                                  entry.getPayPeriod().contains(selectedPeriod.format(DateTimeFormatter.ofPattern("MMM yyyy")));
            
            if (matchesSearch && matchesStatus && matchesPeriod) {
                filtered.add(entry);
            }
        }
        payrollTable.setItems(filtered);
    }

    @FXML
    private void onAddPayroll() {
        Dialog<PayrollEntry> dialog = buildPayrollDialog(null);
        Optional<PayrollEntry> result = dialog.showAndWait();
        result.ifPresent(entry -> {
            payrollData.add(entry);
            showSuccessAlert("Payroll entry added successfully!");
        });
    }

    @FXML
    private void onEditPayroll() {
        PayrollEntry selected = payrollTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Please select a payroll entry to edit.");
            return;
        }
        
        Dialog<PayrollEntry> dialog = buildPayrollDialog(selected);
        Optional<PayrollEntry> result = dialog.showAndWait();
        result.ifPresent(updated -> {
            int idx = payrollData.indexOf(selected);
            if (idx >= 0) {
                payrollData.set(idx, updated);
                showSuccessAlert("Payroll entry updated successfully!");
            }
        });
    }

    @FXML
    private void onDeletePayroll() {
        PayrollEntry selected = payrollTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Please select a payroll entry to delete.");
            return;
        }
        
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Payroll Entry");
        confirm.setHeaderText(null);
        confirm.setContentText("Delete payroll entry " + selected.getPayrollId() + " for " + selected.getEmployeeName() + "?");
        Optional<ButtonType> res = confirm.showAndWait();
        if (res.isPresent() && res.get() == ButtonType.OK) {
            payrollData.remove(selected);
            showSuccessAlert("Payroll entry deleted successfully!");
        }
    }

    @FXML
    private void onViewGenerated() {
        PayrollEntry selected = payrollTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Please select a payroll entry to view details.");
            return;
        }
        
        showPayrollDetails(selected);
    }

    private Dialog<PayrollEntry> buildPayrollDialog(PayrollEntry existing) {
        Dialog<PayrollEntry> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "Add Payroll Entry" : "Edit Payroll Entry");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        
        // Style the dialog
        dialog.getDialogPane().setStyle("-fx-background-color: #f5f5f5;");
        dialog.getDialogPane().setPrefSize(600, 500);

        // Create form fields
        TextField payrollIdField = new TextField(existing == null ? generatePayrollId() : existing.getPayrollId());
        payrollIdField.setPromptText("Payroll ID (auto-generated)");
        payrollIdField.setEditable(false);
        payrollIdField.setStyle("-fx-background-radius: 6; -fx-border-radius: 6; -fx-border-color: #c8e6c9; -fx-focus-color: #66bb6a; -fx-padding: 8;");
        
        TextField employeeIdField = new TextField(existing == null ? "" : existing.getEmployeeId());
        employeeIdField.setPromptText("Enter Employee ID (e.g., E-001)");
        employeeIdField.setStyle("-fx-background-radius: 6; -fx-border-radius: 6; -fx-border-color: #c8e6c9; -fx-focus-color: #66bb6a; -fx-padding: 8;");
        
        TextField employeeNameField = new TextField(existing == null ? "" : existing.getEmployeeName());
        employeeNameField.setPromptText("Enter Employee Name");
        employeeNameField.setStyle("-fx-background-radius: 6; -fx-border-radius: 6; -fx-border-color: #c8e6c9; -fx-focus-color: #66bb6a; -fx-padding: 8;");
        
        ComboBox<String> departmentCombo = new ComboBox<>();
        departmentCombo.getItems().addAll("Math", "Science", "English", "Admin", "Finance", "ICT", "Library", "Transport", "HR", "Registrar", "Student Affairs", "Clinic");
        departmentCombo.setValue(existing == null ? "Select Department" : existing.getDepartment());
        departmentCombo.setStyle("-fx-background-radius: 6; -fx-border-radius: 6; -fx-border-color: #c8e6c9; -fx-focus-color: #66bb6a; -fx-padding: 8;");
        
        TextField positionField = new TextField(existing == null ? "" : existing.getPosition());
        positionField.setPromptText("Enter Position");
        positionField.setStyle("-fx-background-radius: 6; -fx-border-radius: 6; -fx-border-color: #c8e6c9; -fx-focus-color: #66bb6a; -fx-padding: 8;");
        
        TextField payPeriodField = new TextField(existing == null ? LocalDate.now().format(DateTimeFormatter.ofPattern("MMM yyyy")) : existing.getPayPeriod());
        payPeriodField.setPromptText("Enter Pay Period (e.g., Dec 2024)");
        payPeriodField.setStyle("-fx-background-radius: 6; -fx-border-radius: 6; -fx-border-color: #c8e6c9; -fx-focus-color: #66bb6a; -fx-padding: 8;");
        
        TextField basicSalaryField = new TextField(existing == null ? "" : String.valueOf(existing.getBasicSalary()));
        basicSalaryField.setPromptText("Enter Basic Salary");
        basicSalaryField.setStyle("-fx-background-radius: 6; -fx-border-radius: 6; -fx-border-color: #c8e6c9; -fx-focus-color: #66bb6a; -fx-padding: 8;");
        
        TextField overtimeField = new TextField(existing == null ? "" : String.valueOf(existing.getOvertime()));
        overtimeField.setPromptText("Enter Overtime Pay");
        overtimeField.setStyle("-fx-background-radius: 6; -fx-border-radius: 6; -fx-border-color: #c8e6c9; -fx-focus-color: #66bb6a; -fx-padding: 8;");
        
        TextField allowancesField = new TextField(existing == null ? "" : String.valueOf(existing.getAllowances()));
        allowancesField.setPromptText("Enter Allowances");
        allowancesField.setStyle("-fx-background-radius: 6; -fx-border-radius: 6; -fx-border-color: #c8e6c9; -fx-focus-color: #66bb6a; -fx-padding: 8;");
        
        TextField deductionsField = new TextField(existing == null ? "" : String.valueOf(existing.getDeductions()));
        deductionsField.setPromptText("Enter Deductions");
        deductionsField.setStyle("-fx-background-radius: 6; -fx-border-radius: 6; -fx-border-color: #c8e6c9; -fx-focus-color: #66bb6a; -fx-padding: 8;");
        
        ComboBox<String> statusCombo = new ComboBox<>();
        statusCombo.getItems().addAll("Draft", "Pending", "Approved", "Processed", "Cancelled");
        statusCombo.setValue(existing == null ? "Draft" : existing.getStatus());
        statusCombo.setStyle("-fx-background-radius: 6; -fx-border-radius: 6; -fx-border-color: #c8e6c9; -fx-focus-color: #66bb6a; -fx-padding: 8;");

        // Create form layout
        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(20));
        
        grid.addRow(0, new Label("Payroll ID:"), payrollIdField);
        grid.addRow(1, new Label("Employee ID:"), employeeIdField);
        grid.addRow(2, new Label("Employee Name:"), employeeNameField);
        grid.addRow(3, new Label("Department:"), departmentCombo);
        grid.addRow(4, new Label("Position:"), positionField);
        grid.addRow(5, new Label("Pay Period:"), payPeriodField);
        grid.addRow(6, new Label("Basic Salary:"), basicSalaryField);
        grid.addRow(7, new Label("Overtime:"), overtimeField);
        grid.addRow(8, new Label("Allowances:"), allowancesField);
        grid.addRow(9, new Label("Deductions:"), deductionsField);
        grid.addRow(10, new Label("Status:"), statusCombo);
        
        // Set column constraints
        javafx.scene.layout.ColumnConstraints col1 = new javafx.scene.layout.ColumnConstraints();
        col1.setPrefWidth(120);
        javafx.scene.layout.ColumnConstraints col2 = new javafx.scene.layout.ColumnConstraints();
        col2.setPrefWidth(300);
        grid.getColumnConstraints().addAll(col1, col2);
        
        dialog.getDialogPane().setContent(grid);

        // Style the buttons
        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.setStyle("-fx-background-color: linear-gradient(to right, #2e7d32, #43a047); -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 6; -fx-padding: 8 16;");
        
        Button cancelButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL);
        cancelButton.setStyle("-fx-background-color: #f5f5f5; -fx-text-fill: #666; -fx-border-color: #ddd; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 8 16;");

        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                // Validate inputs
                if (employeeIdField.getText().trim().isEmpty()) {
                    showAlert("Please enter Employee ID.");
                    return null;
                }
                if (employeeNameField.getText().trim().isEmpty()) {
                    showAlert("Please enter Employee Name.");
                    return null;
                }
                if (departmentCombo.getValue() == null || departmentCombo.getValue().equals("Select Department")) {
                    showAlert("Please select Department.");
                    return null;
                }
                
                double basicSalary, overtime, allowances, deductions;
                try {
                    basicSalary = Double.parseDouble(basicSalaryField.getText());
                    overtime = Double.parseDouble(overtimeField.getText());
                    allowances = Double.parseDouble(allowancesField.getText());
                    deductions = Double.parseDouble(deductionsField.getText());
                    
                    if (basicSalary < 0 || overtime < 0 || allowances < 0 || deductions < 0) {
                        showAlert("All amounts must be positive numbers.");
                        return null;
                    }
                } catch (NumberFormatException ex) {
                    showAlert("Please enter valid numeric values for all amounts.");
                    return null;
                }
                
                double netPay = basicSalary + overtime + allowances - deductions;
                String createdDate = existing == null ? LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) : existing.getCreatedDate();
                
                return new PayrollEntry(
                    payrollIdField.getText().trim(),
                    employeeIdField.getText().trim(),
                    employeeNameField.getText().trim(),
                    departmentCombo.getValue(),
                    positionField.getText().trim(),
                    payPeriodField.getText().trim(),
                    basicSalary,
                    overtime,
                    allowances,
                    deductions,
                    netPay,
                    statusCombo.getValue(),
                    createdDate
                );
            }
            return null;
        });
        return dialog;
    }

    private String generatePayrollId() {
        return "P-" + String.format("%03d", payrollData.size() + 1);
    }

    private void showPayrollDetails(PayrollEntry entry) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Payroll Entry Details");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        VBox content = new VBox(10);
        content.setPadding(new javafx.geometry.Insets(20));

        Label title = new Label("Payroll Entry Details - " + entry.getPayrollId());
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #1b5e20;");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);

        grid.add(new Label("Payroll ID:"), 0, 0);
        grid.add(new Label(entry.getPayrollId()), 1, 0);
        grid.add(new Label("Employee:"), 0, 1);
        grid.add(new Label(entry.getEmployeeName() + " (" + entry.getEmployeeId() + ")"), 1, 1);
        grid.add(new Label("Department:"), 0, 2);
        grid.add(new Label(entry.getDepartment()), 1, 2);
        grid.add(new Label("Position:"), 0, 3);
        grid.add(new Label(entry.getPosition()), 1, 3);
        grid.add(new Label("Pay Period:"), 0, 4);
        grid.add(new Label(entry.getPayPeriod()), 1, 4);
        grid.add(new Label("Status:"), 0, 5);
        grid.add(new Label(entry.getStatus()), 1, 5);
        grid.add(new Label("Created:"), 0, 6);
        grid.add(new Label(entry.getCreatedDate()), 1, 6);

        grid.add(new Label("EARNINGS:"), 0, 7);
        grid.add(new Label("Basic Salary:"), 0, 8);
        grid.add(new Label(String.format("₱%,.2f", entry.getBasicSalary())), 1, 8);
        grid.add(new Label("Overtime:"), 0, 9);
        grid.add(new Label(String.format("₱%,.2f", entry.getOvertime())), 1, 9);
        grid.add(new Label("Allowances:"), 0, 10);
        grid.add(new Label(String.format("₱%,.2f", entry.getAllowances())), 1, 10);
        
        double grossPay = entry.getBasicSalary() + entry.getOvertime() + entry.getAllowances();
        grid.add(new Label("Total Gross:"), 0, 11);
        grid.add(new Label(String.format("₱%,.2f", grossPay)), 1, 11);

        grid.add(new Label("DEDUCTIONS:"), 0, 12);
        grid.add(new Label("Total Deductions:"), 0, 13);
        grid.add(new Label(String.format("₱%,.2f", entry.getDeductions())), 1, 13);

        grid.add(new Label("NET PAY:"), 0, 14);
        grid.add(new Label(String.format("₱%,.2f", entry.getNetPay())), 1, 14);
        grid.add(new Label("(Bold)"), 1, 14);
        grid.getChildren().get(grid.getChildren().size() - 1).setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #2e7d32;");

        content.getChildren().addAll(title, grid);
        dialog.getDialogPane().setContent(content);
        dialog.showAndWait();
    }


    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Payroll Generator");
        alert.setHeaderText(null);
        alert.setContentText(message);
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
}
