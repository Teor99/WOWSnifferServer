package wow.sniffer.game.entity;

import com.sun.istack.NotNull;

import javax.persistence.*;
import java.util.Date;

@Entity
public class TradeHistoryRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer recordId;

    @NotNull
    private final Integer itemId;
    @NotNull
    @Column(columnDefinition = "TIMESTAMP")
    private final Date timestamp;

    @NotNull
    @Column(length = 16)
    private final String action;

    @NotNull
    private final Integer count;
    @NotNull
    private final Integer cost;

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
