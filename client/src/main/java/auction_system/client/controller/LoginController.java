package auction_system.client.controller;

import auction_system.client.service.AuthService;
import auction_system.client.service.AuctionListService; // <-- THÊM IMPORT
import auction_system.common.dto.UserDTO;
import javafx.animation.PauseTransition;
import javafx.animation.TranslateTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;


public class LoginController  {
    @FXML private PasswordField LoginPasswordTextField;
    @FXML private Label LoginStatusLabel;
    @FXML private TextField LoginUsernameTextField;
    @FXML private Label RegisterStatusLabel;
    @FXML private PasswordField registerConfirmPasswordTextField;
    @FXML private TextField registerFullnameTextField;
    @FXML private PasswordField registerPasswordTextField;
    @FXML private TextField registerUsernameTextField;
    @FXML private Button side_CreateBtn;
    @FXML private Button side_alreadyHave;
    @FXML private AnchorPane side_form;
    @FXML private Button registerBtn;

    @FXML
    void loginBtnAction(ActionEvent event) {
        UserDTO user = new UserDTO(LoginUsernameTextField.getText(),LoginPasswordTextField.getText());
        if(user.getUsername().isEmpty() || user.getPassword().isEmpty()) {
            LoginStatusLabel.setText("Vui lòng nhập đầy đủ thông tin!");
            return;
        }

        int varCheck = AuthService.getInstance().checkLogin(user);
        if(varCheck == 1) {
            // BẮT ĐẦU TẢI DỮ LIỆU NGAY SAU KHI ĐĂNG NHẬP THÀNH CÔNG
            AuctionListService.getInstance().fetchAllAuctions();

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
        else if(varCheck == -1) {
            //Admin will be load in here :>
            try {
                FXMLLoader loader = new FXMLLoader();
                loader.setLocation(getClass().getResource("/fxml/AdminMainAuction.fxml"));
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
            LoginStatusLabel.setText("Sai tài khoản hoặc mật khẩu!");
        }
    }

    @FXML
    void registerBtnAction(ActionEvent event) {
        String fullname = registerFullnameTextField.getText();
        String username = registerUsernameTextField.getText();
        String password = registerPasswordTextField.getText();
        String confirmPassword = registerConfirmPasswordTextField.getText();
        if(fullname.isEmpty() || username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            RegisterStatusLabel.setText("Vui lòng nhập đầy đủ thông tin!");
            return;
        }
        if(!password.equals(confirmPassword)) {
            RegisterStatusLabel.setText("Mật khẩu không khớp!");
            return;
        }

        UserDTO user = new UserDTO(fullname,username,password);
        if(AuthService.getInstance().checkRegister(user)) {
            RegisterStatusLabel.setTextFill(Color.GREEN);
            RegisterStatusLabel.setText("Đăng ký thành công, quay lại trang đăng nhập sau 3s!");
            PauseTransition delay = new PauseTransition(Duration.seconds(3));
            delay.setOnFinished(e -> {
                switchForm(event);
            });
            delay.play();
        }
        else {
            RegisterStatusLabel.setText("Tài khoản đã tồn tại!");
        }
    }

    @FXML
    void switchForm(ActionEvent event) {
        RegisterStatusLabel.setText("");
        LoginStatusLabel.setText("");
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
        } else if (event.getSource() == side_alreadyHave || event.getSource() == registerBtn) {
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