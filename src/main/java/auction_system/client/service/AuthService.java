package auction_system.client.service;

import auction_system.client.model.User;
import auction_system.client.socket.SocketClient;
import auction_system.common.protocol.MessageType;
import auction_system.common.protocol.Request;
import auction_system.common.protocol.Response;
import com.google.gson.Gson;

public class AuthService {
    private static AuthService instance;
    private User user;
    private Gson gson = new Gson();

    private AuthService() {};

    public static AuthService getInstance() {
        if(instance == null) {
            instance = new AuthService();
        }
        return instance;
    }

    public User getUser() {
        return user;
    }

    public boolean checkLogin(User user) {
        Request request = new Request(MessageType.LOGIN, gson.toJson(user));
        SocketClient.getInstance().send(request);
        Response response = SocketClient.getInstance().receive();
        if(response.getStatus().equals("SUCCESS")) {
            return true;
        }
        else {
            return false;
        }

    }

}
