package auction_system.server.controller;

import auction_system.common.enums.Status;
import auction_system.server.model.User;
import auction_system.common.dto.UserDTO;
import auction_system.common.protocol.Request;
import auction_system.common.protocol.Response;
import auction_system.server.service.UserService;
import auction_system.server.util.GsonUtil;
import com.google.gson.JsonElement;

public class RegisterController implements RequestHandler {

    private final UserService userService = UserService.getInstance();

    @Override
    public Response handle(Request request) {
        try {
            JsonElement data = request.getData();
            UserDTO user = GsonUtil.fromJson(data, UserDTO.class);

            if (user.getFullname() == null || user.getFullname().trim().isEmpty()) {
                throw new RuntimeException("Fullname cannot be null or empty");
            }

            if (user.getUsername() == null || user.getUsername().trim().isEmpty()) {
                throw new RuntimeException("Username cannot be null or empty");
            }

            if (user.getPassword() == null || user.getPassword().trim().isEmpty()) {
                throw new RuntimeException("Password cannot be null or empty");
            }

            User userRespond = userService.registerUser(user.getFullname(), user.getUsername(), user.getPassword());

            UserDTO responseUser = new UserDTO();
            responseUser.setId(userRespond.getId());
            responseUser.setFullname(userRespond.getFullname());
            responseUser.setUsername(userRespond.getUsername());
            responseUser.setAvailableBalance(userRespond.getAvailableBalance());
            responseUser.setFrozenBalance(userRespond.getFrozenBalance());
            responseUser.setRoles(userRespond.getRoles());
            // password left as null for security

            return new Response(Status.SUCCESS, "Register successfully", responseUser);

        } catch (Exception e) {
            return new Response(Status.ERROR, "Register failed: " + e.getMessage(), null);
        }
    }
}