package javafxapplication1;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.ResourceBundle;

public class HistoryController implements Initializable {

    public static class ActivityRecord {
        private final String timestamp;
        private final String user;
        private final String activityType;
        private final String description;
        private final String entityId;
        private final String entityType;
        private final String status;
        private final String ipAddress;

        public ActivityRecord(String timestamp, String user, String activityType, String description, 
                            String entityId, String entityType, String status, String ipAddress) {
            this.timestamp = timestamp;
            this.user = user;
            this.activityType = activityType;
            this.description = description;
            this.entityId = entityId;
            this.entityType = entityType;
            this.status = status;
            this.ipAddress = ipAddress;
        }

        public String getTimestamp() { return timestamp; }
        public String getUser() { return user; }
        public String getActivityType() { return activityType; }
        public String getDescription() { return description; }
        public String getEntityId() { return entityId; }
        public String getEntityType() { return entityType; }
        public String getStatus() { return status; }
        public String getIpAddress() { return ipAddress; }
    }

    @FXML private TextField searchField;
    @FXML private ComboBox<String> activityTypeFilter;
    @FXML private TableView<ActivityRecord> historyTable;
    @FXML private TableColumn<ActivityRecord, String> colTimestamp;
    @FXML private TableColumn<ActivityRecord, String> colUser;
    @FXML private TableColumn<ActivityRecord, String> colActivityType;
    @FXML private TableColumn<ActivityRecord, String> colDescription;
    @FXML private TableColumn<ActivityRecord, String> colEntityId;
    @FXML private TableColumn<ActivityRecord, String> colEntityType;
    @FXML private TableColumn<ActivityRecord, String> colStatus;
    @FXML private TableColumn<ActivityRecord, String> colIpAddress;

