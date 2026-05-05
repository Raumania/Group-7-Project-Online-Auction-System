package auction_system.server.service;

import auction_system.server.model.Bidder;
import auction_system.server.model.Seller;
import auction_system.server.model.User;

import java.util.List;

public class UserService {

    private UserManager userManager;

    public UserService() {
        this.userManager = UserManager.getInstance();
    }

    public Seller createSeller(String username, String password, String email) {
        Seller seller = new Seller(username, password, email);
        userManager.addUser(seller);
        return seller;
    }

    public Bidder createBidder(String username, String password, String email) {
        Bidder bidder = new Bidder(username, password, email);
        userManager.addUser(bidder);
        return bidder;
    }

    public User getUserById(String id) {
        User user = userManager.findUserById(id);

        if (user == null) {
            throw new RuntimeException("User not found");
        }

        return user;
    }

    public User getUserByUsername(String username) {
        User user = userManager.findUserByUsername(username);

        if (user == null) {
            throw new RuntimeException("User not found");
        }

        return user;
    }

    public List<User> getAllUsers() {
        return userManager.getAllUsers();
    }

    public void removeUser(String id) {
        boolean removed = userManager.removeUser(id);

        if (!removed) {
            throw new RuntimeException("User not found");
        }
    }
}