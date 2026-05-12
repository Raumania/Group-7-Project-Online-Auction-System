package auction_system.server.controller;

import auction_system.server.common.protocol.Action;
import auction_system.server.common.protocol.RegisterData;
import auction_system.server.common.protocol.Request;
import auction_system.server.common.protocol.Response;
import auction_system.server.model.User;
import auction_system.server.model.UserRole;
import auction_system.server.service.UserService;
import com.google.gson.Gson;
import com.google.gson.JsonElement;

import java.util.HashSet;
import java.util.Set;

public class RegisterController implements RequestHandler {

    private final Gson gson = new Gson();
    private final UserService userService = new UserService();

    @Override
    public Response handle(Request request) {
        if (!Action.REGISTER.equals(request.getAction())) {
            return new Response("ERROR", "REGISTER", null, "Invalid action for RegisterController");
        }

        try {
            /*
                Xử lý data có thể là JsonElement hoặc Object.
            */
            RegisterData data;
            Object dataObj = request.getData();

            if (dataObj instanceof JsonElement) {
                data = gson.fromJson((JsonElement) dataObj, RegisterData.class);
            } else {
                String json = gson.toJson(dataObj);
                data = gson.fromJson(json, RegisterData.class);
            }

            /*
                Bây giờ không còn data.getRole() nữa.
                Vì 1 user có thể có nhiều role,
                nên dùng data.getRoles().
            */
            if (data.getRoles() == null || data.getRoles().isEmpty()) {
                throw new RuntimeException("Roles cannot be empty");
            }

            /*
                Chuyển Set<String> roles từ client
                thành Set<UserRole> để dùng trong model.
            */
            Set<UserRole> roles = new HashSet<>();

            for (String roleText : data.getRoles()) {
                if (roleText == null || roleText.trim().isEmpty()) {
                    throw new RuntimeException("Role cannot be empty");
                }

                try {
                    UserRole role = UserRole.valueOf(roleText.trim().toUpperCase());
                    roles.add(role);
                } catch (IllegalArgumentException e) {
                    throw new RuntimeException("Invalid role: " + roleText);
                }
            }

            /*
                Không còn createBidder / createSeller theo nhánh if nữa.
                Bây giờ gọi registerUser và truyền nhiều role vào.
            */
            User user = userService.registerUser(
                    data.getUsername(),
                    data.getPassword(),
                    data.getEmail(),
                    roles
            );

            /*
                Không gửi password về client.
            */
            user.setPassword(null);

            return new Response(
                    "SUCCESS",
                    "REGISTER",
                    user,
                    "Register successfully"
            );

        } catch (Exception e) {
            return new Response(
                    "ERROR",
                    "REGISTER",
                    null,
                    "Register failed: " + e.getMessage()
            );
        }
    }
}