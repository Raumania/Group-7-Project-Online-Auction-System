import java.sql.*;

public class CheckDB {
    public static void main(String[] args) throws Exception {
        String url = "jdbc:mysql://db-aws-test.cbyq8kk8c2tm.ap-southeast-1.rds.amazonaws.com:3306/auction_system?useSSL=false&serverTimezone=UTC";
        String user = "admin";
        String pass = "chuataidaxiu";
        try (Connection c = DriverManager.getConnection(url, user, pass)) {
            Statement s = c.createStatement();
            ResultSet rs = s.executeQuery("SELECT username, available_balance, frozen_balance FROM users");
            while (rs.next()) {
                System.out.println(rs.getString("username") + " | " + rs.getBigDecimal("available_balance") + " | " + rs.getBigDecimal("frozen_balance"));
            }
        }
    }
}
