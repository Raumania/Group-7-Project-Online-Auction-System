package auction_system.client.util;

import com.google.gson.*;

import java.lang.reflect.Type;
import java.time.LocalDateTime;

public class GsonUtil {

    // Sử dụng duy nhất một instance để tối ưu bộ nhớ
    private static final Gson GSON = new GsonBuilder()
            // 1. Adapter để ghi dữ liệu (Object -> JSON)
            .registerTypeAdapter(LocalDateTime.class, (JsonSerializer<LocalDateTime>) (src, typeOfSrc, context) ->
                    new JsonPrimitive(src.toString()))

            // 2. Adapter để đọc dữ liệu (JSON -> Object)
            .registerTypeAdapter(LocalDateTime.class, (JsonDeserializer<LocalDateTime>) (json, typeOfT, context) ->
                    LocalDateTime.parse(json.getAsString()))

            // Tùy chọn: Giúp JSON in ra đẹp hơn khi log để bạn dễ debug
            .setPrettyPrinting()
            .create();

    // Lấy đối tượng Gson đã cấu hình
    public static Gson getGson() {
        return GSON;
    }

    // Helper: Chuyển Object thành chuỗi JSON nhanh
    public static String toJson(Object src) {
        return GSON.toJson(src);
    }

    // Helper: Chuyển JSON thành Object nhanh
    public static <T> T fromJson(String json, Class<T> clazz) {
        return GSON.fromJson(json, clazz);
    }

    public static <T> T fromJson(String json, Type typeOfT) {
        return GSON.fromJson(json, typeOfT);
    }
    // Helper: Chuyển JsonElement thành Object nhanh
    public static <T> T fromJsonElement(JsonElement json, Class<T> clazz) {
        return GSON.fromJson(json, clazz);
    }
}