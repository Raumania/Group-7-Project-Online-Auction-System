package auction_system.client.service;

import auction_system.client.session.UserSession;
import auction_system.client.socket.SocketClient;
import auction_system.common.dto.UserDTO;
import auction_system.common.enums.Action;
import auction_system.common.enums.Status;
import auction_system.common.enums.UserRole;
import auction_system.common.protocol.Request;
import auction_system.common.protocol.Response;
import auction_system.client.util.GsonUtil;
import com.google.gson.Gson;

public class AuthService {
    //Singleton for AuthService
    private static AuthService instance;
    private AuthService() {}

    public static AuthService getInstance() {
        if(instance == null) {
            instance = new AuthService();
        }
        return instance;
    }
    Gson gson = new Gson();
    //Main code in below

    private String lastErrorMessage;

    public String getLastErrorMessage() {
        return lastErrorMessage;
    }

    public int checkLogin(UserDTO user) {
        lastErrorMessage = null;
        Request request = new Request(Action.LOGIN, GsonUtil.getGson().toJsonTree(user));
        SocketClient.getInstance().send(request);
        Response response = SocketClient.getInstance().receive();
        if(response != null && response.getStatus() == Status.SUCCESS) {
            //fill full data for user session hehe :>
            UserSession.getInstance().setUser(gson.fromJson(GsonUtil.getGson().toJsonTree(response.getData()),UserDTO.class));
            if(UserSession.getInstance().getUser().getRoles().contains(UserRole.ADMIN)) {
                return -1;
            }
            else return 1;
        }
        else {
            if (response != null) {
                lastErrorMessage = response.getMessage();
            }
            return 0;
        }
    }

    public boolean checkRegister(UserDTO user) {
        Request request = new Request(Action.REGISTER, GsonUtil.getGson().toJsonTree(user));
        SocketClient.getInstance().send(request);
        Response response = SocketClient.getInstance().receive();
        return response.getStatus() == Status.SUCCESS;
    }

}
