module com.auction_system {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;


    opens auction_system to javafx.fxml;
    exports auction_system;

    opens auction_system.client.controllers to javafx.fxml;
    exports auction_system.client.controllers;

}