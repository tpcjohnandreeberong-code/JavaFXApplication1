package javafxapplication1;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;

import java.io.File;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.ResourceBundle;
import javafx.scene.layout.GridPane;

public class GovernmentRemittancesController implements Initializable {

    // Controls
    @FXML private TextField searchField;
    @FXML private ComboBox<String> typeFilter;
    @FXML private ComboBox<String> statusFilter;
    @FXML private TableView<DeductionType> deductionTable;
    @FXML private TableColumn<DeductionType, Integer> colId;
    @FXML private TableColumn<DeductionType, String> colCode;
    @FXML private TableColumn<DeductionType, String> colName;
    @FXML private TableColumn<DeductionType, String> colDescription;
    @FXML private TableColumn<DeductionType, String> colBasis;
    @FXML private TableColumn<DeductionType, Double> colRatePercent;
    @FXML private TableColumn<DeductionType, Double> colFixedAmount;
    @FXML private TableColumn<DeductionType, Double> colMinSalary;
    @FXML private TableColumn<DeductionType, Double> colMaxSalary;
    @FXML private TableColumn<DeductionType, Double> colEmployeeShare;
    @FXML private TableColumn<DeductionType, Double> colEmployerShare;
    @FXML private TableColumn<DeductionType, Double> colBaseTax;
    @FXML private TableColumn<DeductionType, Double> colExcessOver;
    @FXML private TableColumn<DeductionType, Double> colEffectiveEmployeePercent;
    @FXML private TableColumn<DeductionType, Double> colEffectiveEmployerPercent;
    @FXML private TableColumn<DeductionType, Double> colEffectiveTotalPercent;

