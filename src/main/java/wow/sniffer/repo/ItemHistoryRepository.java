package wow.sniffer.repo;

import org.springframework.data.repository.CrudRepository;
import wow.sniffer.entity.ItemHistory;

public interface ItemHistoryRepository extends CrudRepository<ItemHistory, Integer> {
}
