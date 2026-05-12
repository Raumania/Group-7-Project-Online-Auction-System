package auction_system.server.model;

import auction_system.util.IdGenerator;

import java.util.HashSet;
import java.util.Set;

public class User extends Entity {
    protected String username;
    protected String password;
    protected String email;

    // Một user có thể có nhiều role: BIDDER, SELLER, ADMIN
    protected Set<UserRole> roles;

    protected double balance;

    public User(String username, String password, String email, Set<UserRole> roles) {
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

        if (roles == null || roles.isEmpty()) {
            throw new RuntimeException("User roles cannot be null or empty");
        }

        this.username = username;
        this.password = password;
        this.email = email;

        // copy sang HashSet để tránh bị sửa từ bên ngoài
        this.roles = new HashSet<>(roles);

        this.id = IdGenerator.generationUserId();
        this.balance = 0;
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

    // Thay getRole() bằng getRoles()
    public Set<UserRole> getRoles() {
        return new HashSet<>(roles);
    }

    // Kiểm tra user có role nào đó không
    public boolean hasRole(UserRole role) {
        if (role == null) {
            return false;
        }
        return roles.contains(role);
    }

    // Thêm role mới cho user
    public void addRole(UserRole role) {
        if (role == null) {
            throw new RuntimeException("User role cannot be null");
        }
        roles.add(role);
    }

    // Xóa role khỏi user
    public void removeRole(UserRole role) {
        if (role == null) {
            throw new RuntimeException("User role cannot be null");
        }

        if (!roles.contains(role)) {
            throw new RuntimeException("User does not have this role");
        }

        if (roles.size() == 1) {
            throw new RuntimeException("User must have at least one role");
        }

        roles.remove(role);
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

    // Thay setRole bằng setRoles
    public void setRoles(Set<UserRole> roles) {
        if (roles == null || roles.isEmpty()) {
            throw new RuntimeException("User roles cannot be null or empty");
        }

        this.roles = new HashSet<>(roles);
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
                "', roles=" + roles +
                ", balance=" + balance +
                "}";
    }
}