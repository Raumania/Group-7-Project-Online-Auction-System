module com.auction_system {
    requires java.sql;
    requires com.google.gson;
    requires java.desktop;
    requires org.slf4j; // Nếu bạn dùng Swing

    // 1. MỞ KHÓA (opens) cho Gson sử dụng Reflection để parse JSON
    opens auction_system.server.model to com.google.gson;
    opens auction_system.server.util to com.google.gson;
    opens auction_system.common.protocol to com.google.gson;
    opens auction_system.common.dto to com.google.gson;
    opens auction_system.common.enums to com.google.gson;



    // 2. XUẤT (exports) để các package khác hoặc module khác nhìn thấy code
    exports auction_system.server.controller;
    exports auction_system.server.model;
    exports auction_system.server.util;
    exports auction_system.common.enums;
    exports auction_system.common.dto;
    exports auction_system.common.protocol;
    exports auction_system.server.service;
}