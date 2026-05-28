package auction_system.server.controller;

import auction_system.common.dto.UserDTO;
import auction_system.common.enums.Action;
import auction_system.common.enums.Status;
import auction_system.common.protocol.Request;
import auction_system.common.protocol.Response;
import auction_system.server.model.User;
import auction_system.server.service.UserService;
import auction_system.server.util.GsonUtil;
import com.google.gson.JsonElement;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class UserController implements RequestHandler {

    private final UserService userService = UserService.getInstance();

    @Override
    public Response handle(Request request) {
        Action action = request.getAction();
        try {
            switch (action) {
                case GET_ALL_USERS:
                    return getAllUsers(action);
                case CREATE_USER:
                    return createUser(action, request.getData());
                case UPDATE_USER:
                    return updateUser(action, request.getData());
                case DELETE_USER:
                    return deleteUser(action, request.getData());
                case BAN_USER:
                    return banUser(action, request.getData());
                default:
                    return new Response(Status.ERROR, action, null, "Unknown action: " + action);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return new Response(Status.ERROR, action, null, e.getMessage());
        }
    }

    private Response getAllUsers(Action action) {
        List<User> users = userService.getAllUsers();
        List<UserDTO> userDTOs = new ArrayList<>();
        for (User u : users) {
            userDTOs.add(u.toDTO());
        }
        return new Response(Status.SUCCESS, action, userDTOs, "List of all users returned");
    }

    private Response createUser(Action action, JsonElement data) {
        try {
            UserDTO userDTO = GsonUtil.fromJson(data, UserDTO.class);
            User user = userService.registerUser(
                userDTO.getFullname(),
                userDTO.getUsername(),
                userDTO.getPassword() != null && !userDTO.getPassword().isEmpty() ? userDTO.getPassword() : "123456",
                userDTO.getRoles()
            );
            // Set balance if provided
            if (userDTO.getAvailableBalance() != null && userDTO.getAvailableBalance().compareTo(java.math.BigDecimal.ZERO) > 0) {
                // Deposit the initial balance safely to DB and memory store
                userService.deposit(user.getId(), userDTO.getAvailableBalance());
            }
            UserDTO responseDTO = user.toDTO();
            return new Response(Status.SUCCESS, action, responseDTO, "User created successfully");
        } catch (Exception e) {
            e.printStackTrace();
            return new Response(Status.ERROR, action, null, "Create user failed: " + e.getMessage());
        }
    }

    private Response updateUser(Action action, JsonElement data) {
        try {
            UserDTO userDTO = GsonUtil.fromJson(data, UserDTO.class);
            User user = userService.getUserById(userDTO.getId());
            user.setFullname(userDTO.getFullname());
            user.setUsername(userDTO.getUsername());
            user.setRoles(userDTO.getRoles());
            user.setAvailableBalance(userDTO.getAvailableBalance());
            
            if (userDTO.getPassword() != null && !userDTO.getPassword().trim().isEmpty()) {
                String hashpassword = auction_system.server.util.HashUtil.hashPassword(userDTO.getPassword());
                user.setPassword(hashpassword);
            }
            
            // Save to DB
            auction_system.server.dao.UserDAO.getInstance().update(user);
            
            UserDTO responseDTO = user.toDTO();
            return new Response(Status.SUCCESS, action, responseDTO, "User updated successfully");
        } catch (Exception e) {
            e.printStackTrace();
            return new Response(Status.ERROR, action, null, "Update user failed: " + e.getMessage());
        }
    }

    private Response deleteUser(Action action, JsonElement data) {
        try {
            int userId = GsonUtil.fromJson(data, Integer.class);
            userService.removeUser(userId);
            return new Response(Status.SUCCESS, action, null, "User deleted successfully");
        } catch (Exception e) {
            e.printStackTrace();
            return new Response(Status.ERROR, action, null, "Delete user failed: " + e.getMessage());
        }
    }

    private Response banUser(Action action, JsonElement data) {
        try {
            int userId = GsonUtil.fromJson(data, Integer.class);
            userService.banUser(userId);
            return new Response(Status.SUCCESS, action, null, "User banned successfully");
        } catch (Exception e) {
            e.printStackTrace();
            return new Response(Status.ERROR, action, null, "Ban user failed: " + e.getMessage());
        }
    }
}
