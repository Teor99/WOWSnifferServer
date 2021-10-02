package wow.sniffer.game;

public class AuctionRecord {
    private final int id;
    private final int count;
    private final int buyout;

    public AuctionRecord(int id, int count, int buyout) {
        this.id = id;
        this.count = count;
        this.buyout = buyout;
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
