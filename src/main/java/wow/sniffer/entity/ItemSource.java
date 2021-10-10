package wow.sniffer.entity;

import javax.persistence.Embeddable;
import java.io.Serializable;

@Embeddable
public class ItemSource implements Serializable {
    private Integer id;
    private String source;

    public ItemSource() {
    }

    public ItemSource(Integer id, String source) {
        this.id = id;
        this.source = source;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ItemSource that = (ItemSource) o;

        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        return source != null ? source.equals(that.source) : that.source == null;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (source != null ? source.hashCode() : 0);
        return result;
    }
}
