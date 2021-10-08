package wow.sniffer.entity;

import javax.persistence.*;

@Entity
public class ItemStat {

    @Id
    private Integer id;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride( name = "timestamp", column = @Column(name = "alliance_timestamp", columnDefinition = "TIMESTAMP")),
            @AttributeOverride( name = "totalCount", column = @Column(name = "alliance_total_count")),
            @AttributeOverride( name = "auctionCount", column = @Column(name = "alliance_auction_count")),
            @AttributeOverride( name = "minBuyout", column = @Column(name = "alliance_min_buyout"))
    })
    private ItemAuctionInfo allianceAuctionInfo;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride( name = "timestamp", column = @Column(name = "horde_timestamp", columnDefinition = "TIMESTAMP")),
            @AttributeOverride( name = "totalCount", column = @Column(name = "horde_total_count")),
            @AttributeOverride( name = "auctionCount", column = @Column(name = "horde_auction_count")),
            @AttributeOverride( name = "minBuyout", column = @Column(name = "horde_min_buyout"))
    })
    private ItemAuctionInfo hordeAuctionInfo;

    public ItemStat() {
    }

    public ItemStat(int id) {
        this.id = id;
    }

    public Integer getId() {
        return id;
    }

    public ItemAuctionInfo getHordeAuctionInfo() {
        return hordeAuctionInfo;
    }

    public ItemAuctionInfo getAllianceAuctionInfo() {
        return allianceAuctionInfo;
    }

    public void setAllianceAuctionInfo(ItemAuctionInfo allianceAuctionInfo) {
        this.allianceAuctionInfo = allianceAuctionInfo;
    }

    public void setHordeAuctionInfo(ItemAuctionInfo hordeAuctionInfo) {
        this.hordeAuctionInfo = hordeAuctionInfo;
    }

}


