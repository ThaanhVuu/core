package kernel.query;

import java.util.List;
import java.util.function.Function;

/**
 * Một trang kết quả kèm thông tin phân trang.
 *
 * <p><b>Các hàm:</b>
 * <ul>
 *   <li>{@code PageResult(...)}: constructor gọn: sao chép danh sách phần tử thành bất biến</li>
 *   <li>{@code of(content, total, page, limit)}: tạo kết quả và tự tính totalPages bằng phép chia làm tròn lên</li>
 *   <li>{@code empty(query)}: tạo kết quả rỗng giữ page và limit của truy vấn</li>
 *   <li>{@code map(mapper)}: đổi từng phần tử sang kiểu khác (entity sang DTO), giữ nguyên thông tin phân trang</li>
 * </ul>
 *
 * @param content      các phần tử trong trang
 * @param totalElements      tổng số phần tử khớp điều kiện
 * @param page       số trang, bắt đầu từ 0
 * @param limit      số phần tử tối đa mỗi trang
 * @param totalPages tổng số trang
 * @param <T>        kiểu phần tử
 */
public record PageResult<T>(List<T> content, long totalElements, int page, int limit, int totalPages) {

    /**
     * Sao chép danh sách phần tử thành bất biến.
     */
    public PageResult {
        content = List.copyOf(content);
    }

    /**
     * Tạo kết quả và tự tính tổng số trang.
     *
     * @param content các phần tử trong trang
     * @param totalElements tổng số phần tử khớp điều kiện
     * @param page  số trang
     * @param limit số phần tử tối đa mỗi trang
     * @param <T>   kiểu phần tử
     * @return kết quả phân trang
     */
    public static <T> PageResult<T> of(List<T> content, long totalElements, int page, int limit) {
        int totalPages = totalElements == 0 ? 0 : (int) ((totalElements + limit - 1) / limit);
        return new PageResult<>(content, totalElements, page, limit, totalPages);
    }

    /**
     * Tạo kết quả rỗng cho một truy vấn.
     *
     * @param query truy vấn cần lấy page và limit
     * @param <T>   kiểu phần tử
     * @return kết quả không có phần tử nào
     */
    public static <T> PageResult<T> empty(SearchQuery query) {
        return of(List.of(), 0, query.page(), query.limit());
    }

    /**
     * Chuyển từng phần tử sang kiểu khác (thường là entity sang DTO), giữ nguyên thông tin phân trang.
     *
     * @param mapper hàm chuyển đổi một phần tử
     * @param <R>    kiểu phần tử mới
     * @return kết quả phân trang với phần tử đã chuyển đổi
     */
    public <R> PageResult<R> map(Function<? super T, ? extends R> mapper) {
        List<R> mapped = content.stream().<R>map(mapper).toList();
        return new PageResult<>(mapped, totalElements, page, limit, totalPages);
    }
}
