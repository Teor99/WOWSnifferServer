package wow.sniffer.repo;

import org.springframework.data.repository.CrudRepository;
import wow.sniffer.entity.TradeHistoryRecord;

public interface TradeHistoryRecordRepository extends CrudRepository<TradeHistoryRecord, Integer> {
}
