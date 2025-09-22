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
    @FXML private TableView<Employee> table;
    @FXML private TableColumn<Employee, String> colId;
    @FXML private TableColumn<Employee, String> colName;
    @FXML private TableColumn<Employee, String> colPosition;
    @FXML private TableColumn<Employee, String> colDepartment;
    @FXML private TableColumn<Employee, Double> colSalary;

    private final ObservableList<Employee> data = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colPosition.setCellValueFactory(new PropertyValueFactory<>("position"));
        colDepartment.setCellValueFactory(new PropertyValueFactory<>("department"));
        colSalary.setCellValueFactory(new PropertyValueFactory<>("salary"));

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
        String q = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        if (q.isEmpty()) {
            table.setItems(data);
            return;
        }
        ObservableList<Employee> filtered = FXCollections.observableArrayList();
        for (Employee e : data) {
            if (e.getId().toLowerCase().contains(q) || e.getName().toLowerCase().contains(q)) {
                filtered.add(e);
            }
        }
        table.setItems(filtered);
    }

    @FXML
    private void onAdd() {
        Dialog<Employee> dialog = buildEmployeeDialog(null);
        Optional<Employee> result = dialog.showAndWait();
        result.ifPresent(data::add);
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
        dialog.setTitle(existing == null ? "Add Employee" : "Edit Employee");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        TextField idField = new TextField(existing == null ? "" : existing.getId());
        TextField nameField = new TextField(existing == null ? "" : existing.getName());
        TextField positionField = new TextField(existing == null ? "" : existing.getPosition());
        TextField departmentField = new TextField(existing == null ? "" : existing.getDepartment());
        TextField salaryField = new TextField(existing == null ? String.valueOf(existing.getSalary()) : String.valueOf(existing.getSalary()));

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.addRow(0, new Label("ID"), idField);
        grid.addRow(1, new Label("Name"), nameField);
        grid.addRow(2, new Label("Position"), positionField);
        grid.addRow(3, new Label("Department"), departmentField);
        grid.addRow(4, new Label("Salary"), salaryField);
        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                double salary;
                try {
                    salary = Double.parseDouble(salaryField.getText());
                } catch (NumberFormatException ex) {
                    showInfo("Invalid salary.");
                    return null;
                }
                return new Employee(idField.getText(), nameField.getText(), positionField.getText(), departmentField.getText(), salary);
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
}


