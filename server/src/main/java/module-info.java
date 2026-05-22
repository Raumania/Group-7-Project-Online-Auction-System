module com.auction_system.server {
    requires java.sql;
    requires spring.security.crypto;
    requires com.google.gson;
    requires org.slf4j;
    requires jdk.unsupported;
    
    requires com.auction_system.common;
    requires langchain4j.core;
    requires langchain4j.open.ai;

    opens auction_system.server.model to com.google.gson;
    opens auction_system.server to com.google.gson;

    exports auction_system.server;
    exports auction_system.server.model;
    exports auction_system.server.controller;
    exports auction_system.server.service;
    exports auction_system.server.dao;
    exports auction_system.server.observer;
    exports auction_system.server.util;
    exports auction_system.server.exception;
    exports auction_system.server.factory;
}
