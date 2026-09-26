package kernel.query;

import java.util.Objects;

/**
 * Một tiêu chí sắp xếp đã được kiểm tra.
 *
 * <p><b>Các hàm:</b>
 * <ul>
 *   <li>{@code SortOrder(...)}: constructor gọn: kiểm tra field và direction không null</li>
 * </ul>
 *
 * @param field     tên thuộc tính
 * @param direction chiều sắp xếp
 */
public record SortOrder(String field, SortDirection direction) {

    /**
     * Kiểm tra các thành phần không null.
     *
     * @throws NullPointerException nếu có thành phần null
     */
    public SortOrder {
        Objects.requireNonNull(field, "field must not be null");
        Objects.requireNonNull(direction, "direction must not be null");
    }
}
