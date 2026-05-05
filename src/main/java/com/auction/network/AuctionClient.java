package com.auction.network;

import java.io.*;
import java.net.Socket;

public class AuctionClient {
    private Socket socket;
    private ObjectOutputStream outputStream;
    private ObjectInputStream inputStream;

    public void connect(String host, int port) throws IOException {
        socket = new Socket(host, port);
        outputStream = new ObjectOutputStream(socket.getOutputStream());
        inputStream = new ObjectInputStream(socket.getInputStream());
        System.out.println("Connected to Auction Server");
    }

    public Response sendRequest(Request request) throws IOException, ClassNotFoundException {
        outputStream.writeObject(request);
        outputStream.flush();
        return (Response) inputStream.readObject();
    }

    // --- Các phương thức tiện ích cho từng nghiệp vụ ---
    public Response login(String username, String password) throws IOException, ClassNotFoundException {
        LoginData loginData = new LoginData(username, password);
        Request request = new Request("LOGIN", loginData);
        return sendRequest(request);
    }

    public Response placeBid(String auctionId, double amount, String bidderId) throws IOException, ClassNotFoundException {
        BidData bidData = new BidData(auctionId, amount, bidderId);
        Request request = new Request("PLACE_BID", bidData);
        return sendRequest(request);
    }

    public void disconnect() throws IOException {
        if (socket != null && !socket.isClosed()) {
            socket.close();
            System.out.println("Disconnected from Auction Server");
        }
    }
}