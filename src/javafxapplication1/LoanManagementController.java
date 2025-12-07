package javafxapplication1;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.fxml.Initializable;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.TableView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TextField;
import javafx.scene.control.ComboBox;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TextInputDialog;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.DatePicker;
import javafx.scene.layout.GridPane;
import java.sql.*;
import java.time.LocalDate;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Controller for Loan Management System
 * Manages employee loans (Pag-IBIG, SSS, Company loans)
 */
public class LoanManagementController implements Initializable {
    
    @FXML private TextField searchField;
    @FXML private ComboBox<String> loanTypeFilter;
    @FXML private TableView<LoanRecord> table;
    @FXML private TableColumn<LoanRecord, String> colId;
    @FXML private TableColumn<LoanRecord, String> colEmployeeName;
    @FXML private TableColumn<LoanRecord, String> colLoanType;
    @FXML private TableColumn<LoanRecord, String> colLoanAmount;
    @FXML private TableColumn<LoanRecord, String> colMonthlyAmortization;
    @FXML private TableColumn<LoanRecord, String> colBalance;
    @FXML private TableColumn<LoanRecord, String> colStartDate;
    @FXML private TableColumn<LoanRecord, String> colEndDate;
    @FXML private TableColumn<LoanRecord, String> colStatus;
    
    private ObservableList<LoanRecord> loanData = FXCollections.observableArrayList();
    private Connection connection;
    private static final Logger logger = Logger.getLogger(LoanManagementController.class.getName());
    
