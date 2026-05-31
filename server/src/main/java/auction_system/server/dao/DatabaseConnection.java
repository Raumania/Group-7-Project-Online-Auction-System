package auction_system.server.dao;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;

public class DatabaseConnection {

    // Change URL back to localhost
    private static final String DEV_URL =
            "jdbc:mysql://localhost:3306/auction_system?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
    private static final String TEST_URL =
            "jdbc:mysql://localhost:3306/auction_system?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";

    // Change the password to match MySQL on your machine
    private static final String USERNAME = "root";
    private static final String PASSWORD = "123456";

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
        // If testMode changes at runtime, close the old pool to recreate a new pool with the corresponding URL
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
            
            // Configure Connection Pool with a maximum of 20 connections as requested
            config.setMaximumPoolSize(20);
            config.setMinimumIdle(5);
            config.setIdleTimeout(30000);
            config.setConnectionTimeout(20000);

            // Some additional HikariCP optimization options for MySQL
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