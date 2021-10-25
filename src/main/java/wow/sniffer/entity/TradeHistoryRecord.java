package wow.sniffer.entity;

import javax.persistence.*;
import java.util.Date;

@Entity
public class TradeHistoryRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer recordId;

    @Column(nullable = false)
    private Integer itemId;
    @Column(columnDefinition = "TIMESTAMP", nullable = false)
    private Date timestamp;

    @Column(length = 16, nullable = false)
    private String action;

    @Column(nullable = false)
    private Integer count;
    @Column(nullable = false)
    private Integer cost;

    public TradeHistoryRecord() {
    }

    public TradeHistoryRecord(Integer itemId, Date timestamp, String action, Integer count, Integer cost) {
        this.itemId = itemId;
        this.timestamp = timestamp;
        this.action = action;
        this.count = count;
        this.cost = cost;
    }

    public Integer getItemId() {
        return itemId;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public String getAction() {
        return action;
    }

    public Integer getCount() {
        return count;
    }

    public Integer getCost() {
        return cost;
    }
}
