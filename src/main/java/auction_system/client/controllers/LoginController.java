package auction_system.client.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;


public class LoginController implements Initializable {
    @FXML private VBox registerForm;
    @FXML private VBox loginForm;
    @FXML private TextField usernameField;
    @FXML private TextField fullnameField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;

    public void initialize(java.net.URL location, java.util.ResourceBundle resources) {
        registerForm.setVisible(false);
        loginForm.setVisible(true);
    }

    public void showRegisterForm(ActionEvent e) {
        loginForm.setVisible(false);
        registerForm.setVisible(true);
    }

    public void showLoginForm(ActionEvent e) {
        registerForm.setVisible(false);
        loginForm.setVisible(true);
    }

    public void exit(ActionEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Exit");
        alert.setHeaderText("You're about to exit!");
        alert.setContentText("Are you sure?");
        alert.initOwner(stage);

        if(alert.showAndWait().get() == ButtonType.OK) {
            stage.close();
        }
    }

    public void login(ActionEvent e) {
        String username = usernameField.getText();
        String password = passwordField.getText();
        // AuthService will be here
    }

    public void register(ActionEvent e) {
        String fullname  = fullnameField.getText();
        String username = usernameField.getText();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();
        // AuthService will be here
    }
}
