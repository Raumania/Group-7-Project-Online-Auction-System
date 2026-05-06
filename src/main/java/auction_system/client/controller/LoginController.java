package auction_system.client.controller;

import auction_system.client.model.User;
import auction_system.client.service.AuthService;
import javafx.animation.TranslateTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;


public class LoginController  {
    @FXML
    private PasswordField confirmPasswordTextField;

    @FXML
    private TextField fullnameTextField;

    @FXML
    private PasswordField passwordTextField;

    @FXML
    private TextField usernameTextField;

    @FXML
    private Button side_CreateBtn;

    @FXML
    private Button side_alreadyHave;

    @FXML
    private AnchorPane side_form;

    @FXML
    private Label statusLabel;

    @FXML
    void loginBtn(ActionEvent event) {
        User user = new User(usernameTextField.getText(),passwordTextField.getText());
        if(AuthService.getInstance().checkLogin(user)) {
            try {
                FXMLLoader loader = new FXMLLoader();
                loader.setLocation(getClass().getResource("/fxml/mainAuction.fxml"));
                Parent root = loader.load();
                Scene scene = new Scene(root);

                Stage stage = (Stage) side_form.getScene().getWindow();
                stage.setScene(scene);
            }
            catch(IOException e) {
                e.printStackTrace();
            }
        }
        else {
            statusLabel.setText("Sai tài khoản hoặc mật khẩu!");
        }
    }

    @FXML
    void registerBtn(ActionEvent event) {

    }

    @FXML
    void switchForm(ActionEvent event) {

        TranslateTransition slider = new TranslateTransition();

        if (event.getSource() == side_CreateBtn) {
            slider.setNode(side_form);
            slider.setToX(640);
            slider.setDuration(Duration.seconds(1));

            slider.setOnFinished((ActionEvent e) -> {
                side_alreadyHave.setVisible(true);
                side_CreateBtn.setVisible(false);
            });

            slider.play();
        } else if (event.getSource() == side_alreadyHave) {
            slider.setNode(side_form);
            slider.setToX(0);
            slider.setDuration(Duration.seconds(1));

            slider.setOnFinished((ActionEvent e) -> {
                side_alreadyHave.setVisible(false);
                side_CreateBtn.setVisible(true);
            });

            slider.play();
        }

    }

}
