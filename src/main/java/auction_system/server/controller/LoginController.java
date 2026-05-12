package auction_system.server.controller;

import auction_system.server.common.protocol.*;
import auction_system.server.model.User;
import auction_system.server.service.UserService;
import com.google.gson.Gson;
import com.google.gson.JsonElement;

public class LoginController implements RequestHandler {

    private final Gson gson = new Gson();
    private final UserService userService = new UserService();

    @Override
    public Response handle(Request request) {
        if (!Action.LOGIN.equals(request.getAction())) {
            return new Response("ERROR", "LOGIN", null, "Invalid action");
        }
        try {
            Object rawData = request.getData();
            System.out.println("DEBUG: rawData class = " + (rawData == null ? "null" : rawData.getClass().getName()));
            System.out.println("DEBUG: rawData toString = " + (rawData == null ? "null" : rawData.toString()));
            if (rawData != null && rawData instanceof String) {
                System.out.println("DEBUG: rawData is String, content = " + rawData);
            }

            LoginData loginData = parseData(rawData, LoginData.class);
            System.out.println("DEBUG: loginData = " + loginData);
            System.out.println("DEBUG: username = " + loginData.getUsername());
            System.out.println("DEBUG: password = " + loginData.getPassword());

            if (loginData.getPassword() == null || loginData.getPassword().trim().isEmpty()) {
                System.err.println("ERROR: Password is null or empty after parse");
                return new Response("ERROR", "LOGIN", null, "Password cannot be null or empty");
            }

            User user = userService.login(loginData.getUsername(), loginData.getPassword());
            if (user != null) {
                return new Response("SUCCESS", "LOGIN", user, "Login successful");
            } else {
                return new Response("ERROR", "LOGIN", null, "Invalid username or password");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return new Response("ERROR", "LOGIN", null, "Login failed: " + e.getMessage());
        }
    }

    private <T> T parseData(Object dataObj, Class<T> clazz) {
        if (dataObj == null) {
            throw new RuntimeException("Data object is null");
        }
        if (dataObj instanceof JsonElement) {
            return gson.fromJson((JsonElement) dataObj, clazz);
        } else if (dataObj instanceof String) {
            // Chuỗi JSON: parse trực tiếp
            return gson.fromJson((String) dataObj, clazz);
        } else {
            // Có thể là LinkedTreeMap hoặc Map khác
            String json = gson.toJson(dataObj);
            return gson.fromJson(json, clazz);
        }
    }
}