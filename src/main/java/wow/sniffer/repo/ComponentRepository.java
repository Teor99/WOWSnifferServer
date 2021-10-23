package wow.sniffer.repo;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import wow.sniffer.entity.Component;

@Repository
public interface ComponentRepository extends CrudRepository<Component, Integer> {
}
