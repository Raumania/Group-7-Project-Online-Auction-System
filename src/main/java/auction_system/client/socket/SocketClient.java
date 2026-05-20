package auction_system.client.socket;

import auction_system.client.Util.GsonUtil;
import auction_system.common.protocol.Request;
import auction_system.common.protocol.Response;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;

import java.io.*;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class SocketClient {
    //Singleton for socket client
    private static SocketClient instance;
    private SocketClient() {};
    public static SocketClient getInstance() {
        if(instance == null) {
            instance = new SocketClient();
        }
        return instance;
    }

    //core of socket client in below
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;

    public void connect(String URL, int PORT) {
        try {
            socket = new Socket(URL,PORT);
            System.out.println("Connected to server");
            in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
        }
        catch(IOException e) {
            e.printStackTrace();
        }
    }

    private String readMessage(DataInputStream in) throws IOException {
        int length = in.readInt();

        byte[] data = new byte[length];
        in.readFully(data);

        return new String(data, "UTF-8");
    }

    private void writeMessage(DataOutputStream out, String message) throws IOException {
        byte[] data = message.getBytes("UTF-8");

        out.writeInt(data.length);
        out.write(data);
        out.flush();
    }

    public <T> void send(Request<T> request) {
        try {
            String json = GsonUtil.toJson(request);
            writeMessage(new DataOutputStream(new BufferedOutputStream(socket.getOutputStream())),json);
            out.flush();
            System.out.println(json);
        }
        catch(IOException e) {
            e.printStackTrace();
        }
    }

    public Response receive() {
        try {
            String json = readMessage(in);
            System.out.println(json);
            return GsonUtil.fromJson(json,Response.class);
        }
        catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

}