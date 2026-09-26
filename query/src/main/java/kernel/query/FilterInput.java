package kernel.query;

/**
 * Một bộ lọc thô client gửi lên, chưa được kiểm tra.
 *
 * <p>Chỉ chứa dữ liệu, không có hàm riêng: Jackson tạo từ JSON, {@code SearchSpec.resolve} đọc.
 *
 * @param property tên thuộc tính cần lọc
 * @param value    giá trị dạng chuỗi; với {@link Operator#IN}, các giá trị cách nhau bởi dấu phẩy
 */
public record FilterInput(String property, String value) {
}
