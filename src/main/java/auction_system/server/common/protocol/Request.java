package auction_system.server.common.protocol;

public class Request {
    private String type;
    private Object data; // JSON string

    public Request() {}

    public Request(String type, Object data) {
        this.type = type;
        this.data = data;
    }

    public String getAction() { return type; }
    public Object getData() { return data; }
}