package wow.sniffer.game;

import java.util.List;

public class Character {
    private final long guid;
    private final String name;
    private final byte raceCode;
    private final byte classCode;
    private final byte level;
    private List<Integer> spellList;

    public Character(long guid, String name, byte raceCode, byte classCode, byte level) {
        this.guid = guid;
        this.name = name;
        this.raceCode = raceCode;
        this.classCode = classCode;
        this.level = level;
    }

    @Override
    public String toString() {
        return "guid: " + Long.toHexString(guid) + " name: " +  name + " race: " + getRaceName() + " class: " + getClassName() + " lvl: " + getLevel();
    }

    public List<Integer> getSpellList() {
        return spellList;
    }

    public void setSpellList(List<Integer> spellList) {
        this.spellList = spellList;
    }

    public byte getLevel() {
        return level;
    }

    public long getGuid() {
        return guid;
    }

    public String getName() {
        return name;
    }

    public byte getRaceCode() {
        return raceCode;
    }

    public byte getClassCode() {
        return classCode;
    }

    public String getRaceName() {
        return Race.values()[raceCode].name();
    }

    public String getClassName() {
        return CharClass.values()[classCode].name();
    }

    public enum CharClass {
        None,
        Warrior,
        Paladin,
        Hunter,
        Rogue,
        Priest,
        DeathKnight,
        Shaman,
        Mage,
        Warlock,
        Monk,
        Druid,
        DemonHunter
    }

    public enum Race {
        None,
        Human,
        Orc,
        Dwarf,
        NightElf,
        Undead,
        Tauren,
        Gnome,
        Troll,
        Goblin,
        BloodElf,
        Draenei,
        FelOrc,
        Naga,
        Broken,
        Skeleton,
        Vrykul,
        Tuskarr,
        ForestTroll,
        Taunka,
        NorthrendSkeleton,
        IceTroll,
        Worgen,
        Gilnean,
        PandarenNeutral,
        PandarenAlliance,
        PandarenHorde,
        Nightborne,
        HighmountainTauren,
        VoidElf,
        LightforgedDraenei,
        ZandalariTroll,
        KulTiran,
        ThinHuman,
        DarkIronDwarf,
        Vulpera,
        MagharOrc,
        Mechagnome
    }
}