    // Database connection details
    private static final String DB_URL = DatabaseConfig.getDbUrl();
    private static final String DB_USER = DatabaseConfig.getDbUser();
    private static final String DB_PASSWORD = DatabaseConfig.getDbPassword();
    
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupTableColumns();
        initializeDatabase();
        setupLoanTypeFilter();
        loadLoanData();
    }
    
    private void setupTableColumns() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colEmployeeName.setCellValueFactory(new PropertyValueFactory<>("employeeName"));
        colLoanType.setCellValueFactory(new PropertyValueFactory<>("loanType"));
        colLoanAmount.setCellValueFactory(new PropertyValueFactory<>("loanAmount"));
        colMonthlyAmortization.setCellValueFactory(new PropertyValueFactory<>("monthlyAmortization"));
        colBalance.setCellValueFactory(new PropertyValueFactory<>("balance"));
        colStartDate.setCellValueFactory(new PropertyValueFactory<>("startDate"));
        colEndDate.setCellValueFactory(new PropertyValueFactory<>("endDate"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        
        table.setItems(loanData);
    }
    
    private void setupLoanTypeFilter() {
        loanTypeFilter.getItems().addAll("All Types", "Pag-IBIG Loan", "SSS Loan", "Company Loan");
        loanTypeFilter.setValue("All Types");
    }
    
    private void initializeDatabase() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            logger.info("Database connection established successfully");
            
            // Create required tables if they don't exist
            createLoanTablesIfNotExist();
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error initializing database connection", e);
            showAlert("Database Error", "Could not connect to database: " + e.getMessage());
        }
    }
    
    private void createLoanTablesIfNotExist() {
        // Create loan_types table
        String createLoanTypesSQL = """
            CREATE TABLE IF NOT EXISTS loan_types (
                id INT AUTO_INCREMENT PRIMARY KEY,
                name VARCHAR(100) NOT NULL UNIQUE,
                description TEXT NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """;
        
        // Create employee_loans table
        String createEmployeeLoansSQL = """
            CREATE TABLE IF NOT EXISTS employee_loans (
                id INT AUTO_INCREMENT PRIMARY KEY,
                employee_id INT NOT NULL,
                loan_type_id INT NOT NULL,
                loan_amount DECIMAL(10,2) NOT NULL,
                monthly_amortization DECIMAL(10,2) NOT NULL,
                balance DECIMAL(10,2) NOT NULL,
                start_date DATE NOT NULL,
                end_date DATE NULL,
                status ENUM('Active','Completed','Stopped') DEFAULT 'Active',
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                FOREIGN KEY (employee_id) REFERENCES employees(id),
                FOREIGN KEY (loan_type_id) REFERENCES loan_types(id),
                INDEX idx_employee_id (employee_id),
                INDEX idx_loan_type_id (loan_type_id),
                INDEX idx_status (status)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """;
        
        try (PreparedStatement stmt1 = connection.prepareStatement(createLoanTypesSQL);
             PreparedStatement stmt2 = connection.prepareStatement(createEmployeeLoansSQL)) {
            
            stmt1.executeUpdate();
            stmt2.executeUpdate();
            
            // Insert default loan types
            insertDefaultLoanTypes();
            
            logger.info("Loan management tables created or already exist");
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error creating loan management tables", e);
        }
    }
    
    private void insertDefaultLoanTypes() {
        String insertSQL = "INSERT IGNORE INTO loan_types (name) VALUES (?)";
        
        String[][] defaultLoanTypes = {
            {"Pag-IBIG Loan", "Home Development Mutual Fund loan"},
            {"SSS Loan", "Social Security System loan"},
            {"Company Loan", "Internal company loan"}
        };
        
        try (PreparedStatement stmt = connection.prepareStatement(insertSQL)) {
            for (String[] loanType : defaultLoanTypes) {
                stmt.setString(1, loanType[0]);
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Error inserting default loan types", e);
        }
    }
    
    @FXML
    private void onAdd() {
        Dialog<LoanData> dialog = createLoanDialog("Add New Loan", null);
        Optional<LoanData> result = dialog.showAndWait();
        
        if (result.isPresent()) {
            LoanData loanData = result.get();
            addLoan(loanData);
            loadLoanData();
        }
    }
    
    @FXML
    private void onEdit() {
        LoanRecord selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("No Selection", "Please select a loan record to edit");
            return;
        }
        
        // Create loan data from selected record for editing
        LoanData currentData = new LoanData();
        currentData.employeeId = Integer.parseInt(selected.getId().split("-")[0]); // Assuming format: employeeId-loanId
        currentData.loanType = selected.getLoanType();
        currentData.loanAmount = new BigDecimal(selected.getLoanAmount().replace("₱", "").replace(",", ""));
        currentData.monthlyAmortization = new BigDecimal(selected.getMonthlyAmortization().replace("₱", "").replace(",", ""));
        currentData.startDate = LocalDate.parse(selected.getStartDate());
        
        Dialog<LoanData> dialog = createLoanDialog("Edit Loan", currentData);
        Optional<LoanData> result = dialog.showAndWait();
        
        if (result.isPresent()) {
            LoanData updatedData = result.get();
            updateLoan(Integer.parseInt(selected.getId()), updatedData);
            loadLoanData();
        }
    }
    
    @FXML
    private void onPayment() {
        LoanRecord selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("No Selection", "Please select a loan record to process payment");
            return;
        }
        
        if (!"Active".equals(selected.getStatus())) {
            showAlert("Invalid Status", "Can only process payments for active loans");
            return;
        }
        
        TextInputDialog dialog = new TextInputDialog(selected.getMonthlyAmortization());
        dialog.setTitle("Process Loan Payment");
        dialog.setHeaderText("Process Payment for " + selected.getEmployeeName());
        dialog.setContentText("Payment Amount (₱):");
        
        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            try {
                BigDecimal paymentAmount = new BigDecimal(result.get());
                if (paymentAmount.compareTo(BigDecimal.ZERO) <= 0) {
                    showAlert("Invalid Amount", "Payment amount must be greater than 0");
                    return;
                }
                
                processLoanPayment(Integer.parseInt(selected.getId()), paymentAmount);
                loadLoanData();
                
            } catch (NumberFormatException e) {
                showAlert("Invalid Input", "Please enter a valid payment amount");
            }
        }
    }
    
    @FXML
    private void onMarkComplete() {
        LoanRecord selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("No Selection", "Please select a loan record to mark as complete");
            return;
        }
        
        Alert confirmAlert = new Alert(AlertType.CONFIRMATION);
        confirmAlert.setTitle("Mark Loan Complete");
        confirmAlert.setHeaderText("Mark Loan as Completed");
        confirmAlert.setContentText("Are you sure you want to mark this loan as completed?\n" +
                                  "Employee: " + selected.getEmployeeName() + "\n" +
                                  "Loan Type: " + selected.getLoanType() + "\n" +
                                  "Remaining Balance: " + selected.getBalance());
        
        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                markLoanCompleted(Integer.parseInt(selected.getId()));
                loadLoanData();
            }
        });
    }
    
    @FXML
    private void onSearch() {
        String searchText = searchField.getText().trim();
        String selectedType = loanTypeFilter.getValue();
        
        if (searchText.isEmpty() && "All Types".equals(selectedType)) {
            loadLoanData();
            return;
        }
        
        StringBuilder sql = new StringBuilder("""
            SELECT el.id, el.employee_id, e.full_name as employee_name, 
                   lt.name as loan_type, el.loan_amount, el.monthly_amortization,
                   el.balance, el.start_date, el.end_date, el.status
            FROM employee_loans el
            JOIN employees e ON el.employee_id = e.id
            JOIN loan_types lt ON el.loan_type_id = lt.id
            WHERE 1=1
            """);
        
        if (!searchText.isEmpty()) {
            sql.append(" AND (e.full_name LIKE ? OR e.id LIKE ?)");
        }
        
        if (!"All Types".equals(selectedType)) {
            sql.append(" AND lt.name = ?");
        }
        
        sql.append(" ORDER BY el.created_at DESC");
        
        try (PreparedStatement stmt = connection.prepareStatement(sql.toString())) {
            int paramIndex = 1;
            
            if (!searchText.isEmpty()) {
                String searchPattern = "%" + searchText + "%";
                stmt.setString(paramIndex++, searchPattern);
                stmt.setString(paramIndex++, searchPattern);
            }
            
            if (!"All Types".equals(selectedType)) {
                stmt.setString(paramIndex, selectedType);
            }
            
            ResultSet rs = stmt.executeQuery();
            loanData.clear();
            
            while (rs.next()) {
                LoanRecord loan = createLoanRecordFromResultSet(rs);
                loanData.add(loan);
            }
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error searching loan records", e);
            showAlert("Database Error", "Could not search loan records: " + e.getMessage());
        }
    }
    
    private Dialog<LoanData> createLoanDialog(String title, LoanData existingData) {
        Dialog<LoanData> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.setHeaderText("Enter loan details");
        
        // Set the button types
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        
        // Create the form
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        
        ComboBox<String> employeeCombo = new ComboBox<>();
        ComboBox<String> loanTypeCombo = new ComboBox<>();
        TextField loanAmountField = new TextField();
        TextField monthlyAmortizationField = new TextField();
        DatePicker startDatePicker = new DatePicker();
        
        // Load employees and loan types
        loadEmployeesIntoComboBox(employeeCombo);
        loanTypeCombo.getItems().addAll("Pag-IBIG Loan", "SSS Loan", "Company Loan");
        
        // Set existing data if editing
        if (existingData != null) {
            loanTypeCombo.setValue(existingData.loanType);
            loanAmountField.setText(existingData.loanAmount.toString());
            monthlyAmortizationField.setText(existingData.monthlyAmortization.toString());
            startDatePicker.setValue(existingData.startDate);
        } else {
            startDatePicker.setValue(LocalDate.now());
        }
        
        grid.add(new Label("Employee:"), 0, 0);
        grid.add(employeeCombo, 1, 0);
        grid.add(new Label("Loan Type:"), 0, 1);
        grid.add(loanTypeCombo, 1, 1);
        grid.add(new Label("Loan Amount (₱):"), 0, 2);
        grid.add(loanAmountField, 1, 2);
        grid.add(new Label("Monthly Amortization (₱):"), 0, 3);
        grid.add(monthlyAmortizationField, 1, 3);
        grid.add(new Label("Start Date:"), 0, 4);
        grid.add(startDatePicker, 1, 4);
        
        dialog.getDialogPane().setContent(grid);
        
        // Convert the result when the OK button is clicked
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                try {
                    LoanData data = new LoanData();
                    String selectedEmployee = employeeCombo.getValue();
                    if (selectedEmployee != null) {
                        data.employeeId = Integer.parseInt(selectedEmployee.split(" - ")[0]);
                    }
                    data.loanType = loanTypeCombo.getValue();
                    data.loanAmount = new BigDecimal(loanAmountField.getText());
                    data.monthlyAmortization = new BigDecimal(monthlyAmortizationField.getText());
                    data.startDate = startDatePicker.getValue();
                    
                    // Validate data
                    if (data.employeeId == 0 || data.loanType == null || 
                        data.loanAmount.compareTo(BigDecimal.ZERO) <= 0 ||
                        data.monthlyAmortization.compareTo(BigDecimal.ZERO) <= 0 ||
                        data.startDate == null) {
                        showAlert("Invalid Input", "Please fill in all required fields with valid values");
                        return null;
                    }
                    
                    return data;
                    
                } catch (NumberFormatException e) {
                    showAlert("Invalid Input", "Please enter valid numeric amounts");
                    return null;
                }
            }
            return null;
        });
        
        return dialog;
    }
    
    private void loadEmployeesIntoComboBox(ComboBox<String> comboBox) {
        String sql = "SELECT id, full_name as name FROM employees ORDER BY full_name";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                String item = rs.getInt("id") + " - " + rs.getString("name");
                comboBox.getItems().add(item);
            }
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error loading employees", e);
        }
    }
    
    private void addLoan(LoanData loanData) {
        String sql = "INSERT INTO employee_loans (employee_id, loan_type_id, loan_amount, monthly_amortization, balance, start_date) VALUES (?, ?, ?, ?, ?, ?)";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            int loanTypeId = getLoanTypeId(loanData.loanType);
            
            stmt.setInt(1, loanData.employeeId);
            stmt.setInt(2, loanTypeId);
            stmt.setBigDecimal(3, loanData.loanAmount);
            stmt.setBigDecimal(4, loanData.monthlyAmortization);
            stmt.setBigDecimal(5, loanData.loanAmount); // Initial balance is loan amount
            stmt.setDate(6, Date.valueOf(loanData.startDate));
            
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected > 0) {
                showAlert("Success", "Loan added successfully!");
                logger.info("Added loan for employee ID: " + loanData.employeeId);
            }
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error adding loan", e);
            showAlert("Database Error", "Could not add loan: " + e.getMessage());
        }
    }
    
    private void updateLoan(int loanId, LoanData loanData) {
        String sql = "UPDATE employee_loans SET loan_type_id = ?, loan_amount = ?, monthly_amortization = ?, start_date = ? WHERE id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            int loanTypeId = getLoanTypeId(loanData.loanType);
            
            stmt.setInt(1, loanTypeId);
            stmt.setBigDecimal(2, loanData.loanAmount);
            stmt.setBigDecimal(3, loanData.monthlyAmortization);
            stmt.setDate(4, Date.valueOf(loanData.startDate));
            stmt.setInt(5, loanId);
            
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected > 0) {
                showAlert("Success", "Loan updated successfully!");
                logger.info("Updated loan ID: " + loanId);
            }
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error updating loan", e);
            showAlert("Database Error", "Could not update loan: " + e.getMessage());
        }
    }
    
    private void processLoanPayment(int loanId, BigDecimal paymentAmount) {
        String sql = "UPDATE employee_loans SET balance = GREATEST(0, balance - ?), status = CASE WHEN balance - ? <= 0 THEN 'Completed' ELSE status END WHERE id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setBigDecimal(1, paymentAmount);
            stmt.setBigDecimal(2, paymentAmount);
            stmt.setInt(3, loanId);
            
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected > 0) {
                showAlert("Success", "Payment processed successfully!");
                logger.info("Processed payment for loan ID: " + loanId + ", Amount: " + paymentAmount);
            }
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error processing loan payment", e);
            showAlert("Database Error", "Could not process payment: " + e.getMessage());
        }
    }
    
    private void markLoanCompleted(int loanId) {
        String sql = "UPDATE employee_loans SET status = 'Completed', end_date = CURDATE(), balance = 0 WHERE id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, loanId);
            
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected > 0) {
                showAlert("Success", "Loan marked as completed successfully!");
                logger.info("Marked loan as completed, ID: " + loanId);
            }
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error marking loan as completed", e);
            showAlert("Database Error", "Could not mark loan as completed: " + e.getMessage());
        }
    }
    
    private int getLoanTypeId(String loanTypeName) throws SQLException {
        String sql = "SELECT id FROM loan_types WHERE name = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, loanTypeName);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt("id");
            } else {
                throw new SQLException("Loan type not found: " + loanTypeName);
            }
        }
    }
    
    private void loadLoanData() {
        String sql = """
            SELECT el.id, el.employee_id, e.full_name as employee_name, 
                   lt.name as loan_type, el.loan_amount, el.monthly_amortization,
                   el.balance, el.start_date, el.end_date, el.status
            FROM employee_loans el
            JOIN employees e ON el.employee_id = e.id
            JOIN loan_types lt ON el.loan_type_id = lt.id
            ORDER BY el.created_at DESC
            """;
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            loanData.clear();
            
            while (rs.next()) {
                LoanRecord loan = createLoanRecordFromResultSet(rs);
                loanData.add(loan);
            }
            
            logger.info("Loaded " + loanData.size() + " loan records");
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error loading loan data", e);
            showAlert("Database Error", "Could not load loan data: " + e.getMessage());
        }
    }
    
    private LoanRecord createLoanRecordFromResultSet(ResultSet rs) throws SQLException {
        Date endDate = rs.getDate("end_date");
        return new LoanRecord(
            String.valueOf(rs.getInt("id")),
            rs.getString("employee_name"),
            rs.getString("loan_type"),
            String.format("₱%.2f", rs.getBigDecimal("loan_amount")),
            String.format("₱%.2f", rs.getBigDecimal("monthly_amortization")),
            String.format("₱%.2f", rs.getBigDecimal("balance")),
            rs.getDate("start_date").toString(),
            endDate != null ? endDate.toString() : "N/A",
            rs.getString("status")
        );
    }
    
    private void showAlert(String title, String content) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
    
    // Inner classes for data models
    private static class LoanData {
        int employeeId;
        String loanType;
        BigDecimal loanAmount;
        BigDecimal monthlyAmortization;
        LocalDate startDate;
    }
    
    public static class LoanRecord {
        private final SimpleStringProperty id;
        private final SimpleStringProperty employeeName;
        private final SimpleStringProperty loanType;
        private final SimpleStringProperty loanAmount;
        private final SimpleStringProperty monthlyAmortization;
        private final SimpleStringProperty balance;
        private final SimpleStringProperty startDate;
        private final SimpleStringProperty endDate;
        private final SimpleStringProperty status;
        
        public LoanRecord(String id, String employeeName, String loanType, String loanAmount,
                         String monthlyAmortization, String balance, String startDate, 
                         String endDate, String status) {
            this.id = new SimpleStringProperty(id);
            this.employeeName = new SimpleStringProperty(employeeName);
            this.loanType = new SimpleStringProperty(loanType);
            this.loanAmount = new SimpleStringProperty(loanAmount);
            this.monthlyAmortization = new SimpleStringProperty(monthlyAmortization);
            this.balance = new SimpleStringProperty(balance);
            this.startDate = new SimpleStringProperty(startDate);
            this.endDate = new SimpleStringProperty(endDate);
            this.status = new SimpleStringProperty(status);
        }
        
        public String getId() { return id.get(); }
        public String getEmployeeName() { return employeeName.get(); }
        public String getLoanType() { return loanType.get(); }
        public String getLoanAmount() { return loanAmount.get(); }
        public String getMonthlyAmortization() { return monthlyAmortization.get(); }
        public String getBalance() { return balance.get(); }
        public String getStartDate() { return startDate.get(); }
        public String getEndDate() { return endDate.get(); }
        public String getStatus() { return status.get(); }
    }
}