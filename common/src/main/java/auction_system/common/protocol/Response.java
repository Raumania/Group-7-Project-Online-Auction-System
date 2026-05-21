package auction_system.common.protocol;

import auction_system.common.enums.Action;
import auction_system.common.enums.Status;

public class Response {

    private Status status;
    private Action type;
    private Object data;
    private String message;

    public Response() {}

    public Response(Status status, Action type, Object data, String message) {
        this.status = status;
        this.type = type;
        this.data = data;
        this.message = message;
    }
    public Response(Status status, String message, Object data) {
        this.status = status;
        this.message = message;
        this.data = data;
    }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public Action getType() { return type; }
    public void setType(Action type) { this.type = type; }
    public Object getData() { return data; }
    public void setData(Object data) { this.data = data; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}