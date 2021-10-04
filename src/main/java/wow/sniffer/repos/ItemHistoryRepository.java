package wow.sniffer.repos;

import org.springframework.data.repository.CrudRepository;
import wow.sniffer.game.entity.ItemHistory;

public interface ItemHistoryRepository extends CrudRepository<ItemHistory, Integer> {
}
