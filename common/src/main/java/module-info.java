module com.auction_system.common {
    requires com.google.gson;
    requires javafx.base;

    exports auction_system.common.dto;
    exports auction_system.common.enums;
    exports auction_system.common.protocol;

    opens auction_system.common.dto to com.google.gson, javafx.base;
    opens auction_system.common.protocol to com.google.gson;
    opens auction_system.common.enums to com.google.gson;
}
