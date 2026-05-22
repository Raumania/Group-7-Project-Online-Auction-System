package auction_system.server.model;

import auction_system.common.protocol.Request;
import auction_system.common.protocol.Response;

/**
 * Interface định nghĩa chuẩn xử lý request cho server.
 * Mỗi controller (LoginController, AuctionController, ...) sẽ implement interface này.
 *
 * Mục đích:
 * - Cho phép ClientHandler gọi xử lý một cách thống nhất.
 * - Dễ dàng thay đổi hoặc thêm mới các handler mà không ảnh hưởng đến phần đọc socket.
 */
public interface RequestHandler {

    /**
     * Xử lý một request và trả về response.
     * @param request đối tượng Request đã được parse từ JSON (chứa action và data)
     * @return Response object sẽ được chuyển thành JSON gửi về client
     */
    Response handle(Request request);
}