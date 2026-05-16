package auction_system.common.protocol;

public class Request<T> {
    private  MessageType type;
    private T data;

    public Request(MessageType type, T data) {
        this.type = type;
        this.data = data;
    }
}