    private final ObservableList<ActivityRecord> data = FXCollections.observableArrayList();
    private final ObservableList<String> activityTypes = FXCollections.observableArrayList();
    private final ObservableList<String> users = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupTableColumns();
        setupFilters();
        loadSampleData();
    }

    private void setupTableColumns() {
        colTimestamp.setCellValueFactory(new PropertyValueFactory<>("timestamp"));
        colUser.setCellValueFactory(new PropertyValueFactory<>("user"));
        colActivityType.setCellValueFactory(new PropertyValueFactory<>("activityType"));
        colDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
        colEntityId.setCellValueFactory(new PropertyValueFactory<>("entityId"));
        colEntityType.setCellValueFactory(new PropertyValueFactory<>("entityType"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colIpAddress.setCellValueFactory(new PropertyValueFactory<>("ipAddress"));

        // Set column resize policy to fill available space
        historyTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Format status column with colors
        colStatus.setCellFactory(column -> new TableCell<ActivityRecord, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if (item.equals("Success")) {
                        setStyle("-fx-text-fill: #4caf50; -fx-font-weight: bold;");
                    } else if (item.equals("Failed")) {
                        setStyle("-fx-text-fill: #f44336; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #ff9800; -fx-font-weight: bold;");
                    }
                }
            }
        });
    }

    private void setupFilters() {
        // Activity types
        activityTypes.addAll("All Activities", "Login", "Logout", "Create", "Update", "Delete", "Export", "Import", "View", "Search", "System");
        activityTypeFilter.setItems(activityTypes);
        activityTypeFilter.setValue("All Activities");
    }

    private void loadSampleData() {
        data.setAll(loadActivityRecords());
        historyTable.setItems(data);
    }

    private ObservableList<ActivityRecord> loadActivityRecords() {
        ObservableList<ActivityRecord> list = FXCollections.observableArrayList();
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        
        list.addAll(
            new ActivityRecord(now.minusMinutes(5).format(formatter), "admin", "Login", "User logged into system", "U-001", "User", "Success", "192.168.1.100"),
            new ActivityRecord(now.minusMinutes(10).format(formatter), "hr_manager", "Create", "Added new employee Juan Dela Cruz", "E-021", "Employee", "Success", "192.168.1.101"),
            new ActivityRecord(now.minusMinutes(15).format(formatter), "payroll_admin", "Update", "Updated payroll record for E-001", "P-001", "Payroll", "Success", "192.168.1.102"),
            new ActivityRecord(now.minusMinutes(20).format(formatter), "finance_staff", "Export", "Exported payroll data to Excel", "EX-001", "Export", "Success", "192.168.1.103"),
            new ActivityRecord(now.minusMinutes(25).format(formatter), "admin", "Delete", "Deleted old attendance records", "A-001", "Attendance", "Success", "192.168.1.100"),
            new ActivityRecord(now.minusMinutes(30).format(formatter), "hr_manager", "Import", "Imported employee data from CSV", "IM-001", "Import", "Success", "192.168.1.101"),
            new ActivityRecord(now.minusMinutes(35).format(formatter), "system", "System", "Automatic backup completed", "BK-001", "Backup", "Success", "127.0.0.1"),
            new ActivityRecord(now.minusMinutes(40).format(formatter), "payroll_admin", "View", "Viewed employee salary report", "R-001", "Report", "Success", "192.168.1.102"),
            new ActivityRecord(now.minusMinutes(45).format(formatter), "finance_staff", "Update", "Updated department budget", "D-001", "Department", "Success", "192.168.1.103"),
            new ActivityRecord(now.minusMinutes(50).format(formatter), "admin", "Create", "Created new user account", "U-002", "User", "Success", "192.168.1.100"),
            new ActivityRecord(now.minusMinutes(55).format(formatter), "hr_manager", "Login", "User logged into system", "U-003", "User", "Success", "192.168.1.101"),
            new ActivityRecord(now.minusMinutes(60).format(formatter), "payroll_admin", "Export", "Failed to export payroll data", "EX-002", "Export", "Failed", "192.168.1.102"),
            new ActivityRecord(now.minusMinutes(65).format(formatter), "finance_staff", "Update", "Updated employee salary", "E-001", "Employee", "Success", "192.168.1.103"),
            new ActivityRecord(now.minusMinutes(70).format(formatter), "admin", "System", "System maintenance completed", "SM-001", "System", "Success", "127.0.0.1"),
            new ActivityRecord(now.minusMinutes(75).format(formatter), "hr_manager", "View", "Viewed employee list", "E-LIST", "Employee", "Success", "192.168.1.101"),
            new ActivityRecord(now.minusMinutes(80).format(formatter), "payroll_admin", "Create", "Generated monthly payroll", "P-002", "Payroll", "Success", "192.168.1.102"),
            new ActivityRecord(now.minusMinutes(85).format(formatter), "finance_staff", "Import", "Imported attendance data", "IM-002", "Import", "Success", "192.168.1.103"),
            new ActivityRecord(now.minusMinutes(90).format(formatter), "admin", "Logout", "User logged out of system", "U-001", "User", "Success", "192.168.1.100"),
            new ActivityRecord(now.minusMinutes(95).format(formatter), "hr_manager", "Delete", "Deleted inactive employee record", "E-020", "Employee", "Success", "192.168.1.101"),
            new ActivityRecord(now.minusMinutes(100).format(formatter), "system", "System", "Database optimization completed", "DB-001", "Database", "Success", "127.0.0.1")
        );
        return list;
    }

    @FXML
    private void onSearch() {
        String searchText = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        String selectedActivityType = activityTypeFilter.getValue();
        
        if (searchText.isEmpty() && 
            (selectedActivityType == null || selectedActivityType.equals("All Activities"))) {
            historyTable.setItems(data);
            return;
        }

        ObservableList<ActivityRecord> filtered = FXCollections.observableArrayList();
        for (ActivityRecord record : data) {
            boolean matchesSearch = searchText.isEmpty() || 
                                  record.getDescription().toLowerCase().contains(searchText) ||
                                  record.getUser().toLowerCase().contains(searchText) ||
                                  record.getEntityId().toLowerCase().contains(searchText);
            boolean matchesActivityType = selectedActivityType == null || selectedActivityType.equals("All Activities") || 
                                        record.getActivityType().equals(selectedActivityType);
            
            if (matchesSearch && matchesActivityType) {
                filtered.add(record);
            }
        }
        historyTable.setItems(filtered);
    }

    @FXML
    private void onViewDetails() {
        ActivityRecord selected = historyTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showInfo("Please select an activity record to view details.");
            return;
        }
        
        showActivityDetails(selected);
    }

    @FXML
    private void onRefresh() {
        loadSampleData();
        showSuccessAlert("Activity history refreshed successfully!");
    }

    @FXML
    private void onExportHistory() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Export Activity History");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        
        VBox content = new VBox(15);
        content.setPadding(new javafx.geometry.Insets(20));

        Label title = new Label("Export Activity History");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #1b5e20;");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);

        grid.add(new Label("Export Format:"), 0, 0);
        grid.add(new Label("CSV, Excel, PDF"), 1, 0);
        grid.add(new Label("Date Range:"), 0, 1);
