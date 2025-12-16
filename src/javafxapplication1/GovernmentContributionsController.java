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
import javafx.scene.control.Dialog;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.Label;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.beans.property.SimpleStringProperty;
import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.math.BigDecimal;
import java.util.Optional;
import javafx.scene.control.Button;

/**
 * Controller for Government Contributions Management
 * Manages Pag-IBIG, SSS, PhilHealth, and Tax contribution rules
 */
public class GovernmentContributionsController implements Initializable {
    
    @FXML private TextField searchField;
    @FXML private TableView<ContributionRecord> table;
    @FXML private TableColumn<ContributionRecord, String> colId;
    @FXML private TableColumn<ContributionRecord, String> colName;
    @FXML private TableColumn<ContributionRecord, String> colFixedAmount;
    @FXML private TableColumn<ContributionRecord, String> colPercentage;
    @FXML private TableColumn<ContributionRecord, String> colFormula;
    
    // Action buttons
    @FXML private Button addContributionButton;
    @FXML private Button searchButton;
    @FXML private Button editButton;
    @FXML private Button deleteButton;
    
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
        loadContributionData();
        
        // Setup permission-based button visibility
        setupPermissionBasedVisibility();
        
        // Log module access
        String currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser != null) {
            SecurityLogger.logSecurityEvent(
                "GOVERNMENT_CONTRIBUTIONS_MODULE_ACCESS",
                "LOW",
                currentUser,
                "Accessed Government Contributions module"
            );
        }
    }
    
    private void setupPermissionBasedVisibility() {
        try {
            // Get current user from SessionManager
            SessionManager sessionManager = SessionManager.getInstance();
            
            if (sessionManager.isLoggedIn()) {
                String currentUser = sessionManager.getCurrentUser();
                logger.info("Setting up permission-based visibility for user: " + currentUser);
                
                // Check user permissions
                boolean canView = hasUserPermission(currentUser, "gov_contrib.view");
                boolean canAdd = hasUserPermission(currentUser, "gov_contrib.add");
                boolean canEdit = hasUserPermission(currentUser, "gov_contrib.edit");
                boolean canDelete = hasUserPermission(currentUser, "gov_contrib.delete");
                
                // Show/hide buttons based on permissions
                if (addContributionButton != null) {
                    addContributionButton.setVisible(canAdd);
                    addContributionButton.setManaged(canAdd);
                }
                if (searchButton != null) {
                    searchButton.setVisible(canView);
                    searchButton.setManaged(canView);
                }
                if (editButton != null) {
                    editButton.setVisible(canEdit);
                    editButton.setManaged(canEdit);
                }
                if (deleteButton != null) {
                    deleteButton.setVisible(canDelete);
                    deleteButton.setManaged(canDelete);
                }
                
                logger.info("Government Contributions buttons visibility - View: " + canView + ", Add: " + canAdd + ", Edit: " + canEdit + ", Delete: " + canDelete);
                
            } else {
                logger.warning("No user session found, hiding all action buttons");
                hideAllActionButtons();
            }
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error setting up permission-based visibility", e);
            hideAllActionButtons();
        }
    }
    
    private void hideAllActionButtons() {
        if (addContributionButton != null) {
            addContributionButton.setVisible(false);
            addContributionButton.setManaged(false);
        }
        if (searchButton != null) {
            searchButton.setVisible(false);
            searchButton.setManaged(false);
        }
        if (editButton != null) {
            editButton.setVisible(false);
            editButton.setManaged(false);
        }
        if (deleteButton != null) {
            deleteButton.setVisible(false);
            deleteButton.setManaged(false);
        }
    }
    
    private boolean hasUserPermission(String username, String permissionName) {
        try {
            if (connection == null || connection.isClosed()) {
                logger.warning("Database connection not available, defaulting to admin permissions for user: " + username);
                return username != null && username.equals("admin");
            }
            
            // Get user's role from database
            String getRoleQuery = "SELECT role FROM users WHERE username = ?";
            PreparedStatement getRoleStmt = connection.prepareStatement(getRoleQuery);
            getRoleStmt.setString(1, username);
            ResultSet roleRs = getRoleStmt.executeQuery();
            
            if (roleRs.next()) {
                String userRole = roleRs.getString("role");
                
                // Check if role has the permission
                String checkPermissionQuery = "SELECT COUNT(*) FROM role_permissions rp " +
                                           "JOIN roles r ON rp.role_id = r.role_id " +
                                           "JOIN permissions p ON rp.permission_id = p.permission_id " +
                                           "WHERE r.role_name = ? AND p.permission_name = ? AND rp.granted = TRUE";
                PreparedStatement checkPermissionStmt = connection.prepareStatement(checkPermissionQuery);
                checkPermissionStmt.setString(1, userRole);
                checkPermissionStmt.setString(2, permissionName);
                ResultSet permissionRs = checkPermissionStmt.executeQuery();
                
                if (permissionRs.next()) {
                    boolean hasPermission = permissionRs.getInt(1) > 0;
                    logger.info("User " + username + " (Role: " + userRole + ") has permission " + permissionName + ": " + hasPermission);
                    return hasPermission;
                }
            }
            
            return false;
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error checking user permission", e);
            // If database error occurs, default to admin permissions for admin user
            return username != null && username.equals("admin");
        }
    }
    
    private void setupTableColumns() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colFixedAmount.setCellValueFactory(new PropertyValueFactory<>("fixedAmount"));
        colPercentage.setCellValueFactory(new PropertyValueFactory<>("percentage"));
        colFormula.setCellValueFactory(new PropertyValueFactory<>("formula"));
        
        table.setItems(contributionData);
    }
    
    private void initializeDatabase() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            logger.info("Database connection established successfully");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error initializing database connection", e);
            showAlert("Database Error", "Could not connect to database: " + e.getMessage());
        }
    }
    
    @FXML
    private void onAdd() {
        // Log add click
        String currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser != null) {
            SecurityLogger.logSecurityEvent(
                "GOVERNMENT_CONTRIBUTIONS_ADD_CLICK",
                "MEDIUM",
                currentUser,
                "Clicked Add Contribution Type button"
            );
        }
        
        Dialog<ContributionRecord> dialog = buildContributionDialog(null);
        Optional<ContributionRecord> result = dialog.showAndWait();
        
        if (result.isPresent()) {
            ContributionRecord newRecord = result.get();
            if (addContributionToDatabase(newRecord)) {
                // Log successful add
                if (currentUser != null) {
                    SecurityLogger.logSecurityEvent(
                        "GOVERNMENT_CONTRIBUTIONS_ADD_SUCCESS",
                        "MEDIUM",
                        currentUser,
                        "Added contribution type - Name: " + newRecord.getName() + 
                        ", Fixed Amount: " + newRecord.getFixedAmount() + 
                        ", Percentage: " + newRecord.getPercentage() + 
                        ", Formula: " + (newRecord.getFormula() != null && !newRecord.getFormula().equals("N/A") ? newRecord.getFormula() : "N/A")
                    );
                }
                showAlert("Success", "Contribution type added successfully!");
                loadContributionData();
            } else {
                // Log failed add
                if (currentUser != null) {
                    SecurityLogger.logSecurityEvent(
                        "GOVERNMENT_CONTRIBUTIONS_ADD_FAILED",
                        "MEDIUM",
                        currentUser,
                        "Failed to add contribution type - Name: " + newRecord.getName()
                    );
                }
            }
        } else {
            // Log add cancelled
            if (currentUser != null) {
                SecurityLogger.logSecurityEvent(
                    "GOVERNMENT_CONTRIBUTIONS_ADD_CANCELLED",
                    "LOW",
                    currentUser,
                    "Cancelled adding contribution type"
                );
            }
        }
    }
    
    @FXML
    private void onEdit() {
        ContributionRecord selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("No Selection", "Please select a contribution type to edit");
            
            // Log failed edit attempt
            String currentUser = SessionManager.getInstance().getCurrentUser();
            if (currentUser != null) {
                SecurityLogger.logSecurityEvent(
                    "GOVERNMENT_CONTRIBUTIONS_EDIT_FAILED",
                    "LOW",
                    currentUser,
                    "Attempted to edit contribution type but no record was selected"
                );
            }
            return;
        }
        
        // Log edit click
        String currentUser = SessionManager.getInstance().getCurrentUser();
        String oldName = selected.getName();
        String oldFixedAmount = selected.getFixedAmount();
        String oldPercentage = selected.getPercentage();
        
        if (currentUser != null) {
            SecurityLogger.logSecurityEvent(
                "GOVERNMENT_CONTRIBUTIONS_EDIT_CLICK",
                "MEDIUM",
                currentUser,
                "Clicked Edit Contribution Type button - ID: " + selected.getId() + 
                ", Name: " + oldName
            );
        }
        
        Dialog<ContributionRecord> dialog = buildContributionDialog(selected);
        Optional<ContributionRecord> result = dialog.showAndWait();
        
        if (result.isPresent()) {
            ContributionRecord updatedRecord = result.get();
            if (updateContributionInDatabase(updatedRecord)) {
                // Log successful edit
                if (currentUser != null) {
                    SecurityLogger.logSecurityEvent(
                        "GOVERNMENT_CONTRIBUTIONS_EDIT_SUCCESS",
                        "MEDIUM",
                        currentUser,
                        "Updated contribution type - ID: " + updatedRecord.getId() + 
                        ", Old Name: " + oldName + 
                        ", New Name: " + updatedRecord.getName() + 
                        ", Old Fixed Amount: " + oldFixedAmount + 
                        ", New Fixed Amount: " + updatedRecord.getFixedAmount() + 
                        ", Old Percentage: " + oldPercentage + 
                        ", New Percentage: " + updatedRecord.getPercentage()
                    );
                }
                showAlert("Success", "Contribution type updated successfully!");
                loadContributionData();
            } else {
                // Log failed update
                if (currentUser != null) {
                    SecurityLogger.logSecurityEvent(
                        "GOVERNMENT_CONTRIBUTIONS_EDIT_FAILED",
                        "MEDIUM",
                        currentUser,
                        "Failed to update contribution type - ID: " + updatedRecord.getId()
                    );
                }
            }
        } else {
            // Log edit cancelled
            if (currentUser != null) {
                SecurityLogger.logSecurityEvent(
                    "GOVERNMENT_CONTRIBUTIONS_EDIT_CANCELLED",
                    "LOW",
                    currentUser,
                    "Cancelled editing contribution type - ID: " + selected.getId()
                );
            }
        }
    }
    
    @FXML
    private void onDelete() {
        ContributionRecord selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("No Selection", "Please select a contribution type to delete");
            
            // Log failed delete attempt
            String currentUser = SessionManager.getInstance().getCurrentUser();
            if (currentUser != null) {
                SecurityLogger.logSecurityEvent(
                    "GOVERNMENT_CONTRIBUTIONS_DELETE_FAILED",
                    "LOW",
                    currentUser,
                    "Attempted to delete contribution type but no record was selected"
                );
            }
            return;
        }
        
        // Log delete click
        String currentUser = SessionManager.getInstance().getCurrentUser();
        String id = selected.getId();
        String name = selected.getName();
        
        if (currentUser != null) {
            SecurityLogger.logSecurityEvent(
                "GOVERNMENT_CONTRIBUTIONS_DELETE_CLICK",
                "HIGH",
                currentUser,
                "Clicked Delete Contribution Type button - ID: " + id + 
                ", Name: " + name
            );
        }
        
        Alert confirmAlert = new Alert(AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirm Delete");
        confirmAlert.setHeaderText(null);
        confirmAlert.setContentText("Are you sure you want to delete '" + name + "'?");
        
        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            if (deleteContributionFromDatabase(Integer.parseInt(id))) {
                // Log successful delete
                if (currentUser != null) {
                    SecurityLogger.logSecurityEvent(
                        "GOVERNMENT_CONTRIBUTIONS_DELETE_SUCCESS",
                        "HIGH",
                        currentUser,
                        "Deleted contribution type - ID: " + id + 
                        ", Name: " + name
                    );
                }
                showAlert("Success", "Contribution type deleted successfully!");
                loadContributionData();
            } else {
                // Log failed delete
                if (currentUser != null) {
                    SecurityLogger.logSecurityEvent(
                        "GOVERNMENT_CONTRIBUTIONS_DELETE_FAILED",
                        "HIGH",
                        currentUser,
                        "Failed to delete contribution type - ID: " + id
                    );
                }
            }
        } else {
            // Log delete cancelled
            if (currentUser != null) {
                SecurityLogger.logSecurityEvent(
                    "GOVERNMENT_CONTRIBUTIONS_DELETE_CANCELLED",
                    "MEDIUM",
                    currentUser,
                    "Cancelled deleting contribution type - ID: " + id
                );
            }
        }
    }
    
    @FXML
    private void onSearch() {
        String searchText = searchField.getText().trim();
        
        // Log search activity (only if there's actual text)
        if (!searchText.isEmpty()) {
            String currentUser = SessionManager.getInstance().getCurrentUser();
            if (currentUser != null) {
                SecurityLogger.logSecurityEvent(
                    "GOVERNMENT_CONTRIBUTIONS_SEARCH",
                    "LOW",
                    currentUser,
                    "Searched contribution types with keyword: '" + searchText + "'"
                );
            }
        }
        
        if (searchText.isEmpty()) {
            loadContributionData();
            return;
        }
        
        searchContributions(searchText);
        
        // Log search results
        String currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser != null && !searchText.isEmpty()) {
            SecurityLogger.logSecurityEvent(
                "GOVERNMENT_CONTRIBUTIONS_SEARCH_RESULTS",
                "LOW",
                currentUser,
                "Search completed - Keyword: '" + searchText + 
                "', Results: " + contributionData.size() + " records"
            );
        }
    }
    
    private boolean addContributionToDatabase(ContributionRecord record) {
        if (connection == null) {
            showAlert("Database Error", "No database connection available.");
            return false;
        }
        
        try {
            String sql = "INSERT INTO deduction_types (name, fixed_amount, percentage, formula) VALUES (?, ?, ?, ?)";
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setString(1, record.getName());
            
            if (record.getFixedAmount() != null && !record.getFixedAmount().equals("N/A") && !record.getFixedAmount().isEmpty()) {
                String fixedStr = record.getFixedAmount().replace("₱", "").replace(",", "").trim();
                if (!fixedStr.isEmpty()) {
                    stmt.setBigDecimal(2, new BigDecimal(fixedStr));
                } else {
                    stmt.setNull(2, Types.DECIMAL);
                }
            } else {
                stmt.setNull(2, Types.DECIMAL);
            }
            
            if (record.getPercentage() != null && !record.getPercentage().equals("N/A") && !record.getPercentage().isEmpty()) {
                String percentStr = record.getPercentage().replace("%", "").trim();
                if (!percentStr.isEmpty()) {
                    stmt.setBigDecimal(3, new BigDecimal(percentStr));
                } else {
                    stmt.setNull(3, Types.DECIMAL);
                }
            } else {
                stmt.setNull(3, Types.DECIMAL);
            }
            
            if (record.getFormula() != null && !record.getFormula().isEmpty() && !record.getFormula().equals("N/A")) {
                stmt.setString(4, record.getFormula());
            } else {
                stmt.setNull(4, Types.VARCHAR);
            }
            
            int rowsAffected = stmt.executeUpdate();
            logger.info("Contribution type added to database: " + record.getName());
            return rowsAffected > 0;
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error adding contribution type to database", e);
            showAlert("Database Error", "Failed to add contribution type: " + e.getMessage());
            return false;
        }
    }
    
    private boolean updateContributionInDatabase(ContributionRecord record) {
        if (connection == null) {
            showAlert("Database Error", "No database connection available.");
            return false;
        }
        
        try {
            String sql = "UPDATE deduction_types SET name = ?, fixed_amount = ?, percentage = ?, formula = ? WHERE id = ?";
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setString(1, record.getName());
            
            if (record.getFixedAmount() != null && !record.getFixedAmount().equals("N/A") && !record.getFixedAmount().isEmpty()) {
                String fixedStr = record.getFixedAmount().replace("₱", "").replace(",", "").trim();
                if (!fixedStr.isEmpty()) {
                    stmt.setBigDecimal(2, new BigDecimal(fixedStr));
                } else {
                    stmt.setNull(2, Types.DECIMAL);
                }
            } else {
                stmt.setNull(2, Types.DECIMAL);
            }
            
            if (record.getPercentage() != null && !record.getPercentage().equals("N/A") && !record.getPercentage().isEmpty()) {
                String percentStr = record.getPercentage().replace("%", "").trim();
                if (!percentStr.isEmpty()) {
                    stmt.setBigDecimal(3, new BigDecimal(percentStr));
                } else {
                    stmt.setNull(3, Types.DECIMAL);
                }
            } else {
                stmt.setNull(3, Types.DECIMAL);
            }
            
            if (record.getFormula() != null && !record.getFormula().isEmpty() && !record.getFormula().equals("N/A")) {
                stmt.setString(4, record.getFormula());
            } else {
                stmt.setNull(4, Types.VARCHAR);
            }
            
            stmt.setInt(5, Integer.parseInt(record.getId()));
            
            int rowsAffected = stmt.executeUpdate();
            logger.info("Contribution type updated in database: " + record.getName());
            return rowsAffected > 0;
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error updating contribution type in database", e);
            showAlert("Database Error", "Failed to update contribution type: " + e.getMessage());
            return false;
        }
    }
    
    private boolean deleteContributionFromDatabase(int id) {
        if (connection == null) {
            showAlert("Database Error", "No database connection available.");
            return false;
        }
        
        try {
            String sql = "DELETE FROM deduction_types WHERE id = ?";
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setInt(1, id);
            
            int rowsAffected = stmt.executeUpdate();
            logger.info("Contribution type deleted from database: ID " + id);
            return rowsAffected > 0;
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error deleting contribution type from database", e);
            showAlert("Database Error", "Failed to delete contribution type: " + e.getMessage());
            return false;
        }
    }
    
    private void searchContributions(String searchText) {
        String sql = "SELECT * FROM deduction_types WHERE name LIKE ? ORDER BY name";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            String searchPattern = "%" + searchText + "%";
            stmt.setString(1, searchPattern);
            
            ResultSet rs = stmt.executeQuery();
            contributionData.clear();
            
            while (rs.next()) {
                ContributionRecord contribution = createContributionRecordFromResultSet(rs);
                contributionData.add(contribution);
            }
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error searching contributions", e);
            
            // Log search error
            String currentUser = SessionManager.getInstance().getCurrentUser();
            if (currentUser != null) {
                SecurityLogger.logSecurityEvent(
                    "GOVERNMENT_CONTRIBUTIONS_SEARCH_FAILED",
                    "LOW",
                    currentUser,
                    "Failed to search contribution types - Keyword: '" + searchText + 
                    "', Error: " + e.getMessage()
                );
            }
            
            showAlert("Database Error", "Could not search contributions: " + e.getMessage());
        }
    }
    
    private void loadContributionData() {
        String sql = "SELECT * FROM deduction_types ORDER BY name";
        
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
        BigDecimal fixedAmount = rs.getBigDecimal("fixed_amount");
        BigDecimal percentage = rs.getBigDecimal("percentage");
        String formula = rs.getString("formula");
        
        return new ContributionRecord(
            String.valueOf(rs.getInt("id")),
            rs.getString("name"),
            fixedAmount != null ? String.format("₱%.2f", fixedAmount) : "N/A",
            percentage != null ? String.format("%.2f%%", percentage) : "N/A",
            formula != null ? formula : "N/A"
        );
    }
    
    private Dialog<ContributionRecord> buildContributionDialog(ContributionRecord existing) {
        Dialog<ContributionRecord> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "Add Contribution Type" : "Edit Contribution Type");
        
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
        Label titleLabel = new Label(existing == null ? "Add New Contribution Type" : "Edit Contribution Type");
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

        // Name field
        javafx.scene.layout.VBox nameContainer = new javafx.scene.layout.VBox(8);
        Label nameLabel = new Label("Name");
        nameLabel.setStyle(labelStyle);
        TextField nameField = new TextField();
        nameField.setPromptText("Enter contribution type name (e.g., Pag-ibig)");
        nameField.setStyle(fieldStyle);
        nameContainer.getChildren().addAll(nameLabel, nameField);
        
        // Fixed Amount field
        javafx.scene.layout.VBox fixedAmountContainer = new javafx.scene.layout.VBox(8);
        Label fixedAmountLabel = new Label("Fixed Amount (Optional)");
        fixedAmountLabel.setStyle(labelStyle);
        TextField fixedAmountField = new TextField();
        fixedAmountField.setPromptText("Enter fixed amount (e.g., 200.00)");
        fixedAmountField.setStyle(fieldStyle);
        fixedAmountContainer.getChildren().addAll(fixedAmountLabel, fixedAmountField);
        
        // Percentage field
        javafx.scene.layout.VBox percentageContainer = new javafx.scene.layout.VBox(8);
        Label percentageLabel = new Label("Percentage (Optional)");
        percentageLabel.setStyle(labelStyle);
        TextField percentageField = new TextField();
        percentageField.setPromptText("Enter percentage (e.g., 5.00 for 5%)");
        percentageField.setStyle(fieldStyle);
        percentageContainer.getChildren().addAll(percentageLabel, percentageField);
        
        // Formula field
        javafx.scene.layout.VBox formulaContainer = new javafx.scene.layout.VBox(8);
        Label formulaLabel = new Label("Formula (Optional)");
        formulaLabel.setStyle(labelStyle);
        TextField formulaField = new TextField();
        formulaField.setPromptText("Enter formula if applicable");
        formulaField.setStyle(fieldStyle);
        formulaContainer.getChildren().addAll(formulaLabel, formulaField);

        // Populate fields if editing
        if (existing != null) {
            nameField.setText(existing.getName());
            
            if (existing.getFixedAmount() != null && !existing.getFixedAmount().equals("N/A")) {
                String fixedStr = existing.getFixedAmount().replace("₱", "").replace(",", "").trim();
                fixedAmountField.setText(fixedStr);
            }
            
            if (existing.getPercentage() != null && !existing.getPercentage().equals("N/A")) {
                String percentStr = existing.getPercentage().replace("%", "").trim();
                percentageField.setText(percentStr);
            }
            
            if (existing.getFormula() != null && !existing.getFormula().equals("N/A")) {
                formulaField.setText(existing.getFormula());
            }
        }

        // Add all form elements to form container
        formContainer.getChildren().addAll(
            nameContainer,
            fixedAmountContainer,
            percentageContainer,
            formulaContainer
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
            "-fx-font-size: 14px; " +
            "-fx-border-color: #ddd; " +
            "-fx-border-radius: 8;"
        );
        
        // Set dialog size
        dialog.getDialogPane().setPrefWidth(450);
        dialog.getDialogPane().setPrefHeight(500);

        // Add validation
        dialog.getDialogPane().lookupButton(saveButtonType).addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            if (nameField.getText().trim().isEmpty()) {
                showAlert("Validation Error", "Name is required.");
                event.consume();
                return;
            }
            
            // Validate fixed amount if provided
            if (!fixedAmountField.getText().trim().isEmpty()) {
                try {
                    new BigDecimal(fixedAmountField.getText().trim());
                } catch (NumberFormatException e) {
                    showAlert("Validation Error", "Fixed amount must be a valid number.");
                    event.consume();
                    return;
                }
            }
            
            // Validate percentage if provided
            if (!percentageField.getText().trim().isEmpty()) {
                try {
                    BigDecimal percent = new BigDecimal(percentageField.getText().trim());
                    if (percent.compareTo(BigDecimal.ZERO) < 0 || percent.compareTo(new BigDecimal("100")) > 0) {
                        showAlert("Validation Error", "Percentage must be between 0 and 100.");
                        event.consume();
                        return;
                    }
                } catch (NumberFormatException e) {
                    showAlert("Validation Error", "Percentage must be a valid number.");
                    event.consume();
                    return;
                }
            }
        });

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                int recordId = (existing != null) ? Integer.parseInt(existing.getId()) : 0;
                
                String fixedAmountStr = fixedAmountField.getText().trim();
                String fixedAmountDisplay = fixedAmountStr.isEmpty() ? "N/A" : 
                    String.format("₱%.2f", new BigDecimal(fixedAmountStr));
                
                String percentageStr = percentageField.getText().trim();
                String percentageDisplay = percentageStr.isEmpty() ? "N/A" : 
                    String.format("%.2f%%", new BigDecimal(percentageStr));
                
                String formulaStr = formulaField.getText().trim();
                String formulaDisplay = formulaStr.isEmpty() ? "N/A" : formulaStr;
                
                return new ContributionRecord(
                    existing != null ? existing.getId() : "0",
                    nameField.getText().trim(),
                    fixedAmountDisplay,
                    percentageDisplay,
                    formulaDisplay
                );
            }
            return null;
        });

        return dialog;
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
        private final SimpleStringProperty name;
        private final SimpleStringProperty fixedAmount;
        private final SimpleStringProperty percentage;
        private final SimpleStringProperty formula;
        
        public ContributionRecord(String id, String name, String fixedAmount, 
                                String percentage, String formula) {
            this.id = new SimpleStringProperty(id);
            this.name = new SimpleStringProperty(name);
            this.fixedAmount = new SimpleStringProperty(fixedAmount);
            this.percentage = new SimpleStringProperty(percentage);
            this.formula = new SimpleStringProperty(formula);
        }
        
        // Getters
        public String getId() { return id.get(); }
        public String getName() { return name.get(); }
        public String getFixedAmount() { return fixedAmount.get(); }
        public String getPercentage() { return percentage.get(); }
        public String getFormula() { return formula.get(); }
    }
}