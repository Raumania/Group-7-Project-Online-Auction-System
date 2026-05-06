package auction_system.server.model;

import auction_system.util.IdGenerator;

public abstract class User extends Entity {
    protected String username;
    protected String password;
    protected String email;
    protected UserRole role; // thêm role để biết user là BIDDER, SELLER hay ADMIN
    protected double balance;

    public User(String username, String password, String email, UserRole role) {
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

        if (role == null) {
            throw new RuntimeException("User role cannot be null");
        }

        this.username = username;
        this.password = password;
        this.email = email;
        this.role = role;
        this.id = IdGenerator.generationUserId();
        this.balance=0;
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

    public UserRole getRole() {
        return role;
    }
    /*
        Cần các setter này nếu sau này bạn muốn update user trong DAO.
        Ví dụ: đổi email, đổi password.
    */
    public void setUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new RuntimeException("Username cannot be null or empty");
        }
        this.username = username;
    }

    public void setPassword(String password) {
        if (password == null || password.trim().isEmpty()) {
            throw new RuntimeException("Password cannot be null or empty");
        }
        this.password = password;
    }

    public void setEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new RuntimeException("Email cannot be null or empty");
        }
        this.email = email;
    }

    public void setRole(UserRole role) {
        if (role == null) {
            throw new RuntimeException("User role cannot be null");
        }
        this.role = role;
    }
    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        if (balance < 0) {
            throw new RuntimeException("Balance cannot be negative");
        }
        this.balance = balance;
    }

    public void deposit(double amount) {
        if (amount <= 0) {
            throw new RuntimeException("Deposit amount must be greater than 0");
        }
        this.balance += amount;
    }

    public void withdraw(double amount) {
        if (amount <= 0) {
            throw new RuntimeException("Withdraw amount must be greater than 0");
        }

        if (amount > balance) {
            throw new RuntimeException("Not enough balance");
        }

        this.balance -= amount;
    }

    @Override
    public String toString() {
        return "User{id='" + id +
                "', username='" + username +
                "', email='" + email +
                "', role=" + role +
                "}";
    }
}