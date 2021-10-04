package wow.sniffer.game;

import java.util.Date;

public class AuctionRecord {
    private final int id;
    private final Date timestamp;
    private final int count;
    private final int buyout;
    private final String factionName;

    public AuctionRecord(int id, Date timestamp, int count, int buyout, String factionName) {
        this.id = id;
        this.timestamp = timestamp;
        this.count = count;
        this.buyout = buyout;
        this.factionName = factionName;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public int getId() {
        return id;
    }

    public int getCount() {
        return count;
    }

    public int getBuyout() {
        return buyout;
    }

    public int getBuyoutPerItem() {
        return buyout / count;
    }
}
