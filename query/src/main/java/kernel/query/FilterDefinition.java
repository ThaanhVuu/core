package kernel.query;

import java.util.Set;
import java.util.function.Function;

/**
 * Khai báo một bộ lọc được phép.
 *
 * <p>Chỉ chứa dữ liệu, không có hàm riêng: {@code SearchSpec.Builder.filter} tạo ra, {@code SearchSpec.resolve} đọc để đổi giá trị.
 *
 * @param property        tên thuộc tính, vừa là tên client gửi vừa là tên thuộc tính trong class target
 * @param defaultOperator phép so sánh dùng khi client không chỉ định
 * @param operators       các phép so sánh client được phép chọn, luôn chứa {@code defaultOperator}
 * @param parser          hàm chuyển chuỗi client gửi sang đúng kiểu của thuộc tính, chọn sẵn khi build
 */
record FilterDefinition(String property, Operator defaultOperator, Set<Operator> operators,
                        Function<String, Object> parser) {
}
