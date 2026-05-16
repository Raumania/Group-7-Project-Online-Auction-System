package auction_system.client.socket;

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
    
    private Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, (JsonSerializer<LocalDateTime>) (src, type, context) -> 
                    new JsonPrimitive(src.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)))
            .registerTypeAdapter(LocalDateTime.class, (JsonDeserializer<LocalDateTime>) (json, type, context) -> 
                    LocalDateTime.parse(json.getAsString(), DateTimeFormatter.ISO_LOCAL_DATE_TIME))
            .create();

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

    public <T> void send(Request<T> request) {
        try {
            String json = gson.toJson(request);
            out.writeUTF(json);
            out.flush();
            System.out.println(json);
        }
        catch(IOException e) {
            e.printStackTrace();
        }
    }

    public Response receive() {
        try {
            String json = in.readUTF();
            return gson.fromJson(json,Response.class);
        }
        catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

}