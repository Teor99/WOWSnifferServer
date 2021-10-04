package wow.sniffer.game.entity;

import com.sun.istack.NotNull;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.Date;

@Entity
public class ItemHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer recordId;
    @NotNull
    private Integer id;
    @NotNull
    private Integer minBuyout;
    @NotNull
    @Column(columnDefinition = "TIMESTAMP")
    private Timestamp timestamp;

    public ItemHistory() {
    }

    public ItemHistory(Integer id, Integer minBuyout, Timestamp timestamp) {
        this.id = id;
        this.minBuyout = minBuyout;
        this.timestamp = timestamp;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    public Integer getMinBuyout() {
        return minBuyout;
    }

    public void setMinBuyout(Integer minBuyout) {
        this.minBuyout = minBuyout;
    }
}
