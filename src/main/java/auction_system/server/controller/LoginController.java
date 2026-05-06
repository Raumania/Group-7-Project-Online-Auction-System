package auction_system.server.controller;

import auction_system.server.common.protocol.*;
import auction_system.server.model.User;
import auction_system.server.service.UserService;
import com.google.gson.Gson;

/**
 * Xử lý action LOGIN.
 * Sử dụng UserService.login() để xác thực.
 */
public class LoginController implements RequestHandler {

    private final Gson gson = new Gson();
    private final UserService userService = new UserService();

    @Override
    public Response handle(Request request) {
        if (!Action.LOGIN.equals(request.getAction())) {
            return new Response("ERROR", null, "Invalid action for LoginController");
        }
        try {
            LoginData loginData = gson.fromJson(request.getData(), LoginData.class);
            User user = userService.login(loginData.getUsername(), loginData.getPassword());
            System.out.println(loginData.getUsername());
            if(user!=null){
                System.out.println("Ket noi duoc roi");
            }
            // Xoá mật khẩu trước khi gửi về client (bảo mậ
            return new Response("SUCCESS", gson.toJson(user), "Login successful");
        } catch (Exception e) {
            return new Response("ERROR", null, "Login failed: " + e.getMessage());
        }
    }
}