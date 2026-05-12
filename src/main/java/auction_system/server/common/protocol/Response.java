package auction_system.server.common.protocol;

public class Response {
    private String status;
    private String type;// SUCCESS / ERROR
    private Object data;
    private String message;

    public Response() {}

    public Response(String status, String type, Object data, String message) {
        this.status = status;
        this.type = type;
        this.data = data;
        this.message = message;
    }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public Object getData() { return data; }
    public void setData(Object data) { this.data = data; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
