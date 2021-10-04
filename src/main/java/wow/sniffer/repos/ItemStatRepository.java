package wow.sniffer.repos;

import org.springframework.data.repository.CrudRepository;
import wow.sniffer.game.entity.ItemStat;

public interface ItemStatRepository extends CrudRepository<ItemStat, Integer> {
}
