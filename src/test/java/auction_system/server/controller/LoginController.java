package auction_system.server.controller;

import auction_system.common.dto.UserDTO;
import auction_system.common.enums.Status;
import auction_system.common.protocol.Request;
import auction_system.common.protocol.Response;
import auction_system.server.model.User;
import auction_system.server.service.UserService;
import auction_system.server.util.GsonUtil;
import com.google.gson.Gson;
import com.google.gson.JsonElement;

public class LoginController implements RequestHandler {

    private final UserService userService = new UserService();

    @Override
    public Response handle(Request request) {
        try {

            UserDTO loginData = GsonUtil.fromJson(request.getData(),UserDTO.class);

            System.out.println("DEBUG: loginData = " + loginData);
            System.out.println("DEBUG: username = " + loginData.getUsername());
            System.out.println("DEBUG: password = " + loginData.getPassword());

            if (loginData.getPassword() == null || loginData.getPassword().trim().isEmpty()) {
                System.err.println("ERROR: Password is null or empty after parse");
                return new Response(Status.ERROR, "Password cannot be null or empty", null);
            }

            User user = userService.login(loginData.getUsername(), loginData.getPassword());

            if (user != null) {
                user.setPassword(null);
                return new Response(Status.SUCCESS, "Login successful", user);
            } else {
                return new Response(Status.ERROR, "Invalid username or password", null);
            }

        } catch (Exception e) {
            e.printStackTrace();
            return new Response(Status.ERROR, "Login failed: " + e.getMessage(), null);
        }
    }

}