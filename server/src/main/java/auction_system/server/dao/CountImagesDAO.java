package auction_system.server.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class CountImagesDAO {

    public int generateNextImageId(Connection connection) throws SQLException {
        // Do not manage transactions here - let the outer transaction (AuctionService) manage it
        String updateQuery = "UPDATE image_counter SET current_count = current_count + 1";
        String selectQuery = "SELECT current_count FROM image_counter";

        try (PreparedStatement updateStmt = connection.prepareStatement(updateQuery)) {
            int affectedRows = updateStmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Cannot update counter. The image_counter table might be empty.");
            }
        }

        try (PreparedStatement selectStmt = connection.prepareStatement(selectQuery);
             ResultSet rs = selectStmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt("current_count");
            }
        }

        throw new SQLException("Cannot read image_counter value after update.");
    }
}