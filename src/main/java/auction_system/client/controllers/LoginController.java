package auction_system.client.controllers;

import auction_system.Utils.CryptoUtils;
import auction_system.Utils.ViewUtils;
import auction_system.server.dao.UserDAO;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;

public class LoginController {
    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private Label statusLabel;

    private Stage stage;
    private Scene scene;
    private Parent root;

    public void login(ActionEvent event) throws IOException {
        //Trường hợp không điền gì
        String username = usernameField.getText();
        String password = passwordField.getText();
        if(username.isEmpty() || password.isEmpty()) {
            statusLabel.setText("Phải điền đầy đủ thông tin đăng nhập!");
            return;
        }
        //Trường hợp có điền đầy đủ
        if(UserDAO.checkLogin(username,password)) {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/auction_list.fxml"));
            root = loader.load();
            AuctionListController auctionListController = loader.getController();

            scene = ViewUtils.getScene();
            scene.setRoot(root);
        }
        else {
            statusLabel.setText("Sai tài khoản hoặc mật khẩu!");
        }
    }

    public void exit(ActionEvent event) {
        stage = ViewUtils.getStage();

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Exit");
        alert.setHeaderText("You're about to exit!");
        alert.setContentText("Are you sure?");
        alert.initOwner(stage);

        if(alert.showAndWait().get() == ButtonType.OK) {
            stage.close();
        }
    }

    public void register(ActionEvent event) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/register.fxml"));
        root = loader.load();
        RegisterController registerController = loader.getController();

        scene = ViewUtils.getScene();
        scene.setRoot(root);

        ViewUtils.scaleLayout(scene);
    }
}
