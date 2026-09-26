package kernel.query;

import java.util.List;
import java.util.Objects;

/**
 * Payload tìm kiếm thô từ client, chưa được kiểm tra.
 *
 * <pre>
 * {
 *   "page": 0,
 *   "limit": 20,
 *   "filters": [{ "property": "name", "value": "thanhvu" }],
 *   "sortBy": "createdAt",
 *   "sortDir": "DESC"
 * }
 * </pre>
 *
 * Mọi thành phần đều có thể null, {@link SearchSpec#resolve(SearchRequest)} sẽ điền giá trị mặc định.
 *
 * <p><b>Các hàm:</b>
 * <ul>
 *   <li>{@code SearchRequest(...)}: constructor gọn: đổi filters null thành danh sách rỗng và loại phần tử null</li>
 *   <li>{@code empty()}: tạo request trống, dùng khi client không gửi body</li>
 * </ul>
 *
 * @param page    số trang, bắt đầu từ 0
 * @param limit   số phần tử mỗi trang
 * @param filters danh sách bộ lọc; null được đổi thành danh sách rỗng, phần tử null bị loại bỏ
 * @param sortBy  tên thuộc tính cần sắp xếp
 * @param sortDir chiều sắp xếp, "asc" hoặc "desc"
 */
public record SearchRequest(Integer page, Integer limit, List<FilterInput> filters, String sortBy, String sortDir) {

    /**
     * Chuẩn hoá danh sách bộ lọc để không bao giờ null và không chứa phần tử null.
     */
    public SearchRequest {
        filters = filters == null ? List.of() : filters.stream().filter(Objects::nonNull).toList();
    }

    /**
     * Tạo request rỗng, tương đương client không gửi gì.
     *
     * @return request với mọi thành phần để trống
     */
    public static SearchRequest empty() {
        return new SearchRequest(null, null, null, null, null);
    }
}
