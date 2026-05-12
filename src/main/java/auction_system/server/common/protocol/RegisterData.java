package auction_system.server.common.protocol;

public class RegisterData {
    private String fullname;
    private String username;
    private String password;

    public RegisterData() {
    }

    public RegisterData(String fullname, String username, String password) {
        this.fullname = fullname;
        this.username = username;
        this.password = password;
    }

    public String getFullname() {
        return fullname;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
}