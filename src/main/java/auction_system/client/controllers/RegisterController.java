package auction_system.client.controllers;

import auction_system.Utils.ViewUtils;
import javafx.animation.PauseTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;

public class RegisterController {
    @FXML
    private TextField fullnameField;
    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private Label statusLabel;
    @FXML
    private Label okLabel1;
    @FXML
    private Label okLabel2;

    private Parent root;
    private Scene scene;
    private Stage stage;

    public void register(ActionEvent event) throws InterruptedException {
        String fullname = fullnameField.getText();
        String username = usernameField.getText();
        String password = passwordField.getText();

        if(fullname.isEmpty() || username.isEmpty() || password.isEmpty()) {
            statusLabel.setText("Phải điền đầy đủ thông tin đăng ký !");
            return;
        }
        //create data (code database ở đây)

        //Trường hợp có điền đầy đủ
        statusLabel.setText("");
        okLabel1.setText("Đã tạo tài khoản thành công!");
        okLabel2.setText("Quay trở lại trang đăng nhập sau 3s...");
        PauseTransition pause = new PauseTransition(Duration.seconds(3));
        pause.setOnFinished(e -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
                root = loader.load();
                LoginController loginController = loader.getController();

                stage = (Stage)((Node)event.getSource()).getScene().getWindow();
                scene = ViewUtils.createScene("/fxml/login.fxml","/css/login.css");
                stage.setScene(scene);
                stage.setFullScreen(true);
                stage.setFullScreenExitHint("");
                stage.show();

                ViewUtils.scaleLayout(scene);
            }
            catch(IOException k) {
                k.printStackTrace();
            }
        });
        pause.play();
    }

    public void login(ActionEvent event) throws IOException{
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
        root = loader.load();
        LoginController loginController = loader.getController();

        stage = (Stage)((Node)event.getSource()).getScene().getWindow();
        scene = ViewUtils.createScene("/fxml/login.fxml","/css/login.css");
        stage.setScene(scene);
        stage.setFullScreen(true);
        stage.setFullScreenExitHint("");
        stage.show();

        ViewUtils.scaleLayout(scene);
    }

    public void exit(ActionEvent event) {
        stage = (Stage)((Node)event.getSource()).getScene().getWindow();

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Exit");
        alert.setHeaderText("You're about to exit!");
        alert.setContentText("Are you sure?");
        alert.initOwner(stage);

        if(alert.showAndWait().get() == ButtonType.OK) {
            stage = (Stage)((Node)event.getSource()).getScene().getWindow();
            stage.close();
        }
    }

}