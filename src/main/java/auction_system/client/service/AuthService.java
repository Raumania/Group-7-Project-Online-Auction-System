package auction_system.client.service;

import auction_system.client.session.UserSession;
import auction_system.client.socket.SocketClient;
import auction_system.common.dto.UserDTO;
import auction_system.common.enums.Action;
import auction_system.common.protocol.Request;
import auction_system.common.protocol.Response;
import com.google.gson.Gson;

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
    Gson gson = new Gson();
    //Main code in below

    public boolean checkLogin(UserDTO user) {

        Request<UserDTO> request = new Request<>(Action.LOGIN, user);
        SocketClient.getInstance().send(request);
        Response response = SocketClient.getInstance().receive();
        if(response.getStatus().equals("SUCCESS")) {
            //fill full data for user session hehe :>
            UserSession.getInstance().setUser(gson.fromJson(response.getData(),UserDTO.class));
            return true;
        }
        else {
            return false;
        }
    }

    public boolean checkRegister(UserDTO user) {
        Request<UserDTO> request = new Request<>(Action.REGISTER,user);
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
