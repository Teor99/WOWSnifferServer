package wow.sniffer.repos;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.transaction.annotation.Transactional;
import wow.sniffer.game.entity.ItemStat;

public interface ItemStatRepository extends CrudRepository<ItemStat, Integer> {

    default void removeOldRecords() {
        removeOldAllianceRecords();
        removeOldHordeRecords();
        removeEmptyRecords();
    }

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM item_stat WHERE alliance_timestamp IS NULL AND horde_timestamp IS NULL", nativeQuery = true)
    void removeEmptyRecords();

    @Modifying
    @Transactional
    @Query(value = "UPDATE item_stat\n" +
            "SET alliance_timestamp = NULL,\n" +
            "    alliance_min_buyout = NULL,\n" +
            "    alliance_auction_count = NULL,\n" +
            "    alliance_total_count = NULL\n" +
            "WHERE TIMESTAMPDIFF(MINUTE, alliance_timestamp, now()) >= 60", nativeQuery = true)
    void removeOldAllianceRecords();

    @Modifying
    @Transactional
    @Query(value = "UPDATE item_stat\n" +
            "SET horde_timestamp = NULL,\n" +
            "    horde_min_buyout = NULL,\n" +
            "    horde_auction_count = NULL,\n" +
            "    horde_total_count = NULL\n" +
            "WHERE TIMESTAMPDIFF(MINUTE, horde_timestamp, now()) >= 60", nativeQuery = true)
    void removeOldHordeRecords();
}
