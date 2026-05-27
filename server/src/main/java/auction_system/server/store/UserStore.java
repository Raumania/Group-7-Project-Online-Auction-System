package auction_system.server.store;

import auction_system.server.model.User;
import auction_system.server.dao.UserDAO;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.ArrayList;

public class UserStore {
    private static UserStore instance;
    private final ConcurrentHashMap<Integer, User> usersById = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, User> usersByUsername = new ConcurrentHashMap<>();
    private final UserDAO userDAO;

    private UserStore() {
        this.userDAO = UserDAO.getInstance();
    }

    public static synchronized UserStore getInstance() {
        if (instance == null) {
            instance = new UserStore();
        }
        return instance;
    }

    public synchronized void init() {
        usersById.clear();
        usersByUsername.clear();
        try {
            System.out.println("[UserStore] Loading all users from database...");
            List<User> allUsers = userDAO.findAll();
            for (User user : allUsers) {
                usersById.put(user.getId(), user);
                if (user.getUsername() != null) {
                    usersByUsername.put(user.getUsername(), user);
                }
            }
            System.out.println("[UserStore] Loaded " + usersById.size() + " users into memory cache.");
        } catch (Exception e) {
            System.err.println("[UserStore] Failed to load users from DB: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void addUser(User user) {
        if (user != null) {
            usersById.put(user.getId(), user);
            if (user.getUsername() != null) {
                usersByUsername.put(user.getUsername(), user);
            }
        }
    }

    public void updateUser(User user) {
        addUser(user);
    }

    public void removeUser(int id) {
        User removed = usersById.remove(id);
        if (removed != null && removed.getUsername() != null) {
            usersByUsername.remove(removed.getUsername());
        }
    }

    public User getUserById(int id) {
        return usersById.get(id);
    }

    public User getUserByUsername(String username) {
        if (username == null) return null;
        return usersByUsername.get(username);
    }

    public List<User> getAllUsers() {
        return new ArrayList<>(usersById.values());
    }
}