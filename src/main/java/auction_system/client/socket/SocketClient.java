package auction_system.client.socket;

import auction_system.common.protocol.Request;
import auction_system.common.protocol.Response;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.net.Socket;

public class SocketClient {
    private static SocketClient instance;
    private SocketClient() {};

    public static SocketClient getInstance() {
        if(instance == null) {
            instance = new SocketClient();
        }
        return instance;
    }

    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private Gson gson = new Gson();

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

    public void send(Request request) {
        try {
            String json = gson.toJson(request);
            out.writeUTF(json);
            out.flush();
        }
        catch(IOException e) {
            e.printStackTrace();
        }
    }

    public <T> Response<T> receive() {
        try {
            String json = in.readUTF();
            return gson.fromJson(json, new TypeToken<Response<T>>(){}.getType());
        }
        catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

}
