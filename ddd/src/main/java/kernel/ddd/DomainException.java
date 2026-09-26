package kernel.ddd;

import kernel.common.AppException;
import kernel.common.ErrorCode;

/**
 * Lỗi vi phạm quy tắc nghiệp vụ trong domain.
 */
public class DomainException extends AppException {

    public DomainException(ErrorCode errorCode, Object... args) {
        super(errorCode, args);
    }

    public DomainException(ErrorCode errorCode, Throwable cause, Object... args) {
        super(errorCode, cause, args);
    }
}