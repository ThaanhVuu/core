package kernel.query;

/**
 * Phép so sánh áp cho một bộ lọc.
 * Do server chọn khi khai báo {@link SearchSpec}, client không gửi.
 *
 * <p><b>Các hàm:</b>
 * <ul>
 *   <li>{@code isTextMatch()}: cho biết operator có phải phép so khớp chuỗi (CONTAINS, STARTS_WITH); Builder dùng để chặn khai báo trên thuộc tính không phải String</li>
 *   <li>{@code isComparison()}: cho biết operator có phải phép so sánh lớn nhỏ (GT, GTE, LT, LTE); Builder dùng để chặn khai báo trên kiểu không so sánh được</li>
 * </ul>
 */
public enum Operator {

    /** Bằng. */
    EQ,

    /** Khác. */
    NE,

    /** Chứa chuỗi con, không phân biệt hoa thường. Chỉ dùng với {@code String}. */
    CONTAINS,

    /** Bắt đầu bằng, không phân biệt hoa thường. Chỉ dùng với {@code String}. */
    STARTS_WITH,

    /** Lớn hơn. Chỉ dùng với kiểu so sánh được. */
    GT,

    /** Lớn hơn hoặc bằng. Chỉ dùng với kiểu so sánh được. */
    GTE,

    /** Nhỏ hơn. Chỉ dùng với kiểu so sánh được. */
    LT,

    /** Nhỏ hơn hoặc bằng. Chỉ dùng với kiểu so sánh được. */
    LTE,

    /** Thuộc danh sách. Client gửi nhiều giá trị cách nhau bởi dấu phẩy, ví dụ {@code "ACTIVE,LOCKED"}. */
    IN;

    /**
     * Operator có phải phép so khớp chuỗi hay không.
     *
     * @return {@code true} nếu là {@link #CONTAINS} hoặc {@link #STARTS_WITH}
     */
    boolean isTextMatch() {
        return this == CONTAINS || this == STARTS_WITH;
    }

    /**
     * Operator có phải phép so sánh lớn nhỏ hay không.
     *
     * @return {@code true} nếu là {@link #GT}, {@link #GTE}, {@link #LT} hoặc {@link #LTE}
     */
    boolean isComparison() {
        return this == GT || this == GTE || this == LT || this == LTE;
    }
}
