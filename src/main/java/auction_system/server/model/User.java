package auction_system.server.model;

import auction_system.common.enums.UserRole;
import auction_system.server.exception.serviceException.User.UserInformationException;

import java.util.HashSet;
import java.util.Set;

public class User extends Entity {
    protected String fullname;
    protected String username;
    protected String password;
    protected Set<UserRole> roles;
    protected double balance;

    public User(String fullname, String username, String password, Set<UserRole> roles) {
        super();

        if (fullname == null || fullname.trim().isEmpty()) {
            throw new UserInformationException("Fullname cannot be null or empty");
        }

        if (username == null || username.trim().isEmpty()) {
            throw new UserInformationException("Username cannot be null or empty");
        }

        if (password == null || password.trim().isEmpty()) {
            throw new UserInformationException("Password cannot be null or empty");
        }

        if (roles == null || roles.isEmpty()) {
            throw new UserInformationException("User roles cannot be null or empty");
        }

        this.fullname = fullname;
        this.username = username;
        this.password = password;
        this.roles = new HashSet<>(roles);

        /*
            Không tự sinh id cho User nữa.
            id sẽ do database AUTO_INCREMENT tạo.
            Sau khi save xong, UserDAO sẽ setId lại.
        */
        this.id = 0;

        this.balance = 0;
    }

    public String getFullname() {
        return fullname;
    }

    public void setFullname(String fullname) {
        if (fullname == null || fullname.trim().isEmpty()) {
            throw new UserInformationException("Fullname cannot be null or empty");
        }
        this.fullname = fullname;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public Set<UserRole> getRoles() {
        return new HashSet<>(roles);
    }

    public boolean hasRole(UserRole role) {
        if (role == null) {
            return false;
        }
        return roles.contains(role);
    }

    public void addRole(UserRole role) {
        if (role == null) {
            throw new UserInformationException("User role cannot be null");
        }
        roles.add(role);
    }

    public void removeRole(UserRole role) {
        if (role == null) {
            throw new UserInformationException("User role cannot be null");
        }

        if (!roles.contains(role)) {
            throw new UserInformationException("User does not have this role");
        }

        if (roles.size() == 1) {
            throw new UserInformationException("User must have at least one role");
        }

        roles.remove(role);
    }

    public void setRoles(Set<UserRole> roles) {
        if (roles == null || roles.isEmpty()) {
            throw new UserInformationException("User roles cannot be null or empty");
        }
        this.roles = new HashSet<>(roles);
    }

    public void setUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new UserInformationException("Username cannot be null or empty");
        }
        this.username = username;
    }

    public void setPassword(String password) {
        if (password != null && password.trim().isEmpty()) {
            throw new UserInformationException("Password cannot be empty");
        }
        this.password = password;
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        if (balance < 0) {
            throw new UserInformationException("Balance cannot be negative");
        }
        this.balance = balance;
    }

    public void deposit(double amount) {
        if (amount <= 0) {
            throw new UserInformationException("Deposit amount must be greater than 0");
        }
        this.balance += amount;
    }

    public void withdraw(double amount) {
        if (amount <= 0) {
            throw new UserInformationException("Withdraw amount must be greater than 0");
        }

        if (amount > balance) {
            throw new UserInformationException("Not enough balance");
        }

        this.balance -= amount;
    }



    @Override
    public String toString() {
        return "User{id='" + id +
                "', fullname='" + fullname +
                "', username='" + username +
                "', roles=" + roles +
                ", balance=" + balance +
                "}";
    }
}