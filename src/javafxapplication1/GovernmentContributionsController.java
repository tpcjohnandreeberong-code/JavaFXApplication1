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
import javafx.beans.property.SimpleStringProperty;
import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.math.BigDecimal;

/**
 * Controller for Government Contributions Management
 * Manages Pag-IBIG, SSS, PhilHealth, and Tax contribution rules
 */
public class GovernmentContributionsController implements Initializable {
    
    @FXML private TextField searchField;
    @FXML private ComboBox<String> contributionTypeFilter;
    @FXML private TableView<ContributionRecord> table;
    @FXML private TableColumn<ContributionRecord, String> colId;
    @FXML private TableColumn<ContributionRecord, String> colCode;
    @FXML private TableColumn<ContributionRecord, String> colName;
    @FXML private TableColumn<ContributionRecord, String> colBasis;
    @FXML private TableColumn<ContributionRecord, String> colRatePercent;
    @FXML private TableColumn<ContributionRecord, String> colFixedAmount;
    @FXML private TableColumn<ContributionRecord, String> colMinSalary;
    @FXML private TableColumn<ContributionRecord, String> colMaxSalary;
    @FXML private TableColumn<ContributionRecord, String> colEmployeeShare;
    @FXML private TableColumn<ContributionRecord, String> colEmployerShare;
    @FXML private TableColumn<ContributionRecord, String> colActive;
    
    private ObservableList<ContributionRecord> contributionData = FXCollections.observableArrayList();
    private Connection connection;
    private static final Logger logger = Logger.getLogger(GovernmentContributionsController.class.getName());
    
