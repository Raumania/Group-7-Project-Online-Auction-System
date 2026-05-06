package auction_system.client;

import auction_system.server.common.protocol.Action;
import auction_system.server.common.protocol.Request;
import com.google.gson.Gson;
import java.io.*;
import java.net.Socket;

public class MultiClientTest {

    private static final int NUM_CLIENTS = 30;      // Số lượng client (nhiều hơn pool size)
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 1234;

    public static void main(String[] args) {
        Gson gson = new Gson();
        System.out.println("Đang gửi " + NUM_CLIENTS + " request đồng thời đến server...");

        for (int i = 0; i < NUM_CLIENTS; i++) {
            final int clientId = i;
            // Mỗi client chạy trong một thread riêng
            new Thread(() -> {
                try (Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
                     PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                     BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

                    // Gửi request GET_ALL
                    Request request = new Request(Action.GET_ALL, null);
                    out.println(gson.toJson(request));

                    // Nhận phản hồi
                    String response = in.readLine();
                    System.out.println("Client " + clientId + " (thread: " + Thread.currentThread().getName()
                            + ") nhận được response: " + response.substring(0, Math.min(80, response.length())) + "...");

                } catch (IOException e) {
                    System.err.println("Client " + clientId + " lỗi: " + e.getMessage());
                }
            }).start();

            // Không có sleep lớn: tất cả client gần như đồng thời
        }
    }
}