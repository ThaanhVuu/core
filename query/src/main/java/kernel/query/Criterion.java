package kernel.query;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Một điều kiện lọc đã được kiểm tra.
 *
 * <p><b>Các hàm:</b>
 * <ul>
 *   <li>{@code Criterion(...)}: constructor gọn: kiểm tra field, operator, value không null; với IN thì bắt buộc value là Collection và sao chép thành danh sách bất biến</li>
 * </ul>
 *
 * @param field    tên thuộc tính, được phép dạng lồng "department.name"
 * @param operator phép so sánh
 * @param value    giá trị đã đúng kiểu; với {@link Operator#IN} là một danh sách bất biến
 */
public record Criterion(String field, Operator operator, Object value) {

    /**
     * Kiểm tra các thành phần không null và chuẩn hoá giá trị của {@link Operator#IN} thành danh sách bất biến.
     *
     * @throws NullPointerException     nếu có thành phần null
     * @throws IllegalArgumentException nếu operator là {@link Operator#IN} mà value không phải Collection
     */
    public Criterion {
        Objects.requireNonNull(field, "field must not be null");
        Objects.requireNonNull(operator, "operator must not be null");
        Objects.requireNonNull(value, "value must not be null");
        if (operator == Operator.IN) {
            if (!(value instanceof Collection<?> values)) {
                throw new IllegalArgumentException("Operator IN requires a Collection value");
            }
            value = List.copyOf(values);
        }
    }
}
