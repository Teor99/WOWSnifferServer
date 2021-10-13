package wow.sniffer.entity;

import javax.persistence.*;
import java.util.Set;

@Entity
public class GameCharacter {
    @Id
    private Long id;
    private String name;

    @ManyToMany
    @JoinTable(
            name = "game_character_spell",
            joinColumns = @JoinColumn(name = "char_id"),
            inverseJoinColumns = @JoinColumn(name = "spell_id"))
    private Set<Spell> spellSet;

    public GameCharacter() {
    }

    public GameCharacter(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    @Override
    public String toString() {
        return "guid: " + Long.toHexString(id) + " name: " + name;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Set<Spell> getSpellSet() {
        return spellSet;
    }

    public void setSpellSet(Set<Spell> spellSet) {
        this.spellSet = spellSet;
    }
}
