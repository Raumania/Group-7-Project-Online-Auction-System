package com.auction.network;

import java.io.Serializable;

public class Request implements Serializable {
    private static final long serialVersionUID = 1L;
    private String command;
    private Object data;

    public Request(String command, Object data) {
        this.command = command;
        this.data = data;
    }

    // Getters
    public String getCommand() { return command; }
    public Object getData() { return data; }
}