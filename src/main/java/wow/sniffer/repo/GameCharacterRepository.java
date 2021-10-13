package wow.sniffer.repo;

import org.springframework.data.repository.CrudRepository;
import wow.sniffer.entity.GameCharacter;

public interface GameCharacterRepository extends CrudRepository<GameCharacter, Long> {
}
