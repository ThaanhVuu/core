package kernel.query.jpa;

import kernel.query.PageResult;
import kernel.query.SearchQuery;
import kernel.query.SortDirection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

/**
 * Chạy một {@link SearchQuery} trên repository JPA.
 * Dùng trực tiếp khi service không kế thừa được {@link BaseSearchService}.
 *
 * <p><b>Các hàm:</b>
 * <ul>
 *   <li>{@code execute(repository, query)}: chạy truy vấn không có điều kiện bổ sung</li>
 *   <li>{@code execute(repository, query, scope)}: dựng Specification và Pageable, gọi repository.findAll, đổi Page của Spring thành PageResult</li>
 *   <li>{@code toPageable(query)}: dịch page, limit và sort của kernel sang Pageable của Spring</li>
 * </ul>
 */
public final class JpaSearch {

    private JpaSearch() {
    }

    /**
     * Chạy truy vấn không có điều kiện bổ sung.
     *
     * @param repository repository hỗ trợ Specification
     * @param query      truy vấn đã được kiểm tra
     * @param <E>        kiểu entity
     * @return một trang kết quả
     */
    public static <E> PageResult<E> execute(JpaSpecificationExecutor<E> repository, SearchQuery query) {
        return execute(repository, query, null);
    }

    /**
     * Chạy truy vấn kèm điều kiện bổ sung.
     *
     * @param repository repository hỗ trợ Specification
     * @param query      truy vấn đã được kiểm tra
     * @param scope      điều kiện bổ sung ghép bằng AND, có thể null
     * @param <E>        kiểu entity
     * @return một trang kết quả
     */
    public static <E> PageResult<E> execute(JpaSpecificationExecutor<E> repository,
                                            SearchQuery query,
                                            Specification<E> scope) {
        Specification<E> specification = JpaSpecifications.of(query.criteria(), scope);
        Page<E> page = repository.findAll(specification, toPageable(query));
        return PageResult.of(page.getContent(), page.getTotalElements(), query.page(), query.limit());
    }

    /** Dịch page, limit và sort của kernel sang Pageable của Spring. */
    static Pageable toPageable(SearchQuery query) {
        List<Sort.Order> orders = query.sort().stream()
                .map(order -> new Sort.Order(
                        order.direction() == SortDirection.DESC ? Sort.Direction.DESC : Sort.Direction.ASC,
                        order.field()))
                .toList();
        return PageRequest.of(query.page(), query.limit(), Sort.by(orders));
    }
}
