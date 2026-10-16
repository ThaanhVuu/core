package kernel.query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BaseRepository <T, ID>{
    Optional<T> findById(ID id);

    T save(T aggregate);

    PageResult<T> search(SearchQuery query);

    void delete(UUID uuid);
}
