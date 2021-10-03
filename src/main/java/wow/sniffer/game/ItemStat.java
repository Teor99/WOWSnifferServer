package wow.sniffer.game;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import java.util.Date;

//@IdClass(ItemStat.class)
//@Table(indexes = @Index(columnList = "id, faction"))
@Entity
@IdClass(ItemStatId.class)
public class ItemStat {

    @Id
    private Integer id;
    @Id
    private String faction;

    @Column(columnDefinition = "TIMESTAMP")
    private Date timestamp;
    private int totalCount;
    private int auctionCount;
    private int minBuyout;

    public ItemStat() {
    }

    public ItemStat(int id, String faction, Date timestamp, int totalCount, int auctionCount, int minBuyout) {
        this.id = id;
        this.faction = faction;
        this.timestamp = timestamp;
        this.totalCount = totalCount;
        this.auctionCount = auctionCount;
        this.minBuyout = minBuyout;
    }

    public ItemStat(int id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return     "id: " + id
                + " faction: " + faction
                + " date: " + timestamp
                + " totalCount: " + totalCount
                + " auctionCount: " + auctionCount
                + " minBuyout: " + minBuyout;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getFaction() {
        return faction;
    }

    public void setFaction(String faction) {
        this.faction = faction;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
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


