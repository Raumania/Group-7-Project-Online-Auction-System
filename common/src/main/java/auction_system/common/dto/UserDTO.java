package auction_system.common.dto;

import auction_system.common.enums.UserRole;
import auction_system.common.enums.UserStatus;

import java.math.BigDecimal;
import java.util.Set;

public class UserDTO {
    private int id;
    private String fullname;
    private String username;
    private String password;
    private BigDecimal availableBalance;
    private BigDecimal frozenBalance;
    private Set<UserRole> roles;
    private UserStatus status;

    public UserDTO() {
        this.status = UserStatus.ACTIVE;
    }

    public UserDTO(String fullname, String username, String password) {
        this.fullname = fullname;
        this.username = username;
        this.password = password;
        this.status = UserStatus.ACTIVE;
    }

    public UserDTO(String username, String password) {
        this.username = username;
        this.password = password;
        this.status = UserStatus.ACTIVE;
    }
    
    public UserDTO(int id, String fullname, String username, Set<UserRole> roles, BigDecimal availableBalance, BigDecimal frozenBalance) {
        this(id, fullname, username, roles, availableBalance, frozenBalance, UserStatus.ACTIVE);
    }

    public UserDTO(int id, String fullname, String username, Set<UserRole> roles, BigDecimal availableBalance, BigDecimal frozenBalance, UserStatus status) {
        this.id = id;
        this.fullname = fullname;
        this.username = username;
        this.roles = roles;
        this.availableBalance = availableBalance;
        this.frozenBalance = frozenBalance;
        this.status = status;
    }

    public int getId() {
        return id;
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

    public BigDecimal getAvailableBalance() {
        return availableBalance;
    }

    public BigDecimal getFrozenBalance() {
        return frozenBalance;
    }

    public Set<UserRole> getRoles() {
        return roles;
    }

    public UserStatus getStatus() {
        return status;
    }

    public boolean isAdmin() {
        return roles != null && roles.contains(UserRole.ADMIN);
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setFullname(String fullname) {
        this.fullname = fullname;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setAvailableBalance(BigDecimal availableBalance) {
        this.availableBalance = availableBalance;
    }

    public void setFrozenBalance(BigDecimal frozenBalance) {
        this.frozenBalance = frozenBalance;
    }

    public void setRoles(Set<UserRole> roles) {
        this.roles = roles;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }
}