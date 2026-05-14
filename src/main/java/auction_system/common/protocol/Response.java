package auction_system.common.protocol;

import com.google.gson.JsonElement;

public class Response {
    private String status;
    private String type;
    private JsonElement data;

    public Response(String status, String type, JsonElement data) {
        this.status = status;
        this.type = type;
        this.data = data;
    }
    public String getStatus() {
        return status;
    }
    public String getType() { return type; }
    public JsonElement getData() {
        return data;
    }

}
