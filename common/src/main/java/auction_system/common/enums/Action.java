package auction_system.common.enums;

public enum Action {

    CREATE_AUCTION,DELETE_AUCTION,EDIT_AUCTION,CANCEL_AUCTION,

    /** Lấy danh sách tất cả phiên đấu giá */
    GET_ALL,

    /** Lấy danh sách các phiên đấu giá đang mở (chưa kết thúc) */
    GET_OPEN_AUCTIONS,
    GET_ALL_AUCTIONS,
    /** Lấy chi tiết một phiên đấu giá theo ID */
    GET_AUCTION_DETAIL,

    /** Đóng một phiên đấu giá (kết thúc sớm) */
    CLOSE_AUCTION,

    /** Xóa một phiên đấu giá */
    REMOVE_AUCTION,

    // ==================== ĐẤU GIÁ ====================
    /** Đặt giá vào một phiên đấu giá */
    PLACE_BID,

    /** Lấy lịch sử đấu giá của một phiên */
    GET_BID_HISTORY,

    // ==================== XÁC THỰC ====================
    /** Đăng nhập */
    LOGIN,
    /**Gửi ảnh của user về lưu */
    SENDING_IMAGES,
    /** Đăng ký tài khoản mới */
    REGISTER,

    /** Đăng xuất */
    LOGOUT,

    /** ĐĂNG KÝ NHẬN THÔNG BÁO */
    SUBSCRIBE,
    /**Tim Auction*/
    SEARCH_AUCTION,
    /**Lọc Status*/
    FILTER_STATUS,
    /**Lọc loại item*/
    FILTER_CATEGORY,
    /**Lọc theo một caí gì đó*/
    FILTER_SORT_BY,


    // ==================== QUẢN LÝ NGƯỜI DÙNG ====================
    /** Lấy thông tin người dùng hiện tại */
    GET_CURRENT_USER,

    /** Cập nhật thông tin người dùng */
    UPDATE_USER,

    /** Lấy tất cả người dùng */
    GET_ALL_USERS,

    /** Tạo người dùng mới */
    CREATE_USER,

    /** Xóa người dùng */
    DELETE_USER,

    // ==================== QUẢN LÝ SẢN PHẨM (Item) ====================
    /** Lấy danh sách sản phẩm của seller */
    GET_SELLER_ITEMS,
    // ==================== TIỆN ÍCH ====================
    /** Ping để kiểm tra kết nối */
    PING,
    // Event from server to all clients
    EVENT_NEW_AUCTION_ADDED,
    EVENT_AUCTION_EDITED,
    EVENT_AUCTION_DELETED,
    //AI Chat
    CHAT_AI,
    //Auto Bid
    SET_AUTO_BID,
    CANCEL_AUTO_BID,
    // Real-time event: Server broadcasts when a new bid is placed successfully
    EVENT_BID_PLACED
}