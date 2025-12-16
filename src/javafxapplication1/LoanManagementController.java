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
import javafx.scene.control.ButtonBar;
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
import javafx.scene.control.Button;

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
    
    // Action buttons
    @FXML private Button addLoanButton;
    @FXML private Button searchButton;
    @FXML private Button editButton;
    @FXML private Button paymentButton;
    @FXML private Button markCompleteButton;
    
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
        
        // Setup permission-based button visibility
        setupPermissionBasedVisibility();
        
        // Log module access
        String currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser != null) {
            SecurityLogger.logSecurityEvent(
                "LOAN_MANAGEMENT_MODULE_ACCESS",
                "LOW",
                currentUser,
                "Accessed Loan Management module"
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
                boolean canView = hasUserPermission(currentUser, "loan_mgmt.view");
                boolean canAdd = hasUserPermission(currentUser, "loan_mgmt.add");
                boolean canEdit = hasUserPermission(currentUser, "loan_mgmt.edit");
                
                // Show/hide buttons based on permissions
                if (addLoanButton != null) {
                    addLoanButton.setVisible(canAdd);
                    addLoanButton.setManaged(canAdd);
                }
                if (searchButton != null) {
                    searchButton.setVisible(canView);
                    searchButton.setManaged(canView);
                }
                if (editButton != null) {
                    editButton.setVisible(canEdit);
                    editButton.setManaged(canEdit);
                }
                if (paymentButton != null) {
                    // Payment modifies loan balance, so it requires edit permission
                    paymentButton.setVisible(canEdit);
                    paymentButton.setManaged(canEdit);
                }
                if (markCompleteButton != null) {
                    // Mark Complete modifies loan status, so it requires edit permission
                    markCompleteButton.setVisible(canEdit);
                    markCompleteButton.setManaged(canEdit);
                }
                
                logger.info("Loan Management buttons visibility - View: " + canView + ", Add: " + canAdd + ", Edit: " + canEdit);
                
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
        if (addLoanButton != null) {
            addLoanButton.setVisible(false);
            addLoanButton.setManaged(false);
        }
        if (searchButton != null) {
            searchButton.setVisible(false);
            searchButton.setManaged(false);
        }
        if (editButton != null) {
            editButton.setVisible(false);
            editButton.setManaged(false);
        }
        if (paymentButton != null) {
            paymentButton.setVisible(false);
            paymentButton.setManaged(false);
        }
        if (markCompleteButton != null) {
            markCompleteButton.setVisible(false);
            markCompleteButton.setManaged(false);
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
        loanTypeFilter.getItems().add("All Types");
        loadLoanTypesIntoFilter();
        loanTypeFilter.setValue("All Types");
    }
    
    private void loadLoanTypesIntoFilter() {
        String sql = "SELECT DISTINCT name FROM loan_types ORDER BY name";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                loanTypeFilter.getItems().add(rs.getString("name"));
            }
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error loading loan types", e);
        }
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
        // The tables already exist in your database, so we'll just check if they exist
        // and update the existing loan_types if needed
        try {
            // Check if loan_types table exists and has data
            String checkSQL = "SELECT COUNT(*) FROM loan_types";
            try (PreparedStatement stmt = connection.prepareStatement(checkSQL);
                 ResultSet rs = stmt.executeQuery()) {
                
                rs.next();
                int count = rs.getInt(1);
                
                if (count == 0) {
                    // Insert default loan types if none exist
                    insertDefaultLoanTypes();
                }
            }
            
            logger.info("Using existing loan management tables");
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error checking loan management tables", e);
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
        // Log add click
        String currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser != null) {
            SecurityLogger.logSecurityEvent(
                "LOAN_MANAGEMENT_ADD_CLICK",
                "MEDIUM",
                currentUser,
                "Clicked Add Loan button"
            );
        }
        
        Dialog<LoanData> dialog = createLoanDialog("Add New Loan", null, 0);
        Optional<LoanData> result = dialog.showAndWait();
        
        if (result.isPresent()) {
            LoanData loanData = result.get();
            if (addLoan(loanData)) {
                // Log successful add
                if (currentUser != null) {
                    SecurityLogger.logSecurityEvent(
                        "LOAN_MANAGEMENT_ADD_SUCCESS",
                        "MEDIUM",
                        currentUser,
                        "Added loan - Employee ID: " + loanData.employeeId + 
                        ", Loan Type: " + loanData.loanType + 
                        ", Loan Amount: ₱" + loanData.loanAmount + 
                        ", Monthly Amortization: ₱" + loanData.monthlyAmortization
                    );
                }
                loadLoanData();
            } else {
                // Log failed add
                if (currentUser != null) {
                    SecurityLogger.logSecurityEvent(
                        "LOAN_MANAGEMENT_ADD_FAILED",
                        "MEDIUM",
                        currentUser,
                        "Failed to add loan - Employee ID: " + loanData.employeeId + 
                        ", Loan Type: " + loanData.loanType
                    );
                }
            }
        } else {
            // Log add cancelled
            if (currentUser != null) {
                SecurityLogger.logSecurityEvent(
                    "LOAN_MANAGEMENT_ADD_CANCELLED",
                    "LOW",
                    currentUser,
                    "Cancelled adding loan"
                );
            }
        }
    }
    
    @FXML
    private void onEdit() {
        LoanRecord selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("No Selection", "Please select a loan record to edit");
            
            // Log failed edit attempt
            String currentUser = SessionManager.getInstance().getCurrentUser();
            if (currentUser != null) {
                SecurityLogger.logSecurityEvent(
                    "LOAN_MANAGEMENT_EDIT_FAILED",
                    "LOW",
                    currentUser,
                    "Attempted to edit loan but no record was selected"
                );
            }
            return;
        }
        
        // Log edit click
        String currentUser = SessionManager.getInstance().getCurrentUser();
        String loanId = selected.getId();
        String employeeName = selected.getEmployeeName();
        String loanType = selected.getLoanType();
        
        if (currentUser != null) {
            SecurityLogger.logSecurityEvent(
                "LOAN_MANAGEMENT_EDIT_CLICK",
                "MEDIUM",
                currentUser,
                "Clicked Edit Loan button - Loan ID: " + loanId + 
                ", Employee: " + employeeName + 
                ", Loan Type: " + loanType
            );
        }
        
        // Get the loan ID and employee ID from database
        int id = Integer.parseInt(loanId);
        LoanData currentData = getLoanDataFromDatabase(id);
        
        if (currentData == null) {
            showAlert("Error", "Could not load loan data for editing");
            
            // Log load error
            if (currentUser != null) {
                SecurityLogger.logSecurityEvent(
                    "LOAN_MANAGEMENT_EDIT_FAILED",
                    "MEDIUM",
                    currentUser,
                    "Failed to load loan data for editing - Loan ID: " + loanId
                );
            }
            return;
        }
        
        Dialog<LoanData> dialog = createLoanDialog("Edit Loan", currentData, id);
        Optional<LoanData> result = dialog.showAndWait();
        
        if (result.isPresent()) {
            LoanData updatedData = result.get();
            if (updateLoan(id, updatedData)) {
                // Log successful edit
                if (currentUser != null) {
                    SecurityLogger.logSecurityEvent(
                        "LOAN_MANAGEMENT_EDIT_SUCCESS",
                        "MEDIUM",
                        currentUser,
                        "Updated loan - Loan ID: " + loanId + 
                        ", Employee ID: " + updatedData.employeeId + 
                        ", Loan Type: " + updatedData.loanType + 
                        ", Loan Amount: ₱" + updatedData.loanAmount + 
                        ", Monthly Amortization: ₱" + updatedData.monthlyAmortization
                    );
                }
                loadLoanData();
            } else {
                // Log failed update
                if (currentUser != null) {
                    SecurityLogger.logSecurityEvent(
                        "LOAN_MANAGEMENT_EDIT_FAILED",
                        "MEDIUM",
                        currentUser,
                        "Failed to update loan - Loan ID: " + loanId
                    );
                }
            }
        } else {
            // Log edit cancelled
            if (currentUser != null) {
                SecurityLogger.logSecurityEvent(
                    "LOAN_MANAGEMENT_EDIT_CANCELLED",
                    "LOW",
                    currentUser,
                    "Cancelled editing loan - Loan ID: " + loanId
                );
            }
        }
    }
    
    private LoanData getLoanDataFromDatabase(int loanId) {
        String sql = """
            SELECT el.employee_id, lt.name as loan_type, el.loan_amount, 
                   el.monthly_amortization, el.start_date
            FROM employee_loans el
            JOIN loan_types lt ON el.loan_type_id = lt.id
            WHERE el.id = ?
            """;
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, loanId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                LoanData data = new LoanData();
                data.employeeId = rs.getInt("employee_id");
                data.loanType = rs.getString("loan_type");
                data.loanAmount = rs.getBigDecimal("loan_amount");
                data.monthlyAmortization = rs.getBigDecimal("monthly_amortization");
                data.startDate = rs.getDate("start_date").toLocalDate();
                return data;
            }
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error loading loan data from database", e);
        }
        
        return null;
    }
    
    @FXML
    private void onPayment() {
        LoanRecord selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("No Selection", "Please select a loan record to process payment");
            
            // Log failed payment attempt
            String currentUser = SessionManager.getInstance().getCurrentUser();
            if (currentUser != null) {
                SecurityLogger.logSecurityEvent(
                    "LOAN_MANAGEMENT_PAYMENT_FAILED",
                    "LOW",
                    currentUser,
                    "Attempted to process payment but no loan record was selected"
                );
            }
            return;
        }
        
        if (!"Active".equals(selected.getStatus())) {
            showAlert("Invalid Status", "Can only process payments for active loans");
            
            // Log invalid status
            String currentUser = SessionManager.getInstance().getCurrentUser();
            if (currentUser != null) {
                SecurityLogger.logSecurityEvent(
                    "LOAN_MANAGEMENT_PAYMENT_FAILED",
                    "MEDIUM",
                    currentUser,
                    "Attempted to process payment for loan ID: " + selected.getId() + 
                    " but loan status is not Active (Status: " + selected.getStatus() + ")"
                );
            }
            return;
        }
        
        // Log payment click
        String currentUser = SessionManager.getInstance().getCurrentUser();
        String loanId = selected.getId();
        String employeeName = selected.getEmployeeName();
        String balance = selected.getBalance();
        
        if (currentUser != null) {
            SecurityLogger.logSecurityEvent(
                "LOAN_MANAGEMENT_PAYMENT_CLICK",
                "HIGH",
                currentUser,
                "Clicked Process Payment button - Loan ID: " + loanId + 
                ", Employee: " + employeeName + 
                ", Current Balance: " + balance
            );
        }
        
        TextInputDialog dialog = new TextInputDialog(selected.getMonthlyAmortization());
        dialog.setTitle("Process Loan Payment");
        dialog.setHeaderText("Process Payment for " + employeeName);
        dialog.setContentText("Payment Amount (₱):");
        
        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            try {
                BigDecimal paymentAmount = new BigDecimal(result.get());
                if (paymentAmount.compareTo(BigDecimal.ZERO) <= 0) {
                    showAlert("Invalid Amount", "Payment amount must be greater than 0");
                    
                    // Log invalid amount
                    if (currentUser != null) {
                        SecurityLogger.logSecurityEvent(
                            "LOAN_MANAGEMENT_PAYMENT_FAILED",
                            "MEDIUM",
                            currentUser,
                            "Invalid payment amount entered - Loan ID: " + loanId + 
                            ", Amount: ₱" + paymentAmount
                        );
                    }
                    return;
                }
                
                if (processLoanPayment(Integer.parseInt(loanId), paymentAmount)) {
                    // Log successful payment
                    if (currentUser != null) {
                        SecurityLogger.logSecurityEvent(
                            "LOAN_MANAGEMENT_PAYMENT_SUCCESS",
                            "HIGH",
                            currentUser,
                            "Processed loan payment - Loan ID: " + loanId + 
                            ", Employee: " + employeeName + 
                            ", Payment Amount: ₱" + paymentAmount + 
                            ", Previous Balance: " + balance
                        );
                    }
                } else {
                    // Log failed payment
                    if (currentUser != null) {
                        SecurityLogger.logSecurityEvent(
                            "LOAN_MANAGEMENT_PAYMENT_FAILED",
                            "HIGH",
                            currentUser,
                            "Failed to process loan payment - Loan ID: " + loanId + 
                            ", Payment Amount: ₱" + paymentAmount
                        );
                    }
                }
                loadLoanData();
                
            } catch (NumberFormatException e) {
                showAlert("Invalid Input", "Please enter a valid payment amount");
                
                // Log invalid input
                if (currentUser != null) {
                    SecurityLogger.logSecurityEvent(
                        "LOAN_MANAGEMENT_PAYMENT_FAILED",
                        "MEDIUM",
                        currentUser,
                        "Invalid payment input format - Loan ID: " + loanId + 
                        ", Input: " + result.get()
                    );
                }
            }
        } else {
            // Log payment cancelled
            if (currentUser != null) {
                SecurityLogger.logSecurityEvent(
                    "LOAN_MANAGEMENT_PAYMENT_CANCELLED",
                    "MEDIUM",
                    currentUser,
                    "Cancelled processing payment - Loan ID: " + loanId
                );
            }
        }
    }
    
    @FXML
    private void onMarkComplete() {
        LoanRecord selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("No Selection", "Please select a loan record to mark as complete");
            
            // Log failed mark complete attempt
            String currentUser = SessionManager.getInstance().getCurrentUser();
            if (currentUser != null) {
                SecurityLogger.logSecurityEvent(
                    "LOAN_MANAGEMENT_MARK_COMPLETE_FAILED",
                    "LOW",
                    currentUser,
                    "Attempted to mark loan as complete but no record was selected"
                );
            }
            return;
        }
        
        // Log mark complete click
        String currentUser = SessionManager.getInstance().getCurrentUser();
        String loanId = selected.getId();
        String employeeName = selected.getEmployeeName();
        String loanType = selected.getLoanType();
        String balance = selected.getBalance();
        
        if (currentUser != null) {
            SecurityLogger.logSecurityEvent(
                "LOAN_MANAGEMENT_MARK_COMPLETE_CLICK",
                "HIGH",
                currentUser,
                "Clicked Mark Complete button - Loan ID: " + loanId + 
                ", Employee: " + employeeName + 
                ", Loan Type: " + loanType + 
                ", Remaining Balance: " + balance
            );
        }
        
        Alert confirmAlert = new Alert(AlertType.CONFIRMATION);
        confirmAlert.setTitle("Mark Loan Complete");
        confirmAlert.setHeaderText("Mark Loan as Completed");
        confirmAlert.setContentText("Are you sure you want to mark this loan as completed?\n" +
                                  "Employee: " + employeeName + "\n" +
                                  "Loan Type: " + loanType + "\n" +
                                  "Remaining Balance: " + balance);
        
        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                if (markLoanCompleted(Integer.parseInt(loanId))) {
                    // Log successful mark complete
                    if (currentUser != null) {
                        SecurityLogger.logSecurityEvent(
                            "LOAN_MANAGEMENT_MARK_COMPLETE_SUCCESS",
                            "HIGH",
                            currentUser,
                            "Marked loan as completed - Loan ID: " + loanId + 
                            ", Employee: " + employeeName + 
                            ", Loan Type: " + loanType + 
                            ", Previous Balance: " + balance
                        );
                    }
                } else {
                    // Log failed mark complete
                    if (currentUser != null) {
                        SecurityLogger.logSecurityEvent(
                            "LOAN_MANAGEMENT_MARK_COMPLETE_FAILED",
                            "HIGH",
                            currentUser,
                            "Failed to mark loan as completed - Loan ID: " + loanId
                        );
                    }
                }
                loadLoanData();
            } else {
                // Log mark complete cancelled
                if (currentUser != null) {
                    SecurityLogger.logSecurityEvent(
                        "LOAN_MANAGEMENT_MARK_COMPLETE_CANCELLED",
                        "MEDIUM",
                        currentUser,
                        "Cancelled marking loan as completed - Loan ID: " + loanId
                    );
                }
            }
        });
    }
    
    @FXML
    private void onSearch() {
        String searchText = searchField.getText().trim();
        String selectedType = loanTypeFilter.getValue();
        
        // Log search activity (only if there's actual search criteria)
        if (!searchText.isEmpty() || (selectedType != null && !"All Types".equals(selectedType))) {
            String currentUser = SessionManager.getInstance().getCurrentUser();
            if (currentUser != null) {
                SecurityLogger.logSecurityEvent(
                    "LOAN_MANAGEMENT_SEARCH",
                    "LOW",
                    currentUser,
                    "Searched loans - Keyword: '" + (searchText.isEmpty() ? "(none)" : searchText) + 
                    "', Loan Type: " + (selectedType != null ? selectedType : "All")
                );
            }
        }
        
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
            
            int resultCount = 0;
            while (rs.next()) {
                LoanRecord loan = createLoanRecordFromResultSet(rs);
                loanData.add(loan);
                resultCount++;
            }
            
            // Log search results
            String currentUser = SessionManager.getInstance().getCurrentUser();
            if (currentUser != null && (!searchText.isEmpty() || (selectedType != null && !"All Types".equals(selectedType)))) {
                SecurityLogger.logSecurityEvent(
                    "LOAN_MANAGEMENT_SEARCH_RESULTS",
                    "LOW",
                    currentUser,
                    "Search completed - Keyword: '" + (searchText.isEmpty() ? "(none)" : searchText) + 
                    "', Loan Type: " + (selectedType != null ? selectedType : "All") + 
                    ", Results: " + resultCount + " records"
                );
            }
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error searching loan records", e);
            
            // Log search error
            String currentUser = SessionManager.getInstance().getCurrentUser();
            if (currentUser != null) {
                SecurityLogger.logSecurityEvent(
                    "LOAN_MANAGEMENT_SEARCH_FAILED",
                    "LOW",
                    currentUser,
                    "Failed to search loans - Error: " + e.getMessage()
                );
            }
            
            showAlert("Database Error", "Could not search loan records: " + e.getMessage());
        }
    }
    
    private Dialog<LoanData> createLoanDialog(String title, LoanData existingData, int loanId) {
        Dialog<LoanData> dialog = new Dialog<>();
        dialog.setTitle(title);
        
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
        Label titleLabel = new Label(title);
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

        // Employee field
        javafx.scene.layout.VBox employeeContainer = new javafx.scene.layout.VBox(8);
        Label employeeLabel = new Label("Employee");
        employeeLabel.setStyle(labelStyle);
        ComboBox<String> employeeCombo = new ComboBox<>();
        employeeCombo.setPromptText("Select employee");
        employeeCombo.setStyle(fieldStyle);
        loadEmployeesIntoComboBox(employeeCombo);
        employeeContainer.getChildren().addAll(employeeLabel, employeeCombo);
        
        // Loan Type field
        javafx.scene.layout.VBox loanTypeContainer = new javafx.scene.layout.VBox(8);
        Label loanTypeLabel = new Label("Loan Type");
        loanTypeLabel.setStyle(labelStyle);
        ComboBox<String> loanTypeCombo = new ComboBox<>();
        loanTypeCombo.setPromptText("Select loan type");
        loanTypeCombo.setStyle(fieldStyle);
        loadLoanTypesIntoComboBox(loanTypeCombo);
        loanTypeContainer.getChildren().addAll(loanTypeLabel, loanTypeCombo);
        
        // Loan Amount field
        javafx.scene.layout.VBox loanAmountContainer = new javafx.scene.layout.VBox(8);
        Label loanAmountLabel = new Label("Loan Amount (₱)");
        loanAmountLabel.setStyle(labelStyle);
        TextField loanAmountField = new TextField();
        loanAmountField.setPromptText("Enter loan amount (e.g., 50000.00)");
        loanAmountField.setStyle(fieldStyle);
        loanAmountContainer.getChildren().addAll(loanAmountLabel, loanAmountField);
        
        // Monthly Amortization field
        javafx.scene.layout.VBox monthlyAmortContainer = new javafx.scene.layout.VBox(8);
        Label monthlyAmortLabel = new Label("Monthly Amortization (₱)");
        monthlyAmortLabel.setStyle(labelStyle);
        TextField monthlyAmortizationField = new TextField();
        monthlyAmortizationField.setPromptText("Enter monthly amortization (e.g., 2000.00)");
        monthlyAmortizationField.setStyle(fieldStyle);
        monthlyAmortContainer.getChildren().addAll(monthlyAmortLabel, monthlyAmortizationField);
        
        // Start Date field
        javafx.scene.layout.VBox startDateContainer = new javafx.scene.layout.VBox(8);
        Label startDateLabel = new Label("Start Date");
        startDateLabel.setStyle(labelStyle);
        DatePicker startDatePicker = new DatePicker();
        startDatePicker.setStyle(fieldStyle);
        startDateContainer.getChildren().addAll(startDateLabel, startDatePicker);

        // Set existing data if editing
        if (existingData != null) {
            // Set employee
            for (String item : employeeCombo.getItems()) {
                if (item.startsWith(existingData.employeeId + " - ")) {
                    employeeCombo.setValue(item);
                    break;
                }
            }
            
            loanTypeCombo.setValue(existingData.loanType);
            loanAmountField.setText(existingData.loanAmount.toString());
            monthlyAmortizationField.setText(existingData.monthlyAmortization.toString());
            startDatePicker.setValue(existingData.startDate);
        } else {
            startDatePicker.setValue(LocalDate.now());
        }

        // Add all form elements to form container
        formContainer.getChildren().addAll(
            employeeContainer,
            loanTypeContainer,
            loanAmountContainer,
            monthlyAmortContainer,
            startDateContainer
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
        dialog.getDialogPane().setPrefHeight(550);

        // Add validation
        dialog.getDialogPane().lookupButton(saveButtonType).addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            if (employeeCombo.getValue() == null) {
                showAlert("Validation Error", "Please select an employee.");
                event.consume();
                return;
            }
            if (loanTypeCombo.getValue() == null) {
                showAlert("Validation Error", "Please select a loan type.");
                event.consume();
                return;
            }
            if (loanAmountField.getText().trim().isEmpty()) {
                showAlert("Validation Error", "Loan amount is required.");
                event.consume();
                return;
            }
            if (monthlyAmortizationField.getText().trim().isEmpty()) {
                showAlert("Validation Error", "Monthly amortization is required.");
                event.consume();
                return;
            }
            if (startDatePicker.getValue() == null) {
                showAlert("Validation Error", "Start date is required.");
                event.consume();
                return;
            }
            
            // Validate numeric amounts
            try {
                BigDecimal loanAmount = new BigDecimal(loanAmountField.getText().trim());
                if (loanAmount.compareTo(BigDecimal.ZERO) <= 0) {
                    showAlert("Validation Error", "Loan amount must be greater than 0.");
                    event.consume();
                    return;
                }
                
                BigDecimal monthlyAmort = new BigDecimal(monthlyAmortizationField.getText().trim());
                if (monthlyAmort.compareTo(BigDecimal.ZERO) <= 0) {
                    showAlert("Validation Error", "Monthly amortization must be greater than 0.");
                    event.consume();
                    return;
                }
            } catch (NumberFormatException e) {
                showAlert("Validation Error", "Please enter valid numeric amounts.");
                event.consume();
                return;
            }
        });

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                try {
                    LoanData data = new LoanData();
                    String selectedEmployee = employeeCombo.getValue();
                    if (selectedEmployee != null) {
                        data.employeeId = Integer.parseInt(selectedEmployee.split(" - ")[0]);
                    }
                    data.loanType = loanTypeCombo.getValue();
                    data.loanAmount = new BigDecimal(loanAmountField.getText().trim());
                    data.monthlyAmortization = new BigDecimal(monthlyAmortizationField.getText().trim());
                    data.startDate = startDatePicker.getValue();
                    
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
    
    private void loadLoanTypesIntoComboBox(ComboBox<String> comboBox) {
        String sql = "SELECT DISTINCT name FROM loan_types ORDER BY name";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                comboBox.getItems().add(rs.getString("name"));
            }
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error loading loan types", e);
        }
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
    
    private boolean addLoan(LoanData loanData) {
        String sql = """
            INSERT INTO employee_loans 
            (employee_id, loan_type_id, loan_amount, monthly_amortization, balance, start_date) 
            VALUES (?, ?, ?, ?, ?, ?)
            """;
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            int loanTypeId = getLoanTypeId(loanData.loanType);
            
            stmt.setInt(1, loanData.employeeId);
            stmt.setInt(2, loanTypeId);
            stmt.setBigDecimal(3, loanData.loanAmount);
            stmt.setBigDecimal(4, loanData.monthlyAmortization);
            stmt.setBigDecimal(5, loanData.loanAmount); // Initial balance is loan amount
            stmt.setDate(6, java.sql.Date.valueOf(loanData.startDate));
            
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected > 0) {
                showAlert("Success", "Loan added successfully!");
                logger.info("Added loan for employee ID: " + loanData.employeeId);
                return true;
            }
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error adding loan", e);
            showAlert("Database Error", "Could not add loan: " + e.getMessage());
        }
        return false;
    }
    
    private boolean updateLoan(int loanId, LoanData loanData) {
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
                return true;
            }
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error updating loan", e);
            showAlert("Database Error", "Could not update loan: " + e.getMessage());
        }
        return false;
    }
    
    private boolean processLoanPayment(int loanId, BigDecimal paymentAmount) {
        String sql = """
            UPDATE employee_loans 
            SET balance = GREATEST(0, balance - ?), 
                status = CASE WHEN balance - ? <= 0 THEN 'Completed' ELSE status END 
            WHERE id = ?
            """;
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setBigDecimal(1, paymentAmount);
            stmt.setBigDecimal(2, paymentAmount);
            stmt.setInt(3, loanId);
            
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected > 0) {
                showAlert("Success", "Payment processed successfully!");
                logger.info("Processed payment for loan ID: " + loanId + ", Amount: " + paymentAmount);
                return true;
            }
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error processing loan payment", e);
            showAlert("Database Error", "Could not process payment: " + e.getMessage());
        }
        return false;
    }
    
    private boolean markLoanCompleted(int loanId) {
        String sql = "UPDATE employee_loans SET status = 'Completed', balance = 0, end_date = CURDATE() WHERE id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, loanId);
            
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected > 0) {
                showAlert("Success", "Loan marked as completed successfully!");
                logger.info("Marked loan as completed, ID: " + loanId);
                return true;
            }
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error marking loan as completed", e);
            showAlert("Database Error", "Could not mark loan as completed: " + e.getMessage());
        }
        return false;
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
                   lt.name as loan_type, el.loan_amount, 
                   el.monthly_amortization, el.balance, 
                   el.start_date, el.end_date, el.status
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