package kernel.query.jpa;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import kernel.query.Criterion;
import kernel.query.Operator;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

/**
 * Chuyển danh sách {@link Criterion} thành {@link Specification} của Spring Data JPA.
 *
 * <p><b>Các hàm:</b>
 * <ul>
 *   <li>{@code of(criteria, scope)}: tạo Specification ghép mọi điều kiện bằng AND; chỉ là công thức, Hibernate gọi khi dựng câu truy vấn và câu đếm</li>
 *   <li>{@code toPredicate(criterion, root, cb)}: dịch một Criterion sang Predicate theo operator</li>
 *   <li>{@code resolvePath(root, field)}: đi theo thuộc tính lồng: "department.name" thành root.get("department").get("name")</li>
 *   <li>{@code like(cb, path, pattern)}: so khớp chuỗi không phân biệt hoa thường bằng LIKE</li>
 *   <li>{@code compare(cb, operator, path, value)}: so sánh lớn nhỏ cho GT, GTE, LT, LTE</li>
 *   <li>{@code escapeLike(value)}: đổi sang chữ thường và escape %, _ và \ để client không chèn được wildcard</li>
 * </ul>
 */
public final class JpaSpecifications {

    private static final char LIKE_ESCAPE = '\\';

    private JpaSpecifications() {
    }

    /**
     * Tạo Specification ghép mọi điều kiện bằng AND.
     * Specification chỉ là công thức; Hibernate gọi nó khi dựng câu truy vấn và câu đếm.
     *
     * @param criteria các điều kiện đã được SearchSpec kiểm tra
     * @param scope    điều kiện bổ sung do service tự viết, có thể null
     * @param <E>      kiểu entity
     * @return Specification tương ứng; không có điều kiện nào thì không lọc
     */
    public static <E> Specification<E> of(List<Criterion> criteria, Specification<E> scope) {
        List<Criterion> copy = List.copyOf(criteria);
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            for (Criterion criterion : copy) {
                predicates.add(toPredicate(criterion, root, cb));
            }
            if (scope != null) {
                Predicate extra = scope.toPredicate(root, query, cb);
                if (extra != null) {
                    predicates.add(extra);
                }
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /** Dịch một điều kiện sang Predicate theo operator. */
    private static Predicate toPredicate(Criterion criterion, Root<?> root, CriteriaBuilder cb) {
        Path<Object> path = resolvePath(root, criterion.field());
        Object value = criterion.value();

        return switch (criterion.operator()) {
            case EQ -> cb.equal(path, value);
            case NE -> cb.notEqual(path, value);
            case CONTAINS -> like(cb, path, "%" + escapeLike(value) + "%");
            case STARTS_WITH -> like(cb, path, escapeLike(value) + "%");
            case GT, GTE, LT, LTE -> compare(cb, criterion.operator(), path, value);
            case IN -> path.in((Collection<?>) value);
        };
    }

    /** Đi theo thuộc tính lồng: "department.name" thành root.get("department").get("name"). */
    private static Path<Object> resolvePath(Root<?> root, String field) {
        Path<Object> path = null;
        for (String part : field.split("\\.")) {
            path = path == null ? root.get(part) : path.get(part);
        }
        return path;
    }

    /** So khớp chuỗi không phân biệt hoa thường. */
    @SuppressWarnings("unchecked")
    private static Predicate like(CriteriaBuilder cb, Path<?> path, String pattern) {
        Expression<String> text = (Expression<String>) path;
        return cb.like(cb.lower(text), pattern, LIKE_ESCAPE);
    }

    /**
     * So sánh lớn nhỏ. Dùng raw type vì kiểu thật chỉ biết lúc chạy;
     * SearchSpec đã đảm bảo thuộc tính là kiểu so sánh được.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Predicate compare(CriteriaBuilder cb, Operator operator, Path<?> path, Object value) {
        Expression expression = path;
        Comparable comparable = (Comparable) value;
        return switch (operator) {
            case GT -> cb.greaterThan(expression, comparable);
            case GTE -> cb.greaterThanOrEqualTo(expression, comparable);
            case LT -> cb.lessThan(expression, comparable);
            case LTE -> cb.lessThanOrEqualTo(expression, comparable);
            default -> throw new IllegalArgumentException("Not a comparison operator: " + operator);
        };
    }

    /**
     * Chuyển giá trị sang chữ thường và escape các ký tự đặc biệt của LIKE,
     * để %, _ và \ do client gửi được hiểu là ký tự thường.
     *
     * @param value giá trị client gửi
     * @return chuỗi đã escape
     */
    static String escapeLike(Object value) {
        return value.toString()
                .toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }
}
