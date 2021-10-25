package wow.sniffer.entity;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(indexes = {@Index(name = "item_id_index1", columnList = "id")})
public class ItemHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer recordId;
    @Column(nullable = false)
    private Integer id;
    @Column(nullable = false)
    private Integer minBuyout;
    @Column(columnDefinition = "TIMESTAMP", nullable = false)
    private Date timestamp;

    public ItemHistory() {
    }

    public ItemHistory(Integer id, Integer minBuyout, Date timestamp) {
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

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public Integer getMinBuyout() {
        return minBuyout;
    }

    public void setMinBuyout(Integer minBuyout) {
        this.minBuyout = minBuyout;
    }
}
