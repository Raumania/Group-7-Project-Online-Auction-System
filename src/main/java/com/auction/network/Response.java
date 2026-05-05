package com.auction.network;

import java.io.Serializable;

public class Response implements Serializable {
    private static final long serialVersionUID = 1L;
    private boolean success;
    private String message;
    private Object data;

    public Response(boolean success, String message, Object data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }

    // Factory methods để tạo response nhanh chóng
    public static Response success(Object data) {
        return new Response(true, "Success", data);
    }

    public static Response error(String message) {
        return new Response(false, message, null);
    }

    // Getters
    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public Object getData() { return data; }
}