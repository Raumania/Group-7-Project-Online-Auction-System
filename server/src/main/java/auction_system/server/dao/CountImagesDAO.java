package auction_system.server.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class CountImagesDAO {

    public int generateNextImageId(Connection connection) throws SQLException {
        // Không tự quản lý transaction ở đây — để outer transaction (AuctionService) quản lý
        String updateQuery = "UPDATE image_counter SET current_count = current_count + 1";
        String selectQuery = "SELECT current_count FROM image_counter";

        try (PreparedStatement updateStmt = connection.prepareStatement(updateQuery)) {
            int affectedRows = updateStmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Không thể cập nhật biến đếm. Bảng image_counter có thể đang trống.");
            }
        }

        try (PreparedStatement selectStmt = connection.prepareStatement(selectQuery);
             ResultSet rs = selectStmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt("current_count");
            }
        }

        throw new SQLException("Không thể đọc giá trị image_counter sau khi update.");
    }
}