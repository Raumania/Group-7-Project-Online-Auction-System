package auction_system.server.dao;

import auction_system.Utils.CryptoUtils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserDAO {
    private static final String url = "jdbc:mysql://gateway01.ap-southeast-1.prod.aws.tidbcloud.com:4000/auction_system";
    private static  final String user = "2aemHQYXMU5q6pN.root";
    private static final String pass ="pW5zrksza2TW0kmk";

    public static boolean checkLogin(String username, String password) {
        String sql = "SELECT password FROM users where username = ?";
        try (Connection conn = DriverManager.getConnection(url,user,pass); java.sql.PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1,username);
            try(ResultSet result = statement.executeQuery()) {
                if(result.next()) {
                    String passwordHash =  result.getString("password");
                    return CryptoUtils.verify(password,passwordHash);
                }
                else return false;
            }
        } catch (SQLException e) {
            return false;
        }
    }

    public static boolean createUser(String fullname, String username, String password) {
        String sql = "INSERT INTO users (fullname, username, password) VALUES (?,?,?)";
        try (Connection conn = DriverManager.getConnection(url,user,pass);java.sql.PreparedStatement statement = conn.prepareStatement(sql) ) {
            statement.setString(1,fullname);
            statement.setString(2,username);
            statement.setString(3, CryptoUtils.hash(password));
            int rowsEffect = statement.executeUpdate();
            return rowsEffect > 0;
        }
        catch(SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}
