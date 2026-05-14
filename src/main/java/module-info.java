module com.auction_system {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;
    requires spring.security.crypto;
    requires java.sql;
    requires jdk.jdi;
    requires com.google.gson;
    requires javafx.graphics;

    opens auction_system.client.controller to javafx.fxml;
    opens auction_system.client to javafx.fxml;

    opens auction_system.common.protocol to com.google.gson;
    opens auction_system.client.model to com.google.gson;
    opens auction_system.common.dto to com.google.gson;

    exports auction_system.client.controller;
    exports auction_system.client;
}