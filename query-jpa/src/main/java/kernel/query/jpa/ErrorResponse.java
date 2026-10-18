package kernel.query.jpa;

/**
 * Body lỗi trả về client. Frontend dựa vào {@code code}, {@code message} dùng cho log và debug.
 *
 * <p>Chỉ chứa dữ liệu, không có hàm riêng.
 */
public record ErrorResponse(String code, String message) {
}
