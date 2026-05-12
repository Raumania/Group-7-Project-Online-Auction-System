package auction_system.server.common.protocol;

import java.util.List;

public class RegisterData {
    private String username;
    private String password;
    private String email;

    // Nhiều role: BIDDER, SELLER, ADMIN
    private List<String> roles;

    public RegisterData() {
    }

    public RegisterData(String username, String password, String email, List<String> roles) {
        this.username = username;
        this.password = password;
        this.email = email;
        this.roles = roles;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getEmail() {
        return email;
    }

    public List<String> getRoles() {
        return roles;
    }
}