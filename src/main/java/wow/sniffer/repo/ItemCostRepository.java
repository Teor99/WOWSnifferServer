package wow.sniffer.repo;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.transaction.annotation.Transactional;
import wow.sniffer.entity.ItemCost;

public interface ItemCostRepository extends CrudRepository<ItemCost, Integer> {

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM item_cost WHERE last_scan IS NOT NULL AND TIMESTAMPDIFF(MINUTE, last_scan, now()) >= 60", nativeQuery = true)
    void removeOldRecords();
}
