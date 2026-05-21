package auction_system.common.protocol;

import auction_system.common.enums.Action;
import com.google.gson.JsonElement;

public class Request {
    private Action type;
    private JsonElement data; // JSON string

    public Request() {}

    public Request(Action action, JsonElement data) {
        this.type = action;
        this.data = data;
    }

    public Action getAction() { return type; }
    public JsonElement getData() { return data; }
}