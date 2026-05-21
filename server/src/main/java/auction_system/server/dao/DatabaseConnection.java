package auction_system.server.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {

    private static final String URL =
            "jdbc:mysql://db-aws-test.cbyq8kk8c2tm.ap-southeast-1.rds.amazonaws.com:3306/auction_system?useSSL=false&serverTimezone=UTC";

    private static final String USERNAME = "admin";
    private static final String PASSWORD = "chuataidaxiu";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USERNAME, PASSWORD);
    }
}