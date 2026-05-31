package auction_system.client.controller;

import auction_system.client.service.AuthService;
import auction_system.client.service.AuctionListService; // <-- ADD IMPORT
import auction_system.common.dto.UserDTO;
import javafx.animation.PauseTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
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

public class LoginController implements javafx.fxml.Initializable {
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

    @FXML private javafx.scene.layout.StackPane loginRoot;
    @FXML private AnchorPane mainForm;

    @Override
    public void initialize(java.net.URL location, java.util.ResourceBundle resources) {
        javafx.application.Platform.runLater(() -> {
            if (loginRoot != null && loginRoot.getScene() != null) {
                bindScale(loginRoot.getScene());
            }
        });
    }

    private void bindScale(Scene scene) {
        javafx.scene.transform.Scale scale = new javafx.scene.transform.Scale(1, 1, 640, 360);
        mainForm.getTransforms().add(scale);

        Runnable updateScale = () -> {
            double w = scene.getWidth();
            double h = scene.getHeight();
            if (w > 0 && h > 0) {
                double scaleFactor = Math.min(w / 1280.0, h / 720.0);
                scale.setX(scaleFactor);
                scale.setY(scaleFactor);
            }
        };

        scene.widthProperty().addListener((obs, oldVal, newVal) -> updateScale.run());
        scene.heightProperty().addListener((obs, oldVal, newVal) -> updateScale.run());
        updateScale.run();
    }

    @FXML
    void loginBtnAction(ActionEvent event) {
        UserDTO user = new UserDTO(LoginUsernameTextField.getText(), LoginPasswordTextField.getText());
        if (user.getUsername().isEmpty() || user.getPassword().isEmpty()) {
            LoginStatusLabel.setText("Please fill in all information!");
            return;
        }

        // Disable button and show loading status to avoid double-click
        Button loginBtn = (Button) ((javafx.scene.Node) event.getSource());
        loginBtn.setDisable(true);
        LoginStatusLabel.setText("Logging in...");

        // Perform network I/O on background thread to avoid blocking JavaFX UI thread
        new Thread(() -> {
            int varCheck = AuthService.getInstance().checkLogin(user);

            Platform.runLater(() -> {
                loginBtn.setDisable(false);
                if (varCheck == 1) {
                    // START LOADING DATA IMMEDIATELY AFTER SUCCESSFUL LOGIN
                    AuctionListService.getInstance().fetchAllAuctions();
                    try {
                        FXMLLoader loader = new FXMLLoader();
                        loader.setLocation(getClass().getResource("/fxml/mainAuction.fxml"));
                        Parent root = loader.load();
                        Scene scene = new Scene(root);
                        Stage stage = (Stage) side_form.getScene().getWindow();
                        stage.setScene(scene);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else if (varCheck == -1) {
                    // Admin will be loaded in here :>
                    try {
                        FXMLLoader loader = new FXMLLoader();
                        loader.setLocation(getClass().getResource("/fxml/AdminMainAuction.fxml"));
                        Parent root = loader.load();
                        Scene scene = new Scene(root);
                        Stage stage = (Stage) side_form.getScene().getWindow();
                        stage.setScene(scene);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    String errorMsg = AuthService.getInstance().getLastErrorMessage();
                    if (errorMsg != null && !errorMsg.isEmpty()) {
                        LoginStatusLabel.setText(errorMsg);
                    } else {
                        LoginStatusLabel.setText("Incorrect username or password!");
                    }
                }
            });
        }, "login-worker").start();
    }

    @FXML
    void registerBtnAction(ActionEvent event) {
        String fullname = registerFullnameTextField.getText();
        String username = registerUsernameTextField.getText();
        String password = registerPasswordTextField.getText();
        String confirmPassword = registerConfirmPasswordTextField.getText();
        if (fullname.isEmpty() || username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            RegisterStatusLabel.setText("Please fill in all information!");
            return;
        }
        if (!password.equals(confirmPassword)) {
            RegisterStatusLabel.setText("Passwords do not match!");
            return;
        }

        // Disable button and show loading status
        registerBtn.setDisable(true);
        RegisterStatusLabel.setTextFill(Color.BLACK);
        RegisterStatusLabel.setText("Registering...");

        UserDTO user = new UserDTO(fullname, username, password);

        // Perform network I/O on background thread
        new Thread(() -> {
            boolean success = AuthService.getInstance().checkRegister(user);

            Platform.runLater(() -> {
                registerBtn.setDisable(false);
                if (success) {
                    RegisterStatusLabel.setTextFill(Color.GREEN);
                    RegisterStatusLabel.setText("Registration successful, returning to login page in 3s!");
                    PauseTransition delay = new PauseTransition(Duration.seconds(3));
                    delay.setOnFinished(e -> {
                        switchForm(event);
                    });
                    delay.play();
                } else {
                    RegisterStatusLabel.setTextFill(Color.RED);
                    RegisterStatusLabel.setText("Account already exists!");
                }
            });
        }, "register-worker").start();
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