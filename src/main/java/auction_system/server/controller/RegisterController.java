package auction_system.server.controller;

import auction_system.server.model.User;
import auction_system.server.common.protocol.Action;
import auction_system.server.common.protocol.RegisterData;
import auction_system.server.common.protocol.Request;
import auction_system.server.common.protocol.Response;
import auction_system.server.service.UserService;
import auction_system.server.util.GsonUtil;
import com.google.gson.JsonElement;

public class RegisterController implements RequestHandler {

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
                data = GsonUtil.fromJson((JsonElement) dataObj, RegisterData.class);
            } else {
                String json = GsonUtil.toJson(dataObj);
                data = GsonUtil.fromJson(json, RegisterData.class);
            }

            /*
                Bây giờ RegisterData chỉ còn:
                - fullname
                - username
                - password

                Không còn email.
                Không còn roles từ client.

                Role mặc định BIDDER,SELLER sẽ được gán trong UserService.
            */
            if (data.getFullname() == null || data.getFullname().trim().isEmpty()) {
                throw new RuntimeException("Fullname cannot be empty");
            }

            if (data.getUsername() == null || data.getUsername().trim().isEmpty()) {
                throw new RuntimeException("Username cannot be empty");
            }

            if (data.getPassword() == null || data.getPassword().trim().isEmpty()) {
                throw new RuntimeException("Password cannot be empty");
            }

            /*
                Không còn createBidder / createSeller theo nhánh if nữa.
                Không còn truyền roles từ client nữa.

                User mới mặc định có cả:
                - BIDDER
                - SELLER
            */
            User user = userService.registerUser(
                    data.getFullname(),
                    data.getUsername(),
                    data.getPassword()
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