package auction_system.server.store;

import auction_system.server.dao.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicInteger;

public class ImageCounterStore {
    private static ImageCounterStore instance;
    private final AtomicInteger currentCount = new AtomicInteger(0);

    private ImageCounterStore() {}

    public static synchronized ImageCounterStore getInstance() {
        if (instance == null) {
            instance = new ImageCounterStore();
        }
        return instance;
    }

    public synchronized void init() {
        String sql = "SELECT current_count FROM image_counter";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement statement = conn.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            if (rs.next()) {
                currentCount.set(rs.getInt("current_count"));
            } else {
                initializeCounterInDb();
            }
            System.out.println("[ImageCounterStore] Loaded current image count: " + currentCount.get());
        } catch (SQLException e) {
            System.err.println("[ImageCounterStore] Failed to load image count from DB: " + e.getMessage());
            currentCount.set(0);
        }
    }

    private void initializeCounterInDb() {
        String insertSql = "INSERT INTO image_counter (current_count) VALUES (0)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement statement = conn.prepareStatement(insertSql)) {
            statement.executeUpdate();
            currentCount.set(0);
        } catch (SQLException e) {
            System.err.println("[ImageCounterStore] Failed to initialize image count in DB: " + e.getMessage());
        }
    }

    public int getNextId() {
        int nextId = currentCount.incrementAndGet();
        // Write-through to database asynchronously to optimize performance
        new Thread(() -> {
            String updateSql = "UPDATE image_counter SET current_count = ?";
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement statement = conn.prepareStatement(updateSql)) {
                statement.setInt(1, nextId);
                statement.executeUpdate();
            } catch (SQLException e) {
                System.err.println("[ImageCounterStore] Failed to sync image count to DB: " + e.getMessage());
            }
        }, "image-counter-sync-thread").start();
        return nextId;
    }
}
