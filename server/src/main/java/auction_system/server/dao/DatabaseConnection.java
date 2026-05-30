package auction_system.server.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {

    // Đổi lại URL thành của localhost
    private static final String DEV_URL =
            "jdbc:mysql://localhost:3306/auction_system?useSSL=false&serverTimezone=UTC";

    private static final String TEST_URL =
            "jdbc:mysql://localhost:3306/auction_system_test?useSSL=false&serverTimezone=UTC";

    // Sửa mật khẩu tương ứng với MySQL trên máy bạn
    private static final String USERNAME = "root";
    private static final String PASSWORD = "123456";

    private static boolean testMode = false;

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
    }

    public static Connection getConnection() throws SQLException {
        String url = testMode ? TEST_URL : DEV_URL;
        return DriverManager.getConnection(url, USERNAME, PASSWORD);
    }
}