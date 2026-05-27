package auction_system.server.controller;

import auction_system.common.dto.UserDTO;
import auction_system.common.enums.Action;
import auction_system.common.enums.Status;
import auction_system.common.protocol.Request;
import auction_system.common.protocol.Response;
import auction_system.server.model.User;
import auction_system.server.service.UserService;
import auction_system.server.util.GsonUtil;

public class LoginController implements RequestHandler {

    private final UserService userService = UserService.getInstance();

    @Override
    public Response handle(Request request) {
        Action action = request.getAction();
        try {

            UserDTO loginData = GsonUtil.fromJson(request.getData(),UserDTO.class);

            System.out.println("DEBUG: loginData = " + loginData);
            System.out.println("DEBUG: username = " + loginData.getUsername());
            System.out.println("DEBUG: password = " + loginData.getPassword());

            if (loginData.getPassword() == null || loginData.getPassword().trim().isEmpty()) {
                System.err.println("ERROR: Password is null or empty after parse");
                return new Response(Status.ERROR, action, null, "Password cannot be null or empty");
            }

            User user = userService.login(loginData.getUsername(), loginData.getPassword());

            if (user != null) {
                UserDTO responseUser = new UserDTO();
                responseUser.setId(user.getId());
                responseUser.setFullname(user.getFullname());
                responseUser.setUsername(user.getUsername());
                responseUser.setBalance(user.getBalance());
                responseUser.setRoles(user.getRoles());
                // password left as null for security
                return new Response(Status.SUCCESS, action, responseUser, "Login successful");
            } else {
                return new Response(Status.ERROR, action, null, "Invalid username or password");
            }

        } catch (Exception e) {
            e.printStackTrace();
            return new Response(Status.ERROR, action, null, "Login failed: " + e.getMessage());
        }
    }

}
