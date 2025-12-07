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
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TextInputDialog;
import javafx.beans.property.SimpleStringProperty;
import java.sql.*;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Controller for Salary Reference Management
 * Manages rate per day, half-day rate, and rate per minute calculations
 */
public class SalaryReferenceController implements Initializable {
    
    @FXML private TextField searchField;
    @FXML private TableView<SalaryReference> table;
    @FXML private TableColumn<SalaryReference, String> colId;
    @FXML private TableColumn<SalaryReference, String> colMonthlySalary;
    @FXML private TableColumn<SalaryReference, String> colRatePerDay;
    @FXML private TableColumn<SalaryReference, String> colHalfDayRate;
    @FXML private TableColumn<SalaryReference, String> colRatePerMinute;
    
    private ObservableList<SalaryReference> salaryData = FXCollections.observableArrayList();
    private Connection connection;
    private static final Logger logger = Logger.getLogger(SalaryReferenceController.class.getName());
    
    // Database connection details
    private static final String DB_URL = DatabaseConfig.getDbUrl();
    private static final String DB_USER = DatabaseConfig.getDbUser();
    private static final String DB_PASSWORD = DatabaseConfig.getDbPassword();
    
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupTableColumns();
        initializeDatabase();
        loadSalaryReferenceData();
    }
    
    private void setupTableColumns() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colMonthlySalary.setCellValueFactory(new PropertyValueFactory<>("monthlySalary"));
        colRatePerDay.setCellValueFactory(new PropertyValueFactory<>("ratePerDay"));
        colHalfDayRate.setCellValueFactory(new PropertyValueFactory<>("halfDayRate"));
        colRatePerMinute.setCellValueFactory(new PropertyValueFactory<>("ratePerMinute"));
        
        table.setItems(salaryData);
    }
    
    private void initializeDatabase() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            logger.info("Database connection established successfully");
            
            // Create salary_reference table if it doesn't exist
            createSalaryReferenceTableIfNotExists();
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error initializing database connection", e);
            showAlert("Database Error", "Could not connect to database: " + e.getMessage());
        }
    }
    
    private void createSalaryReferenceTableIfNotExists() {
        String createTableSQL = """
            CREATE TABLE IF NOT EXISTS salary_reference (
                id INT AUTO_INCREMENT PRIMARY KEY,
                monthly_salary DECIMAL(10,2) NOT NULL,
                rate_per_day DECIMAL(10,2) NOT NULL,
                half_day_rate DECIMAL(10,2) NOT NULL,
                rate_per_minute DECIMAL(10,4) NOT NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                UNIQUE KEY unique_monthly_salary (monthly_salary)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """;
        
        try (PreparedStatement stmt = connection.prepareStatement(createTableSQL)) {
            stmt.executeUpdate();
            logger.info("Salary reference table created or already exists");
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error creating salary reference table", e);
        }
    }
    
    @FXML
    private void onAdd() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Add Salary Reference");
        dialog.setHeaderText("Enter Monthly Salary Amount");
        dialog.setContentText("Monthly Salary (₱):");
        
        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            try {
                BigDecimal monthlySalary = new BigDecimal(result.get());
                if (monthlySalary.compareTo(BigDecimal.ZERO) <= 0) {
                    showAlert("Invalid Input", "Monthly salary must be greater than 0");
                    return;
                }
                
                // Calculate rates
                BigDecimal ratePerDay = calculateRatePerDay(monthlySalary);
                BigDecimal halfDayRate = calculateHalfDayRate(ratePerDay);
                BigDecimal ratePerMinute = calculateRatePerMinute(ratePerDay);
                
                addSalaryReference(monthlySalary, ratePerDay, halfDayRate, ratePerMinute);
                loadSalaryReferenceData();
                
            } catch (NumberFormatException e) {
                showAlert("Invalid Input", "Please enter a valid monetary amount");
            }
        }
    }
    
    private BigDecimal calculateRatePerDay(BigDecimal monthlySalary) {
        // Monthly salary divided by 22 working days (standard)
        return monthlySalary.divide(new BigDecimal("22"), 2, RoundingMode.HALF_UP);
    }
    
    private BigDecimal calculateHalfDayRate(BigDecimal ratePerDay) {
        // Half of the daily rate
        return ratePerDay.divide(new BigDecimal("2"), 2, RoundingMode.HALF_UP);
    }
    
    private BigDecimal calculateRatePerMinute(BigDecimal ratePerDay) {
        // Daily rate divided by 480 minutes (8 hours * 60 minutes)
        return ratePerDay.divide(new BigDecimal("480"), 4, RoundingMode.HALF_UP);
    }
    
    private void addSalaryReference(BigDecimal monthlySalary, BigDecimal ratePerDay, 
                                  BigDecimal halfDayRate, BigDecimal ratePerMinute) {
        String sql = "INSERT INTO salary_reference (monthly_salary, rate_per_day, half_day_rate, rate_per_minute) VALUES (?, ?, ?, ?)";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setBigDecimal(1, monthlySalary);
            stmt.setBigDecimal(2, ratePerDay);
            stmt.setBigDecimal(3, halfDayRate);
            stmt.setBigDecimal(4, ratePerMinute);
            
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected > 0) {
                showAlert("Success", "Salary reference added successfully!");
                logger.info("Added salary reference for monthly salary: " + monthlySalary);
            }
            
        } catch (SQLException e) {
            if (e.getErrorCode() == 1062) { // Duplicate entry
                showAlert("Duplicate Entry", "Salary reference for this monthly salary already exists");
            } else {
                logger.log(Level.SEVERE, "Error adding salary reference", e);
                showAlert("Database Error", "Could not add salary reference: " + e.getMessage());
            }
        }
    }
    
    @FXML
    private void onEdit() {
        SalaryReference selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("No Selection", "Please select a salary reference to edit");
            return;
        }
        
        TextInputDialog dialog = new TextInputDialog(selected.getMonthlySalary());
        dialog.setTitle("Edit Salary Reference");
        dialog.setHeaderText("Edit Monthly Salary Amount");
        dialog.setContentText("Monthly Salary (₱):");
        
        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            try {
                BigDecimal newMonthlySalary = new BigDecimal(result.get());
                if (newMonthlySalary.compareTo(BigDecimal.ZERO) <= 0) {
                    showAlert("Invalid Input", "Monthly salary must be greater than 0");
                    return;
                }
                
                // Recalculate rates
                BigDecimal ratePerDay = calculateRatePerDay(newMonthlySalary);
                BigDecimal halfDayRate = calculateHalfDayRate(ratePerDay);
                BigDecimal ratePerMinute = calculateRatePerMinute(ratePerDay);
                
                updateSalaryReference(Integer.parseInt(selected.getId()), newMonthlySalary, 
                                    ratePerDay, halfDayRate, ratePerMinute);
                loadSalaryReferenceData();
                
            } catch (NumberFormatException e) {
                showAlert("Invalid Input", "Please enter a valid monetary amount");
            }
        }
    }
    
    private void updateSalaryReference(int id, BigDecimal monthlySalary, BigDecimal ratePerDay, 
                                     BigDecimal halfDayRate, BigDecimal ratePerMinute) {
        String sql = "UPDATE salary_reference SET monthly_salary = ?, rate_per_day = ?, half_day_rate = ?, rate_per_minute = ? WHERE id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setBigDecimal(1, monthlySalary);
            stmt.setBigDecimal(2, ratePerDay);
            stmt.setBigDecimal(3, halfDayRate);
            stmt.setBigDecimal(4, ratePerMinute);
            stmt.setInt(5, id);
            
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected > 0) {
                showAlert("Success", "Salary reference updated successfully!");
                logger.info("Updated salary reference ID: " + id);
            }
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error updating salary reference", e);
            showAlert("Database Error", "Could not update salary reference: " + e.getMessage());
        }
    }
    
    @FXML
    private void onDelete() {
        SalaryReference selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("No Selection", "Please select a salary reference to delete");
            return;
        }
        
        Alert confirmAlert = new Alert(AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirm Delete");
        confirmAlert.setHeaderText("Delete Salary Reference");
        confirmAlert.setContentText("Are you sure you want to delete this salary reference?\nMonthly Salary: ₱" + selected.getMonthlySalary());
        
        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == javafx.scene.control.ButtonType.OK) {
                deleteSalaryReference(Integer.parseInt(selected.getId()));
                loadSalaryReferenceData();
            }
        });
    }
    
    private void deleteSalaryReference(int id) {
        String sql = "DELETE FROM salary_reference WHERE id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected > 0) {
                showAlert("Success", "Salary reference deleted successfully!");
                logger.info("Deleted salary reference ID: " + id);
            }
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error deleting salary reference", e);
            showAlert("Database Error", "Could not delete salary reference: " + e.getMessage());
        }
    }
    
    @FXML
    private void onSearch() {
        String searchText = searchField.getText().trim();
        if (searchText.isEmpty()) {
            loadSalaryReferenceData();
            return;
        }
        
        String sql = "SELECT * FROM salary_reference WHERE monthly_salary LIKE ? OR rate_per_day LIKE ? ORDER BY monthly_salary";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            String searchPattern = "%" + searchText + "%";
            stmt.setString(1, searchPattern);
            stmt.setString(2, searchPattern);
            
            ResultSet rs = stmt.executeQuery();
            salaryData.clear();
            
            while (rs.next()) {
                SalaryReference salary = new SalaryReference(
                    String.valueOf(rs.getInt("id")),
                    String.format("%.2f", rs.getBigDecimal("monthly_salary")),
                    String.format("%.2f", rs.getBigDecimal("rate_per_day")),
                    String.format("%.2f", rs.getBigDecimal("half_day_rate")),
                    String.format("%.4f", rs.getBigDecimal("rate_per_minute"))
                );
                salaryData.add(salary);
            }
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error searching salary references", e);
            showAlert("Database Error", "Could not search salary references: " + e.getMessage());
        }
    }
    
    @FXML
    private void onCalculateAll() {
        Alert confirmAlert = new Alert(AlertType.CONFIRMATION);
        confirmAlert.setTitle("Recalculate All Rates");
        confirmAlert.setHeaderText("Recalculate All Salary Rates");
        confirmAlert.setContentText("This will recalculate all daily rates, half-day rates, and per-minute rates based on current monthly salaries. Continue?");
        
        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == javafx.scene.control.ButtonType.OK) {
                recalculateAllRates();
                loadSalaryReferenceData();
            }
        });
    }
    
    private void recalculateAllRates() {
        String selectSQL = "SELECT id, monthly_salary FROM salary_reference";
        String updateSQL = "UPDATE salary_reference SET rate_per_day = ?, half_day_rate = ?, rate_per_minute = ? WHERE id = ?";
        
        try (PreparedStatement selectStmt = connection.prepareStatement(selectSQL);
             PreparedStatement updateStmt = connection.prepareStatement(updateSQL)) {
            
            ResultSet rs = selectStmt.executeQuery();
            int updatedCount = 0;
            
            while (rs.next()) {
                int id = rs.getInt("id");
                BigDecimal monthlySalary = rs.getBigDecimal("monthly_salary");
                
                BigDecimal ratePerDay = calculateRatePerDay(monthlySalary);
                BigDecimal halfDayRate = calculateHalfDayRate(ratePerDay);
                BigDecimal ratePerMinute = calculateRatePerMinute(ratePerDay);
                
                updateStmt.setBigDecimal(1, ratePerDay);
                updateStmt.setBigDecimal(2, halfDayRate);
                updateStmt.setBigDecimal(3, ratePerMinute);
                updateStmt.setInt(4, id);
                
                updateStmt.executeUpdate();
                updatedCount++;
            }
            
            showAlert("Success", "Recalculated rates for " + updatedCount + " salary references!");
            logger.info("Recalculated rates for " + updatedCount + " salary references");
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error recalculating salary rates", e);
            showAlert("Database Error", "Could not recalculate rates: " + e.getMessage());
        }
    }
    
    private void loadSalaryReferenceData() {
        String sql = "SELECT * FROM salary_reference ORDER BY monthly_salary";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            salaryData.clear();
            
            while (rs.next()) {
                SalaryReference salary = new SalaryReference(
                    String.valueOf(rs.getInt("id")),
                    String.format("%.2f", rs.getBigDecimal("monthly_salary")),
                    String.format("%.2f", rs.getBigDecimal("rate_per_day")),
                    String.format("%.2f", rs.getBigDecimal("half_day_rate")),
                    String.format("%.4f", rs.getBigDecimal("rate_per_minute"))
                );
                salaryData.add(salary);
            }
            
            logger.info("Loaded " + salaryData.size() + " salary references");
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error loading salary reference data", e);
            showAlert("Database Error", "Could not load salary reference data: " + e.getMessage());
        }
    }
    
    private void showAlert(String title, String content) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
    
    // Inner class for SalaryReference data model
    public static class SalaryReference {
        private final SimpleStringProperty id;
        private final SimpleStringProperty monthlySalary;
        private final SimpleStringProperty ratePerDay;
        private final SimpleStringProperty halfDayRate;
        private final SimpleStringProperty ratePerMinute;
        
        public SalaryReference(String id, String monthlySalary, String ratePerDay, String halfDayRate, String ratePerMinute) {
            this.id = new SimpleStringProperty(id);
            this.monthlySalary = new SimpleStringProperty(monthlySalary);
            this.ratePerDay = new SimpleStringProperty(ratePerDay);
            this.halfDayRate = new SimpleStringProperty(halfDayRate);
            this.ratePerMinute = new SimpleStringProperty(ratePerMinute);
        }
        
        public String getId() { return id.get(); }
        public String getMonthlySalary() { return monthlySalary.get(); }
        public String getRatePerDay() { return ratePerDay.get(); }
        public String getHalfDayRate() { return halfDayRate.get(); }
        public String getRatePerMinute() { return ratePerMinute.get(); }
    }
}