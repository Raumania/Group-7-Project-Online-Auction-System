package auction_system.server.dao;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;

public class DatabaseConnection {

    // Đổi lại URL thành của localhost
    private static final String DEV_URL =
            "jdbc:mysql://localhost:3306/auction_system?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
    private static final String TEST_URL =
            "jdbc:mysql://localhost:3306/auction_system?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";

    // Sửa mật khẩu tương ứng với MySQL trên máy bạn
    private static final String USERNAME = "root";
    private static final String PASSWORD = "NCh200507@";

    private static boolean testMode = false;
    private static HikariDataSource dataSource;

    static {
        for (StackTraceElement element : Thread.currentThread().getStackTrace()) {
            if (element.getClassName().contains("org.junit") || element.getClassName().contains("org.testng")
                    || System.getProperty("testMode") != null) {
                testMode = true;
                break;
            }
        }
    }

    public static void setTestMode(boolean mode) {
        testMode = mode;
        // Nếu testMode thay đổi ở runtime, cần đóng pool cũ để tạo lại pool mới với URL tương ứng
        if (dataSource != null) {
            dataSource.close();
            dataSource = null;
        }
    }

    private static synchronized HikariDataSource getDataSource() {
        if (dataSource == null) {
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(testMode ? TEST_URL : DEV_URL);
            config.setUsername(USERNAME);
            config.setPassword(PASSWORD);
            
            // Cấu hình Connection Pool với tối đa 20 connection như yêu cầu
            config.setMaximumPoolSize(20);
            config.setMinimumIdle(5);
            config.setIdleTimeout(30000);
            config.setConnectionTimeout(20000);

            // Một số tuỳ chọn tối ưu thêm của HikariCP cho MySQL
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

            dataSource = new HikariDataSource(config);
        }
        return dataSource;
    }

    public static Connection getConnection() throws SQLException {
        return getDataSource().getConnection();
    }
}