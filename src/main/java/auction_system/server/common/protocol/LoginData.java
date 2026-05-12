package auction_system.server.common.protocol;

public class LoginData {
    private String username;
    private String password;

    public LoginData() {
        // Constructor rỗng cần cho Gson
    }

    public LoginData(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}