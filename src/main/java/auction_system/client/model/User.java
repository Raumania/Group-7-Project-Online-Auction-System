package auction_system.client.model;

public class User extends Entity {
    private String fullname;
    private String username;
    private String password;

    public User(String fullname, String username, String password) {
        this.fullname = fullname;
        this.username = username;
        this.password = password;
    }
    public User(String username, String password) {
        this.username = username;
        this.password = password;
    }

}
