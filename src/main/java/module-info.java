module com.auction_system {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;
    requires spring.security.crypto;
    requires java.sql;


    opens auction_system to javafx.fxml;
    exports auction_system;

    opens auction_system.client.controllers to javafx.fxml;
    exports auction_system.client.controllers;

}