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
    @FXML private Button userManagementBtn;
    @FXML private Button userAccessBtn;

    @FXML private void onLogout() { info("Logout", "Implement logout navigation here."); }
    @FXML private void onEditProfile() { setCenterContent("/javafxapplication1/EditProfile.fxml"); }
    @FXML private void openDashboard() { if (defaultDashboardContent != null) contentScroll.setContent(defaultDashboardContent); }
    @FXML private void openEmployeeManagement() { setCenterContent("/javafxapplication1/Employee.fxml"); }
    @FXML private void openPayrollProcessing() { setCenterContent("/javafxapplication1/PayrollProcessing.fxml"); }
    @FXML private void openPayrollGenerator() { setCenterContent("/javafxapplication1/PayrollGenerator.fxml"); }
    @FXML private void openReports() { setCenterContent("/javafxapplication1/Reports.fxml"); }
    @FXML private void openImportExport() { setCenterContent("/javafxapplication1/ImportExport.fxml"); }
    @FXML private void openHistory() { setCenterContent("/javafxapplication1/History.fxml"); }
    @FXML private void openSecurity() { setCenterContent("/javafxapplication1/SecurityMaintenance.fxml"); }
    @FXML private void openGovernmentRemittances() { setCenterContent("/javafxapplication1/GovernmentRemittances.fxml"); }
    @FXML private void openUserManagement() { setCenterContent("/javafxapplication1/UserManagement.fxml"); }
    @FXML private void openUserAccess() { setCenterContent("/javafxapplication1/UserAccess.fxml"); }

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
