package auction_system.server.common.protocol;

import com.google.gson.JsonElement;

public class Request {
    private String type;
    private JsonElement data; // JSON string

    public Request() {}

    public Request(String type, JsonElement data) {
        this.type = type;
        this.data = data;
    }

    public String getAction() { return type; }
    public JsonElement getData() { return data; }
}