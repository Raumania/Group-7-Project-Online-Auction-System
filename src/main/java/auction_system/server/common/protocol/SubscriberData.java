package auction_system.server.common.protocol;

public class SubscriberData {
    private String aunctionID;
    private String userID;

    public SubscriberData() {
        // Constructor rỗng cần cho Gson
    }

    public SubscriberData(String aunctionID, String userID) {
        this.aunctionID = aunctionID;
        this.userID = userID;
    }

    public String getAunctionID() {
        return aunctionID;
    }

    public void setAunctionID(String aunctionID) {
        this.aunctionID = aunctionID;
    }

    public String getUserID() {
        return userID;
    }

    public void setUserID(String userID) {
        this.userID = userID;
    }
}
