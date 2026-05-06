package auction_system.server.common.protocol;

public class RegisterData {
    private String username;
    private String password;
    private String email;
    private String role; // BIDDER hoặc SELLER

    public RegisterData() {
    }

    public RegisterData(String username, String password, String email, String role) {
        this.username = username;
        this.password = password;
        this.email = email;
        this.role = role;
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

    public String getRole() {
        return role;
    }
}