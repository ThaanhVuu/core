package kernel.query;

import java.util.ArrayList;
import java.util.List;

/**
 * Truy vấn đã được kiểm tra, sinh ra từ {@link SearchSpec#resolve(SearchRequest)}.
 * Tầng truy vấn dùng trực tiếp, không cần kiểm tra lại.
 *
 * <p><b>Các hàm:</b>
 * <ul>
 *   <li>{@code SearchQuery(...)}: constructor gọn: kiểm tra page không âm, limit dương; sao chép danh sách thành bất biến</li>
 *   <li>{@code offset()}: vị trí bắt đầu của trang, tính bằng long để không tràn số</li>
 *   <li>{@code withCriterion(field, operator, value)}: trả về SearchQuery mới có thêm một điều kiện do server ép buộc, client không bỏ được</li>
 * </ul>
 *
 * @param page     số trang, bắt đầu từ 0
 * @param limit    số phần tử mỗi trang, lớn hơn 0
 * @param criteria các điều kiện lọc, ghép với nhau bằng AND
 * @param sort     các tiêu chí sắp xếp theo thứ tự ưu tiên
 */
public record SearchQuery(int page, int limit, List<Criterion> criteria, List<SortOrder> sort) {

    /**
     * Kiểm tra page, limit và sao chép danh sách thành bất biến.
     *
     * @throws IllegalArgumentException nếu page âm hoặc limit không dương
     */
    public SearchQuery {
        if (page < 0) {
            throw new IllegalArgumentException("page must not be negative");
        }
        if (limit <= 0) {
            throw new IllegalArgumentException("limit must be positive");
        }
        criteria = List.copyOf(criteria);
        sort = List.copyOf(sort);
    }

    /**
     * Vị trí bắt đầu của trang hiện tại.
     *
     * @return {@code page * limit}
     */
    public long offset() {
        return (long) page * limit;
    }

    /**
     * Thêm một điều kiện do server ép buộc, client không ghi đè hay bỏ được.
     * Ví dụ API công khai luôn chỉ lấy user ACTIVE.
     *
     * @param field    tên thuộc tính
     * @param operator phép so sánh
     * @param value    giá trị đã đúng kiểu
     * @return SearchQuery mới có thêm điều kiện; object hiện tại không bị thay đổi
     */
    public SearchQuery withCriterion(String field, Operator operator, Object value) {
        List<Criterion> merged = new ArrayList<>(criteria);
        merged.add(new Criterion(field, operator, value));
        return new SearchQuery(page, limit, merged, sort);
    }
}
