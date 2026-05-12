package auction_system;

import auction_system.server.common.protocol.*;
import auction_system.server.model.User;
import com.google.gson.Gson;
import java.io.*;
import java.net.Socket;

public class TestLogin {
    public static void main(String[] args) {
        String host = "localhost";
        int port = 3636;
        Gson gson = new Gson();

        // Tạo LoginData object
        LoginData loginData = new LoginData("chuongque", "123456");
        // Tạo Request với data là object (quan trọng: không gọi gson.toJson trên loginData)
        Request request = new Request("LOGIN", loginData);
        String jsonRequest = gson.toJson(request);
        System.out.println("Sending JSON: " + jsonRequest);
        // Kết quả mong muốn: {"action":"LOGIN","data":{"username":"chuongque","password":"123456"}}
        // Nếu thấy data là string có dấu \", thì sai.

        try (Socket socket = new Socket(host, port);
             DataOutputStream out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
             DataInputStream in = new DataInputStream(new BufferedInputStream(socket.getInputStream()))) {

            out.writeUTF(jsonRequest);
            out.flush();

            String responseLine = in.readUTF();
            System.out.println("Response: " + responseLine);

            Response response = gson.fromJson(responseLine, Response.class);
            if ("SUCCESS".equals(response.getStatus())) {
                // Chuyển data (LinkedTreeMap) thành User
                String userJson = gson.toJson(response.getData());
                User user = gson.fromJson(userJson, User.class);
                System.out.println("Login OK - Username: " + user.getUsername());
            } else {
                System.out.println("Login failed: " + response.getMessage());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}