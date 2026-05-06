package auction_system.controller;

import auction_system.client.AuctionClient;
import auction_system.server.common.protocol.Response;
import auction_system.util.SceneSwitcher;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginController {
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;

    private AuctionClient client;
    private SceneSwitcher switcher;

    public void setClient(AuctionClient client) { this.client = client; }
    public void setSceneSwitcher(SceneSwitcher switcher) { this.switcher = switcher; }

    @FXML
    private void handleLogin() {
        String username = usernameField.getText();
        String password = passwordField.getText();

        Task<Response> loginTask = new Task<>() {
            @Override
            protected Response call() throws Exception {
                return client.login(username, password);
            }
        };
        loginTask.setOnSucceeded(event -> {
            Response res = loginTask.getValue();
            if ("SUCCESS".equals(res.getStatus())) {
                // Lưu user nếu cần (có thể parse res.getData() thành User)
                try {
                    switcher.switchTo("/view/dashboard.fxml", "Dashboard");
                } catch (Exception e) {
                    errorLabel.setText("Lỗi chuyển màn hình: " + e.getMessage());
                }
            } else {
                errorLabel.setText(res.getMessage());
            }
        });
        loginTask.setOnFailed(event ->
                errorLabel.setText("Lỗi kết nối: " + loginTask.getException().getMessage())
        );
        new Thread(loginTask).start();
    }
}