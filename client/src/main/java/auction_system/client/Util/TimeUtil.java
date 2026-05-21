package auction_system.client.Util;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class TimeUtil {

    /**
     * Tính toán thời gian còn lại (giờ, phút, giây) từ một thời điểm kết thúc cho trước.
     * @param endTime Thời điểm kết thúc.
     * @return Một Map chứa các key "hours", "minutes", "seconds" với giá trị tương ứng.
     *         Trả về 0 cho tất cả các giá trị nếu thời gian đã qua hoặc endTime là null.
     */
    public static Map<String, Long> getTimeRemaining(LocalDateTime endTime) {
        Map<String, Long> timeParts = new HashMap<>();
        
        if (endTime == null) {
            timeParts.put("hours", 0L);
            timeParts.put("minutes", 0L);
            timeParts.put("seconds", 0L);
            return timeParts;
        }

        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(endTime)) {
            timeParts.put("hours", 0L);
            timeParts.put("minutes", 0L);
            timeParts.put("seconds", 0L);
            return timeParts;
        }

        Duration duration = Duration.between(now, endTime);
        long totalSeconds = duration.getSeconds();

        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        timeParts.put("hours", hours);
        timeParts.put("minutes", minutes);
        timeParts.put("seconds", seconds);

        return timeParts;
    }
}
