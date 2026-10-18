package kernel.ddd;

import java.util.Optional;

public interface AggregateRepository<T extends AggregateRoot<ID>, ID> {
    Optional<T> findById(ID id);

    T save(T aggregate);

    void delete(ID id);
}