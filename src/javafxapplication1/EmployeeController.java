package javafxapplication1;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.HashSet;
import javafx.scene.layout.GridPane;

public class EmployeeController implements Initializable {

    public static class Employee {
        private final String id;
        private final String name;
        private final String position;
        private final String department;
        private final double salary;

        public Employee(String id, String name, String position, String department, double salary) {
            this.id = id;
            this.name = name;
            this.position = position;
            this.department = department;
            this.salary = salary;
        }
        public String getId() { return id; }
        public String getName() { return name; }
        public String getPosition() { return position; }
        public String getDepartment() { return department; }
        public double getSalary() { return salary; }
    }

    @FXML private TextField searchField;
    @FXML private ComboBox<String> departmentFilter;
    @FXML private TableView<Employee> table;
    @FXML private TableColumn<Employee, String> colId;
    @FXML private TableColumn<Employee, String> colName;
    @FXML private TableColumn<Employee, String> colPosition;
    @FXML private TableColumn<Employee, String> colDepartment;
    @FXML private TableColumn<Employee, Double> colSalary;

    private final ObservableList<Employee> data = FXCollections.observableArrayList();
    private final ObservableList<String> departments = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupTableColumns();
        setupDepartmentFilter();
        loadSampleData();
    }

    private void setupTableColumns() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colPosition.setCellValueFactory(new PropertyValueFactory<>("position"));
        colDepartment.setCellValueFactory(new PropertyValueFactory<>("department"));
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

    private void setupDepartmentFilter() {
        departments.addAll("All Departments", "Math", "Science", "English", "Admin", "Finance", "ICT", "Library", "Transport", "HR", "Registrar", "Student Affairs", "Clinic");
        departmentFilter.setItems(departments);
        departmentFilter.setValue("All Departments");
    }

    private void loadSampleData() {
        data.setAll(loadEmployees());
        table.setItems(data);
    }

    /**
     * Replace this method with a DB/service call later.
     * Keep the returned list shape the same so UI code stays unchanged.
     */
    private ObservableList<Employee> loadEmployees() {
        ObservableList<Employee> list = FXCollections.observableArrayList();
        list.addAll(
                new Employee("E-001", "Juan Dela Cruz", "Teacher I", "Math", 25000),
                new Employee("E-002", "Maria Santos", "Registrar", "Admin", 30000),
                new Employee("E-003", "Pedro Reyes", "Teacher II", "Science", 28000),
                new Employee("E-004", "Ana Lopez", "Cashier", "Finance", 27000),
                new Employee("E-005", "Rico Valdez", "IT Support", "ICT", 32000),
                new Employee("E-006", "Liza Cruz", "Teacher I", "English", 25500),
                new Employee("E-007", "Jose Garcia", "Teacher III", "Math", 35000),
                new Employee("E-008", "Carla Ramos", "Librarian", "Library", 26000),
                new Employee("E-009", "Mark Aquino", "Security Officer", "Admin", 20000),
                new Employee("E-010", "Jessa Lim", "Nurse", "Clinic", 29000),
                new Employee("E-011", "Noel Santos", "Guidance Counselor", "Student Affairs", 31000),
                new Employee("E-012", "Eden Cruz", "Teacher I", "Filipino", 25000),
                new Employee("E-013", "Cathy Uy", "Teacher II", "AP", 27500),
                new Employee("E-014", "Benjie Flores", "Driver", "Transport", 22000),
                new Employee("E-015", "Rhea Pangan", "Encoder", "Registrar", 23000),
                new Employee("E-016", "Arvin Dizon", "Technician", "ICT", 26000),
                new Employee("E-017", "Grace Dela Rosa", "HR Staff", "HR", 30000),
                new Employee("E-018", "Daniel Cruz", "Teacher I", "MAPEH", 25000),
                new Employee("E-019", "Ivy Mendoza", "Accountant", "Finance", 36000),
                new Employee("E-020", "Pauline Reyes", "Assistant Registrar", "Registrar", 28000)
        );
        return list;
    }

    @FXML
    private void onSearch() {
        String searchText = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        String selectedDept = departmentFilter.getValue();
        
        if (searchText.isEmpty() && (selectedDept == null || selectedDept.equals("All Departments"))) {
            table.setItems(data);
            return;
        }

        ObservableList<Employee> filtered = FXCollections.observableArrayList();
        for (Employee employee : data) {
            boolean matchesSearch = searchText.isEmpty() || 
                                  employee.getId().toLowerCase().contains(searchText) || 
                                  employee.getName().toLowerCase().contains(searchText);
            boolean matchesDept = selectedDept == null || selectedDept.equals("All Departments") || 
                                employee.getDepartment().equals(selectedDept);
            
            if (matchesSearch && matchesDept) {
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
            data.add(employee);
            showSuccessAlert("Employee added successfully!");
        });
    }

    @FXML
    private void onEdit() {
        Employee selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) { showInfo("Please select a row to edit."); return; }
        Dialog<Employee> dialog = buildEmployeeDialog(selected);
        Optional<Employee> result = dialog.showAndWait();
        result.ifPresent(updated -> {
            int idx = data.indexOf(selected);
            if (idx >= 0) {
                data.set(idx, updated);
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
            data.remove(selected);
        }
    }

    private Dialog<Employee> buildEmployeeDialog(Employee existing) {
        Dialog<Employee> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "Add New Employee" : "Edit Employee");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        
        // Style the dialog
        dialog.getDialogPane().setStyle("-fx-background-color: #f5f5f5;");
        dialog.getDialogPane().setPrefSize(500, 400);

        // Create form fields with modern styling
        TextField idField = new TextField(existing == null ? "" : existing.getId());
        idField.setPromptText("Enter Employee ID (e.g., E-001)");
        idField.setStyle("-fx-background-radius: 6; -fx-border-radius: 6; -fx-border-color: #c8e6c9; -fx-focus-color: #66bb6a; -fx-padding: 8;");
        
        TextField nameField = new TextField(existing == null ? "" : existing.getName());
        nameField.setPromptText("Enter Full Name");
        nameField.setStyle("-fx-background-radius: 6; -fx-border-radius: 6; -fx-border-color: #c8e6c9; -fx-focus-color: #66bb6a; -fx-padding: 8;");
        
        TextField positionField = new TextField(existing == null ? "" : existing.getPosition());
        positionField.setPromptText("Enter Position (e.g., Teacher I)");
        positionField.setStyle("-fx-background-radius: 6; -fx-border-radius: 6; -fx-border-color: #c8e6c9; -fx-focus-color: #66bb6a; -fx-padding: 8;");
        
        ComboBox<String> departmentCombo = new ComboBox<>();
        departmentCombo.getItems().addAll("Math", "Science", "English", "Admin", "Finance", "ICT", "Library", "Transport", "HR", "Registrar", "Student Affairs", "Clinic", "Filipino", "AP", "MAPEH");
        departmentCombo.setValue(existing == null ? "Select Department" : existing.getDepartment());
        departmentCombo.setPromptText("Select Department");
        departmentCombo.setStyle("-fx-background-radius: 6; -fx-border-radius: 6; -fx-border-color: #c8e6c9; -fx-focus-color: #66bb6a; -fx-padding: 8;");
        
        TextField salaryField = new TextField(existing == null ? "" : String.valueOf(existing.getSalary()));
        salaryField.setPromptText("Enter Salary (e.g., 25000)");
        salaryField.setStyle("-fx-background-radius: 6; -fx-border-radius: 6; -fx-border-color: #c8e6c9; -fx-focus-color: #66bb6a; -fx-padding: 8;");

        // Create styled labels
        Label idLabel = new Label("Employee ID:");
        idLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #2e7d32; -fx-font-size: 14px;");
        
        Label nameLabel = new Label("Full Name:");
        nameLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #2e7d32; -fx-font-size: 14px;");
        
        Label positionLabel = new Label("Position:");
        positionLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #2e7d32; -fx-font-size: 14px;");
        
        Label departmentLabel = new Label("Department:");
        departmentLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #2e7d32; -fx-font-size: 14px;");
        
        Label salaryLabel = new Label("Salary:");
        salaryLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #2e7d32; -fx-font-size: 14px;");

        // Create form layout
        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(15);
        grid.setPadding(new javafx.geometry.Insets(20));
        
        grid.addRow(0, idLabel, idField);
        grid.addRow(1, nameLabel, nameField);
        grid.addRow(2, positionLabel, positionField);
        grid.addRow(3, departmentLabel, departmentCombo);
        grid.addRow(4, salaryLabel, salaryField);
        
        // Set column constraints for better layout
        javafx.scene.layout.ColumnConstraints col1 = new javafx.scene.layout.ColumnConstraints();
        col1.setPrefWidth(120);
        javafx.scene.layout.ColumnConstraints col2 = new javafx.scene.layout.ColumnConstraints();
        col2.setPrefWidth(300);
        grid.getColumnConstraints().addAll(col1, col2);
        
        dialog.getDialogPane().setContent(grid);

        // Style the buttons
        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.setStyle("-fx-background-color: linear-gradient(to right, #2e7d32, #43a047); -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 6; -fx-padding: 8 16;");
        
        Button cancelButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL);
        cancelButton.setStyle("-fx-background-color: #f5f5f5; -fx-text-fill: #666; -fx-border-color: #ddd; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 8 16;");

        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                // Validate inputs
                if (idField.getText().trim().isEmpty()) {
                    showInfo("Please enter Employee ID.");
                    return null;
                }
                if (nameField.getText().trim().isEmpty()) {
                    showInfo("Please enter Full Name.");
                    return null;
                }
                if (positionField.getText().trim().isEmpty()) {
                    showInfo("Please enter Position.");
                    return null;
                }
                if (departmentCombo.getValue() == null || departmentCombo.getValue().equals("Select Department")) {
                    showInfo("Please select Department.");
                    return null;
                }
                
                double salary;
                try {
                    salary = Double.parseDouble(salaryField.getText());
                    if (salary < 0) {
                        showInfo("Salary must be a positive number.");
                        return null;
                    }
                } catch (NumberFormatException ex) {
                    showInfo("Please enter a valid salary amount.");
                    return null;
                }
                
                return new Employee(idField.getText().trim(), nameField.getText().trim(), 
                                  positionField.getText().trim(), departmentCombo.getValue(), salary);
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
}


