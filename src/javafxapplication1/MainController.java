/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/javafx/FXMLController.java to edit this template
 */
package javafxapplication1;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.fxml.Initializable;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.VBox;

/**
 * FXML Controller class
 *
 * @author marke
 */
public class MainController implements Initializable {

    /**
     * Initializes the controller class.
     */
    private Node defaultDashboardContent;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Keep the original dashboard content so we can restore it later
        // when navigating back from other modules.
        // contentScroll is injected after FXML load, so it's safe here.
        if (contentScroll != null) {
            defaultDashboardContent = contentScroll.getContent();
        }
    }    
    @FXML private Button dashboardBtn;
    @FXML private Button employeeMgmtBtn;
    @FXML private Button payrollProcessingBtn;
    @FXML private Button payrollGeneratorBtn;
    @FXML private Button reportsBtn;
    @FXML private Button importExportBtn;
    @FXML private Button historyBtn;
    @FXML private Button securityBtn;
    @FXML private Button govRemitBtn;
    @FXML private Button userAccessBtn;

    @FXML private void onLogout() { info("Logout", "Implement logout navigation here."); }
    @FXML private void onEditProfile() { info("Profile", "Open edit profile view."); }
    @FXML private void openDashboard() { if (defaultDashboardContent != null) contentScroll.setContent(defaultDashboardContent); }
    @FXML private void openEmployeeManagement() { setCenterContent("/javafxapplication1/Employee.fxml"); }
    @FXML private void openPayrollProcessing() { info("Payroll Processing", "Open payroll processing view."); }
    @FXML private void openPayrollGenerator() { info("Payroll Generator", "Open payroll generator view."); }
    @FXML private void openReports() { info("Reports", "Open reports view."); }
    @FXML private void openImportExport() { info("Import / Export", "Open import/export view."); }
    @FXML private void openHistory() { info("History", "Open history view."); }
    @FXML private void openSecurity() { info("Security Maintenance", "Open security maintenance view."); }
    @FXML private void openGovernmentRemittances() { info("Government Remittances", "Open remittances view."); }
    @FXML private void openUserAccess() { info("User Access", "Open user access view."); }

    private void info(String title, String content) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    @FXML private javafx.scene.control.ScrollPane contentScroll;

    private void setCenterContent(String fxmlPath) {
        try {
            Node content = FXMLLoader.load(getClass().getResource(fxmlPath));
            contentScroll.setContent(content);
        } catch (Exception ex) {
            info("Load Error", "Cannot load view: " + ex.getMessage());
        }
    }
}
