package auction_system.common.dto;

import auction_system.common.enums.UserRole;

import java.util.Set;

public class UserDTO {
    private int id;
    private String fullname;
    private String username;
    private String password;
    private double balance;
    private Set<UserRole> roles;

    public UserDTO() {

    }

    public UserDTO(String fullname, String username, String password) {
        this.fullname = fullname;
        this.username = username;
        this.password = password;
    }

    public UserDTO(String username, String password) {
        this.username = username;
        this.password = password;
    }
    
    public UserDTO(int id, String fullname, String username, Set<UserRole> roles, double balance) {
        this.id = id;
        this.fullname = fullname;
        this.username = username;
        this.roles = roles;
        this.balance = balance;
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

    public double getBalance() {
        return balance;
    }

    public Set<UserRole> getRoles() {
        return roles;
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

    public void setBalance(double balance) {
        this.balance = balance;
    }

    public void setRoles(Set<UserRole> roles) {
        this.roles = roles;
    }
}