package kernel.common;

/**
 * Mã lỗi dùng chung, không gắn với nghiệp vụ cụ thể.
 * Lỗi nghiệp vụ được định nghĩa trong enum riêng của từng ứng dụng.
 *
 * <p>Mỗi hằng số ghi rõ thứ tự tham số cần truyền vào {@link AppException} để
 * tạo thông báo, ví dụ:
 * <pre>{@code
 * throw new AppException(CommonError.OUT_OF_RANGE, "age", 18, 60);
 * // -> "age must be between 18 and 60"
 * }</pre>
 */
public enum CommonError implements ErrorCode {

    /** Trường bắt buộc bị thiếu. Tham số: tên trường. */
    REQUIRED("COMMON_REQUIRED", ErrorType.VALIDATION,
            "%s is required"),

    /** Giá trị sai định dạng. Tham số: tên trường. */
    INVALID_FORMAT("COMMON_INVALID_FORMAT", ErrorType.VALIDATION,
            "%s has an invalid format"),

    /** Giá trị nằm ngoài khoảng cho phép. Tham số: tên trường, giá trị nhỏ nhất, giá trị lớn nhất. */
    OUT_OF_RANGE("COMMON_OUT_OF_RANGE", ErrorType.VALIDATION,
            "%s must be between %s and %s"),

    /** Độ dài chuỗi không hợp lệ. Tham số: tên trường, độ dài nhỏ nhất, độ dài lớn nhất. */
    INVALID_LENGTH("COMMON_INVALID_LENGTH", ErrorType.VALIDATION,
            "%s must be between %s and %s characters long"),

    /** Không tìm thấy đối tượng. Tham số: tên đối tượng, định danh. */
    NOT_FOUND("COMMON_NOT_FOUND", ErrorType.NOT_FOUND,
            "%s not found: %s"),

    /** Lỗi hệ thống không lường trước. Không có tham số. */
    INTERNAL("COMMON_INTERNAL", ErrorType.INTERNAL,
            "An internal error occurred");

    /** Mã gửi cho frontend. */
    private final String code;
    /** Loại lỗi. */
    private final ErrorType type;
    /** Mẫu thông báo theo cú pháp {@link String#format(String, Object...)}. */
    private final String messageTemplate;

    CommonError(String code, ErrorType type, String messageTemplate) {
        this.code = code;
        this.type = type;
        this.messageTemplate = messageTemplate;
    }

    /**
     * Mã gửi cho frontend, có tiền tố {@code COMMON_} để tránh trùng với mã của ứng dụng.
     *
     * @return mã lỗi
     */
    @Override
    public String code() {
        return code;
    }

    /** {@inheritDoc} */
    @Override
    public ErrorType type() {
        return type;
    }

    /** {@inheritDoc} */
    @Override
    public String messageTemplate() {
        return messageTemplate;
    }
}