    // Database connection details
    private static final String DB_URL = DatabaseConfig.getDbUrl();
    private static final String DB_USER = DatabaseConfig.getDbUser();
    private static final String DB_PASSWORD = DatabaseConfig.getDbPassword();
    
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupTableColumns();
        initializeDatabase();
        setupContributionTypeFilter();
        loadContributionData();
    }
    
    private void setupTableColumns() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colCode.setCellValueFactory(new PropertyValueFactory<>("code"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colBasis.setCellValueFactory(new PropertyValueFactory<>("basis"));
        colRatePercent.setCellValueFactory(new PropertyValueFactory<>("ratePercent"));
        colFixedAmount.setCellValueFactory(new PropertyValueFactory<>("fixedAmount"));
        colMinSalary.setCellValueFactory(new PropertyValueFactory<>("minSalary"));
        colMaxSalary.setCellValueFactory(new PropertyValueFactory<>("maxSalary"));
        colEmployeeShare.setCellValueFactory(new PropertyValueFactory<>("employeeShare"));
        colEmployerShare.setCellValueFactory(new PropertyValueFactory<>("employerShare"));
        colActive.setCellValueFactory(new PropertyValueFactory<>("active"));
        
        table.setItems(contributionData);
    }
    
    private void setupContributionTypeFilter() {
        contributionTypeFilter.getItems().addAll("All Types", "SSS", "PHILHEALTH", "PAGIBIG", "TAX", "LATE");
        contributionTypeFilter.setValue("All Types");
    }
    
    private void initializeDatabase() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            logger.info("Database connection established successfully");
            
            // The deductions table should already exist based on your schema
            // We'll just verify and load the data
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error initializing database connection", e);
            showAlert("Database Error", "Could not connect to database: " + e.getMessage());
        }
    }
    
    @FXML
    private void onAdd() {
        // This would open a dialog to add new contribution rule
        showAlert("Feature Coming Soon", "Add new contribution rule feature will be implemented");
    }
    
    @FXML
    private void onEdit() {
        ContributionRecord selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("No Selection", "Please select a contribution rule to edit");
            return;
        }
        
        // This would open an edit dialog
        showAlert("Feature Coming Soon", "Edit contribution rule feature will be implemented");
    }
    
    @FXML
    private void onToggleActive() {
        ContributionRecord selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("No Selection", "Please select a contribution rule to toggle active status");
            return;
        }
        
        boolean newStatus = !"Yes".equals(selected.getActive());
        toggleContributionStatus(Integer.parseInt(selected.getId()), newStatus);
        loadContributionData();
    }
    
    @FXML
    private void onCalculatePreview() {
        showAlert("Preview", "This would show a preview calculation of contributions for all employees");
    }
    
    @FXML
    private void onSearch() {
        String searchText = searchField.getText().trim();
        String selectedType = contributionTypeFilter.getValue();
        
        if (searchText.isEmpty() && "All Types".equals(selectedType)) {
            loadContributionData();
            return;
        }
        
        searchContributions(searchText, selectedType);
    }
    
    private void toggleContributionStatus(int id, boolean isActive) {
        String sql = "UPDATE deductions SET is_active = ? WHERE id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setBoolean(1, isActive);
            stmt.setInt(2, id);
            
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected > 0) {
                String status = isActive ? "activated" : "deactivated";
                showAlert("Success", "Contribution rule " + status + " successfully!");
                logger.info("Toggled contribution status for ID: " + id + " to " + isActive);
            }
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error toggling contribution status", e);
            showAlert("Database Error", "Could not update contribution status: " + e.getMessage());
        }
    }
    
    private void searchContributions(String searchText, String selectedType) {
        StringBuilder sql = new StringBuilder("SELECT * FROM deductions WHERE 1=1");
        
        if (!searchText.isEmpty()) {
            sql.append(" AND (code LIKE ? OR name LIKE ?)");
        }
        
        if (!"All Types".equals(selectedType)) {
            sql.append(" AND code LIKE ?");
        }
        
        sql.append(" ORDER BY code");
        
        try (PreparedStatement stmt = connection.prepareStatement(sql.toString())) {
            int paramIndex = 1;
            
            if (!searchText.isEmpty()) {
                String searchPattern = "%" + searchText + "%";
                stmt.setString(paramIndex++, searchPattern);
                stmt.setString(paramIndex++, searchPattern);
            }
            
            if (!"All Types".equals(selectedType)) {
                stmt.setString(paramIndex, selectedType + "%");
            }
            
            ResultSet rs = stmt.executeQuery();
            contributionData.clear();
            
            while (rs.next()) {
                ContributionRecord contribution = createContributionRecordFromResultSet(rs);
                contributionData.add(contribution);
            }
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error searching contributions", e);
            showAlert("Database Error", "Could not search contributions: " + e.getMessage());
        }
    }
    
    private void loadContributionData() {
        String sql = "SELECT * FROM deductions ORDER BY code";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            contributionData.clear();
            
            while (rs.next()) {
                ContributionRecord contribution = createContributionRecordFromResultSet(rs);
                contributionData.add(contribution);
            }
            
            logger.info("Loaded " + contributionData.size() + " contribution records");
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error loading contribution data", e);
            showAlert("Database Error", "Could not load contribution data: " + e.getMessage());
        }
    }
    
    private ContributionRecord createContributionRecordFromResultSet(ResultSet rs) throws SQLException {
        BigDecimal ratePercent = rs.getBigDecimal("rate_percent");
        BigDecimal fixedAmount = rs.getBigDecimal("fixed_amount");
        BigDecimal minSalary = rs.getBigDecimal("min_salary");
        BigDecimal maxSalary = rs.getBigDecimal("max_salary");
        BigDecimal employeeShare = rs.getBigDecimal("employee_share");
        BigDecimal employerShare = rs.getBigDecimal("employer_share");
        
        return new ContributionRecord(
            String.valueOf(rs.getInt("id")),
            rs.getString("code"),
            rs.getString("name"),
            rs.getString("basis"),
            ratePercent != null ? String.format("%.2f%%", ratePercent) : "N/A",
            fixedAmount != null ? String.format("₱%.2f", fixedAmount) : "N/A",
            minSalary != null ? String.format("₱%.2f", minSalary) : "N/A",
            maxSalary != null ? String.format("₱%.2f", maxSalary) : "N/A",
            employeeShare != null ? String.format("%.2f%%", employeeShare) : "N/A",
            employerShare != null ? String.format("%.2f%%", employerShare) : "N/A",
            rs.getBoolean("is_active") ? "Yes" : "No"
        );
    }
    
    private void showAlert(String title, String content) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
    
    // Inner class for ContributionRecord data model
    public static class ContributionRecord {
        private final SimpleStringProperty id;
        private final SimpleStringProperty code;
        private final SimpleStringProperty name;
        private final SimpleStringProperty basis;
        private final SimpleStringProperty ratePercent;
        private final SimpleStringProperty fixedAmount;
        private final SimpleStringProperty minSalary;
        private final SimpleStringProperty maxSalary;
        private final SimpleStringProperty employeeShare;
        private final SimpleStringProperty employerShare;
        private final SimpleStringProperty active;
        
        public ContributionRecord(String id, String code, String name, String basis,
                                String ratePercent, String fixedAmount, String minSalary,
                                String maxSalary, String employeeShare, String employerShare,
                                String active) {
            this.id = new SimpleStringProperty(id);
            this.code = new SimpleStringProperty(code);
            this.name = new SimpleStringProperty(name);
            this.basis = new SimpleStringProperty(basis);
            this.ratePercent = new SimpleStringProperty(ratePercent);
            this.fixedAmount = new SimpleStringProperty(fixedAmount);
            this.minSalary = new SimpleStringProperty(minSalary);
            this.maxSalary = new SimpleStringProperty(maxSalary);
            this.employeeShare = new SimpleStringProperty(employeeShare);
            this.employerShare = new SimpleStringProperty(employerShare);
            this.active = new SimpleStringProperty(active);
        }
        
        // Getters
        public String getId() { return id.get(); }
        public String getCode() { return code.get(); }
        public String getName() { return name.get(); }
        public String getBasis() { return basis.get(); }
        public String getRatePercent() { return ratePercent.get(); }
        public String getFixedAmount() { return fixedAmount.get(); }
        public String getMinSalary() { return minSalary.get(); }
        public String getMaxSalary() { return maxSalary.get(); }
        public String getEmployeeShare() { return employeeShare.get(); }
        public String getEmployerShare() { return employerShare.get(); }
        public String getActive() { return active.get(); }
    }
}