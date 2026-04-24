package auction_system.service;

import auction_system.model.User;

import java.util.ArrayList;
import java.util.List;

public class UserManager {

    private static UserManager instance;
    private List<User> users;

    private UserManager() {
        users = new ArrayList<>();
    }

    public static UserManager getInstance() {
        if (instance == null) {
            instance = new UserManager();
        }
        return instance;
    }

    public void addUser(User user) {
        if (user == null) {
            throw new RuntimeException("User cannot be null");
        }

        for (User u : users) {
            if (u.getUsername().equals(user.getUsername())) {
                throw new RuntimeException("Username already exists");
            }

            if (u.getEmail().equals(user.getEmail())) {
                throw new RuntimeException("Email already exists");
            }
        }

        users.add(user);
    }

    public User findUserById(String id) {
        if (id == null || id.trim().isEmpty()) {
            throw new RuntimeException("User id cannot be null or empty");
        }

        for (User user : users) {
            if (user.getId().equals(id)) {
                return user;
            }
        }

        return null;
    }

    public User findUserByUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new RuntimeException("Username cannot be null or empty");
        }

        for (User user : users) {
            if (user.getUsername().equals(username)) {
                return user;
            }
        }

        return null;
    }

    public List<User> getAllUsers() {
        return new ArrayList<>(users);
    }

    public boolean removeUser(String id) {
        User user = findUserById(id);

        if (user != null) {
            users.remove(user);
            return true;
        }

        return false;
    }
}