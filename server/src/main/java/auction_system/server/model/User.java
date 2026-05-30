package auction_system.server.model;

import auction_system.common.enums.UserRole;
import auction_system.common.enums.UserStatus;
import auction_system.common.dto.UserDTO;
import auction_system.server.exception.UserInformationException;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

public class User extends Entity {
    protected String fullname;
    protected String username;
    protected String password;
    protected Set<UserRole> roles;
    protected BigDecimal availableBalance;
    protected BigDecimal frozenBalance;
    protected UserStatus status;

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
            Do not generate id for User anymore.
            id will be created by database AUTO_INCREMENT.
            After saving, UserDAO will call setId back.
        */
        this.id = 0;

        this.availableBalance = BigDecimal.ZERO;
        this.frozenBalance = BigDecimal.ZERO;
        this.status = UserStatus.ACTIVE;
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

    public BigDecimal getAvailableBalance() {
        return availableBalance;
    }

    public void setAvailableBalance(BigDecimal availableBalance) {
        if (availableBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new UserInformationException("Balance cannot be negative");
        }
        this.availableBalance = availableBalance.setScale(4, java.math.RoundingMode.HALF_UP);
    }

    public BigDecimal getFrozenBalance() {
        return frozenBalance;
    }

    public void setFrozenBalance(BigDecimal frozenBalance) {
        if (frozenBalance.compareTo(BigDecimal.ZERO) < 0) {
            // Automatically fix corrupted negative frozen balance data from previous bug
            frozenBalance = BigDecimal.ZERO;
        }
        this.frozenBalance = frozenBalance.setScale(4, java.math.RoundingMode.HALF_UP);
    }

    public void deposit(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new UserInformationException("Deposit amount must be greater than 0");
        }
        this.availableBalance = this.availableBalance.add(amount).setScale(4, java.math.RoundingMode.HALF_UP);
    }

    public void withdraw(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new UserInformationException("Withdraw amount must be greater than 0");
        }

        if (amount.compareTo(availableBalance) > 0) {
            throw new UserInformationException("Not enough balance");
        }

        this.availableBalance = this.availableBalance.subtract(amount).setScale(4, java.math.RoundingMode.HALF_UP);
    }

    public synchronized void freezeBalance(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new UserInformationException("Amount must be greater than 0");
        }
        if (amount.compareTo(availableBalance) > 0) {
            throw new UserInformationException("Not enough available balance to freeze");
        }
        this.availableBalance = this.availableBalance.subtract(amount).setScale(4, java.math.RoundingMode.HALF_UP);
        this.frozenBalance = this.frozenBalance.add(amount).setScale(4, java.math.RoundingMode.HALF_UP);
    }

    public void unfreezeBalance(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new UserInformationException("Amount must be greater than 0");
        }
        
        // Hoàn lại tiền vào availableBalance để user không bị mất tiền (do bug cũ làm sai lệch DB)
        this.availableBalance = this.availableBalance.add(amount).setScale(4, java.math.RoundingMode.HALF_UP);
        
        // Chỉ trừ frozenBalance tối đa bằng số nó đang có để tránh bị âm
        if (this.frozenBalance.compareTo(amount) < 0) {
            this.frozenBalance = BigDecimal.ZERO;
        } else {
            this.frozenBalance = this.frozenBalance.subtract(amount).setScale(4, java.math.RoundingMode.HALF_UP);
        }
    }



    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        if (status == null) {
            throw new UserInformationException("Status cannot be null");
        }
        this.status = status;
    }

    public UserDTO toDTO() {
        return new UserDTO(this.id, this.fullname, this.username, this.getRoles(), this.availableBalance, this.frozenBalance, this.status);
    }

    @Override
    public String toString() {
        return "User{id='" + id +
                "', fullname='" + fullname +
                "', username='" + username +
                "', roles=" + roles +
                ", availableBalance=" + availableBalance +
                ", frozenBalance=" + frozenBalance +
                ", status=" + status +
                "}";
    }
}