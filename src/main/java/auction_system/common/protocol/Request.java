package auction_system.common.protocol;

import auction_system.common.enums.Action;

public class Request<T> {
    private Action type;
    private T data;

    public Request(Action type, T data) {
        this.type = type;
        this.data = data;
    }
}
