package wow.sniffer.repo;

import org.springframework.data.repository.CrudRepository;
import wow.sniffer.entity.Spell;

public interface SpellRepository extends CrudRepository<Spell, Integer> {
}
