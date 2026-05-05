package auction_system.server.protocol;

public class Response {
    private String status; // SUCCESS / ERROR
    private String data;
    private String message;

    public Response() {}

    public Response(String status, String data, String message) {
        this.status = status;
        this.data = data;
        this.message = message;
    }

    public String getStatus() { return status; }
    public String getData() { return data; }
    public String getMessage() { return message; }
}
