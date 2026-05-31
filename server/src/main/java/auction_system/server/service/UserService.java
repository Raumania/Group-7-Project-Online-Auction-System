package auction_system.server.service;

import auction_system.common.enums.UserRole;
import auction_system.server.dao.UserDAO;
import auction_system.server.dao.AuctionDAO;
import auction_system.server.dao.BidTransactionDAO;
import auction_system.server.dao.DatabaseConnection;
import auction_system.server.exception.AuthorizationException;
import auction_system.server.exception.ControllerException.*;
import auction_system.server.model.User;
import auction_system.server.model.Auction;
import auction_system.server.model.BidTransaction;
import auction_system.server.store.UserStore;
import auction_system.server.util.HashUtil;
import auction_system.server.AuctionServer;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;

public class UserService {
    private static UserService instance;
    private final UserDAO userDAO;
    private final UserStore userStore;

    private UserService() {
        this.userDAO = UserDAO.getInstance();
        this.userStore = UserStore.getInstance();
    }

    public static UserService getInstance() {
        if (instance == null) {
            instance = new UserService();
        }
        return instance;
    }

    public User registerUser(String fullname, String username, String password) {
        return registerUser(
                fullname,
                username,
                password,
                Set.of(UserRole.BIDDER, UserRole.SELLER)
        );
    }

    public User registerUser(String fullname, String username, String password, Set<UserRole> roles) {
        User existingUser = userStore.getUserByUsername(username);

        if (existingUser != null) {
            throw new DuplicateResourceException("Username already exists");
        }

        if (roles == null || roles.isEmpty()) {
            throw new InvalidInputException("User must have at least one role");
        }

        String hashpassword = HashUtil.hashPassword(password);
        User user = new User(fullname, username, hashpassword, roles);

        userDAO.save(user);

        // Sync with memory cache
        userStore.addUser(user);

        return user;
    }

    public User createSeller(String fullname, String username, String password) {
        return registerUser(fullname, username, password, Set.of(UserRole.SELLER));
    }

    public User createBidder(String fullname, String username, String password) {
        return registerUser(fullname, username, password, Set.of(UserRole.BIDDER));
    }

    public User createBidderAndSeller(String fullname, String username, String password) {
        return registerUser(fullname, username, password);
    }

    public User createAdmin(String fullname, String username, String password) {
        return registerUser(fullname, username, password, Set.of(UserRole.ADMIN));
    }

    public User createFullAdmin(String fullname, String username, String password) {
        return registerUser(fullname, username, password, Set.of(UserRole.ADMIN, UserRole.BIDDER, UserRole.SELLER));
    }

    /*
        Find user by id.
        Read from Store (RAM) - do not touch DB.
    */
    public User getUserById(int id) {
        User user = userStore.getUserById(id);

        if (user == null) {
            throw new UserNotFoundException(id);
        }

        return user;
    }

    /*
        Find user by username.
        Read from Store (RAM).
    */
    public User getUserByUsername(String username) {
        User user = userStore.getUserByUsername(username);

        if (user == null) {
            throw new UserNotFoundException(username);
        }

        return user;
    }

    public User getSellerById(int id) {
        User user = getUserById(id);

        if (user.hasRole(UserRole.SELLER)) {
            return user;
        }

        throw new AuthorizationException("User with id " + id + " is not a seller");
    }

    public User getAdminById(int id) {
        User user = getUserById(id);

        if (user.hasRole(UserRole.ADMIN)) {
            return user;
        }

        throw new AuthorizationException("User with id " + id + " is not an admin");
    }

    public void addRole(int userId, UserRole role) {
        User user = getUserById(userId);
        user.addRole(role);
        userDAO.update(user);
        userStore.updateUser(user);
    }

    public void removeRole(int userId, UserRole role) {
        User user = getUserById(userId);
        user.removeRole(role);
        userDAO.update(user);
        userStore.updateUser(user);
    }

    /*
        Login.
        Read from Store.
    */
    public User login(String username, String password) {
        User user = userStore.getUserByUsername(username);

        if (user == null) {
            throw new AuthenticationException("Username does not exist");
        }

        boolean correctPassword = HashUtil.checkPassword(password, user.getPassword());

        if (!correctPassword) {
            throw new AuthenticationException("Wrong password");
        }

        return user;
    }

    /*
        Get all users from Store.
    */
    public List<User> getAllUsers() {
        return userStore.getAllUsers();
    }

    /*
        Delete user.
    */
    public void removeUser(int id) {
        boolean removed = userDAO.deleteById(id);

        if (!removed) {
            throw new UserNotFoundException(id);
        }

        // Sync cache
        userStore.removeUser(id);
    }

