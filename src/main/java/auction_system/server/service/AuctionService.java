package auction_system.server.service;

import auction_system.common.enums.AuctionStatus;
import auction_system.common.enums.ItemType;
import auction_system.server.dao.AuctionDAO;
import auction_system.server.dao.DatabaseConnection;
import auction_system.server.dao.ItemDAO;
import auction_system.server.model.*;

import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AuctionService {

    // Singleton for service
    private static AuctionService instance;

    private final AuctionDAO auctionDAO = new AuctionDAO();
    private final ItemDAO itemDAO = new ItemDAO();

    private AuctionService() {
    }

    public static AuctionService getInstance() {
        if (instance == null) {
            instance = new AuctionService();
        }
        return instance;
    }

    /*
        Tạo auction.

        Cần transaction vì:
        - insert vào bảng auctions
        - insert vào bảng items

        Nếu lưu auction thành công nhưng lưu item lỗi
        thì phải rollback để database không bị lệch.
    */
    public void createAuction(Auction auction) {
        Connection connection = null;

        try {
            connection = DatabaseConnection.getConnection();
            connection.setAutoCommit(false);

            int id = auctionDAO.save(connection, auction);
            itemDAO.save(connection, auction, id);

            connection.commit();

        } catch (Exception e) {
            rollback(connection);
            throw new RuntimeException("Cannot create auction", e);

        } finally {
            closeConnection(connection);
        }
    }

    /*
        Xóa auction.

        Cần transaction vì:
        - delete bảng items
        - delete bảng auctions

        Có SELECT FOR UPDATE để khóa auction đang bị xóa,
        tránh trường hợp thread khác đang edit/close/bid cùng auction đó.
    */
    public void deleteAuction(int id) {
        Connection connection = null;

        try {
            connection = DatabaseConnection.getConnection();
            connection.setAutoCommit(false);

            Auction auction = auctionDAO.findByIdForUpdate(connection, id);

            if (auction == null) {
                throw new RuntimeException("Auction not found");
            }

            /*
                Nếu currentPrice > 0 nghĩa là đã có người bid.
                Thường không nên cho xóa auction đã có lịch sử bid.
            */
            if (auction.getCurrentPrice() > 0) {
                throw new RuntimeException("Cannot delete auction after bidding started");
            }

            itemDAO.delete(connection, id);
            auctionDAO.delete(connection, id);

            connection.commit();

        } catch (Exception e) {
            rollback(connection);
            throw new RuntimeException("Cannot delete auction", e);

        } finally {
            closeConnection(connection);
        }
    }

    /*
        Sửa auction.

        Cần transaction vì:
        - update bảng auctions
        - update bảng items
    */
    public void editAuction(Auction auction) {
        Connection connection = null;

        try {
            connection = DatabaseConnection.getConnection();
            connection.setAutoCommit(false);

            Auction oldAuction = auctionDAO.findByIdForUpdate(connection, auction.getId());

            if (oldAuction == null) {
                throw new RuntimeException("Auction not found");
            }

            /*
                Nếu đã có người bid thì không nên cho sửa auction nữa.
                Vì sửa startingPrice, time, status... sau khi có bid dễ sai nghiệp vụ.
            */
            if (oldAuction.getCurrentPrice() > 0) {
                throw new RuntimeException("Cannot edit auction after bidding started");
            }

            auctionDAO.update(connection, auction);
            itemDAO.update(connection, auction);

            connection.commit();

        } catch (Exception e) {
            rollback(connection);
            throw new RuntimeException("Cannot edit auction", e);

        } finally {
            closeConnection(connection);
        }
    }

    /*
        Tìm danh sách auction theo loại item.
        Chỉ đọc dữ liệu nên không cần transaction/lock.
    */
    public List<Auction> findbyItemType(ItemType type) {
        if (type == null) {
            throw new RuntimeException("Item type cannot be null");
        }

        List<Auction> auctions = new ArrayList<>();
        List<Item> items = itemDAO.findByType(type);

        for (Item item : items) {
            Auction auction = auctionDAO.findById(item.getId());

            if (auction != null) {
                auctions.add(auction);
            }
        }

        return auctions;
    }

    /*
        Tìm danh sách auction theo tên sản phẩm.
        Chỉ đọc dữ liệu nên không cần transaction/lock.
    */
    public List<Auction> findbyItemName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new RuntimeException("Item name cannot be empty");
        }

        List<Auction> auctions = new ArrayList<>();
        List<Item> items = itemDAO.findByItemName(name.trim());

        for (Item item : items) {
            Auction auction = auctionDAO.findById(item.getId());

            if (auction != null) {
                auctions.add(auction);
            }
        }

        return auctions;
    }

    public Auction getAuctionById(int id) {
        return auctionDAO.findById(id);
    }

    public List<Auction> getAllAuctions() {
        return auctionDAO.findAll();
    }

    public List<Auction> getMyAuctions(int sellerId) {
        return auctionDAO.findAllBySellerId(sellerId);
    }

    /*
        Đóng auction.

        Cần transaction + SELECT FOR UPDATE vì:
        - đọc auction
        - kiểm tra status
        - đổi status
        - update database

        Nếu không khóa, có thể xảy ra:
        - bidder đang đặt giá
        - seller đóng auction cùng lúc
    */
    public void closeAuction(int auctionId) {
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

            auction.setStatus(AuctionStatus.FINISHED);

            auctionDAO.update(connection, auction);

            connection.commit();

        } catch (Exception e) {
            rollback(connection);
            throw new RuntimeException("Cannot close auction", e);

        } finally {
            closeConnection(connection);
        }
    }

    public Electronics createElectronics(String name, String description,
                                         LocalDateTime startTime, LocalDateTime endTime) {
        return new Electronics(name, description, startTime, endTime);
    }

    public Art createArt(String name, String description,
                         LocalDateTime startTime, LocalDateTime endTime) {
        return new Art(name, description, startTime, endTime);
    }

    public Vehicle createVehicle(String name, String description,
                                 LocalDateTime startTime, LocalDateTime endTime) {
        return new Vehicle(name, description, startTime, endTime);
    }

    public Item getItemById(int id) {
        Item item = itemDAO.findById(id);

        if (item == null) {
            throw new RuntimeException("Item not found");
        }

        return item;
    }

    /*
        Hàm phụ để rollback cho gọn code.
    */
    private void rollback(Connection connection) {
        try {
            if (connection != null) {
                connection.rollback();
            }
        } catch (Exception e) {
            throw new RuntimeException("Rollback failed", e);
        }
    }

    /*
        Hàm phụ để đóng connection cho gọn code.
    */
    private void closeConnection(Connection connection) {
        try {
            if (connection != null) {
                connection.setAutoCommit(true);
                connection.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}