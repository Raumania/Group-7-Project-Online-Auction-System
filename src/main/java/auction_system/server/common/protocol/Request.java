package auction_system.server.common.protocol;

public class Request {
    private String type;
    private String data; // JSON string

    public Request() {}

    public Request(String type, String data) {
        this.type = type;
        this.data = data;
    }

    public String getAction() { return type; }
    public String getData() { return data; }
}