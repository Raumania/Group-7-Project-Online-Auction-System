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
        Tìm user theo id.
        Đọc từ Store (RAM) - không chọc DB.
    */
    public User getUserById(int id) {
        User user = userStore.getUserById(id);

        if (user == null) {
            throw new UserNotFoundException(id);
        }

        return user;
    }

    /*
        Tìm user theo username.
        Đọc từ Store (RAM).
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
        Đăng nhập.
        Đọc từ Store.
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
        Lấy tất cả user từ Store.
    */
    public List<User> getAllUsers() {
        return userStore.getAllUsers();
    }

    /*
        Xóa user.
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
        Nạp tiền - đọc từ Store, ghi cả Store + DB.
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
        Rút tiền - đọc từ Store, ghi cả Store + DB.
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
        Cập nhật thông tin user (fullname, username, roles, balance).
        Đọc từ Store, ghi cả Store + DB.
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
        Tạo user mới từ Admin panel.
        Khác registerUser: không hash lại password vì admin tự set.
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
        Flow: ĐỌC từ Store → ghi cả Store + DB (trong transaction).
        Xử lý:
          1. Đặt status BANNED
          2. Hủy phiên đấu giá do user đó tạo (seller) → hoàn tiền đóng băng cho highest bidder
          3. Hủy vị trí highest bidder trong các phiên user đang dẫn đầu → tìm người thay thế
          4. Ngắt kết nối TCP của user bị ban
    */
    public void banUser(int userId) {
        // 1. ĐỌC từ Store (nhanh)
        User user = getUserById(userId);

        // 2. Ghi: mở Transaction để đảm bảo atomic trong DB
        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                System.out.println("[BAN] Starting ban for userId=" + userId + ", frozenBalance=" + user.getFrozenBalance());

                // Cập nhật trạng thái BANNED → ghi DB + Store
                user.setStatus(auction_system.common.enums.UserStatus.BANNED);
                userDAO.update(connection, user);
                userStore.updateUser(user);
                System.out.println("[BAN] User status set to BANNED in DB+Store");

                // --- Bước 2: Phiên đấu giá do user bị ban tạo ra (Seller role) ---
                List<Auction> activeAuctionsBySeller = AuctionDAO.getInstance().findActiveAuctionsBySeller(connection, userId);
                for (Auction auction : activeAuctionsBySeller) {
                    auction.setStatus(auction_system.common.enums.AuctionStatus.CANCELLED);

                    // Hoàn lại tiền đóng băng cho người giữ giá cao nhất (đọc từ Store)
                    if (auction.getHighestBidderId() != null) {
                        User highestBidder = userStore.getUserById(auction.getHighestBidderId());
                        if (highestBidder != null) {
                            BigDecimal toUnfreeze = auction.getCurrentPrice();
                            // Guard: chỉ unfreeze tối đa bằng frozenBalance thực tế trong Store
                            if (toUnfreeze != null && toUnfreeze.compareTo(BigDecimal.ZERO) > 0) {
                                toUnfreeze = toUnfreeze.min(highestBidder.getFrozenBalance());
                                if (toUnfreeze.compareTo(BigDecimal.ZERO) > 0) {
                                    highestBidder.unfreezeBalance(toUnfreeze);
                                    userDAO.update(connection, highestBidder); // Ghi DB
                                    userStore.updateUser(highestBidder);       // Ghi Store
                                }
                            }
                        }
                    }

                    // Hủy tất cả Auto-Bids liên quan và hoàn lại phần tiền chênh lệch (nếu có)
                    AutoBidService.getInstance().cancelAllAutoBidsForAuction(connection, auction.getId());

                    AuctionDAO.getInstance().update(connection, auction);
                    auction_system.server.store.AuctionStore.getInstance().updateAuction(auction);

                    // Broadcast hủy phiên
                    try {
                        auction_system.common.protocol.Response cancelResponse = new auction_system.common.protocol.Response(
                            auction_system.common.enums.Status.SUCCESS,
                            auction_system.common.enums.Action.EVENT_AUCTION_CANCELLED,
                            auction.getId(),
                            "Phiên đấu giá bị hủy do tài khoản người bán bị khóa."
                        );
                        AuctionServer.broadcast(auction_system.server.util.GsonUtil.toJson(cancelResponse));
                    } catch (Exception e) {
                        System.err.println("Failed to broadcast EVENT_AUCTION_CANCELLED: " + e.getMessage());
                    }
                }

                // --- Bước 3: Phiên đấu giá mà user bị ban đang giữ giá cao nhất (Bidder role) ---
                List<Auction> activeAuctionsByHighestBidder = AuctionDAO.getInstance().findActiveAuctionsByHighestBidder(connection, userId);
                System.out.println("[BAN] Auctions where user is highest bidder: " + activeAuctionsByHighestBidder.size());
                for (Auction auction : activeAuctionsByHighestBidder) {
                    System.out.println("[BAN] Processing auction id=" + auction.getId() + ", currentPrice=" + auction.getCurrentPrice());
                    // Tìm người giữ giá cao thứ 2 hợp lệ từ lịch sử thầu
                    List<BidTransaction> bids = BidTransactionDAO.getInstance().findByAuctionId(connection, auction.getId());

                    BidTransaction newHighestBid = null;
                    User newBidderUser = null;

                    for (int i = bids.size() - 1; i >= 0; i--) {
                        BidTransaction bid = bids.get(i);
                        User bidder = bid.getBidder();

                        if (bidder.getId() == userId) continue; // bỏ qua chính user bị ban

                        // Đọc từ Store để lấy trạng thái mới nhất
                        User storeBidder = userStore.getUserById(bidder.getId());
                        if (storeBidder == null || storeBidder.getStatus() == auction_system.common.enums.UserStatus.BANNED) {
                            continue;
                        }

                        // Kiểm tra đủ available balance để đóng băng
                        if (storeBidder.getAvailableBalance().compareTo(bid.getAmount()) >= 0) {
                            storeBidder.freezeBalance(bid.getAmount());
                            userDAO.update(connection, storeBidder); // Ghi DB
                            userStore.updateUser(storeBidder);       // Ghi Store

                            newHighestBid = bid;
                            newBidderUser = storeBidder;
                            break;
                        }
                    }

                    // Giải phóng tiền đóng băng của user bị ban tại phiên này
                    // Guard: min(currentPrice, frozenBalance) để không bao giờ bị "Not enough frozen"
                    BigDecimal toUnfreeze = auction.getCurrentPrice();
                    System.out.println("[BAN] toUnfreeze=" + toUnfreeze + ", user.frozenBalance=" + user.getFrozenBalance());
                    if (toUnfreeze != null && toUnfreeze.compareTo(BigDecimal.ZERO) > 0) {
                        toUnfreeze = toUnfreeze.min(user.getFrozenBalance());
                        if (toUnfreeze.compareTo(BigDecimal.ZERO) > 0) {
                            user.unfreezeBalance(toUnfreeze);
                            userDAO.update(connection, user); // Ghi DB
                            userStore.updateUser(user);       // Ghi Store
                            System.out.println("[BAN] Unfroze " + toUnfreeze + " from banned user. New frozen=" + user.getFrozenBalance());
                        }
                    }

                    // Cập nhật phiên với người giữ giá mới (hoặc về giá khởi điểm)
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

                    // Xóa các bid của user bị ban trong Database và RAM Cache
                    auction_system.server.dao.BidTransactionDAO.getInstance().deleteByBidderAndAuction(connection, userId, auction.getId());
                    auction_system.server.store.BidTransactionStore.getInstance().removeBidsByBidderAndAuction(userId, auction.getId());

                    // Broadcast cập nhật phiên
                    try {
                        auction_system.common.protocol.Response updateResponse = new auction_system.common.protocol.Response(
                            auction_system.common.enums.Status.SUCCESS,
                            auction_system.common.enums.Action.EVENT_AUCTION_EDITED,
                            auction,
                            "Thông tin phiên đấu giá được cập nhật sau khi thầu cũ bị hủy."
                        );
                        AuctionServer.broadcast(auction_system.server.util.GsonUtil.toJson(updateResponse));
                    } catch (Exception e) {
                        System.err.println("Failed to broadcast EVENT_AUCTION_EDITED: " + e.getMessage());
                    }
                }

                connection.commit();

                // Bước 4: Broadcast sự kiện Ban cho tất cả các client
                try {
                    auction_system.common.protocol.Response banResponse = new auction_system.common.protocol.Response(
                        auction_system.common.enums.Status.SUCCESS,
                        auction_system.common.enums.Action.EVENT_USER_BANNED,
                        userId,
                        "Tài khoản của bạn đã bị khóa do vi phạm điều khoản của sàn."
                    );
                    AuctionServer.broadcast(auction_system.server.util.GsonUtil.toJson(banResponse));
                } catch (Exception e) {
                    System.err.println("Failed to broadcast EVENT_USER_BANNED: " + e.getMessage());
                }

                // Chờ 500ms để broadcast kịp gửi tới client trước khi ngắt kết nối
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                // Bước 5: Cưỡng chế ngắt kết nối TCP của user bị ban
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