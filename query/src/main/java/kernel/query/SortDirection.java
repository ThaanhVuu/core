package kernel.query;

import java.util.Locale;

/**
 * Chiều sắp xếp.
 *
 * <p><b>Các hàm:</b>
 * <ul>
 *   <li>{@code parse(raw)}: đọc chiều sắp xếp từ chuỗi client gửi, không phân biệt hoa thường; null hoặc rỗng thành ASC; chuỗi lạ thì ném IllegalArgumentException</li>
 * </ul>
 */
public enum SortDirection {

    /** Tăng dần. */
    ASC,

    /** Giảm dần. */
    DESC;

    /**
     * Đọc chiều sắp xếp từ chuỗi client gửi, không phân biệt hoa thường.
     *
     * @param raw chuỗi client gửi, có thể null
     * @return chiều sắp xếp tương ứng; {@link #ASC} nếu chuỗi null hoặc rỗng
     * @throws IllegalArgumentException nếu chuỗi không phải "asc" hoặc "desc"
     */
    static SortDirection parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return ASC;
        }
        return switch (raw.trim().toUpperCase(Locale.ROOT)) {
            case "ASC" -> ASC;
            case "DESC" -> DESC;
            default -> throw new IllegalArgumentException("Invalid sort direction: " + raw);
        };
    }
}
