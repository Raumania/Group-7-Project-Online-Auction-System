package auction_system.client.service;

import auction_system.client.util.GsonUtil;
import auction_system.client.socket.SocketClient;
import auction_system.common.dto.UserDTO;
import auction_system.common.enums.Action;
import auction_system.common.enums.Status;
import auction_system.common.protocol.Request;
import auction_system.common.protocol.Response;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class AdminUserService {
    private static AdminUserService instance;

    private AdminUserService() {
    }

    public static AdminUserService getInstance() {
        if (instance == null) {
            instance = new AdminUserService();
        }
        return instance;
    }

    public List<UserDTO> getAllUsers() {
        Request request = new Request(Action.GET_ALL_USERS, null);
        SocketClient.getInstance().send(request);
        Response response = SocketClient.getInstance().receive();

        if (response != null && response.getStatus() == Status.SUCCESS) {
            String jsonData = GsonUtil.toJson(response.getData());
            Type listType = new TypeToken<List<UserDTO>>(){}.getType();
            return GsonUtil.fromJson(jsonData, listType);
        } else {
            return new ArrayList<>();
        }
    }

    public boolean createUser(UserDTO userDTO) {
        Request request = new Request(Action.CREATE_USER, GsonUtil.getGson().toJsonTree(userDTO));
        SocketClient.getInstance().send(request);
        Response response = SocketClient.getInstance().receive();
        
        if (response != null && response.getStatus() == Status.SUCCESS) {
            // Update local DTO with DB-generated fields (like ID)
            UserDTO createdUser = GsonUtil.getGson().fromJson(
                GsonUtil.getGson().toJsonTree(response.getData()), 
                UserDTO.class
            );
            userDTO.setId(createdUser.getId());
            return true;
        }
        return false;
    }

    public boolean updateUser(UserDTO userDTO) {
        Request request = new Request(Action.UPDATE_USER, GsonUtil.getGson().toJsonTree(userDTO));
        SocketClient.getInstance().send(request);
        Response response = SocketClient.getInstance().receive();
        return response != null && response.getStatus() == Status.SUCCESS;
    }

    public boolean deleteUser(int userId) {
        Request request = new Request(Action.DELETE_USER, GsonUtil.getGson().toJsonTree(userId));
        SocketClient.getInstance().send(request);
        Response response = SocketClient.getInstance().receive();
        return response != null && response.getStatus() == Status.SUCCESS;
    }

    public boolean banUser(int userId) {
        Request request = new Request(Action.BAN_USER, GsonUtil.getGson().toJsonTree(userId));
        SocketClient.getInstance().send(request);
        Response response = SocketClient.getInstance().receive();
        return response != null && response.getStatus() == Status.SUCCESS;
    }
}