    /*
        Deposit - read from Store, write to both Store + DB.
    */
    public void deposit(int userId, BigDecimal amount) {
        User user = getUserById(userId);

        user.deposit(amount);

        boolean updated = userDAO.updateBalance(user.getId(), user.getAvailableBalance(), user.getFrozenBalance());

        if (!updated) {
            throw new DatabaseException("Cannot update balance");
        }

        // Sync Store
        userStore.updateUser(user);
    }

    /*
        Withdraw - read from Store, write to both Store + DB.
    */
    public void withdraw(int userId, BigDecimal amount) {
        User user = getUserById(userId);

        user.withdraw(amount);

        boolean updated = userDAO.updateBalance(user.getId(), user.getAvailableBalance(), user.getFrozenBalance());

        if (!updated) {
            throw new DatabaseException("Cannot update balance");
        }

        // Sync Store
        userStore.updateUser(user);
    }

    /*
        Update user information (fullname, username, roles, balance).
        Read from Store, write to both Store + DB.
    */
    public void updateUser(User updatedUser) {
        User user = getUserById(updatedUser.getId());

        user.setFullname(updatedUser.getFullname());
        user.setUsername(updatedUser.getUsername());
        user.setRoles(updatedUser.getRoles());
        user.setAvailableBalance(updatedUser.getAvailableBalance());

        userDAO.update(user);
        userStore.updateUser(user);
    }

    /*
        Create new user from Admin panel.
        Different from registerUser: do not re-hash password because admin sets it.
    */
    public User createUser(User newUser) {
        User existingUser = userStore.getUserByUsername(newUser.getUsername());
        if (existingUser != null) {
            throw new DuplicateResourceException("Username already exists");
        }

        userDAO.save(newUser);
        userStore.addUser(newUser);

        return newUser;
    }

