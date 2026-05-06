package auction_system.common.protocol;

import auction_system.client.model.Entity;

public class Request {
    private  MessageType type;
    private String data;

    public Request(MessageType type, String data) {
        this.type = type;
        this.data = data;
    }
}
