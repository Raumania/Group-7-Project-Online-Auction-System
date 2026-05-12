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

        // ĐÚNG: Gán object trực tiếp
        LoginData loginData = new LoginData("chuongque", "123456");
        Request request = new Request("LOGIN", gson.toJsonTree(loginData));
        String jsonRequest = gson.toJson(request);
        System.out.println("Sending: " + jsonRequest);

        try (Socket socket = new Socket(host, port);
             DataOutputStream out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
             DataInputStream in = new DataInputStream(new BufferedInputStream(socket.getInputStream()))) {

            out.writeUTF(jsonRequest);
            out.flush();

            String responseLine = in.readUTF();
            System.out.println("Response: " + responseLine);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}