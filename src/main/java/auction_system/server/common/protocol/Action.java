package auction_system.server.common.protocol;

/**
 * Lớp chứa các hằng số action (lệnh) để giao tiếp giữa client và server.
 * Các action được sử dụng trong trường "action" của Request JSON.
 */
public final class Action {

    private Action() {
        // Private constructor để không thể khởi tạo instance
    }

    // ==================== QUẢN LÝ ĐẤU GIÁ ====================
    /** Tạo phiên đấu giá mới (seller) */
    public static final String CREATE_AUCTION = "CREATE_AUCTION";

    /** Lấy danh sách tất cả phiên đấu giá */
    public static final String GET_ALL = "GET_ALL";

    /** Lấy danh sách các phiên đấu giá đang mở (chưa kết thúc) */
    public static final String GET_OPEN_AUCTIONS = "GET_OPEN_AUCTIONS";
    public static final String GET_ALL_AUCTIONS = "GET_ALL_AUCTIONS";
    /** Lấy chi tiết một phiên đấu giá theo ID */
    public static final String GET_AUCTION_DETAIL = "GET_AUCTION_DETAIL";

    /** Đóng một phiên đấu giá (kết thúc sớm) */
    public static final String CLOSE_AUCTION = "CLOSE_AUCTION";

    /** Xóa một phiên đấu giá */
    public static final String REMOVE_AUCTION = "REMOVE_AUCTION";

    // ==================== ĐẤU GIÁ ====================
    /** Đặt giá vào một phiên đấu giá */
    public static final String PLACE_BID = "PLACE_BID";

    /** Lấy lịch sử đấu giá của một phiên */
    public static final String GET_BID_HISTORY = "GET_BID_HISTORY";

    // ==================== XÁC THỰC ====================
    /** Đăng nhập */
    public static final String LOGIN = "LOGIN";

    /** Đăng ký tài khoản mới */
    public static final String REGISTER = "REGISTER";

    /** Đăng xuất */
    public static final String LOGOUT = "LOGOUT";

    // ==================== QUẢN LÝ NGƯỜI DÙNG ====================
    /** Lấy thông tin người dùng hiện tại */
    public static final String GET_CURRENT_USER = "GET_CURRENT_USER";

    /** Cập nhật thông tin người dùng */
    public static final String UPDATE_USER = "UPDATE_USER";

    // ==================== QUẢN LÝ SẢN PHẨM (Item) ====================
    /** Lấy danh sách sản phẩm của seller */
    public static final String GET_SELLER_ITEMS = "GET_SELLER_ITEMS";

    /** Tạo sản phẩm mới (seller) */
    public static final String CREATE_ITEM = "CREATE_ITEM";

    /** Xóa sản phẩm */
    public static final String DELETE_ITEM = "DELETE_ITEM";

    // ==================== TIỆN ÍCH ====================
    /** Ping để kiểm tra kết nối */
    public static final String PING = "PING";
}