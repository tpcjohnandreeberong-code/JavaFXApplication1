package javafxapplication1;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

public class UserManagementController implements Initializable {

    // FXML Controls
    @FXML private TextField userSearchField;
    @FXML private ComboBox<String> roleFilter;
    @FXML private ComboBox<String> statusFilter;
    @FXML private TableView<SystemUser> userTable;
    @FXML private TableColumn<SystemUser, Integer> colUserId;
    @FXML private TableColumn<SystemUser, String> colUsername;
    @FXML private TableColumn<SystemUser, String> colFullName;
    @FXML private TableColumn<SystemUser, String> colEmail;
    @FXML private TableColumn<SystemUser, String> colUserRole;
    @FXML private TableColumn<SystemUser, String> colUserStatus;
    @FXML private TableColumn<SystemUser, String> colLastLogin;
    @FXML private TableColumn<SystemUser, String> colCreatedDate;

    // Data Collections
    private final ObservableList<SystemUser> users = FXCollections.observableArrayList();
    private final ObservableList<String> roleOptions = FXCollections.observableArrayList();
    private final ObservableList<String> statusOptions = FXCollections.observableArrayList();

    // Current Selection
    private SystemUser selectedUser = null;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTableColumns();
        setupComboBoxes();
        loadSampleData();
        setupEventHandlers();
    }

    private void setupTableColumns() {
        colUserId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colUsername.setCellValueFactory(new PropertyValueFactory<>("username"));
        colFullName.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colUserRole.setCellValueFactory(new PropertyValueFactory<>("role"));
        colUserStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colLastLogin.setCellValueFactory(new PropertyValueFactory<>("lastLogin"));
        colCreatedDate.setCellValueFactory(new PropertyValueFactory<>("createdDate"));
    }

    private void setupComboBoxes() {
        // Role options
        roleOptions.addAll("All Roles", "Admin", "Payroll Maker", "Staff");
        roleFilter.setItems(roleOptions);
        roleFilter.setValue("All Roles");

        // Status options
        statusOptions.addAll("All Status", "Active", "Inactive", "Suspended");
        statusFilter.setItems(statusOptions);
        statusFilter.setValue("All Status");
    }

    private void setupEventHandlers() {
        // User table selection
        userTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            selectedUser = newVal;
        });
    }

    private void loadSampleData() {
        // Sample users
        users.addAll(
            new SystemUser(1, "admin", "System Administrator", "admin@company.com", "Admin", "Active", "2024-01-15 09:30", "2024-01-01"),
            new SystemUser(2, "payroll1", "John Payroll", "john.payroll@company.com", "Payroll Maker", "Active", "2024-01-15 08:45", "2024-01-01"),
            new SystemUser(3, "payroll2", "Jane Payroll", "jane.payroll@company.com", "Payroll Maker", "Active", "2024-01-15 09:15", "2024-01-01"),
            new SystemUser(4, "staff1", "Mike Staff", "mike.staff@company.com", "Staff", "Active", "2024-01-15 08:30", "2024-01-01"),
            new SystemUser(5, "staff2", "Sarah Staff", "sarah.staff@company.com", "Staff", "Inactive", "2024-01-10 17:00", "2024-01-01"),
            new SystemUser(6, "hr1", "HR Manager", "hr@company.com", "Admin", "Active", "2024-01-15 09:00", "2024-01-01"),
            new SystemUser(7, "accountant1", "Finance Accountant", "finance@company.com", "Payroll Maker", "Active", "2024-01-15 08:15", "2024-01-01"),
            new SystemUser(8, "clerk1", "Data Entry Clerk", "clerk@company.com", "Staff", "Suspended", "2024-01-05 16:30", "2024-01-01")
        );
        userTable.setItems(users);
    }

    // Action Methods
    @FXML
    private void onSearchUsers() {
        String searchText = userSearchField.getText() == null ? "" : userSearchField.getText().trim().toLowerCase();
        String roleFilterValue = roleFilter.getValue();
        String statusFilterValue = statusFilter.getValue();
        
        ObservableList<SystemUser> filteredUsers = users.filtered(user -> {
            boolean matchesSearch = searchText.isEmpty() || 
                user.getUsername().toLowerCase().contains(searchText) ||
                user.getFullName().toLowerCase().contains(searchText) ||
                user.getEmail().toLowerCase().contains(searchText);
            
            boolean matchesRole = roleFilterValue == null || roleFilterValue.equals("All Roles") || 
                user.getRole().equals(roleFilterValue);
            
            boolean matchesStatus = statusFilterValue == null || statusFilterValue.equals("All Status") || 
                user.getStatus().equals(statusFilterValue);
            
            return matchesSearch && matchesRole && matchesStatus;
        });
        
        userTable.setItems(filteredUsers);
    }

    @FXML
    private void onClearFilters() {
        userSearchField.clear();
        roleFilter.setValue("All Roles");
        statusFilter.setValue("All Status");
        userTable.setItems(users);
    }

    @FXML
    private void onAddUser() {
        SystemUser newUser = buildUserDialog(null);
        if (newUser != null) {
            users.add(newUser);
            showInfo("Success", "User added successfully!");
        }
    }

    @FXML
    private void onEditUser() {
        if (selectedUser == null) {
            showError("No User Selected", "Please select a user to edit.");
            return;
        }
        
        SystemUser editedUser = buildUserDialog(selectedUser);
        if (editedUser != null) {
            int index = users.indexOf(selectedUser);
            users.set(index, editedUser);
            showInfo("Success", "User updated successfully!");
        }
    }

    @FXML
    private void onDeleteUser() {
        if (selectedUser == null) {
            showError("No User Selected", "Please select a user to delete.");
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete User");
        alert.setHeaderText("Are you sure you want to delete this user?");
        alert.setContentText("User: " + selectedUser.getFullName() + " (" + selectedUser.getUsername() + ")\nThis action cannot be undone.");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                users.remove(selectedUser);
                selectedUser = null;
                showInfo("Success", "User deleted successfully!");
            }
        });
    }

    @FXML
    private void onAssignRole() {
        if (selectedUser == null) {
            showError("No User Selected", "Please select a user to assign a role to.");
            return;
        }

        ChoiceDialog<String> dialog = new ChoiceDialog<>(selectedUser.getRole(), roleOptions.subList(1, roleOptions.size()));
        dialog.setTitle("Assign Role");
        dialog.setHeaderText("Select a role for user: " + selectedUser.getFullName());
        dialog.setContentText("Choose role:");

        dialog.showAndWait().ifPresent(role -> {
            selectedUser.setRole(role);
            userTable.refresh();
            showInfo("Success", "Role assigned successfully!");
        });
    }

    @FXML
    private void onResetPassword() {
        if (selectedUser == null) {
            showError("No User Selected", "Please select a user to reset password for.");
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Reset Password");
        alert.setHeaderText("Reset password for user: " + selectedUser.getFullName());
        alert.setContentText("This will generate a temporary password for the user.\nContinue?");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                // Generate temporary password (in real app, this would be sent via email)
                String tempPassword = generateTempPassword();
                showInfo("Password Reset", "Temporary password generated: " + tempPassword + "\nPlease share this with the user securely.");
            }
        });
    }

    @FXML
    private void onToggleStatus() {
        if (selectedUser == null) {
            showError("No User Selected", "Please select a user to toggle status for.");
            return;
        }

        String currentStatus = selectedUser.getStatus();
        String newStatus = currentStatus.equals("Active") ? "Inactive" : "Active";
        
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Toggle User Status");
        alert.setHeaderText("Change user status from " + currentStatus + " to " + newStatus + "?");
        alert.setContentText("User: " + selectedUser.getFullName());

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                selectedUser.setStatus(newStatus);
                userTable.refresh();
                showInfo("Success", "User status changed to " + newStatus);
            }
        });
    }

    @FXML
    private void onRefresh() {
        loadSampleData();
        showInfo("Refreshed", "User data refreshed successfully!");
    }

    @FXML
    private void onExportUsers() {
        showInfo("Export", "User data exported successfully!");
    }

    // Dialog Builders
    private SystemUser buildUserDialog(SystemUser user) {
        Dialog<SystemUser> dialog = new Dialog<>();
        dialog.setTitle(user == null ? "Add User" : "Edit User");
        dialog.setHeaderText(user == null ? "Create a new system user" : "Edit user information");

        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(20, 150, 10, 10));

        TextField usernameField = new TextField();
        usernameField.setPromptText("Username");
        TextField fullNameField = new TextField();
        fullNameField.setPromptText("Full Name");
        TextField emailField = new TextField();
        emailField.setPromptText("Email");
        TextField passwordField = new TextField();
        passwordField.setPromptText("Password");
        ComboBox<String> roleCombo = new ComboBox<>();
        roleCombo.getItems().addAll("Admin", "Payroll Maker", "Staff");
        roleCombo.setValue("Staff");
        ComboBox<String> statusCombo = new ComboBox<>();
        statusCombo.getItems().addAll("Active", "Inactive", "Suspended");
        statusCombo.setValue("Active");

        if (user != null) {
            usernameField.setText(user.getUsername());
            fullNameField.setText(user.getFullName());
            emailField.setText(user.getEmail());
            passwordField.setText("******"); // Don't show actual password
            roleCombo.setValue(user.getRole());
            statusCombo.setValue(user.getStatus());
        }

        grid.add(new Label("Username:"), 0, 0);
        grid.add(usernameField, 1, 0);
        grid.add(new Label("Full Name:"), 0, 1);
        grid.add(fullNameField, 1, 1);
        grid.add(new Label("Email:"), 0, 2);
        grid.add(emailField, 1, 2);
        grid.add(new Label("Password:"), 0, 3);
        grid.add(passwordField, 1, 3);
        grid.add(new Label("Role:"), 0, 4);
        grid.add(roleCombo, 1, 4);
        grid.add(new Label("Status:"), 0, 5);
        grid.add(statusCombo, 1, 5);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                return new SystemUser(
                    user != null ? user.getId() : users.size() + 1,
                    usernameField.getText(),
                    fullNameField.getText(),
                    emailField.getText(),
                    roleCombo.getValue(),
                    statusCombo.getValue(),
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                );
            }
            return null;
        });

        return dialog.showAndWait().orElse(null);
    }

    // Utility Methods
    private String generateTempPassword() {
        // Simple temporary password generator
        return "Temp" + (int)(Math.random() * 10000);
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // Data Class
    public static class SystemUser {
        private int id;
        private String username;
        private String fullName;
        private String email;
        private String role;
        private String status;
        private String lastLogin;
        private String createdDate;

        public SystemUser(int id, String username, String fullName, String email, String role, String status, String lastLogin, String createdDate) {
            this.id = id;
            this.username = username;
            this.fullName = fullName;
            this.email = email;
            this.role = role;
            this.status = status;
            this.lastLogin = lastLogin;
            this.createdDate = createdDate;
        }

        // Getters and Setters
        public int getId() { return id; }
        public String getUsername() { return username; }
        public String getFullName() { return fullName; }
        public String getEmail() { return email; }
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getLastLogin() { return lastLogin; }
        public String getCreatedDate() { return createdDate; }
    }
}
