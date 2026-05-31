package auction_system.server.controller;

import auction_system.common.protocol.Request;
import auction_system.common.protocol.Response;

/**
 * Interface defining request handling standard for server.
 * Each controller (LoginController, AuctionController, ...) will implement this interface.
 *
 * Purpose:
 * - Allow ClientHandler to call processing uniformly.
 * - Easily change or add new handlers without affecting socket reading part.
 */
public interface RequestHandler {

    /**
     * Process a request and return response.
     * @param request Request object parsed from JSON (contains action and data)
     * @return Response object will be converted to JSON and sent to client
     */
    Response handle(Request request);
}