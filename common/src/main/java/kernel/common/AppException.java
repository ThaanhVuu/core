package kernel.common;

import java.io.Serial;
import java.util.IllegalFormatException;
import java.util.Objects;

/**
 * Exception gốc của ứng dụng, luôn gắn với một {@link ErrorCode}.
 * Thông báo được tạo từ {@link ErrorCode#formatMessage(Object...)}.
 *
 * <p>Ví dụ:
 * <pre>{@code
 * throw new AppException(CommonError.NOT_FOUND, "Order", orderId);
 * }</pre>
 *
 * <p>Tầng ngoài nên dựa vào {@link #getErrorCode()} (không phải {@link #getMessage()})
 * để quyết định cách phản hồi.
 */
public class AppException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final Object[] NO_ARGS = new Object[0];

    /** Mã lỗi gắn với exception. */
    private final ErrorCode errorCode;
    /** Tham số dùng để tạo thông báo. */
    private final Object[] args;

    /**
     * Tạo exception với mã lỗi và tham số cho thông báo.
     *
     * @param errorCode mã lỗi, không được null
     * @param args      tham số chèn vào mẫu thông báo của mã lỗi
     * @throws NullPointerException nếu {@code errorCode} là null
     */
    public AppException(ErrorCode errorCode, Object... args) {
        this(errorCode, null, args);
    }

    /**
     * Tạo exception bọc một lỗi gốc, giữ lại stack trace của lỗi đó.
     *
     * <p>Nếu mẫu thông báo và tham số không khớp, thông báo sẽ là mẫu gốc chưa định dạng
     * thay vì ném lỗi định dạng.
     *
     * @param errorCode mã lỗi, không được null
     * @param cause     lỗi gốc, có thể null
     * @param args      tham số chèn vào mẫu thông báo của mã lỗi
     * @throws NullPointerException nếu {@code errorCode} là null
     */
    public AppException(ErrorCode errorCode, Throwable cause, Object... args) {
        super(safeMessage(errorCode, normalize(args)), cause);
        this.errorCode = errorCode;
        this.args = normalize(args).clone();
    }

    /**
     * Mã lỗi của exception.
     *
     * @return mã lỗi, không bao giờ null
     */
    public ErrorCode getErrorCode() {
        return errorCode;
    }

    /**
     * Tham số đã dùng để tạo thông báo.
     *
     * @return bản sao mảng tham số, rỗng nếu không có
     */
    public Object[] getArgs() {
        return args.clone();
    }

    /** Mảng args null được coi như không có tham số. */
    private static Object[] normalize(Object[] args) {
        return args == null ? NO_ARGS : args;
    }

    /**
     * Tạo thông báo mà không bao giờ ném lỗi. Nếu mẫu và tham số không khớp,
     * trả về mẫu gốc để exception vẫn giữ đúng mã lỗi.
     */
    private static String safeMessage(ErrorCode errorCode, Object[] args) {
        Objects.requireNonNull(errorCode, "errorCode must not be null");
        try {
            return errorCode.formatMessage(args);
        } catch (IllegalFormatException e) {
            return errorCode.messageTemplate();
        }
    }
}