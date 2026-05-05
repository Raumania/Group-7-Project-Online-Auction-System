package auction_system.server.util;
import java.util.UUID;
public class IdGenerator {
    private IdGenerator(){}
    public static String generationId(String prefix){
        return prefix + " _ " + UUID.randomUUID().toString();
    }
    public static String generationAdminId(){
        return generationId("ADMIN");
    }
    public static String generationUserId(){
        return generationId("USER");
    }
    public static String generationSellerId(){
        return generationId("SELLER");
    }
    public static String generationItemId(){
        return generationId("ITEM");
    }
    public static String generationAuctionId(){
        return generationId("AUCTION");
    }
    public static String generationBidId(){
        return generationId("BID");
    }
    public static String generateEmtityId() {
        return UUID.randomUUID().toString();
    }
}