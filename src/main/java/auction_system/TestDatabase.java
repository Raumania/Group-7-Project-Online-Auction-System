package auction_system;

import auction_system.server.dao.DatabaseConnection;

import java.sql.Connection;

public class TestDatabase {
    public static void main(String[] args) {
        try {
            Connection connection = DatabaseConnection.getConnection();

            System.out.println("Connect database successfully!");

            connection.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}