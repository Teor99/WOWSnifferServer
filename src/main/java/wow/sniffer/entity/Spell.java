package wow.sniffer.entity;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class Spell {
    @Id
    private Integer id;
    private String name;

    public Spell(Integer id, String name) {
        this.id = id;
        this.name = name;
    }

    public Spell() {
    }
}
