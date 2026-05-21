module com.auction_system.client {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;
    requires spring.security.crypto;
    requires org.controlsfx.controls;
    requires com.google.gson;
    
    requires com.auction_system.common; 

    opens auction_system.client to javafx.fxml;
    opens auction_system.client.controller to javafx.fxml;

    exports auction_system.client;
    exports auction_system.client.controller;
}
