package auction_system.server.common.protocol;

public class Request {
    private String action;
    private String data; // JSON string

    public Request() {}

    public Request(String action, String data) {
        this.action = action;
        this.data = data;
    }

    public String getAction() { return action; }
    public String getData() { return data; }
}