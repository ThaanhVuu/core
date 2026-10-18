package kernel.query;

public interface SearchPort<R> {
    PageResult<R> search(SearchQuery query);
}