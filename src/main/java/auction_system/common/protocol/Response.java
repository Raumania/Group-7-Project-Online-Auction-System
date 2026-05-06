package auction_system.common.protocol;

public class Response<T> {
    private String status;
    private String type;
    private String data;

    public Response(String status, String type, String data) {
        this.status = status;
        this.type = type;
        this.data = data;
    }
    public String getStatus() {
        return status;
    }
    public String getType() { return type; }
    public String getData() {
        return data;
    }

}
