package auction_system.client.session;

import auction_system.common.dto.UserDTO;

public class UserSession {
    //singleton for user session
    private static UserSession instance;
    private UserSession() {}

    public static UserSession getInstance() {
        if(instance == null) instance = new UserSession();
        return instance;
    }
    //core in below
    private UserDTO user;
    public void setUser(UserDTO user) {
        this.user = user;
    }
    public UserDTO getUser() {
        return this.user;
    }

    public void logout() {
        this.user = null;
    }
}
