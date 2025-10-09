package javafxapplication1;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.control.SpinnerValueFactory;
import javafx.stage.FileChooser;
import javafx.util.converter.LocalDateTimeStringConverter;

import java.io.File;
import java.io.FileWriter;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.ResourceBundle;
import javafx.scene.layout.GridPane;

public class SecurityMaintenanceController implements Initializable {

    // Password Policy Controls
    @FXML private Spinner<Integer> minPasswordLength;
    @FXML private Spinner<Integer> passwordExpiration;
    @FXML private Spinner<Integer> passwordHistory;
    @FXML private CheckBox requireUppercase;
    @FXML private CheckBox requireLowercase;
    @FXML private CheckBox requireNumbers;
    @FXML private CheckBox requireSpecialChars;

    // Access Control Controls
    @FXML private Spinner<Integer> sessionTimeout;
    @FXML private Spinner<Integer> maxLoginAttempts;
    @FXML private Spinner<Integer> lockoutDuration;
    @FXML private CheckBox enable2FA;
    @FXML private CheckBox requireStrongPasswords;
    @FXML private CheckBox enableSecurityLogging;

    // Security Monitoring Controls
    @FXML private TextField searchField;
    @FXML private ComboBox<String> eventTypeFilter;
    @FXML private ComboBox<String> severityFilter;
    @FXML private TableView<SecurityEvent> securityEventTable;
    @FXML private TableColumn<SecurityEvent, LocalDateTime> colTimestamp;
    @FXML private TableColumn<SecurityEvent, String> colEventType;
    @FXML private TableColumn<SecurityEvent, String> colSeverity;
    @FXML private TableColumn<SecurityEvent, String> colUser;
    @FXML private TableColumn<SecurityEvent, String> colDescription;
    @FXML private TableColumn<SecurityEvent, String> colIPAddress;
    @FXML private TableColumn<SecurityEvent, String> colEventStatus;


    // Tab Pane
    @FXML private TabPane securityTabPane;

