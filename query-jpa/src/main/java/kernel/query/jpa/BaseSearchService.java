package kernel.query.jpa;

import kernel.query.PageResult;
import kernel.query.SearchQuery;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * Lớp nền cho service tìm kiếm trên JPA.
 *
 * <p>Các method search để {@code protected} có chủ đích: lớp con phải mở ra từng method public
 * theo đúng use case (ví dụ searchPublic, searchForAdmin). Nhờ vậy điều kiện bắt buộc của từng
 * use case (chỉ lấy user ACTIVE...) và việc chuyển sang DTO nằm trong service, controller không gọi vòng qua được.
 *
 * <p><b>Các hàm:</b>
 * <ul>
 *   <li>{@code searchRepository()}: lớp con cung cấp repository dùng để tìm kiếm</li>
 *   <li>{@code search(query)}: tìm kiếm theo truy vấn đã được kiểm tra, trả về một trang entity</li>
 *   <li>{@code search(query, scope)}: như trên, kèm điều kiện bổ sung phức tạp (OR, subquery...) do service tự viết</li>
 * </ul>
 *
 * @param <E> kiểu entity
 */
public abstract class BaseSearchService<E> {

    /**
     * Repository dùng để tìm kiếm, do lớp con cung cấp.
     *
     * @return repository hỗ trợ Specification
     */
    protected abstract JpaSpecificationExecutor<E> searchRepository();

    /**
     * Tìm kiếm theo truy vấn đã được kiểm tra.
     *
     * @param query truy vấn
     * @return một trang entity
     */
    protected PageResult<E> search(SearchQuery query) {
        return JpaSearch.execute(searchRepository(), query);
    }

    /**
     * Tìm kiếm kèm điều kiện bổ sung phức tạp (OR, subquery...) không diễn đạt được bằng Criterion.
     *
     * @param query truy vấn
     * @param scope điều kiện bổ sung ghép bằng AND
     * @return một trang entity
     */
    protected PageResult<E> search(SearchQuery query, Specification<E> scope) {
        return JpaSearch.execute(searchRepository(), query, scope);
    }
}
