package auction_system.server.model;

public class Art extends Item {

    private String artist;
    private int year;

    public Art(String name, String description, double startingPrice,
               User owner, String artist, int year) {

        super(name, description, startingPrice, owner, ItemType.ART);

        if (artist == null || artist.trim().isEmpty()) {
            throw new RuntimeException("Artist cannot be empty");
        }

        if (year <= 0) {
            throw new RuntimeException("Year must be valid");
        }

        this.artist = artist;
        this.year = year;
    }

    public String getArtist() {
        return artist;
    }

    public int getYear() {
        return year;
    }

    @Override
    public String toString() {
        return super.toString() +
                ", artist='" + artist + '\'' +
                ", year=" + year;
    }
}