package kernel.ddd;

/**
 * Interface đánh dấu cho value object.
 *
 * <p>Value object không có định danh, là bất biến và được so sánh bằng toàn bộ thuộc tính.
 * Nên hiện thực bằng {@code record} để có sẵn {@code equals} và {@code hashCode}.
 */
public interface ValueObject {
}