//        grid.add(new Label(dateFromFilter.getValue() + " to " + dateToFilter.getValue()), 1, 1);
        grid.add(new Label("Total Records:"), 0, 2);
        grid.add(new Label(String.valueOf(historyTable.getItems().size())), 1, 2);
        grid.add(new Label("Export Location:"), 0, 3);
        grid.add(new Label("Desktop/Exports/ActivityHistory/"), 1, 3);

        content.getChildren().addAll(title, grid);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setStyle("-fx-background-color: #f5f5f5;");
        
        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.setStyle("-fx-background-color: linear-gradient(to right, #2e7d32, #43a047); -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 6; -fx-padding: 8 16;");
        
        Optional<Void> result = dialog.showAndWait();
        if (result.isPresent()) {
            showSuccessAlert("Activity history exported successfully!\n" +
                           "File saved to: Desktop/Exports/ActivityHistory/\n" +
                           "Records exported: " + historyTable.getItems().size());
        }
    }

    @FXML
    private void onClearOldRecords() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Clear Old Records");
        confirm.setHeaderText("Clear Old Activity Records");
        confirm.setContentText("This will delete activity records older than 6 months. This action cannot be undone. Continue?");
        
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            // Simulate clearing old records
            showSuccessAlert("Old activity records cleared successfully!\n" +
                           "Records older than 6 months have been deleted.");
            loadSampleData();
        }
    }

    private void showActivityDetails(ActivityRecord record) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Activity Details");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        VBox content = new VBox(10);
        content.setPadding(new javafx.geometry.Insets(20));

        Label title = new Label("Activity Details - " + record.getEntityId());
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #1b5e20;");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);

        grid.add(new Label("Timestamp:"), 0, 0);
        grid.add(new Label(record.getTimestamp()), 1, 0);
        grid.add(new Label("User:"), 0, 1);
        grid.add(new Label(record.getUser()), 1, 1);
        grid.add(new Label("Activity Type:"), 0, 2);
        grid.add(new Label(record.getActivityType()), 1, 2);
        grid.add(new Label("Description:"), 0, 3);
        grid.add(new Label(record.getDescription()), 1, 3);
        grid.add(new Label("Entity ID:"), 0, 4);
        grid.add(new Label(record.getEntityId()), 1, 4);
        grid.add(new Label("Entity Type:"), 0, 5);
        grid.add(new Label(record.getEntityType()), 1, 5);
        grid.add(new Label("Status:"), 0, 6);
        grid.add(new Label(record.getStatus()), 1, 6);
        grid.add(new Label("IP Address:"), 0, 7);
        grid.add(new Label(record.getIpAddress()), 1, 7);

        content.getChildren().addAll(title, grid);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setStyle("-fx-background-color: #f5f5f5;");
        dialog.showAndWait();
    }


    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Activity History");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.getDialogPane().setStyle("-fx-background-color: #f5f5f5;");
        alert.showAndWait();
    }

    private void showSuccessAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.getDialogPane().setStyle("-fx-background-color: #f5f5f5;");
        alert.showAndWait();
    }
}
