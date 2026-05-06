package auction_system;
import auction_system.server.common.protocol.Request;
import auction_system.server.common.protocol.Response;
import com.auction.network.LoginData;
import com.google.gson.Gson;
import java.io.*;
import java.net.Socket;

public class TestLogin {
    public static void main(String[] args) {
        String host = "localhost";
        int port = 3636;
        Gson gson = new Gson();

        // Tạo LoginData và chuyển thành JSON
        LoginData loginData = new LoginData("chuongque", "123478");
        System.out.println(loginData.getPassword());
        String jsonData = gson.toJson(loginData);
        // Tạo Request với action LOGIN
        Request request = new Request("LOGIN", jsonData);
        String jsonRequest = gson.toJson(request);

        try (Socket socket = new Socket(host, port);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            System.out.println("Send: " + jsonRequest);
            out.println(jsonRequest);
            String responseLine = in.readLine();
            System.out.println("Response: " + responseLine);

            // Nếu muốn parse response thành object Response
            Response response = gson.fromJson(responseLine, Response.class);
            if ("SUCCESS".equals(response.getStatus())) {
                System.out.println("Login OK - User: " + response.getData());
            } else {
                System.out.println("Login failed: " + response.getMessage());
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}