    // Data Collections
    private final ObservableList<SecurityEvent> securityEvents = FXCollections.observableArrayList();
    private final ObservableList<String> eventTypes = FXCollections.observableArrayList();
    private final ObservableList<String> severityLevels = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupTableColumns();
        setupComboBoxes();
        loadSampleData();
        loadCurrentSettings();
    }

    private void setupTableColumns() {
        // Security Event Table
        colTimestamp.setCellValueFactory(new PropertyValueFactory<>("timestamp"));
        DateTimeFormatter timestampFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        colTimestamp.setCellFactory(TextFieldTableCell.forTableColumn(new LocalDateTimeStringConverter(timestampFormatter, timestampFormatter)));
        
        colEventType.setCellValueFactory(new PropertyValueFactory<>("eventType"));
        colSeverity.setCellValueFactory(new PropertyValueFactory<>("severity"));
        colUser.setCellValueFactory(new PropertyValueFactory<>("user"));
        colDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
        colIPAddress.setCellValueFactory(new PropertyValueFactory<>("ipAddress"));
        colEventStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

    }

    private void setupComboBoxes() {
        // Event Types
        eventTypes.addAll("All Events", "Login", "Logout", "Failed Login", "Password Change", "Account Locked", "Account Unlocked", "Permission Change", "System Error", "Security Alert");
        eventTypeFilter.setItems(eventTypes);
        eventTypeFilter.setValue("All Events");

        // Severity Levels
        severityLevels.addAll("All Severity", "Low", "Medium", "High", "Critical");
        severityFilter.setItems(severityLevels);
        severityFilter.setValue("All Severity");

    }

    private void loadSampleData() {
        // Sample Security Events
        securityEvents.addAll(
            new SecurityEvent(LocalDateTime.now().minusHours(2), "Login", "Low", "admin", "Successful login", "192.168.1.100", "Success"),
            new SecurityEvent(LocalDateTime.now().minusHours(3), "Failed Login", "Medium", "user1", "Invalid password attempt", "192.168.1.101", "Failed"),
            new SecurityEvent(LocalDateTime.now().minusHours(4), "Password Change", "Low", "admin", "Password changed successfully", "192.168.1.100", "Success"),
            new SecurityEvent(LocalDateTime.now().minusHours(5), "Account Locked", "High", "user2", "Account locked due to multiple failed attempts", "192.168.1.102", "Locked"),
            new SecurityEvent(LocalDateTime.now().minusHours(6), "System Error", "Critical", "system", "Database connection failed", "127.0.0.1", "Error"),
            new SecurityEvent(LocalDateTime.now().minusHours(7), "Permission Change", "Medium", "admin", "User role changed", "192.168.1.100", "Success"),
            new SecurityEvent(LocalDateTime.now().minusHours(8), "Security Alert", "High", "system", "Suspicious activity detected", "192.168.1.103", "Alert"),
            new SecurityEvent(LocalDateTime.now().minusHours(9), "Logout", "Low", "user3", "User logged out", "192.168.1.104", "Success")
        );
        securityEventTable.setItems(securityEvents);

    }

    private void loadCurrentSettings() {
        // Set up spinner value factories
        minPasswordLength.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(4, 50, 8));
        passwordExpiration.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 365, 90));
        passwordHistory.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 20, 5));
        sessionTimeout.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(5, 480, 30));
        maxLoginAttempts.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 20, 5));
        lockoutDuration.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 1440, 15));
        
        // Set checkbox values
        requireUppercase.setSelected(true);
        requireLowercase.setSelected(true);
        requireNumbers.setSelected(true);
        requireSpecialChars.setSelected(true);
        enable2FA.setSelected(false);
        requireStrongPasswords.setSelected(true);
        enableSecurityLogging.setSelected(true);
    }

    // Password Policy Actions
    @FXML
    private void onSavePasswordPolicy() {
        Task<Void> saveTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                // Simulate saving password policy
                Thread.sleep(1000);
                return null;
            }
        };

        saveTask.setOnSucceeded(e -> {
            showSuccessAlert("Password policy saved successfully!");
            logSecurityEvent("Password Policy", "Low", "admin", "Password policy updated");
        });

        saveTask.setOnFailed(e -> {
            showErrorAlert("Failed to save password policy!");
        });

        new Thread(saveTask).start();
    }

    @FXML
    private void onResetPasswordPolicy() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Reset Password Policy");
        confirm.setHeaderText(null);
        confirm.setContentText("Reset password policy to default settings?");
        Optional<ButtonType> result = confirm.showAndWait();
        
        if (result.isPresent() && result.get() == ButtonType.OK) {
            loadCurrentSettings();
            showSuccessAlert("Password policy reset to default settings!");
            logSecurityEvent("Password Policy", "Low", "admin", "Password policy reset to defaults");
        }
    }

    // Access Control Actions
    @FXML
    private void onSaveAccessControl() {
        Task<Void> saveTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                // Simulate saving access control settings
                Thread.sleep(1000);
                return null;
            }
        };

        saveTask.setOnSucceeded(e -> {
            showSuccessAlert("Access control settings saved successfully!");
            logSecurityEvent("Access Control", "Low", "admin", "Access control settings updated");
        });

        saveTask.setOnFailed(e -> {
            showErrorAlert("Failed to save access control settings!");
        });

        new Thread(saveTask).start();
    }

    @FXML
    private void onResetAccessControl() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Reset Access Control");
        confirm.setHeaderText(null);
        confirm.setContentText("Reset access control settings to default?");
        Optional<ButtonType> result = confirm.showAndWait();
        
        if (result.isPresent() && result.get() == ButtonType.OK) {
            loadCurrentSettings();
            showSuccessAlert("Access control settings reset to default!");
            logSecurityEvent("Access Control", "Low", "admin", "Access control settings reset to defaults");
        }
    }

    // Security Monitoring Actions
    @FXML
    private void onSearchSecurityEvents() {
        String searchText = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        String selectedEventType = eventTypeFilter.getValue();
        String selectedSeverity = severityFilter.getValue();

        if (searchText.isEmpty() && 
            (selectedEventType == null || selectedEventType.equals("All Events")) &&
            (selectedSeverity == null || selectedSeverity.equals("All Severity"))) {
            securityEventTable.setItems(securityEvents);
            return;
        }

        ObservableList<SecurityEvent> filtered = FXCollections.observableArrayList();
        for (SecurityEvent event : securityEvents) {
            boolean matchesSearch = searchText.isEmpty() ||
                                  event.getDescription().toLowerCase().contains(searchText) ||
                                  event.getUser().toLowerCase().contains(searchText) ||
                                  event.getIpAddress().toLowerCase().contains(searchText);
            boolean matchesEventType = selectedEventType == null || selectedEventType.equals("All Events") ||
                                     event.getEventType().equals(selectedEventType);
            boolean matchesSeverity = selectedSeverity == null || selectedSeverity.equals("All Severity") ||
                                    event.getSeverity().equals(selectedSeverity);

            if (matchesSearch && matchesEventType && matchesSeverity) {
                filtered.add(event);
            }
        }
        securityEventTable.setItems(filtered);
    }

    @FXML
    private void onClearSecurityLog() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Clear Security Log");
        confirm.setHeaderText(null);
        confirm.setContentText("Clear all security event logs? This action cannot be undone.");
        Optional<ButtonType> result = confirm.showAndWait();
        
        if (result.isPresent() && result.get() == ButtonType.OK) {
            securityEvents.clear();
            showSuccessAlert("Security log cleared successfully!");
            logSecurityEvent("System", "Medium", "admin", "Security log cleared");
        }
    }





    // General Actions
    @FXML
    private void onRefresh() {
        loadSampleData();
        showSuccessAlert("Security data refreshed successfully!");
    }

    @FXML
    private void onExportSecurityLog() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Security Log");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        fileChooser.setInitialFileName("security_log_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".csv");
        
        File file = fileChooser.showSaveDialog(null);
        if (file != null) {
            Task<Void> exportTask = new Task<Void>() {
                @Override
                protected Void call() throws Exception {
                    try (FileWriter writer = new FileWriter(file)) {
                        writer.append("Timestamp,Event Type,Severity,User,Description,IP Address,Status\n");
                        for (SecurityEvent event : securityEvents) {
                            writer.append(String.format("%s,%s,%s,%s,%s,%s,%s\n",
                                event.getTimestamp().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                                event.getEventType(),
                                event.getSeverity(),
                                event.getUser(),
                                event.getDescription(),
                                event.getIpAddress(),
                                event.getStatus()
                            ));
                        }
                    }
                    return null;
                }
            };

            exportTask.setOnSucceeded(e -> {
                showSuccessAlert("Security log exported successfully to: " + file.getName());
                logSecurityEvent("Export", "Low", "admin", "Security log exported");
            });

            exportTask.setOnFailed(e -> {
                showErrorAlert("Failed to export security log!");
            });

            new Thread(exportTask).start();
        }
    }

    // Helper Methods

    private void logSecurityEvent(String eventType, String severity, String user, String description) {
        Platform.runLater(() -> {
            SecurityEvent event = new SecurityEvent(
                LocalDateTime.now(),
                eventType,
                severity,
                user,
                description,
                "192.168.1.100",
                "Success"
            );
            securityEvents.add(0, event); // Add to top of list
        });
    }

    private void showSuccessAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Security Maintenance");
        alert.setHeaderText("Success");
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showErrorAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Security Maintenance");
        alert.setHeaderText("Error");
        alert.setContentText(message);
        alert.showAndWait();
    }

    // Data Classes
    public static class SecurityEvent {
        private final LocalDateTime timestamp;
        private final String eventType;
        private final String severity;
        private final String user;
        private final String description;
        private final String ipAddress;
        private final String status;

        public SecurityEvent(LocalDateTime timestamp, String eventType, String severity, String user, String description, String ipAddress, String status) {
            this.timestamp = timestamp;
            this.eventType = eventType;
            this.severity = severity;
            this.user = user;
            this.description = description;
            this.ipAddress = ipAddress;
            this.status = status;
        }

        // Getters
        public LocalDateTime getTimestamp() { return timestamp; }
        public String getEventType() { return eventType; }
        public String getSeverity() { return severity; }
        public String getUser() { return user; }
        public String getDescription() { return description; }
        public String getIpAddress() { return ipAddress; }
        public String getStatus() { return status; }
    }

}
