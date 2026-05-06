package auction_system.server;

import com.google.gson.Gson;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

public class Server {
    private ServerSocket serverSocket;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private final int PORT = 3636;
    private Gson gson;

    public Server() {
        try {
            serverSocket = new ServerSocket(PORT);
            socket = serverSocket.accept();
            System.out.println("Connected");
            in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
            while(true) {
                System.out.println("Client say: "+ in.readUTF());
                Scanner sc = new Scanner(System.in);
                String text = sc.nextLine();
                out.writeUTF(text);
                out.flush();
            }

        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        new Server();
    }

}
