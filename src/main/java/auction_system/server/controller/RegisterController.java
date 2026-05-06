package auction_system.server.controller;

import auction_system.server.model.Bidder;
import auction_system.server.model.Seller;
import auction_system.server.model.User;
import auction_system.server.common.protocol.Action;
import auction_system.server.common.protocol.RegisterData;
import auction_system.server.common.protocol.Request;
import auction_system.server.common.protocol.Response;
import auction_system.server.service.UserService;
import com.google.gson.Gson;

public class RegisterController implements RequestHandler {

    private final Gson gson = new Gson();
    private final UserService userService = new UserService();

    @Override
    public Response handle(Request request) {
        if (!Action.REGISTER.equals(request.getAction())) {
            return new Response("ERROR", null, "Invalid action for RegisterController");
        }

        try {
            RegisterData data = gson.fromJson(request.getData(), RegisterData.class);

            if (data.getRole() == null || data.getRole().trim().isEmpty()) {
                throw new RuntimeException("Role cannot be empty");
            }

            User user;

            if (data.getRole().equalsIgnoreCase("BIDDER")) {
                Bidder bidder = userService.createBidder(
                        data.getUsername(),
                        data.getPassword(),
                        data.getEmail()
                );
                user = bidder;
            } else if (data.getRole().equalsIgnoreCase("SELLER")) {
                Seller seller = userService.createSeller(
                        data.getUsername(),
                        data.getPassword(),
                        data.getEmail()
                );
                user = seller;
            } else {
                throw new RuntimeException("Invalid role. Role must be BIDDER or SELLER");
            }

            return new Response(
                    "SUCCESS",
                    gson.toJson(user),
                    "Register successfully"
            );

        } catch (Exception e) {
            return new Response(
                    "ERROR",
                    null,
                    "Register failed: " + e.getMessage()
            );
        }
    }
}