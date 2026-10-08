package kernel.query;

/**
 * Một bộ lọc thô client gửi lên, chưa được kiểm tra.
 *
 * <p>Chỉ chứa dữ liệu, không có hàm riêng: Jackson tạo từ JSON, {@code SearchSpec.resolve} đọc.
 *
 * @param property tên thuộc tính cần lọc
 * @param operator tên phép so sánh (không phân biệt hoa thường), phải nằm trong danh sách server cho phép
 *                 của thuộc tính; để trống thì dùng phép mặc định của server
 * @param value    giá trị dạng chuỗi; với {@link Operator#IN}, các giá trị cách nhau bởi dấu phẩy
 */
public record FilterInput(String property, String operator, String value) {

    /** Bộ lọc không chỉ định operator: server dùng phép mặc định của thuộc tính. */
    public FilterInput(String property, String value) {
        this(property, null, value);
    }
}
