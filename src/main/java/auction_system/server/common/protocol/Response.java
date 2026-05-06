package auction_system.server.common.protocol;

public class Response {
    private String status;
    private String type;// SUCCESS / ERROR
    private String data;
    private String message;

    public Response() {}

    public Response(String status, String type, String data, String message) {
        this.status = status;
        this.type = type;
        this.data = data;
        this.message = message;
    }

    public String getStatus() { return status; }
    public String getData() { return data; }
    public String getMessage() { return message; }
}
