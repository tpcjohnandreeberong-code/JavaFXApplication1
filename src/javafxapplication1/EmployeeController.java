package javafxapplication1;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.net.URL;
import java.sql.*;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.scene.layout.GridPane;

public class EmployeeController implements Initializable {

    private static final Logger logger = Logger.getLogger(EmployeeController.class.getName());
    
    // Database connection parameters
    private static final String DB_URL = DatabaseConfig.getDbUrl();
    private static final String DB_USER = DatabaseConfig.getDbUser();
    private static final String DB_PASSWORD = DatabaseConfig.getDbPassword();
    private Connection connection;

    public static class Employee {
        private final int id;
        private final String accountNumber;
        private final String name;
        private final String position;
        private final double salary;

        public Employee(int id, String accountNumber, String name, String position, double salary) {
            this.id = id;
            this.accountNumber = accountNumber;
            this.name = name;
            this.position = position;
            this.salary = salary;
        }
        public int getId() { return id; }
        public String getAccountNumber() { return accountNumber; }
        public String getName() { return name; }
        public String getPosition() { return position; }
        public double getSalary() { return salary; }
    }

    @FXML private TextField searchField;
    @FXML private TableView<Employee> table;
    @FXML private TableColumn<Employee, Integer> colId;
    @FXML private TableColumn<Employee, String> colAccountNumber;
    @FXML private TableColumn<Employee, String> colName;
    @FXML private TableColumn<Employee, String> colPosition;
    @FXML private TableColumn<Employee, Double> colSalary;

