package auction_system.client.controllers;

import auction_system.Utils.ViewUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
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
        //check data (code database ở đây)

        //Trường hợp có điền đầy đủ
        System.out.println(username);
        System.out.println(password);
        if(username.equals("52ducbanh") && password.equals("123")) {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/auction_list.fxml"));
            root = loader.load();
            AuctionListController auctionListController = loader.getController();

            stage = (Stage)((Node)event.getSource()).getScene().getWindow();
            scene = ViewUtils.createScene("/fxml/auction_list.fxml","/css/login.css");
            stage.setScene(scene);

            stage.setFullScreen(true);
            stage.setFullScreenExitHint("");
            stage.show();
        }
        else {
            statusLabel.setText("Sai tài khoản hoặc mật khẩu!");
        }
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

    public void register(ActionEvent event) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/register.fxml"));
        root = loader.load();
        RegisterController registerController = loader.getController();

        stage = (Stage)((Node)event.getSource()).getScene().getWindow();
        scene = ViewUtils.createScene("/fxml/register.fxml","/css/login.css");
        stage.setScene(scene);
        stage.setFullScreen(true);
        stage.setFullScreenExitHint("");
        stage.show();

        ViewUtils.scaleLayout(scene);
    }
}
