package kernel.query;

import kernel.common.AppException;
import kernel.common.ErrorCode;

/**
 * Lỗi do client gửi tham số tìm kiếm không hợp lệ.
 *
 * <p><b>Các hàm:</b>
 * <ul>
 *   <li>{@code QueryException(errorCode, args)}: tạo lỗi với mã lỗi và tham số cho thông báo</li>
 *   <li>{@code QueryException(errorCode, cause, args)}: tạo lỗi bọc một lỗi gốc, giữ stack trace của lỗi đó trong log</li>
 * </ul>
 */
public class QueryException extends AppException {

    /**
     * Tạo lỗi với mã lỗi và tham số cho thông báo.
     *
     * @param errorCode mã lỗi
     * @param args      tham số chèn vào mẫu thông báo
     */
    public QueryException(ErrorCode errorCode, Object... args) {
        super(errorCode, args);
    }

    /**
     * Tạo lỗi bọc một lỗi gốc, giữ lại stack trace của lỗi đó.
     *
     * @param errorCode mã lỗi
     * @param cause     lỗi gốc
     * @param args      tham số chèn vào mẫu thông báo
     */
    public QueryException(ErrorCode errorCode, Throwable cause, Object... args) {
        super(errorCode, cause, args);
    }
}
