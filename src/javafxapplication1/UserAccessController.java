package javafxapplication1;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TreeItemPropertyValueFactory;
import javafx.util.Callback;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;
import javafx.scene.layout.GridPane;

public class UserAccessController implements Initializable {

    // FXML Controls - Role Management
    @FXML private TextField roleSearchField;
    @FXML private TableView<SystemRole> roleTable;
    @FXML private TableColumn<SystemRole, Integer> colRoleId;
    @FXML private TableColumn<SystemRole, String> colRoleName;
    @FXML private TableColumn<SystemRole, String> colDescription;
    @FXML private TableColumn<SystemRole, Integer> colUserCount;
    @FXML private TableColumn<SystemRole, String> colStatus;
    @FXML private TableColumn<SystemRole, String> colCreatedDate;

    // FXML Controls - Permission Management
    @FXML private Label selectedRoleLabel;
    @FXML private TreeView<String> permissionTree;


    // Data Collections
    private final ObservableList<SystemRole> roles = FXCollections.observableArrayList();

    // Current Selection
    private SystemRole selectedRole = null;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTableColumns();
        setupComboBoxes();
        setupPermissionTree();
        loadSampleData();
        setupEventHandlers();
    }

    private void setupTableColumns() {
        // Role Table Columns
        colRoleId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colRoleName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
        colUserCount.setCellValueFactory(new PropertyValueFactory<>("userCount"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colCreatedDate.setCellValueFactory(new PropertyValueFactory<>("createdDate"));
    }

    private void setupComboBoxes() {
        // No combo boxes needed for User Access (only role management)
    }

    private void setupPermissionTree() {
        // Create the permission tree structure
        TreeItem<String> rootItem = new TreeItem<>("System Modules");
        rootItem.setExpanded(true);
        
        // Dashboard
        TreeItem<String> dashboardItem = new TreeItem<>("Dashboard");
        dashboardItem.getChildren().add(new TreeItem<>("Quick Actions"));
        
        // Employee Management
        TreeItem<String> employeeItem = new TreeItem<>("Employee Management");
        employeeItem.getChildren().addAll(
            new TreeItem<>("Add Employee"),
            new TreeItem<>("View Employee List/Details"),
            new TreeItem<>("Edit Employee Info"),
            new TreeItem<>("Delete Employee Record")
        );
        
        // Payroll Processing
        TreeItem<String> payrollProcessingItem = new TreeItem<>("Payroll Processing");
        payrollProcessingItem.getChildren().addAll(
            new TreeItem<>("Generate Payroll"),
            new TreeItem<>("Export Payroll"),
            new TreeItem<>("View Payroll Computation")
        );
        
        // Payroll Generator
        TreeItem<String> payrollGeneratorItem = new TreeItem<>("Payroll Generator");
        payrollGeneratorItem.getChildren().addAll(
            new TreeItem<>("Add Payroll Entry"),
            new TreeItem<>("View Generated Payroll"),
            new TreeItem<>("Edit Payroll Entry"),
            new TreeItem<>("Delete Payroll Record")
        );
        
        // Reports
        TreeItem<String> reportsItem = new TreeItem<>("Reports");
        reportsItem.getChildren().addAll(
            new TreeItem<>("Generate Report"),
            new TreeItem<>("Print Payroll/Employee Reports"),
            new TreeItem<>("Quick Reports"),
            new TreeItem<>("Download Button"),
            new TreeItem<>("Print Button")
        );
        
        // Import/Export File
        TreeItem<String> importExportItem = new TreeItem<>("Import/Export File");
        importExportItem.getChildren().addAll(
            new TreeItem<>("Upload Employee Data"),
            new TreeItem<>("Download Employee Data"),
            new TreeItem<>("Upload Payroll Data"),
            new TreeItem<>("Download Payroll Data")
        );
        
        // History
        TreeItem<String> historyItem = new TreeItem<>("History");
        historyItem.getChildren().addAll(
            new TreeItem<>("View Transaction History"),
            new TreeItem<>("View Activity History"),
            new TreeItem<>("Export History"),
            new TreeItem<>("Clear Old Records")
        );
        
        // Security Maintenance
        TreeItem<String> securityItem = new TreeItem<>("Security Maintenance");
        securityItem.getChildren().addAll(
            new TreeItem<>("Manage Password Policies"),
            new TreeItem<>("Manage Access Controls"),
            new TreeItem<>("View Security Events"),
            new TreeItem<>("Manage User Accounts"),
            new TreeItem<>("Export Security Logs"),
            new TreeItem<>("Clear Security Logs")
        );
        
        // Government Remittances
        TreeItem<String> govRemitItem = new TreeItem<>("Government Remittances");
        govRemitItem.getChildren().addAll(
            new TreeItem<>("Manage SSS Contributions"),
            new TreeItem<>("Manage PhilHealth Contributions"),
            new TreeItem<>("Manage Pag-IBIG Contributions"),
            new TreeItem<>("Manage Tax Withholding"),
            new TreeItem<>("Generate Remittance Reports"),
            new TreeItem<>("Export Remittance Data")
        );
        
        // User Access
        TreeItem<String> userAccessItem = new TreeItem<>("User Access");
        userAccessItem.getChildren().addAll(
            new TreeItem<>("Manage User Roles"),
            new TreeItem<>("Manage Permissions"),
            new TreeItem<>("Assign Roles to Users"),
            new TreeItem<>("View Access Logs")
        );
        
        // Add all modules to root
        rootItem.getChildren().addAll(
            dashboardItem, employeeItem, payrollProcessingItem, payrollGeneratorItem,
            reportsItem, importExportItem, historyItem, securityItem, govRemitItem, userAccessItem
        );
        
        // Set the root item
        permissionTree.setRoot(rootItem);
        
        // Set up custom cell factory for checkboxes
        permissionTree.setCellFactory(new Callback<TreeView<String>, TreeCell<String>>() {
            @Override
            public TreeCell<String> call(TreeView<String> param) {
                return new TreeCell<String>() {
                    private final CheckBox checkBox = new CheckBox();

                    @Override
                    protected void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setGraphic(null);
                            setText(null);
                        } else {
                            setText(item);
                            // Only show checkboxes for leaf nodes (permissions)
                            if (getTreeItem() != null && getTreeItem().isLeaf() && getTreeItem().getParent() != null) {
                                setGraphic(checkBox);
                                // Set up checkbox event handler
                                checkBox.setOnAction(event -> {
                                    // Handle permission toggle
                                    togglePermission(item, checkBox.isSelected());
                                });
                            } else {
                                setGraphic(null);
                            }
                        }
                    }
                };
            }
        });
    }

    private void setupEventHandlers() {
        // Role table selection
        roleTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            selectedRole = newVal;
            if (newVal != null) {
                selectedRoleLabel.setText(newVal.getName());
                loadRolePermissions(newVal);
            } else {
                selectedRoleLabel.setText("None");
            }
        });
    }

    private void loadSampleData() {
        // Sample roles
        roles.addAll(
            new SystemRole(1, "Admin", "Full system access with all permissions", 3, "Active", "2024-01-01"),
            new SystemRole(2, "Payroll Maker", "Can process payroll and generate reports", 5, "Active", "2024-01-01"),
            new SystemRole(3, "Staff", "Basic access to view employee data and reports", 8, "Active", "2024-01-01")
        );
        roleTable.setItems(roles);
    }

    private void loadRolePermissions(SystemRole role) {
        // Load permissions for selected role
        // This would typically load from database
        // For now, we'll just show the tree structure
    }

    private void togglePermission(String permission, boolean granted) {
        if (selectedRole != null) {
            // Handle permission toggle logic
            System.out.println("Permission '" + permission + "' " + (granted ? "granted" : "revoked") + " for role: " + selectedRole.getName());
        }
    }

    // Action Methods - Role Management
    @FXML
    private void onSearchRoles() {
        String searchText = roleSearchField.getText() == null ? "" : roleSearchField.getText().trim().toLowerCase();
        
        if (searchText.isEmpty()) {
            roleTable.setItems(roles);
        } else {
            ObservableList<SystemRole> filteredRoles = roles.filtered(role -> 
                role.getName().toLowerCase().contains(searchText) ||
                role.getDescription().toLowerCase().contains(searchText)
            );
            roleTable.setItems(filteredRoles);
        }
    }

    @FXML
    private void onAddRole() {
        SystemRole newRole = buildRoleDialog(null);
        if (newRole != null) {
            roles.add(newRole);
            showInfo("Success", "Role added successfully!");
        }
    }

    @FXML
    private void onEditRole() {
        if (selectedRole == null) {
            showError("No Role Selected", "Please select a role to edit.");
            return;
        }
        
        SystemRole editedRole = buildRoleDialog(selectedRole);
        if (editedRole != null) {
            int index = roles.indexOf(selectedRole);
            roles.set(index, editedRole);
            showInfo("Success", "Role updated successfully!");
        }
    }

    @FXML
    private void onDeleteRole() {
        if (selectedRole == null) {
            showError("No Role Selected", "Please select a role to delete.");
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Role");
        alert.setHeaderText("Are you sure you want to delete this role?");
        alert.setContentText("Role: " + selectedRole.getName() + "\nThis action cannot be undone.");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                roles.remove(selectedRole);
                selectedRole = null;
                selectedRoleLabel.setText("None");
                showInfo("Success", "Role deleted successfully!");
            }
        });
    }

    @FXML
    private void onRefresh() {
        loadSampleData();
        showInfo("Refreshed", "Data refreshed successfully!");
    }

    @FXML
    private void onExportRoles() {
        showInfo("Export", "Role data exported successfully!");
    }

    // Action Methods - Permission Management
    @FXML
    private void onSavePermissions() {
        if (selectedRole == null) {
            showError("No Role Selected", "Please select a role to save permissions for.");
            return;
        }
        showInfo("Success", "Permissions saved successfully for role: " + selectedRole.getName());
    }

    @FXML
    private void onResetPermissions() {
        if (selectedRole == null) {
            showError("No Role Selected", "Please select a role to reset permissions for.");
            return;
        }
        showInfo("Reset", "Permissions reset to default for role: " + selectedRole.getName());
    }


    // Dialog Builders
    private SystemRole buildRoleDialog(SystemRole role) {
        Dialog<SystemRole> dialog = new Dialog<>();
        dialog.setTitle(role == null ? "Add Role" : "Edit Role");
        dialog.setHeaderText(role == null ? "Create a new system role" : "Edit role information");

        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(20, 150, 10, 10));

        TextField nameField = new TextField();
        nameField.setPromptText("Role Name");
        TextField descriptionField = new TextField();
        descriptionField.setPromptText("Description");
        ComboBox<String> statusCombo = new ComboBox<>();
        statusCombo.getItems().addAll("Active", "Inactive");
        statusCombo.setValue("Active");

        if (role != null) {
            nameField.setText(role.getName());
            descriptionField.setText(role.getDescription());
            statusCombo.setValue(role.getStatus());
        }

        grid.add(new Label("Role Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Description:"), 0, 1);
        grid.add(descriptionField, 1, 1);
        grid.add(new Label("Status:"), 0, 2);
        grid.add(statusCombo, 1, 2);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                return new SystemRole(
                    role != null ? role.getId() : roles.size() + 1,
                    nameField.getText(),
                    descriptionField.getText(),
                    0,
                    statusCombo.getValue(),
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                );
            }
            return null;
        });

        return dialog.showAndWait().orElse(null);
    }


    // Utility Methods
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

    // Data Classes
    public static class SystemRole {
        private int id;
        private String name;
        private String description;
        private int userCount;
        private String status;
        private String createdDate;

        public SystemRole(int id, String name, String description, int userCount, String status, String createdDate) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.userCount = userCount;
            this.status = status;
            this.createdDate = createdDate;
        }

        // Getters
        public int getId() { return id; }
        public String getName() { return name; }
        public String getDescription() { return description; }
        public int getUserCount() { return userCount; }
        public String getStatus() { return status; }
        public String getCreatedDate() { return createdDate; }
    }
}
