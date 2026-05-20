module com.auction_system {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;
    requires spring.security.crypto;
    requires java.sql;
    requires jdk.jdi;
    requires com.google.gson;
    requires javafx.graphics;
    requires org.controlsfx.controls;

    opens auction_system.client.controller to javafx.fxml;
    opens auction_system.client to javafx.fxml;
    opens auction_system.common.enums to com.google.gson;
    opens auction_system.common.protocol to com.google.gson;
    opens auction_system.common.dto to com.google.gson, javafx.base;

    exports auction_system.client.controller;
    exports auction_system.client;
}