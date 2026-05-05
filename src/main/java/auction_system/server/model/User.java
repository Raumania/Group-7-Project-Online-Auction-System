package auction_system.server.model;

import auction_system.server.util.IdGenerator;

public abstract class User extends Entity {
    protected String username;
    protected String password;
    protected String email;

    public User(String username, String password, String email) {
        super();

        if (username == null || username.trim().isEmpty()) {
            throw new RuntimeException("Username cannot be null or empty");
        }

        if (password == null || password.trim().isEmpty()) {
            throw new RuntimeException("Password cannot be null or empty");
        }

        if (email == null || email.trim().isEmpty()) {
            throw new RuntimeException("Email cannot be null or empty");
        }

        this.username = username;
        this.password = password;
        this.email = email;
        this.id = IdGenerator.generationUserId();
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

    @Override
    public String toString() {
        return "User{id='" + id + "', username='" + username + "', email='" + email + "'}";
    }
}