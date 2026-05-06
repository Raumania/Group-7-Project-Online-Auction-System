package auction_system;

import auction_system.server.common.protocol.*;
import auction_system.server.controller.RegisterController;
import com.google.gson.Gson;

public class TestRegisterController {

    public static void main(String[] args) {

        Gson gson = new Gson();

        // Tạo dữ liệu register giả
        RegisterData data = new RegisterData("datdz","123456","vtd@","SELLER");

        // Convert sang JSON
        String jsonData = gson.toJson(data);

        // Tạo request giả
        Request request = new Request(
                Action.REGISTER,
                jsonData
        );

        // Gọi controller
        RegisterController controller = new RegisterController();
        Response response = controller.handle(request);

        // In kết quả
        System.out.println("Status: " + response.getStatus());
        System.out.println("Message: " + response.getMessage());
        System.out.println("Data: " + response.getData());
    }
}