    private final ObservableList<Employee> data = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        initializeDatabase();
        setupTableColumns();
        loadEmployeesFromDatabase();
    }
    
    private void initializeDatabase() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            logger.info("Database connection established for Employee management");
            createEmployeeTable();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Database connection failed", e);
            showErrorAlert("Database Error", "Cannot connect to database. Using offline mode.");
        }
    }
    
    private void createEmployeeTable() {
        try {
            String createTableSQL = """
                CREATE TABLE IF NOT EXISTS employees (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    account_number VARCHAR(50) UNIQUE NOT NULL,
                    full_name VARCHAR(100) NOT NULL,
                    position VARCHAR(100),
                    salary DECIMAL(10,2),
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
                )
            """;
            
            PreparedStatement stmt = connection.prepareStatement(createTableSQL);
            stmt.executeUpdate();
            logger.info("Employee table created/verified successfully");
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error creating employee table", e);
        }
    }

    private void setupTableColumns() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colAccountNumber.setCellValueFactory(new PropertyValueFactory<>("accountNumber"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colPosition.setCellValueFactory(new PropertyValueFactory<>("position"));
        colSalary.setCellValueFactory(new PropertyValueFactory<>("salary"));

        // Set column resize policy to fill available space
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Format salary column
        colSalary.setCellFactory(column -> new TableCell<Employee, Double>() {
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

    private void loadEmployeesFromDatabase() {
        if (connection == null) {
            return;
        }
        
        try {
            String query = "SELECT id, account_number, full_name, position, salary FROM employees ORDER BY id";
            PreparedStatement stmt = connection.prepareStatement(query);
            ResultSet rs = stmt.executeQuery();
            
            data.clear();
            while (rs.next()) {
                Employee employee = new Employee(
                    rs.getInt("id"),
                    rs.getString("account_number"),
                    rs.getString("full_name"),
                    rs.getString("position"),
                    rs.getDouble("salary")
                );
                data.add(employee);
            }
            
            table.setItems(data);
            logger.info("Loaded " + data.size() + " employees from database");
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error loading employees from database", e);
            showErrorAlert("Database Error", "Failed to load employees: " + e.getMessage());
        }
    }

    @FXML
    private void onSearch() {
        String searchText = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        
        if (searchText.isEmpty()) {
            table.setItems(data);
            return;
        }

        ObservableList<Employee> filtered = FXCollections.observableArrayList();
        for (Employee employee : data) {
            boolean matchesSearch = searchText.isEmpty() || 
                                  String.valueOf(employee.getId()).contains(searchText) || 
                                  employee.getAccountNumber().toLowerCase().contains(searchText) ||
                                  employee.getName().toLowerCase().contains(searchText) ||
                                  employee.getPosition().toLowerCase().contains(searchText);
            
            if (matchesSearch) {
                filtered.add(employee);
            }
        }
        table.setItems(filtered);
    }

    @FXML
    private void onAdd() {
        Dialog<Employee> dialog = buildEmployeeDialog(null);
        Optional<Employee> result = dialog.showAndWait();
        result.ifPresent(employee -> {
            if (addEmployeeToDatabase(employee)) {
                loadEmployeesFromDatabase(); // Refresh the table
                showSuccessAlert("Employee added successfully!");
            }
        });
    }

    @FXML
    private void onEdit() {
        Employee selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) { showInfo("Please select a row to edit."); return; }
        Dialog<Employee> dialog = buildEmployeeDialog(selected);
        Optional<Employee> result = dialog.showAndWait();
        result.ifPresent(updated -> {
            if (updateEmployeeInDatabase(updated)) {
                loadEmployeesFromDatabase(); // Refresh the table
                showSuccessAlert("Employee updated successfully!");
            }
        });
    }

    @FXML
    private void onDelete() {
        Employee selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) { showInfo("Please select a row to delete."); return; }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Employee");
        confirm.setHeaderText(null);
        confirm.setContentText("Delete employee " + selected.getName() + "?");
        Optional<ButtonType> res = confirm.showAndWait();
        if (res.isPresent() && res.get() == ButtonType.OK) {
            if (deleteEmployeeFromDatabase(selected)) {
                loadEmployeesFromDatabase(); // Refresh the table
                showSuccessAlert("Employee deleted successfully!");
            }
        }
    }
    
    private boolean addEmployeeToDatabase(Employee employee) {
        if (connection == null) {
            showErrorAlert("Database Error", "No database connection available.");
            return false;
        }
        
        try {
            String query = "INSERT INTO employees (account_number, full_name, position, salary) VALUES (?, ?, ?, ?)";
            PreparedStatement stmt = connection.prepareStatement(query);
            stmt.setString(1, employee.getAccountNumber());
            stmt.setString(2, employee.getName());
            stmt.setString(3, employee.getPosition());
            stmt.setDouble(4, employee.getSalary());
            
            int rowsAffected = stmt.executeUpdate();
            logger.info("Employee added to database: " + employee.getName());
            return rowsAffected > 0;
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error adding employee to database", e);
            if (e.getErrorCode() == 1062) { // MySQL duplicate entry error
                showErrorAlert("Validation Error", "Account number already exists. Please use a unique account number.");
            } else {
                showErrorAlert("Database Error", "Failed to add employee: " + e.getMessage());
            }
            return false;
        }
    }
    
    private boolean updateEmployeeInDatabase(Employee employee) {
        if (connection == null) {
            showErrorAlert("Database Error", "No database connection available.");
            return false;
        }
        
        try {
            String query = "UPDATE employees SET account_number = ?, full_name = ?, position = ?, salary = ? WHERE id = ?";
            PreparedStatement stmt = connection.prepareStatement(query);
            stmt.setString(1, employee.getAccountNumber());
            stmt.setString(2, employee.getName());
            stmt.setString(3, employee.getPosition());
            stmt.setDouble(4, employee.getSalary());
            stmt.setInt(5, employee.getId());
            
            int rowsAffected = stmt.executeUpdate();
            logger.info("Employee updated in database: " + employee.getName());
            return rowsAffected > 0;
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error updating employee in database", e);
            if (e.getErrorCode() == 1062) { // MySQL duplicate entry error
                showErrorAlert("Validation Error", "Account number already exists. Please use a unique account number.");
            } else {
                showErrorAlert("Database Error", "Failed to update employee: " + e.getMessage());
            }
            return false;
        }
    }
    
    private boolean deleteEmployeeFromDatabase(Employee employee) {
        if (connection == null) {
            showErrorAlert("Database Error", "No database connection available.");
            return false;
        }
        
        try {
            String query = "DELETE FROM employees WHERE id = ?";
            PreparedStatement stmt = connection.prepareStatement(query);
            stmt.setInt(1, employee.getId());
            
            int rowsAffected = stmt.executeUpdate();
            logger.info("Employee deleted from database: " + employee.getName());
            return rowsAffected > 0;
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error deleting employee from database", e);
            showErrorAlert("Database Error", "Failed to delete employee: " + e.getMessage());
            return false;
        }
    }

    private Dialog<Employee> buildEmployeeDialog(Employee existing) {
        Dialog<Employee> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "Add Employee" : "Edit Employee");
        
        // Remove default header
        dialog.setHeaderText(null);
        
        // Custom button types with styled appearance
        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        // Create main container with green gradient background
        javafx.scene.layout.VBox mainContainer = new javafx.scene.layout.VBox();
        mainContainer.setStyle("-fx-background-color: linear-gradient(to bottom right, #2e7d32, #66bb6a); -fx-padding: 0;");
        
        // Create content container with white background
        javafx.scene.layout.VBox contentContainer = new javafx.scene.layout.VBox();
        contentContainer.setStyle("-fx-background-color: white;");
        contentContainer.setPadding(new javafx.geometry.Insets(40, 40, 40, 40));
        contentContainer.setSpacing(20);
        
        // Title Label
        Label titleLabel = new Label(existing == null ? "Add New Employee" : "Edit Employee");
        titleLabel.setStyle(
            "-fx-text-fill: #1b5e20; " +
            "-fx-font-size: 24px; " +
            "-fx-font-weight: bold; " +
            "-fx-alignment: center;"
        );
        
        // Form fields container
        javafx.scene.layout.VBox formContainer = new javafx.scene.layout.VBox();
        formContainer.setSpacing(18);
        
        // Styled input fields
        String fieldStyle = 
            "-fx-background-radius: 8; " +
            "-fx-border-radius: 8; " +
            "-fx-border-color: #c8e6c9; " +
            "-fx-border-width: 1; " +
            "-fx-focus-color: #66bb6a; " +
            "-fx-faint-focus-color: transparent; " +
            "-fx-padding: 6 10 6 10; " +
            "-fx-font-size: 14px; " +
            "-fx-pref-width: 350;";
            
        String labelStyle = 
            "-fx-text-fill: #1b5e20; " +
            "-fx-font-size: 14px; " +
            "-fx-font-weight: bold;";

        // Account Number field
        javafx.scene.layout.VBox accountNumberContainer = new javafx.scene.layout.VBox(8);
        Label accountNumberLabel = new Label("Account Number");
        accountNumberLabel.setStyle(labelStyle);
        TextField accountNumberField = new TextField();
        accountNumberField.setPromptText("Enter account number (e.g., EMP-001)");
        accountNumberField.setStyle(fieldStyle);
        accountNumberContainer.getChildren().addAll(accountNumberLabel, accountNumberField);
        
        // Full Name field
        javafx.scene.layout.VBox nameContainer = new javafx.scene.layout.VBox(8);
        Label nameLabel = new Label("Full Name");
        nameLabel.setStyle(labelStyle);
        TextField nameField = new TextField();
        nameField.setPromptText("Enter full name");
        nameField.setStyle(fieldStyle);
        nameContainer.getChildren().addAll(nameLabel, nameField);
        
        // Position field
        javafx.scene.layout.VBox positionContainer = new javafx.scene.layout.VBox(8);
        Label positionLabel = new Label("Position");
        positionLabel.setStyle(labelStyle);
        TextField positionField = new TextField();
        positionField.setPromptText("Enter position (e.g., Teacher I)");
        positionField.setStyle(fieldStyle);
        positionContainer.getChildren().addAll(positionLabel, positionField);
        
        // Salary field
        javafx.scene.layout.VBox salaryContainer = new javafx.scene.layout.VBox(8);
        Label salaryLabel = new Label("Salary");
        salaryLabel.setStyle(labelStyle);
        TextField salaryField = new TextField();
        salaryField.setPromptText("Enter salary amount (e.g., 25000.00)");
        salaryField.setStyle(fieldStyle);
        salaryContainer.getChildren().addAll(salaryLabel, salaryField);

        // Populate fields if editing employee
        if (existing != null) {
            accountNumberField.setText(existing.getAccountNumber());
            nameField.setText(existing.getName());
            positionField.setText(existing.getPosition());
            salaryField.setText(String.valueOf(existing.getSalary()));
        }

        // Add all form elements to form container
        formContainer.getChildren().addAll(
            accountNumberContainer,
            nameContainer, 
            positionContainer,
            salaryContainer
        );
        
        // Add all elements to content container
        contentContainer.getChildren().addAll(titleLabel, formContainer);
        
        // Add content to main container with minimal padding
        mainContainer.getChildren().add(contentContainer);
        mainContainer.setPadding(new javafx.geometry.Insets(0));
        
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
        
        // Set dialog size to fit content better
        dialog.getDialogPane().setPrefWidth(450);
        dialog.getDialogPane().setPrefHeight(450);

        // Add validation
        dialog.getDialogPane().lookupButton(saveButtonType).addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            if (accountNumberField.getText().trim().isEmpty()) {
                showErrorAlert("Validation Error", "Account number is required.");
                event.consume();
                return;
            }
            if (nameField.getText().trim().isEmpty()) {
                showErrorAlert("Validation Error", "Full name is required.");
                event.consume();
                return;
            }
            if (positionField.getText().trim().isEmpty()) {
                showErrorAlert("Validation Error", "Position is required.");
                event.consume();
                return;
            }
            if (salaryField.getText().trim().isEmpty()) {
                showErrorAlert("Validation Error", "Salary is required.");
                event.consume();
                return;
            }
            
            // Validate salary is a valid number
            try {
                double salary = Double.parseDouble(salaryField.getText().trim());
                if (salary < 0) {
                    showErrorAlert("Validation Error", "Salary must be a positive number.");
                    event.consume();
                    return;
                }
            } catch (NumberFormatException ex) {
                showErrorAlert("Validation Error", "Please enter a valid salary amount.");
                event.consume();
                return;
            }
        });

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                try {
                    double salary = Double.parseDouble(salaryField.getText().trim());
                    
                    // For existing employee, preserve the ID, for new employee, ID will be auto-generated
                    int employeeId = (existing != null) ? existing.getId() : 0;
                    
                    return new Employee(
                        employeeId, 
                        accountNumberField.getText().trim(),
                        nameField.getText().trim(), 
                        positionField.getText().trim(), 
                        salary
                    );
                } catch (NumberFormatException ex) {
                    // This should not happen due to validation, but just in case
                    showErrorAlert("Error", "Invalid salary format.");
                    return null;
                }
            }
            return null;
        });

        return dialog;
    }

    private void showInfo(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    private void showSuccessAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.getDialogPane().setStyle("-fx-background-color: #f5f5f5;");
        alert.showAndWait();
    }
    
    private void showErrorAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.getDialogPane().setStyle("-fx-background-color: #f5f5f5;");
        alert.showAndWait();
    }
}


