package wow.sniffer.repo;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import wow.sniffer.entity.ItemProfitAction;

public interface ItemProfitActionRepository extends CrudRepository<ItemProfitAction, Integer> {

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM item_profit_action WHERE id = :id", nativeQuery = true)
    void removeByItemId(@Param("id") Integer id);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM item_profit_action WHERE TIMESTAMPDIFF(MINUTE, record_timestamp, now()) >= 60", nativeQuery = true)
    void removeOldRecords();
}
