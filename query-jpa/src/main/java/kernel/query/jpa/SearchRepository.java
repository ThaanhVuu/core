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
import org.springframework.data.repository.NoRepositoryBean;

import java.util.List;

/**
 * Repository hỗ trợ tìm kiếm theo {@link SearchQuery}.
 *
 * <p>Repository của service kế thừa interface này để có sẵn hàm search, không cần tự khai báo hay viết implementation.
 * Việc dựng Specification, Sort và Pageable nằm hết ở đây; service chỉ resolve SearchSpec rồi gọi search.
 *
 * <pre>
 * public interface UserRepository extends JpaRepository&lt;UserEntity, Long&gt;, SearchRepository&lt;UserEntity&gt; {
 * }
 *
 * // trong service
 * public PageResult&lt;UserDto&gt; searchPublic(SearchRequest request) {
 *     SearchQuery query = USER_SEARCH.resolve(request).withCriterion("status", Operator.EQ, Status.ACTIVE);
 *     return userRepository.search(query).map(UserDto::from);
 * }
 * </pre>
 *
 * <p><b>Các hàm:</b>
 * <ul>
 *   <li>{@code search(query)}: tìm kiếm theo truy vấn đã được kiểm tra, trả về một trang entity</li>
 *   <li>{@code search(query, scope)}: dựng Specification và Pageable, gọi findAll, đổi Page của Spring thành PageResult</li>
 *   <li>{@code toSort(query)}: dịch sort của kernel sang Sort của Spring</li>
 *   <li>{@code toPageable(query)}: dịch page, limit và sort của kernel sang Pageable của Spring</li>
 * </ul>
 *
 * @param <E> kiểu entity
 */
@NoRepositoryBean
public interface SearchRepository<E> extends JpaSpecificationExecutor<E> {

    /**
     * Tìm kiếm theo truy vấn đã được kiểm tra.
     *
     * @param query truy vấn
     * @return một trang entity
     */
    default PageResult<E> search(SearchQuery query) {
        return search(query, null);
    }

    /**
     * Tìm kiếm kèm điều kiện bổ sung phức tạp (OR, subquery...) không diễn đạt được bằng Criterion.
     *
     * @param query truy vấn
     * @param scope điều kiện bổ sung ghép bằng AND, có thể null
     * @return một trang entity
     */
    default PageResult<E> search(SearchQuery query, Specification<E> scope) {
        Specification<E> specification = JpaSpecifications.of(query.criteria(), scope);
        Page<E> page = findAll(specification, toPageable(query));
        return PageResult.of(page.getContent(), page.getTotalElements(), query.page(), query.limit());
    }

    /** Dịch sort của kernel sang Sort của Spring. */
    private static Sort toSort(SearchQuery query) {
        List<Sort.Order> orders = query.sort().stream()
                .map(order -> new Sort.Order(
                        order.direction() == SortDirection.DESC ? Sort.Direction.DESC : Sort.Direction.ASC,
                        order.field()))
                .toList();
        return Sort.by(orders);
    }

    /** Dịch page, limit và sort của kernel sang Pageable của Spring. */
    private static Pageable toPageable(SearchQuery query) {
        return PageRequest.of(query.page(), query.limit(), toSort(query));
    }
}
