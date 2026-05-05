package com.auction.network;

import java.io.*;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private Socket socket;
    private LoginData loginData;
    private ObjectInputStream inputStream;
    private ObjectOutputStream outputStream;

    public ClientHandler(Socket socket) {
        this.socket = socket;
        try {
            // Lưu ý: Luôn khởi tạo ObjectOutputStream trước để tránh deadlock
            outputStream = new ObjectOutputStream(socket.getOutputStream());
            inputStream = new ObjectInputStream(socket.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            // Vòng lặp liên tục đọc các request từ client
            while (true) {
                // Đọc đối tượng Request từ InputStream
                Request request = (Request) inputStream.readObject();

                // Xử lý request dựa trên command
                if (request.getCommand().equals("LOGIN")) {
                    // ... xử lý logic đăng nhập ...
                    // Tạo response và gửi lại cho client
                    Response response = Response.success(loginData);
                    outputStream.writeObject(response);
                    outputStream.flush();
                }
                else if (request.getCommand().equals("PLACE_BID")) {
                    // ... xử lý logic đặt giá ...
                }
                // ... các command khác
            }
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Client disconnected: " + socket.getInetAddress());
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}