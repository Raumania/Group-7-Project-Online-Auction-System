package auction_system.server;

import auction_system.server.dao.UserDAO;
import auction_system.server.model.User;
import auction_system.server.observer.EventBus;
import auction_system.server.service.BidService;
import auction_system.server.service.NotificationService;
import auction_system.server.service.UserService;

import java.util.Objects;
import java.util.Scanner;

public class EventBusTest {
    UserDAO userDAO = UserDAO.getInstance();
    NotificationService notificationService = NotificationService.getInstance();
    BidService bidService = BidService.getInstance();
    UserService userService = UserService.getInstance();
    void main() {
        User rauma  = userDAO.findById(1);//au ID: 6
        System.out.println(rauma.getUsername());
        notificationService.register(6,1);
        System.out.println(NotificationService.auctions);
        userService.deposit(1, 1000000);
        System.out.println(rauma.getBalance());

        EventBus bus = new EventBus();

        Scanner scanner = new Scanner(System.in);

        while (true) {
            String line = scanner.nextLine();

            if (Objects.equals(line, "")) {
                break;
            } else {
                String[] parts = line.split(" ");
                int auctionID = Integer.parseInt(parts[0]);
                double amount = Double.parseDouble(parts[1]);
                try {
                    bidService.placeBid(auctionID, rauma, amount);
                } catch (Exception e) {
                    System.err.println(e.getMessage());
                }
            }
        }
        bus.shutdown();
    }
}
