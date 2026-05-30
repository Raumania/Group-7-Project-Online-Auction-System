package auction_system.server.service;

import auction_system.common.enums.AuctionStatus;
import auction_system.common.enums.ItemType;
import auction_system.server.dao.AuctionDAO;
import auction_system.server.dao.DatabaseConnection;
import auction_system.server.dao.ItemDAO;
import auction_system.server.exception.daoException.DataBaseException;
import auction_system.server.exception.serviceException.InValidAuctionData;
import auction_system.server.model.*;
import auction_system.server.observer.EventBus;
import auction_system.server.store.AuctionStore;
import auction_system.server.store.UserStore;
import auction_system.server.dao.UserDAO;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AuctionService {

    // Singleton for service
    private static AuctionService instance;

    private final AuctionDAO auctionDAO = AuctionDAO.getInstance();
    private final ItemDAO itemDAO = ItemDAO.getInstance();
    private final ImageService imageService = ImageService.getInstance();
    private final EventBus eventBus = EventBus.getInstance();
    private final AuctionStore auctionStore = AuctionStore.getInstance();
    private final UserStore userStore = UserStore.getInstance();

    private AuctionService() {
    }

    public static AuctionService getInstance() {
        if (instance == null) {
            instance = new AuctionService();
        }
        return instance;
    }

    public void createAuction(Auction auction) {
        //Sync with Database

        Connection connection = null;

        try {
            connection = DatabaseConnection.getConnection();
            connection.setAutoCommit(false); // Bắt đầu transaction

            validateItemData(auction);

            // 1. Tạo Item trước và lấy ID của nó
            int itemId = itemDAO.save(connection, auction);
            auction.setItemId(itemId); 

            // 2. Xác định trạng thái Auction
            LocalDateTime now = LocalDateTime.now();
            if (now.isAfter(auction.getStartTime()) && now.isBefore(auction.getEndTime())) {
                auction.setStatus(AuctionStatus.RUNNING);
            } else if (now.isAfter(auction.getEndTime())) {
                auction.setStatus(AuctionStatus.FINISHED);
            } else {
                auction.setStatus(AuctionStatus.OPEN);
            }

            // 3. Lưu ảnh (nếu có)
            String imagePathToSave = null;
            String imageBase64 = auction.getImageBase64();
            if (imageBase64 != null && !imageBase64.isBlank()) {
                imagePathToSave = imageService.saveBase64Image(connection, imageBase64);
            }

            // 4. Tạo Auction với item_id và image path
            int auctionId = auctionDAO.save(connection, auction, itemId, imagePathToSave);
            auction.setId(auctionId);
            auction.setImagePath(imagePathToSave); // PRESERVE PATH IN RAM CACHE

            connection.commit();
            
            // Sync with Server Store RAM Cache
            auctionStore.addAuction(auction);
            eventBus.publishAuctionCreated(auction);

        } catch (Exception e) {
            rollback(connection);
            e.printStackTrace();
            throw new RuntimeException("Cannot create auction: " + e.getMessage(), e);
        } finally {
            closeConnection(connection);
        }
    }

    public void deleteAuction(int auctionId) {
        //Sync with Database
        Connection connection = null;
        try {
            connection = DatabaseConnection.getConnection();
            connection.setAutoCommit(false);

            Auction auction = auctionDAO.findByIdForUpdate(connection, auctionId);
            if (auction == null) {
                throw new RuntimeException("Auction not found");
            }

            if (auction.getStatus() != AuctionStatus.OPEN) {
                throw new RuntimeException("Cannot delete auction: status is not OPEN");
            }

            if (auction.getCurrentPrice() != null && auction.getCurrentPrice().compareTo(BigDecimal.ZERO) > 0) {
                throw new RuntimeException("Cannot delete auction after bidding started");
            }

            int itemId = auction.getItemId();

            // Do đã có ON DELETE CASCADE, về lý thuyết chỉ cần xóa item là auction sẽ bị xóa theo.
            // Tuy nhiên, xóa cả 2 để logic tường minh hơn.
            auctionDAO.delete(connection, auctionId);
            itemDAO.delete(connection, itemId); 

            connection.commit();
            
            // Sync with Server Store RAM Cache
            auctionStore.removeAuction(auctionId);
            eventBus.publishAuctionDeleted(auctionId);

            // Xóa ảnh vật lý khỏi ổ đĩa sau khi xóa thành công trong Database
            if (auction.getImagePath() != null) {
                imageService.deleteImage(auction.getImagePath());
            }

        } catch (Exception e) {
            rollback(connection);
            throw new RuntimeException("Cannot delete auction", e);
        } finally {
            closeConnection(connection);
        }
    }
    
    public void editAuction(Auction auction) {
        //Sync with Database
        Connection connection = null;

        try {
            connection = DatabaseConnection.getConnection();
            connection.setAutoCommit(false);

            Auction oldAuction = auctionDAO.findByIdForUpdate(connection, auction.getId());

            if (oldAuction == null) {
                throw new RuntimeException("Auction not found");
            }

            if (oldAuction.getStatus() != AuctionStatus.OPEN) {
                throw new RuntimeException("Cannot edit auction: status is not OPEN");
            }

            if (oldAuction.getCurrentPrice() != null && oldAuction.getCurrentPrice().compareTo(BigDecimal.ZERO) > 0) {
                throw new RuntimeException("Cannot edit auction after bidding started");
            }

            // Gán itemId từ auction cũ sang auction mới để đảm bảo không bị mất
            auction.setItemId(oldAuction.getItemId());

            // Lưu ảnh mới (nếu được thay đổi) và xóa ảnh cũ
            String newImageBase64 = auction.getImageBase64();
            String oldImageBase64 = oldAuction.getImageBase64();
            String imagePathToSave = oldAuction.getImagePath();
            String imagePathToDelete = null;

            if (newImageBase64 != null && !newImageBase64.isBlank() && !newImageBase64.equals(oldImageBase64)) {
                // Có ảnh mới và ảnh mới khác ảnh cũ
                imagePathToSave = imageService.saveBase64Image(connection, newImageBase64);
                imagePathToDelete = oldAuction.getImagePath();
            }

            auction.setImagePath(imagePathToSave);

            auctionDAO.update(connection, auction);
            itemDAO.update(connection, auction);

            connection.commit();

            // Safeguard: Ensure the broadcasted auction object carries the base64 image data!
            if (auction.getImageBase64() == null || auction.getImageBase64().isBlank()) {
                auction.setImageBase64(oldAuction.getImageBase64());
            }

            // Sync with Server Store RAM Cache
            auctionStore.updateAuction(auction);

            eventBus.publishAuctionEdited(auction);

            // Xóa ảnh cũ khỏi ổ đĩa sau khi cập nhật thành công trong Database
            if (imagePathToDelete != null) {
                imageService.deleteImage(imagePathToDelete);
            }

        } catch (Exception e) {
            rollback(connection);
            throw new RuntimeException("Cannot edit auction", e);

        } finally {
            closeConnection(connection);
        }
    }

    public void updateAuctionStatus(Auction auction) {
        LocalDateTime now = LocalDateTime.now();
        AuctionStatus currentStatus = auction.getStatus();
        AuctionStatus newStatus = currentStatus;

        if (currentStatus == AuctionStatus.OPEN && now.isAfter(auction.getStartTime())) {
            newStatus = AuctionStatus.RUNNING;
        } else if (currentStatus == AuctionStatus.RUNNING && now.isAfter(auction.getEndTime())) {
            // Add a 2-second buffer before marking FINISHED to avoid a race condition
            // with anti-sniping: when a bid is placed in the final 30s, BidService extends
            // endTime by +1 minute. The Scheduler runs every 500ms and may detect the old
            // (expired) endTime just before BidService updates the AuctionStore, incorrectly
            // broadcasting FINISHED while the auction should still be RUNNING.
            LocalDateTime finishThreshold = auction.getEndTime().plusSeconds(2);
            if (now.isAfter(finishThreshold)) {
                newStatus = AuctionStatus.FINISHED;
            }
        }

        if (newStatus != currentStatus) {
            auction.setStatus(newStatus);
            //Sync with Database
            auctionDAO.update(auction);
            // Sync with RAM Cache
            auctionStore.updateAuction(auction);
            // Notify observers (e.g. clients) via EventBus
            eventBus.publishAuctionEdited(auction);
        }
    }

    private void rollback(Connection connection) {
        try {
            if (connection != null) {
                connection.rollback();
            }
        } catch (Exception e) {
            System.err.println("Rollback failed");
            throw new RuntimeException("Rollback failed", e);
        }
    }

    private void closeConnection(Connection connection) {
        try {
            if (connection != null) {
                connection.setAutoCommit(true);
                connection.close();
            }
        } catch (Exception e) {
            System.err.println("Cannot close Database connection");
            throw new RuntimeException("Cannot close Database connection", e);
        }
    }

    public List<Auction> findbyItemType(String type) {
        if (type == null) {
            throw new RuntimeException("Item type cannot be null");
        }

        List<Auction> auctionsList = new ArrayList<>();
        List<Item> items = itemDAO.findByType(type);

        for (Item item : items) {
            Auction auction = auctionStore.getAuctionById(item.getId());

            if (auction != null) {
                auctionsList.add(auction);
            }
        }

        return auctionsList;
    }

    public List<Auction> findbyItemName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new RuntimeException("Item name cannot be empty");
        }

        List<Auction> auctionsList = new ArrayList<>();
        List<Item> items = itemDAO.findByItemName(name.trim());

        for (Item item : items) {
            Auction auction = auctionStore.getAuctionById(item.getId());

            if (auction != null) {
                auctionsList.add(auction);
            }
        }

        return auctionsList;
    }

    public Auction getAuctionById(int id) {
        return auctionStore.getAuctionById(id);
    }

    public List<Auction> getAllAuctions() {
        return auctionStore.getAllAuctions();
    }

    public List<Auction> getMyAuctions(int sellerId) {
        User user = userStore.getUserById(sellerId);
        if (user != null && user.hasRole(auction_system.common.enums.UserRole.ADMIN)) {
            return auctionStore.getAllAuctions();
        }
        
        List<Auction> results = new ArrayList<>();
        for (Auction a : auctionStore.getAllAuctions()) {
            if (a.getSellerId() == sellerId) {
                results.add(a);
            }
        }
        return results;
    }

    public void closeAuction(int auctionId) {
        //Sync with Database
        Connection connection = null;

        try {
            connection = DatabaseConnection.getConnection();
            connection.setAutoCommit(false);

            Auction auction = auctionDAO.findByIdForUpdate(connection, auctionId);

            if (auction == null) {
                throw new RuntimeException("Auction not found");
            }

            if (auction.getStatus() != AuctionStatus.OPEN &&
                    auction.getStatus() != AuctionStatus.RUNNING) {
                throw new RuntimeException("Auction is not open");
            }

            Integer highestBidderId = auction.getHighestBidderId();
            BigDecimal winningPrice = auction.getCurrentPrice();
            int sellerId = auction.getSellerId();
            
            User winnerFromRam = null;
            User sellerFromRam = null;

            if (highestBidderId != null && highestBidderId != 0 && winningPrice != null) {
                winnerFromRam = userStore.getUserById(highestBidderId);
                sellerFromRam = userStore.getUserById(sellerId);
                
                // Tiền vẫn được đóng băng (frozen_balance) của winner
                // Tạm thời chưa chuyển tiền (available_balance) cho seller.
                // Tính năng xác nhận chuyển tiền sẽ được thực hiện sau.
            }

            auction.setStatus(AuctionStatus.FINISHED);

            auctionDAO.update(connection, auction);

            connection.commit();
            
            // Sync with RAM Cache
            auctionStore.updateAuction(auction);

            
            eventBus.publishAuctionEdited(auction);

        } catch (Exception e) {
            rollback(connection);
            throw new RuntimeException("Cannot close auction", e);

        } finally {
            closeConnection(connection);
        }
    }

    public void cancelAuction(int auctionId) {
        //Sync with Database
        Connection connection = null;

        try {
            connection = DatabaseConnection.getConnection();
            connection.setAutoCommit(false);

            Auction auction = auctionDAO.findByIdForUpdate(connection, auctionId);

            if (auction == null) {
                throw new RuntimeException("Auction not found");
            }

            if (auction.getStatus() != AuctionStatus.OPEN &&
                    auction.getStatus() != AuctionStatus.RUNNING &&
                    auction.getStatus() != AuctionStatus.FINISHED) {
                throw new RuntimeException("Auction is not in OPEN, RUNNING or FINISHED state");
            }

            auction.setStatus(AuctionStatus.CANCELLED);

            // Hoàn lại tiền đóng băng cho người giữ giá cao nhất nếu có
            if (auction.getHighestBidderId() != null) {
                User highestBidder = userStore.getUserById(auction.getHighestBidderId());
                if (highestBidder != null) {
                    BigDecimal toUnfreeze = auction.getCurrentPrice();
                    if (toUnfreeze != null && toUnfreeze.compareTo(BigDecimal.ZERO) > 0) {
                        toUnfreeze = toUnfreeze.min(highestBidder.getFrozenBalance());
                        if (toUnfreeze.compareTo(BigDecimal.ZERO) > 0) {
                            highestBidder.unfreezeBalance(toUnfreeze);
                            UserDAO.getInstance().update(connection, highestBidder);
                            userStore.updateUser(highestBidder);
                        }
                    }
                }
            }

            // Hủy tất cả Auto-Bids liên quan và hoàn lại phần tiền chênh lệch (nếu có)
            AutoBidService.getInstance().cancelAllAutoBidsForAuction(connection, auctionId);

            auctionDAO.update(connection, auction);

            connection.commit();
            
            // Sync with RAM Cache
            auctionStore.updateAuction(auction);

            // Gửi sự kiện CANCELLED cho các client
            try {
                auction_system.common.protocol.Response cancelResponse = new auction_system.common.protocol.Response(
                    auction_system.common.enums.Status.SUCCESS,
                    auction_system.common.enums.Action.EVENT_AUCTION_CANCELLED,
                    auction.getId(),
                    "Phiên đấu giá bị hủy bởi quản trị viên."
                );
                auction_system.server.AuctionServer.broadcast(auction_system.server.util.GsonUtil.toJson(cancelResponse));
            } catch (Exception e) {
                System.err.println("Failed to broadcast EVENT_AUCTION_CANCELLED: " + e.getMessage());
            }

        } catch (Exception e) {
            rollback(connection);
            throw new RuntimeException("Cannot cancel auction", e);

        } finally {
            closeConnection(connection);
        }
    }
    
    public void validateItemData(Auction auction) {
        try {
            if (auction.getName() == null || auction.getName().trim().isEmpty()) {
                throw new RuntimeException("Item name cannot be null or empty");
            }
            if (auction.getStartTime() == null) {
                throw new RuntimeException("Starting time cannot be null");
            }

            if (auction.getEndTime() == null) {
                throw new RuntimeException("Ending time cannot be null");
            }

            if (!auction.getEndTime().isAfter(auction.getStartTime())) {
                throw new RuntimeException("Ending time must be after starting time");
            }

            if (userStore.getUserById(auction.getSellerId()) == null) {
                throw new RuntimeException("Owner cannot be null");
            }

            if (auction.getStartingPrice() == null || auction.getStartingPrice().compareTo(BigDecimal.ZERO) <= 0) {
                throw new RuntimeException("Starting price must be greater than 0");
            }

            if (auction.getType() == null) {
                throw new RuntimeException("Status cannot be null");
            }
        }catch (RuntimeException e) {
            throw new InValidAuctionData(e.getMessage());}
    }
}
