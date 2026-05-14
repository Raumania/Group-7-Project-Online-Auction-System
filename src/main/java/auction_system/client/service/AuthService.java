package auction_system.client.service;

import auction_system.client.model.User;
import auction_system.client.socket.SocketClient;
import auction_system.common.dto.UserDTO;
import auction_system.common.protocol.MessageType;
import auction_system.common.protocol.Request;
import auction_system.common.protocol.Response;

import java.net.Socket;

public class AuthService {
    //Singleton for AuthService
    private static AuthService instance;
    private AuthService() {};
    public static AuthService getInstance() {
        if(instance == null) {
            instance = new AuthService();
        }
        return instance;
    }
    //Main code in below

    public boolean checkLogin(UserDTO user) {

        Request<UserDTO> request = new Request<>(MessageType.LOGIN, user);
        SocketClient.getInstance().send(request);
        Response response = SocketClient.getInstance().receive();
        if(response.getStatus().equals("SUCCESS")) {
            return true;
        }
        else {
            return false;
        }
    }

    public boolean checkRegister(UserDTO user) {
        Request<UserDTO> request = new Request<>(MessageType.REGISTER,user);
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