    // Data Collections
    private final ObservableList<DeductionType> deductionTypes = FXCollections.observableArrayList();
    private final ObservableList<String> typeOptions = FXCollections.observableArrayList();
    private final ObservableList<String> statusOptions = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupTableColumns();
        setupComboBoxes();
        loadSampleData();
    }

    private void setupTableColumns() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colCode.setCellValueFactory(new PropertyValueFactory<>("code"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
        colBasis.setCellValueFactory(new PropertyValueFactory<>("basis"));
        colRatePercent.setCellValueFactory(new PropertyValueFactory<>("ratePercent"));
        colFixedAmount.setCellValueFactory(new PropertyValueFactory<>("fixedAmount"));
        colMinSalary.setCellValueFactory(new PropertyValueFactory<>("minSalary"));
        colMaxSalary.setCellValueFactory(new PropertyValueFactory<>("maxSalary"));
        colEmployeeShare.setCellValueFactory(new PropertyValueFactory<>("employeeShare"));
        colEmployerShare.setCellValueFactory(new PropertyValueFactory<>("employerShare"));
        colBaseTax.setCellValueFactory(new PropertyValueFactory<>("baseTax"));
        colExcessOver.setCellValueFactory(new PropertyValueFactory<>("excessOver"));
        colEffectiveEmployeePercent.setCellValueFactory(new PropertyValueFactory<>("effectiveEmployeePercent"));
        colEffectiveEmployerPercent.setCellValueFactory(new PropertyValueFactory<>("effectiveEmployerPercent"));
        colEffectiveTotalPercent.setCellValueFactory(new PropertyValueFactory<>("effectiveTotalPercent"));
    }

    private void setupComboBoxes() {
        // Type options
        typeOptions.addAll("All Types", "SSS", "PHILHEALTH", "PAGIBIG", "TAX", "OTHER");
        typeFilter.setItems(typeOptions);
        typeFilter.setValue("All Types");

        // Status options
        statusOptions.addAll("All Types", "SSS", "PHILHEALTH", "PAGIBIG", "TAX", "OTHER");
        statusFilter.setItems(statusOptions);
        statusFilter.setValue("All Types");
    }

    private void loadSampleData() {
        // Sample deduction types based on your exact database structure
        deductionTypes.addAll(
            new DeductionType(1, "SSS", "Social Security System", "Social security contribution", 
                             "salary", 15.0, null, 5000.0, 35000.0, 5.0, 10.0, null, null, 5.0, 10.0, 15.0),
            new DeductionType(2, "PHILHEALTH", "PhilHealth Insurance", "Health insurance contribution", 
                             "salary", 5.0, null, 10000.0, 100000.0, 2.5, 2.5, null, null, 2.5, 2.5, 5.0),
            new DeductionType(3, "PAGIBIG", "Pag-IBIG Fund", "Home Development Mutual Fund", 
                             "salary", 2.0, null, 0.0, 10000.0, 1.0, 1.0, null, null, 1.0, 1.0, 2.0),
            new DeductionType(4, "TAX", "Withholding Tax", "Income tax (TRAIN Law 2025)", 
                             "salary", 0.0, 0.0, 0.0, 20833.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0),
            new DeductionType(5, "TAX", "Withholding Tax", "Income tax (TRAIN Law 2025)", 
                             "salary", 15.0, null, 20833.01, 33332.0, 100.0, 0.0, 0.0, 20833.0, 15.0, 0.0, 15.0)
        );
        deductionTable.setItems(deductionTypes);
    }

    // Action Methods
    @FXML
    private void onSearch() {
        String searchText = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        String selectedType = typeFilter.getValue();

        if (searchText.isEmpty() && 
            (selectedType == null || selectedType.equals("All Types"))) {
            deductionTable.setItems(deductionTypes);
            return;
        }

        ObservableList<DeductionType> filtered = FXCollections.observableArrayList();
        for (DeductionType deduction : deductionTypes) {
            boolean matchesSearch = searchText.isEmpty() ||
                                  deduction.getCode().toLowerCase().contains(searchText) ||
                                  deduction.getName().toLowerCase().contains(searchText) ||
                                  deduction.getDescription().toLowerCase().contains(searchText);
            boolean matchesType = selectedType == null || selectedType.equals("All Types") ||
                                deduction.getCode().equals(selectedType);

            if (matchesSearch && matchesType) {
                filtered.add(deduction);
            }
        }
        deductionTable.setItems(filtered);
    }

    @FXML
    private void onAddDeduction() {
        Dialog<DeductionType> dialog = buildDeductionDialog(null);
        Optional<DeductionType> result = dialog.showAndWait();
        result.ifPresent(deduction -> {
            deductionTypes.add(deduction);
            showSuccessAlert("Deduction type added successfully!");
        });
    }

    @FXML
    private void onEditDeduction() {
        DeductionType selected = deductionTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showErrorAlert("Please select a deduction type to edit!");
            return;
        }

        Dialog<DeductionType> dialog = buildDeductionDialog(selected);
        Optional<DeductionType> result = dialog.showAndWait();
        result.ifPresent(updated -> {
            int idx = deductionTypes.indexOf(selected);
            if (idx >= 0) {
                deductionTypes.set(idx, updated);
                showSuccessAlert("Deduction type updated successfully!");
            }
        });
    }

    @FXML
    private void onDeleteDeduction() {
        DeductionType selected = deductionTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showErrorAlert("Please select a deduction type to delete!");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Deduction Type");
        confirm.setHeaderText(null);
        confirm.setContentText("Delete deduction type " + selected.getCode() + " - " + selected.getName() + "?");
        Optional<ButtonType> result = confirm.showAndWait();
        
        if (result.isPresent() && result.get() == ButtonType.OK) {
            deductionTypes.remove(selected);
            showSuccessAlert("Deduction type deleted successfully!");
        }
    }

    @FXML
    private void onRefresh() {
        loadSampleData();
        showSuccessAlert("Data refreshed successfully!");
    }

    @FXML
    private void onGenerateReport() {
        showSuccessAlert("Generate Report functionality will be implemented here.");
    }

    @FXML
    private void onExportRemittances() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Deduction Types");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        fileChooser.setInitialFileName("deduction_types_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".csv");
        
        File file = fileChooser.showSaveDialog(null);
        if (file != null) {
            showSuccessAlert("Export functionality will be implemented here.\nFile: " + file.getName());
        }
    }

    // Helper Methods
    private Dialog<DeductionType> buildDeductionDialog(DeductionType deduction) {
        Dialog<DeductionType> dialog = new Dialog<>();
        dialog.setTitle(deduction == null ? "Add New Deduction Type" : "Edit Deduction Type");
        dialog.setHeaderText(null);

        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(20, 150, 10, 10));

        TextField codeField = new TextField();
        codeField.setPromptText("Code (e.g., SSS, PHILHEALTH)");
        TextField nameField = new TextField();
        nameField.setPromptText("Name");
        TextField descriptionField = new TextField();
        descriptionField.setPromptText("Description");
        ComboBox<String> basisCombo = new ComboBox<>();
        basisCombo.getItems().addAll("salary", "range", "fixed");
        TextField rateField = new TextField();
        rateField.setPromptText("Rate %");
        TextField fixedAmountField = new TextField();
        fixedAmountField.setPromptText("Fixed Amount");
        TextField minSalaryField = new TextField();
        minSalaryField.setPromptText("Min Salary");
        TextField maxSalaryField = new TextField();
        maxSalaryField.setPromptText("Max Salary");
        TextField employeeShareField = new TextField();
        employeeShareField.setPromptText("Employee Share");
        TextField employerShareField = new TextField();
        employerShareField.setPromptText("Employer Share");
        TextField baseTaxField = new TextField();
        baseTaxField.setPromptText("Base Tax");
        TextField excessOverField = new TextField();
        excessOverField.setPromptText("Excess Over");
        TextField effectiveEmployeePercentField = new TextField();
        effectiveEmployeePercentField.setPromptText("Effective Employee %");
        TextField effectiveEmployerPercentField = new TextField();
        effectiveEmployerPercentField.setPromptText("Effective Employer %");
        TextField effectiveTotalPercentField = new TextField();
        effectiveTotalPercentField.setPromptText("Effective Total %");

        if (deduction != null) {
            codeField.setText(deduction.getCode());
            nameField.setText(deduction.getName());
            descriptionField.setText(deduction.getDescription());
            basisCombo.setValue(deduction.getBasis());
            rateField.setText(deduction.getRatePercent() != null ? String.valueOf(deduction.getRatePercent()) : "");
            fixedAmountField.setText(deduction.getFixedAmount() != null ? String.valueOf(deduction.getFixedAmount()) : "");
            minSalaryField.setText(String.valueOf(deduction.getMinSalary()));
            maxSalaryField.setText(String.valueOf(deduction.getMaxSalary()));
            employeeShareField.setText(String.valueOf(deduction.getEmployeeShare()));
            employerShareField.setText(String.valueOf(deduction.getEmployerShare()));
            baseTaxField.setText(deduction.getBaseTax() != null ? String.valueOf(deduction.getBaseTax()) : "");
            excessOverField.setText(deduction.getExcessOver() != null ? String.valueOf(deduction.getExcessOver()) : "");
            effectiveEmployeePercentField.setText(String.valueOf(deduction.getEffectiveEmployeePercent()));
            effectiveEmployerPercentField.setText(String.valueOf(deduction.getEffectiveEmployerPercent()));
            effectiveTotalPercentField.setText(String.valueOf(deduction.getEffectiveTotalPercent()));
        } else {
            basisCombo.setValue("salary");
        }

        grid.add(new Label("Code:"), 0, 0);
        grid.add(codeField, 1, 0);
        grid.add(new Label("Name:"), 0, 1);
        grid.add(nameField, 1, 1);
        grid.add(new Label("Description:"), 0, 2);
        grid.add(descriptionField, 1, 2);
        grid.add(new Label("Basis:"), 0, 3);
        grid.add(basisCombo, 1, 3);
        grid.add(new Label("Rate %:"), 0, 4);
        grid.add(rateField, 1, 4);
        grid.add(new Label("Fixed Amount:"), 0, 5);
        grid.add(fixedAmountField, 1, 5);
        grid.add(new Label("Min Salary:"), 0, 6);
        grid.add(minSalaryField, 1, 6);
        grid.add(new Label("Max Salary:"), 0, 7);
        grid.add(maxSalaryField, 1, 7);
        grid.add(new Label("Employee Share:"), 0, 8);
        grid.add(employeeShareField, 1, 8);
        grid.add(new Label("Employer Share:"), 0, 9);
        grid.add(employerShareField, 1, 9);
        grid.add(new Label("Base Tax:"), 0, 10);
        grid.add(baseTaxField, 1, 10);
        grid.add(new Label("Excess Over:"), 0, 11);
        grid.add(excessOverField, 1, 11);
        grid.add(new Label("Eff. Employee %:"), 0, 12);
        grid.add(effectiveEmployeePercentField, 1, 12);
        grid.add(new Label("Eff. Employer %:"), 0, 13);
        grid.add(effectiveEmployerPercentField, 1, 13);
        grid.add(new Label("Eff. Total %:"), 0, 14);
        grid.add(effectiveTotalPercentField, 1, 14);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                return new DeductionType(
                    deduction != null ? deduction.getId() : deductionTypes.size() + 1,
                    codeField.getText(),
                    nameField.getText(),
                    descriptionField.getText(),
                    basisCombo.getValue(),
                    rateField.getText().isEmpty() ? null : Double.parseDouble(rateField.getText()),
                    fixedAmountField.getText().isEmpty() ? null : Double.parseDouble(fixedAmountField.getText()),
                    minSalaryField.getText().isEmpty() ? 0.0 : Double.parseDouble(minSalaryField.getText()),
                    maxSalaryField.getText().isEmpty() ? 0.0 : Double.parseDouble(maxSalaryField.getText()),
                    employeeShareField.getText().isEmpty() ? 0.0 : Double.parseDouble(employeeShareField.getText()),
                    employerShareField.getText().isEmpty() ? 0.0 : Double.parseDouble(employerShareField.getText()),
                    baseTaxField.getText().isEmpty() ? null : Double.parseDouble(baseTaxField.getText()),
                    excessOverField.getText().isEmpty() ? null : Double.parseDouble(excessOverField.getText()),
                    effectiveEmployeePercentField.getText().isEmpty() ? 0.0 : Double.parseDouble(effectiveEmployeePercentField.getText()),
                    effectiveEmployerPercentField.getText().isEmpty() ? 0.0 : Double.parseDouble(effectiveEmployerPercentField.getText()),
                    effectiveTotalPercentField.getText().isEmpty() ? 0.0 : Double.parseDouble(effectiveTotalPercentField.getText())
                );
            }
            return null;
        });

        return dialog;
    }

    private void showSuccessAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Government Remittances");
        alert.setHeaderText("Success");
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showErrorAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Government Remittances");
        alert.setHeaderText("Error");
        alert.setContentText(message);
        alert.showAndWait();
    }

    // Data Class
    public static class DeductionType {
        private int id;
        private String code;
        private String name;
        private String description;
        private String basis;
        private Double ratePercent;
        private Double fixedAmount;
        private double minSalary;
        private double maxSalary;
        private double employeeShare;
        private double employerShare;
        private Double baseTax;
        private Double excessOver;
        private double effectiveEmployeePercent;
        private double effectiveEmployerPercent;
        private double effectiveTotalPercent;

        public DeductionType(int id, String code, String name, String description, String basis, 
                            Double ratePercent, Double fixedAmount, double minSalary, double maxSalary, 
                            double employeeShare, double employerShare, Double baseTax, Double excessOver,
                            double effectiveEmployeePercent, double effectiveEmployerPercent, double effectiveTotalPercent) {
            this.id = id;
            this.code = code;
            this.name = name;
            this.description = description;
            this.basis = basis;
            this.ratePercent = ratePercent;
            this.fixedAmount = fixedAmount;
            this.minSalary = minSalary;
            this.maxSalary = maxSalary;
            this.employeeShare = employeeShare;
            this.employerShare = employerShare;
            this.baseTax = baseTax;
            this.excessOver = excessOver;
            this.effectiveEmployeePercent = effectiveEmployeePercent;
            this.effectiveEmployerPercent = effectiveEmployerPercent;
            this.effectiveTotalPercent = effectiveTotalPercent;
        }

        // Getters
        public int getId() { return id; }
        public String getCode() { return code; }
        public String getName() { return name; }
        public String getDescription() { return description; }
        public String getBasis() { return basis; }
        public Double getRatePercent() { return ratePercent; }
        public Double getFixedAmount() { return fixedAmount; }
        public double getMinSalary() { return minSalary; }
        public double getMaxSalary() { return maxSalary; }
        public double getEmployeeShare() { return employeeShare; }
        public double getEmployerShare() { return employerShare; }
        public Double getBaseTax() { return baseTax; }
        public Double getExcessOver() { return excessOver; }
        public double getEffectiveEmployeePercent() { return effectiveEmployeePercent; }
        public double getEffectiveEmployerPercent() { return effectiveEmployerPercent; }
        public double getEffectiveTotalPercent() { return effectiveTotalPercent; }
    }
}