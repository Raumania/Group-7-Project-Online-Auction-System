package auction_system.server;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class Client {
    private Socket socket;
    private DataOutputStream out;

    public Client() {
        try {
            socket = new Socket("localhost",3030);
            out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
            Scanner sc = new Scanner(System.in);
            String s = sc.nextLine();
            out.writeUTF(s);
            out.flush();
            sc.close();
            out.close();
        }
        catch(IOException e) {
            e.printStackTrace();
        }

    }
    public static void main(String[] args) {
        new Client();
    }
}
