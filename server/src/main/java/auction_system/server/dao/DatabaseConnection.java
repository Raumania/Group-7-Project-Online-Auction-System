package auction_system.server.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {

    private static final String DEV_URL =
            "jdbc:mysql://db-aws-test.cbyq8kk8c2tm.ap-southeast-1.rds.amazonaws.com:3306/auction_system?useSSL=false&serverTimezone=UTC";

    private static final String TEST_URL =
            "jdbc:mysql://db-aws-test.cbyq8kk8c2tm.ap-southeast-1.rds.amazonaws.com:3306/auction_system_test?useSSL=false&serverTimezone=UTC";

    private static final String USERNAME = "admin";
    private static final String PASSWORD = "chuataidaxiu";

    private static boolean isRunningTest() {
        for (StackTraceElement element : Thread.currentThread().getStackTrace()) {
            if (element.getClassName().contains("org.junit") || element.getClassName().contains("org.testng")) {
                return true;
            }
        }
        return false;
    }

    public static Connection getConnection() throws SQLException {
        String url = isRunningTest() ? TEST_URL : DEV_URL;
        return DriverManager.getConnection(url, USERNAME, PASSWORD);
    }
}