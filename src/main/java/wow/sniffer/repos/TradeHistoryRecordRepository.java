package wow.sniffer.repos;

import org.springframework.data.repository.CrudRepository;
import wow.sniffer.game.entity.TradeHistoryRecord;

public interface TradeHistoryRecordRepository extends CrudRepository<TradeHistoryRecord, Integer> {
}
