package com.auction.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class SceneSwitcher {

    private Stage stage;

    public SceneSwitcher(Stage stage) {
        this.stage = stage;
    }

    // Chuyển màn hình đơn giản
    public void switchTo(String fxmlPath, String title) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
        stage.setScene(new Scene(root));
        stage.setTitle(title);
        stage.show();
    }

    // Chuyển màn hình có controller riêng
    public void switchTo(String fxmlPath, String title, Object controller) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
        loader.setController(controller);
        Parent root = loader.load();
        stage.setScene(new Scene(root));
        stage.setTitle(title);
        stage.show();
    }

    // Lấy stage hiện tại
    public Stage getStage() {
        return stage;
    }
}