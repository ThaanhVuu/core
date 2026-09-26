package kernel.common;

/**
 * Loại lỗi. Tầng ngoài dựa vào đây để chọn HTTP status, domain không cần biết HTTP.
 */
public enum ErrorType {
    /** Dữ liệu đầu vào không hợp lệ (thường ứng với HTTP 400). */
    VALIDATION,
    /** Không tìm thấy tài nguyên được yêu cầu (thường ứng với HTTP 404). */
    NOT_FOUND,
    /** Xung đột trạng thái, ví dụ trùng dữ liệu hoặc sai phiên bản (thường ứng với HTTP 409). */
    CONFLICT,
    /** Vi phạm quy tắc nghiệp vụ (thường ứng với HTTP 422). */
    BUSINESS_RULE,
    /** Chưa xác thực danh tính (thường ứng với HTTP 401). */
    UNAUTHORIZED,
    /** Đã xác thực nhưng không có quyền (thường ứng với HTTP 403). */
    FORBIDDEN,
    /** Lỗi nội bộ không lường trước (thường ứng với HTTP 500). */
    INTERNAL
}