    /*
        Ban user:
        Flow: READ from Store -> write to both Store + DB (in transaction).
        Process:
          1. Set status to BANNED
          2. Cancel auctions created by the user (seller) -> refund frozen money to highest bidder
          3. Cancel highest bidder position in auctions the user is leading -> find replacement
          4. Disconnect TCP of banned user
    */
    public void banUser(int userId) {
        // 1. READ from Store (fast)
        User user = getUserById(userId);

        // 2. Write: open Transaction to ensure atomic in DB
        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                System.out.println("[BAN] Starting ban for userId=" + userId + ", frozenBalance=" + user.getFrozenBalance());

                // Update status to BANNED -> write DB + Store
                user.setStatus(auction_system.common.enums.UserStatus.BANNED);
                userDAO.update(connection, user);
                userStore.updateUser(user);
                System.out.println("[BAN] User status set to BANNED in DB+Store");

                // --- Step 2: Auctions created by banned user (Seller role) ---
                List<Auction> activeAuctionsBySeller = AuctionDAO.getInstance().findActiveAuctionsBySeller(connection, userId);
                for (Auction auction : activeAuctionsBySeller) {
                    auction.setStatus(auction_system.common.enums.AuctionStatus.CANCELLED);

                    // Refund frozen money to highest bidder (read from Store)
                    if (auction.getHighestBidderId() != null) {
                        User highestBidder = userStore.getUserById(auction.getHighestBidderId());
                        if (highestBidder != null) {
                            BigDecimal toUnfreeze = auction.getCurrentPrice();
                            // Guard: only unfreeze up to actual frozenBalance in Store
                            if (toUnfreeze != null && toUnfreeze.compareTo(BigDecimal.ZERO) > 0) {
                                toUnfreeze = toUnfreeze.min(highestBidder.getFrozenBalance());
                                if (toUnfreeze.compareTo(BigDecimal.ZERO) > 0) {
                                    highestBidder.unfreezeBalance(toUnfreeze);
                                    userDAO.update(connection, highestBidder); // Write DB
                                    userStore.updateUser(highestBidder);       // Write Store
                                }
                            }
                        }
                    }

                    // Cancel all related Auto-Bids and refund the difference (if any)
                    AutoBidService.getInstance().cancelAllAutoBidsForAuction(connection, auction.getId());

                    AuctionDAO.getInstance().update(connection, auction);
                    auction_system.server.store.AuctionStore.getInstance().updateAuction(auction);

                    // Broadcast auction cancellation
                    try {
                        auction_system.common.protocol.Response cancelResponse = new auction_system.common.protocol.Response(
                            auction_system.common.enums.Status.SUCCESS,
                            auction_system.common.enums.Action.EVENT_AUCTION_CANCELLED,
                            auction.getId(),
                            "Auction cancelled because seller account was banned."
                        );
                        AuctionServer.broadcast(auction_system.server.util.GsonUtil.toJson(cancelResponse));
                    } catch (Exception e) {
                        System.err.println("Failed to broadcast EVENT_AUCTION_CANCELLED: " + e.getMessage());
                    }
                }

                // --- Step 3: Auctions where banned user is highest bidder (Bidder role) ---
                List<Auction> activeAuctionsByHighestBidder = AuctionDAO.getInstance().findActiveAuctionsByHighestBidder(connection, userId);
                System.out.println("[BAN] Auctions where user is highest bidder: " + activeAuctionsByHighestBidder.size());
                for (Auction auction : activeAuctionsByHighestBidder) {
                    System.out.println("[BAN] Processing auction id=" + auction.getId() + ", currentPrice=" + auction.getCurrentPrice());
                    // Find valid 2nd highest bidder from bid history
                    List<BidTransaction> bids = BidTransactionDAO.getInstance().findByAuctionId(connection, auction.getId());

                    BidTransaction newHighestBid = null;
                    User newBidderUser = null;

                    for (int i = bids.size() - 1; i >= 0; i--) {
                        BidTransaction bid = bids.get(i);
                        User bidder = bid.getBidder();

                        if (bidder.getId() == userId) continue; // skip the banned user

                        // Read from Store to get the latest status
                        User storeBidder = userStore.getUserById(bidder.getId());
                        if (storeBidder == null || storeBidder.getStatus() == auction_system.common.enums.UserStatus.BANNED) {
                            continue;
                        }

                        // Check for sufficient available balance to freeze
                        if (storeBidder.getAvailableBalance().compareTo(bid.getAmount()) >= 0) {
                            storeBidder.freezeBalance(bid.getAmount());
                            userDAO.update(connection, storeBidder); // Write DB
                            userStore.updateUser(storeBidder);       // Write Store

                            newHighestBid = bid;
                            newBidderUser = storeBidder;
                            break;
                        }
                    }

                    // Unfreeze money of banned user in this auction
                    // Guard: min(currentPrice, frozenBalance) to never get "Not enough frozen"
                    BigDecimal toUnfreeze = auction.getCurrentPrice();
                    System.out.println("[BAN] toUnfreeze=" + toUnfreeze + ", user.frozenBalance=" + user.getFrozenBalance());
                    if (toUnfreeze != null && toUnfreeze.compareTo(BigDecimal.ZERO) > 0) {
                        toUnfreeze = toUnfreeze.min(user.getFrozenBalance());
                        if (toUnfreeze.compareTo(BigDecimal.ZERO) > 0) {
                            user.unfreezeBalance(toUnfreeze);
                            userDAO.update(connection, user); // Write DB
                            userStore.updateUser(user);       // Write Store
                            System.out.println("[BAN] Unfroze " + toUnfreeze + " from banned user. New frozen=" + user.getFrozenBalance());
                        }
                    }

                    // Update auction with new highest bidder (or reset to starting price)
                    if (newHighestBid != null && newBidderUser != null) {
                        auction.setHighestBidderId(newBidderUser.getId());
                        auction.setHighestBidderUsername(newBidderUser.getUsername());
                        auction.setCurrentPrice(newHighestBid.getAmount());
                    } else {
                        auction.setHighestBidderId(null);
                        auction.setHighestBidderUsername(null);
                        auction.setCurrentPrice(null);
                    }

                    AuctionDAO.getInstance().update(connection, auction);
                    auction_system.server.store.AuctionStore.getInstance().updateAuction(auction);

                    // Delete bids of banned user in Database and RAM Cache
                    auction_system.server.dao.BidTransactionDAO.getInstance().deleteByBidderAndAuction(connection, userId, auction.getId());
                    auction_system.server.store.BidTransactionStore.getInstance().removeBidsByBidderAndAuction(userId, auction.getId());

                    // Broadcast auction update
                    try {
                        auction_system.common.protocol.Response updateResponse = new auction_system.common.protocol.Response(
                            auction_system.common.enums.Status.SUCCESS,
                            auction_system.common.enums.Action.EVENT_AUCTION_EDITED,
                            auction,
                            "Auction info updated after previous bid was cancelled."
                        );
                        AuctionServer.broadcast(auction_system.server.util.GsonUtil.toJson(updateResponse));
                    } catch (Exception e) {
                        System.err.println("Failed to broadcast EVENT_AUCTION_EDITED: " + e.getMessage());
                    }
                }

                connection.commit();

                // Step 4: Broadcast Ban event to all clients
                try {
                    auction_system.common.protocol.Response banResponse = new auction_system.common.protocol.Response(
                        auction_system.common.enums.Status.SUCCESS,
                        auction_system.common.enums.Action.EVENT_USER_BANNED,
                        userId,
                        "Your account has been banned due to violation of platform terms."
                    );
                    AuctionServer.broadcast(auction_system.server.util.GsonUtil.toJson(banResponse));
                } catch (Exception e) {
                    System.err.println("Failed to broadcast EVENT_USER_BANNED: " + e.getMessage());
                }

                // Wait 500ms for broadcast to reach client before disconnecting
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                // Step 5: Force disconnect TCP of banned user
                AuctionServer.disconnectUser(userId);

            } catch (Exception e) {
                connection.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Transaction error while banning user", e);
        }
    }
}