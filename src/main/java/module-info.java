module com.auction_system { // Tên module của bạn (kiểm tra lại tên trong file của bạn nhé)
    requires java.sql;
    requires com.google.gson;
    requires java.desktop; // Nếu bạn dùng Swing

    // Cho phép Gson truy cập vào các package chứa dữ liệu (Model, Protocol)
    opens auction_system.server.model to com.google.gson;
    opens auction_system.server.common.protocol to com.google.gson;

    // Mở package util để các lớp khác hoặc thư viện có thể truy cập nếu cần
    opens auction_system.server.util to com.google.gson;

    // Export các package để các module khác có thể dùng
    exports auction_system.server.controller;
    exports auction_system.server.model;
    exports auction_system.server.util;
}