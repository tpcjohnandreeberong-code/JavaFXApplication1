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
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.ResourceBundle;
import javafx.scene.layout.GridPane;

public class GovernmentRemittancesController implements Initializable {

    // Database connection constants
    private static final String DB_URL = DatabaseConfig.getDbUrl();
    private static final String DB_USER = DatabaseConfig.getDbUser();
    private static final String DB_PASSWORD = DatabaseConfig.getDbPassword();

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
        loadDeductionsFromDatabase();
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

    // Database connection method
    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }

    private void loadDeductionsFromDatabase() {
        deductionTypes.clear();
        
        String sql = "SELECT * FROM deductions WHERE is_active = TRUE ORDER BY code";
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                DeductionType deduction = new DeductionType(
                    rs.getInt("id"),
                    rs.getString("code"),
                    rs.getString("name"),
                    rs.getString("description"),
                    rs.getString("basis"),
                    rs.getObject("rate_percent") != null ? rs.getDouble("rate_percent") : null,
                    rs.getObject("fixed_amount") != null ? rs.getDouble("fixed_amount") : null,
                    rs.getDouble("min_salary"),
                    rs.getDouble("max_salary"),
                    rs.getDouble("employee_share"),
                    rs.getDouble("employer_share"),
                    rs.getObject("base_tax") != null ? rs.getDouble("base_tax") : null,
                    rs.getObject("excess_over") != null ? rs.getDouble("excess_over") : null,
                    rs.getDouble("effective_employee_percent"),
                    rs.getDouble("effective_employer_percent"),
                    rs.getDouble("effective_total_percent")
                );
                deductionTypes.add(deduction);
            }
            
            deductionTable.setItems(deductionTypes);
            
        } catch (SQLException e) {
            showErrorAlert("Database Error: Failed to load deductions.\n" + e.getMessage());
        }
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
            if (insertDeductionToDatabase(deduction)) {
                loadDeductionsFromDatabase(); // Refresh the table
                showSuccessAlert("Deduction type added successfully!");
            }
        });
    }

    private boolean insertDeductionToDatabase(DeductionType deduction) {
        String sql = "INSERT INTO deductions (code, name, description, basis, rate_percent, " +
                    "fixed_amount, min_salary, max_salary, employee_share, employer_share, " +
                    "base_tax, excess_over, effective_employee_percent, effective_employer_percent, " +
                    "effective_total_percent, created_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, deduction.getCode());
            stmt.setString(2, deduction.getName());
            stmt.setString(3, deduction.getDescription());
            stmt.setString(4, deduction.getBasis());
            
            if (deduction.getRatePercent() != null) {
                stmt.setDouble(5, deduction.getRatePercent());
            } else {
                stmt.setNull(5, Types.DECIMAL);
            }
            
            if (deduction.getFixedAmount() != null) {
                stmt.setDouble(6, deduction.getFixedAmount());
            } else {
                stmt.setNull(6, Types.DECIMAL);
            }
            
            stmt.setDouble(7, deduction.getMinSalary());
            stmt.setDouble(8, deduction.getMaxSalary());
            stmt.setDouble(9, deduction.getEmployeeShare());
            stmt.setDouble(10, deduction.getEmployerShare());
            
            if (deduction.getBaseTax() != null) {
                stmt.setDouble(11, deduction.getBaseTax());
            } else {
                stmt.setNull(11, Types.DECIMAL);
            }
            
            if (deduction.getExcessOver() != null) {
                stmt.setDouble(12, deduction.getExcessOver());
            } else {
                stmt.setNull(12, Types.DECIMAL);
            }
            
            stmt.setDouble(13, deduction.getEffectiveEmployeePercent());
            stmt.setDouble(14, deduction.getEffectiveEmployerPercent());
            stmt.setDouble(15, deduction.getEffectiveTotalPercent());
            stmt.setString(16, "admin"); // Current user - you can get this from session
            
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
            
        } catch (SQLException e) {
            showErrorAlert("Database Error: Failed to add deduction.\n" + e.getMessage());
            return false;
        }
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
            if (updateDeductionInDatabase(updated)) {
                loadDeductionsFromDatabase(); // Refresh the table
                showSuccessAlert("Deduction type updated successfully!");
            }
        });
    }

    private boolean updateDeductionInDatabase(DeductionType deduction) {
        String sql = "UPDATE deductions SET name = ?, description = ?, basis = ?, rate_percent = ?, " +
                    "fixed_amount = ?, min_salary = ?, max_salary = ?, employee_share = ?, employer_share = ?, " +
                    "base_tax = ?, excess_over = ?, effective_employee_percent = ?, effective_employer_percent = ?, " +
                    "effective_total_percent = ?, updated_by = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, deduction.getName());
            stmt.setString(2, deduction.getDescription());
            stmt.setString(3, deduction.getBasis());
            
            if (deduction.getRatePercent() != null) {
                stmt.setDouble(4, deduction.getRatePercent());
            } else {
                stmt.setNull(4, Types.DECIMAL);
            }
            
            if (deduction.getFixedAmount() != null) {
                stmt.setDouble(5, deduction.getFixedAmount());
            } else {
                stmt.setNull(5, Types.DECIMAL);
            }
            
            stmt.setDouble(6, deduction.getMinSalary());
            stmt.setDouble(7, deduction.getMaxSalary());
            stmt.setDouble(8, deduction.getEmployeeShare());
            stmt.setDouble(9, deduction.getEmployerShare());
            
            if (deduction.getBaseTax() != null) {
                stmt.setDouble(10, deduction.getBaseTax());
            } else {
                stmt.setNull(10, Types.DECIMAL);
            }
            
            if (deduction.getExcessOver() != null) {
                stmt.setDouble(11, deduction.getExcessOver());
            } else {
                stmt.setNull(11, Types.DECIMAL);
            }
            
            stmt.setDouble(12, deduction.getEffectiveEmployeePercent());
            stmt.setDouble(13, deduction.getEffectiveEmployerPercent());
            stmt.setDouble(14, deduction.getEffectiveTotalPercent());
            stmt.setString(15, "admin"); // Current user
            stmt.setInt(16, deduction.getId());
            
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
            
        } catch (SQLException e) {
            showErrorAlert("Database Error: Failed to update deduction.\n" + e.getMessage());
            return false;
        }
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
        confirm.setContentText("Delete deduction type " + selected.getCode() + " - " + selected.getName() + "?\n\nNote: This will permanently remove the deduction type.");
        Optional<ButtonType> result = confirm.showAndWait();
        
        if (result.isPresent() && result.get() == ButtonType.OK) {
            if (deleteDeductionFromDatabase(selected.getId())) {
                loadDeductionsFromDatabase(); // Refresh the table
                showSuccessAlert("Deduction type deleted successfully!");
            }
        }
    }

    private boolean deleteDeductionFromDatabase(int deductionId) {
        // Instead of hard delete, we'll soft delete by setting is_active = FALSE
        String sql = "UPDATE deductions SET is_active = FALSE, updated_by = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, "admin"); // Current user
            stmt.setInt(2, deductionId);
            
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
            
        } catch (SQLException e) {
            showErrorAlert("Database Error: Failed to delete deduction.\n" + e.getMessage());
            return false;
        }
    }

    @FXML
    private void onRefresh() {
        loadDeductionsFromDatabase();
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
        dialog.setTitle(deduction == null ? "Add Deduction Type" : "Edit Deduction Type");
        
        // Remove default header
        dialog.setHeaderText(null);
        
        // Custom button types with styled appearance
        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        // Create main container with green gradient background
        javafx.scene.layout.VBox mainContainer = new javafx.scene.layout.VBox();
        mainContainer.setStyle("-fx-background-color: linear-gradient(to bottom right, #ffffffff, #ffffffff);");
        
        // Create content container with white background
        javafx.scene.layout.VBox contentContainer = new javafx.scene.layout.VBox();
        contentContainer.setStyle("-fx-background-color: white;");
        contentContainer.setPadding(new javafx.geometry.Insets(30, 40, 30, 40));
        contentContainer.setSpacing(0);
        
        // Title Label
        Label titleLabel = new Label(deduction == null ? "Add New Deduction Type" : "Edit Deduction Type");
        titleLabel.setStyle(
            "-fx-text-fill: #1b5e20; " +
            "-fx-font-size: 22px; " +
            "-fx-font-weight: bold; " +
            "-fx-alignment: center;"
        );
        
        // Styled input fields
        String fieldStyle = 
            "-fx-background-radius: 6; " +
            "-fx-border-radius: 6; " +
            "-fx-border-color: #c8e6c9; " +
            "-fx-border-width: 1; " +
            "-fx-focus-color: #66bb6a; " +
            "-fx-faint-focus-color: transparent; " +
            "-fx-padding: 4 8 4 8; " +
            "-fx-font-size: 12px; " +
            "-fx-pref-width: 220;";
            
        String comboBoxStyle = fieldStyle + "-fx-background-color: white;";
            
        String labelStyle = 
            "-fx-text-fill: #1b5e20; " +
            "-fx-font-size: 12px; " +
            "-fx-font-weight: bold;";

        // Create form fields in a three-column layout for better space utilization
        javafx.scene.layout.GridPane formGrid = new javafx.scene.layout.GridPane();
        formGrid.setHgap(15);
        formGrid.setVgap(12);
        
        // Create fields directly in grid layout
        TextField codeField = new TextField();
        TextField nameField = new TextField();
        TextField descriptionField = new TextField();
        ComboBox<String> basisCombo = new ComboBox<>();
        TextField rateField = new TextField();
        TextField fixedAmountField = new TextField();
        TextField minSalaryField = new TextField();
        TextField maxSalaryField = new TextField();
        TextField employeeShareField = new TextField();
        TextField employerShareField = new TextField();
        TextField baseTaxField = new TextField();
        TextField excessOverField = new TextField();
        TextField effectiveEmployeePercentField = new TextField();
        TextField effectiveEmployerPercentField = new TextField();
        TextField effectiveTotalPercentField = new TextField();

        // Set prompts and styles
        codeField.setPromptText("Enter code (e.g., SSS)");
        codeField.setStyle(fieldStyle);
        nameField.setPromptText("Enter deduction name");
        nameField.setStyle(fieldStyle);
        descriptionField.setPromptText("Enter description");
        descriptionField.setStyle(fieldStyle);
        basisCombo.getItems().addAll("salary", "range", "fixed");
        basisCombo.setPromptText("Select basis");
        basisCombo.setStyle(comboBoxStyle);
        rateField.setPromptText("Rate %");
        rateField.setStyle(fieldStyle);
        fixedAmountField.setPromptText("Fixed amount");
        fixedAmountField.setStyle(fieldStyle);
        minSalaryField.setPromptText("Min salary");
        minSalaryField.setStyle(fieldStyle);
        maxSalaryField.setPromptText("Max salary");
        maxSalaryField.setStyle(fieldStyle);
        employeeShareField.setPromptText("Employee share");
        employeeShareField.setStyle(fieldStyle);
        employerShareField.setPromptText("Employer share");
        employerShareField.setStyle(fieldStyle);
        baseTaxField.setPromptText("Base tax");
        baseTaxField.setStyle(fieldStyle);
        excessOverField.setPromptText("Excess over");
        excessOverField.setStyle(fieldStyle);
        effectiveEmployeePercentField.setPromptText("Eff. employee %");
        effectiveEmployeePercentField.setStyle(fieldStyle);
        effectiveEmployerPercentField.setPromptText("Eff. employer %");
        effectiveEmployerPercentField.setStyle(fieldStyle);
        effectiveTotalPercentField.setPromptText("Eff. total %");
        effectiveTotalPercentField.setStyle(fieldStyle);

        // Add fields to grid in 3 columns
        int row = 0;
        // Column 1 - Basic Info
        formGrid.add(new Label("Code:") {{ setStyle(labelStyle); }}, 0, row);
        formGrid.add(codeField, 1, row);
        formGrid.add(new Label("Min Salary:") {{ setStyle(labelStyle); }}, 2, row);
        formGrid.add(minSalaryField, 3, row++);
        
        formGrid.add(new Label("Name:") {{ setStyle(labelStyle); }}, 0, row);
        formGrid.add(nameField, 1, row);
        formGrid.add(new Label("Max Salary:") {{ setStyle(labelStyle); }}, 2, row);
        formGrid.add(maxSalaryField, 3, row++);
        
        formGrid.add(new Label("Description:") {{ setStyle(labelStyle); }}, 0, row);
        formGrid.add(descriptionField, 1, row);
        formGrid.add(new Label("Employee Share:") {{ setStyle(labelStyle); }}, 2, row);
        formGrid.add(employeeShareField, 3, row++);
        
        formGrid.add(new Label("Basis:") {{ setStyle(labelStyle); }}, 0, row);
        formGrid.add(basisCombo, 1, row);
        formGrid.add(new Label("Employer Share:") {{ setStyle(labelStyle); }}, 2, row);
        formGrid.add(employerShareField, 3, row++);
        
        formGrid.add(new Label("Rate %:") {{ setStyle(labelStyle); }}, 0, row);
        formGrid.add(rateField, 1, row);
        formGrid.add(new Label("Base Tax:") {{ setStyle(labelStyle); }}, 2, row);
        formGrid.add(baseTaxField, 3, row++);
        
        formGrid.add(new Label("Fixed Amount:") {{ setStyle(labelStyle); }}, 0, row);
        formGrid.add(fixedAmountField, 1, row);
        formGrid.add(new Label("Excess Over:") {{ setStyle(labelStyle); }}, 2, row);
        formGrid.add(excessOverField, 3, row++);
        
        formGrid.add(new Label("Eff. Employee %:") {{ setStyle(labelStyle); }}, 0, row);
        formGrid.add(effectiveEmployeePercentField, 1, row);
        formGrid.add(new Label("Eff. Employer %:") {{ setStyle(labelStyle); }}, 2, row);
        formGrid.add(effectiveEmployerPercentField, 3, row++);
        
        formGrid.add(new Label("Eff. Total %:") {{ setStyle(labelStyle); }}, 0, row);
        formGrid.add(effectiveTotalPercentField, 1, row, 3, 1);

        // Populate fields if editing deduction
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
        
        // Add all elements to content container
        contentContainer.getChildren().addAll(titleLabel, formGrid);
        
        // Add content to main container
        mainContainer.getChildren().add(contentContainer);
        
        dialog.getDialogPane().setContent(mainContainer);
        
        // Style the buttons
        dialog.getDialogPane().lookupButton(saveButtonType).setStyle(
            "-fx-background-color: linear-gradient(to right, #2e7d32, #43a047); " +
            "-fx-text-fill: white; " +
            "-fx-font-weight: bold; " +
            "-fx-background-radius: 8; " +
            "-fx-padding: 10 20 10 20; " +
            "-fx-font-size: 14px;"
        );
        
        dialog.getDialogPane().lookupButton(ButtonType.CANCEL).setStyle(
            "-fx-background-color: #f5f5f5; " +
            "-fx-text-fill: #666; " +
            "-fx-background-radius: 8; " +
            "-fx-padding: 10 20 10 20; " +
            "-fx-font-size: 14px;" +
            "-fx-border-color: #ddd; " +
            "-fx-border-radius: 8;"
        );
        
        // Set dialog size to fit content properly
        dialog.getDialogPane().setPrefWidth(760);
        dialog.getDialogPane().setPrefHeight(480);

        // Remove validation from event filter since it prevents the dialog from closing
        // Validation will be handled in the result converter

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                // Validate required fields
                if (codeField.getText() == null || codeField.getText().trim().isEmpty()) {
                    showErrorAlert("Code is required.");
                    return null;
                }
                if (nameField.getText() == null || nameField.getText().trim().isEmpty()) {
                    showErrorAlert("Name is required.");
                    return null;
                }
                if (basisCombo.getValue() == null || basisCombo.getValue().isEmpty()) {
                    showErrorAlert("Basis is required.");
                    return null;
                }
                
                try {
                    return new DeductionType(
                        deduction != null ? deduction.getId() : 0, // ID will be auto-generated by database
                        codeField.getText().trim(),
                        nameField.getText().trim(),
                        descriptionField.getText() != null ? descriptionField.getText().trim() : "",
                        basisCombo.getValue(),
                        rateField.getText().trim().isEmpty() ? null : Double.parseDouble(rateField.getText().trim()),
                        fixedAmountField.getText().trim().isEmpty() ? null : Double.parseDouble(fixedAmountField.getText().trim()),
                        minSalaryField.getText().trim().isEmpty() ? 0.0 : Double.parseDouble(minSalaryField.getText().trim()),
                        maxSalaryField.getText().trim().isEmpty() ? 0.0 : Double.parseDouble(maxSalaryField.getText().trim()),
                        employeeShareField.getText().trim().isEmpty() ? 0.0 : Double.parseDouble(employeeShareField.getText().trim()),
                        employerShareField.getText().trim().isEmpty() ? 0.0 : Double.parseDouble(employerShareField.getText().trim()),
                        baseTaxField.getText().trim().isEmpty() ? null : Double.parseDouble(baseTaxField.getText().trim()),
                        excessOverField.getText().trim().isEmpty() ? null : Double.parseDouble(excessOverField.getText().trim()),
                        effectiveEmployeePercentField.getText().trim().isEmpty() ? 0.0 : Double.parseDouble(effectiveEmployeePercentField.getText().trim()),
                        effectiveEmployerPercentField.getText().trim().isEmpty() ? 0.0 : Double.parseDouble(effectiveEmployerPercentField.getText().trim()),
                        effectiveTotalPercentField.getText().trim().isEmpty() ? 0.0 : Double.parseDouble(effectiveTotalPercentField.getText().trim())
                    );
                } catch (NumberFormatException ex) {
                    showErrorAlert("Please enter valid numeric values for all numeric fields.\nError: " + ex.getMessage());
                    return null;
                }
            }
            return null;
        });

        return dialog;
    }

    private void showSuccessAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Dedcutions");
        alert.setHeaderText("Success");
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showErrorAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Dedcutions");
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