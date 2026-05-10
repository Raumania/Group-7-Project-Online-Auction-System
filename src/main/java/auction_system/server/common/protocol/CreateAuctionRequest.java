package auction_system.server.common.protocol;

public class CreateAuctionRequest {
    private String sellerId;
    private String itemType; // "ELECTRONICS", "ART", "VEHICLE"

    // Thông tin chung
    private String name;
    private String description;
    private double startingPrice;

    // Electronics
    private String brand;
    private String model;

    // Art
    private String artist;
    private int year;
    private String material;      // tuỳ chọn

    // Vehicle
    private String licensePlate;  // tuỳ chọn

    // Constructor
    public CreateAuctionRequest() {}

    // Getters & Setters
    public String getSellerId() { return sellerId; }
    public void setSellerId(String sellerId) { this.sellerId = sellerId; }

    public String getItemType() { return itemType; }
    public void setItemType(String itemType) { this.itemType = itemType; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public double getStartingPrice() { return startingPrice; }
    public void setStartingPrice(double startingPrice) { this.startingPrice = startingPrice; }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public String getArtist() { return artist; }
    public void setArtist(String artist) { this.artist = artist; }

    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }

    public String getMaterial() { return material; }
    public void setMaterial(String material) { this.material = material; }

    public String getLicensePlate() { return licensePlate; }
    public void setLicensePlate(String licensePlate) { this.licensePlate = licensePlate; }
}