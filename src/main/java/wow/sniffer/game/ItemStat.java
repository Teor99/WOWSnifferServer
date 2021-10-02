package wow.sniffer.game;

public class ItemStat {
    private int id;
    private int totalCount;
    private int auctionCount;
    private int minBuyout;

    public ItemStat(int id, int totalCount, int auctionCount, int minBuyout) {
        this.id = id;
        this.totalCount = totalCount;
        this.auctionCount = auctionCount;
        this.minBuyout = minBuyout;
    }

    public ItemStat(int id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return "id: " + id + " totalCount: " + totalCount + " auctionCount: " + auctionCount + " minBuyout: " + minBuyout;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }

    public void setAuctionCount(int auctionCount) {
        this.auctionCount = auctionCount;
    }

    public void setMinBuyout(int minBuyout) {
        this.minBuyout = minBuyout;
    }

    public int getId() {
        return id;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public int getAuctionCount() {
        return auctionCount;
    }

    public int getMinBuyout() {
        return minBuyout;
    }
}
