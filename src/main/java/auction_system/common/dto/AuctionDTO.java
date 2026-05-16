package auction_system.common.dto;

import auction_system.common.enums.Auction;
import auction_system.common.enums.ItemType;
import auction_system.common.protocol.MessageType;

import java.time.LocalDateTime;

public class AuctionDTO {
    private int id;
    private int sellerId;
    private String name;
    private String description;
    private ItemType type;
    private String status;
    private double startingPrice;
    private double currentPrice;
    private int highestBidderId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;


    public AuctionDTO(String name, String description, ItemType type, int sellerId, double startingPrice, LocalDateTime startTime, LocalDateTime endTime ) {
        this.name = name;
        this.description = description;
        this.type = type;
        this.sellerId = sellerId;
        this.startingPrice = startingPrice;
        this.startTime = startTime;
        this.endTime = endTime;
    }


}