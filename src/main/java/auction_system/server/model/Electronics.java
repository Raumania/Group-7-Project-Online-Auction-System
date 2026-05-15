package auction_system.server.model;

import java.time.LocalDateTime;

public class Electronics extends Item {

    /*
        CẬP NHẬT: Loại bỏ brand và model để khớp với DB mới.
        Constructor bây giờ chỉ nhận các thông tin cơ bản và thời gian.
    */
    public Electronics(String name, String description, User owner,
                       LocalDateTime startingTime, LocalDateTime endingTime) {

        // Truyền thẳng type là ELECTRONICS vào lớp cha
        super(name, description, owner, ItemType.ELECTRONICS, startingTime, endingTime);
    }

    @Override
    public String toString() {
        // Tận dụng toString của Item để in ra ID, Name và Thời gian
        return "Electronics" + super.toString().substring(4);
    }
}