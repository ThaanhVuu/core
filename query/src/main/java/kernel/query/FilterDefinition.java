package kernel.query;

import java.util.function.Function;

/**
 * Khai báo một bộ lọc được phép.
 *
 * <p>Chỉ chứa dữ liệu, không có hàm riêng: {@code SearchSpec.Builder.filter} tạo ra, {@code SearchSpec.resolve} đọc để đổi giá trị.
 *
 * @param property tên thuộc tính, vừa là tên client gửi vừa là tên thuộc tính trong class target
 * @param operator phép so sánh do server chọn
 * @param parser   hàm chuyển chuỗi client gửi sang đúng kiểu của thuộc tính, chọn sẵn khi build
 */
record FilterDefinition(String property, Operator operator, Function<String, Object> parser) {
}
