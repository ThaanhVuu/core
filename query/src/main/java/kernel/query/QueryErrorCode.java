package kernel.query;

import kernel.common.ErrorCode;
import kernel.common.ErrorType;

/**
 * Mã lỗi của module query, trả về client khi tham số tìm kiếm không hợp lệ.
 *
 * <p><b>Các hàm:</b>
 * <ul>
 *   <li>{@code type()}: loại lỗi, global handler dựa vào đây để chọn HTTP status</li>
 *   <li>{@code messageTemplate()}: mẫu thông báo có chỗ trống %s</li>
 *   <li>{@code code(), formatMessage(...)}: kế thừa mặc định từ ErrorCode: mã gửi client là tên hằng số; thông báo được điền tham số</li>
 * </ul>
 */
public enum QueryErrorCode implements ErrorCode {

    /** Giá trị không chuyển được sang kiểu của thuộc tính. Tham số: giá trị, tên thuộc tính. */
    QUERY_FILTER_VALUE_INVALID(ErrorType.VALIDATION, "Invalid value \"%s\" for filter \"%s\""),

    /** Bộ lọc IN có quá nhiều giá trị. Tham số: tên thuộc tính, số lượng tối đa. */
    QUERY_TOO_MANY_VALUES(ErrorType.VALIDATION, "Filter \"%s\" accepts at most %s values"),

    /** Chiều sắp xếp không phải ASC hoặc DESC. Tham số: giá trị client gửi. */
    QUERY_SORT_DIRECTION_INVALID(ErrorType.VALIDATION, "Invalid sort direction \"%s\", expected ASC or DESC");

    private final ErrorType type;
    private final String messageTemplate;

    QueryErrorCode(ErrorType type, String messageTemplate) {
        this.type = type;
        this.messageTemplate = messageTemplate;
    }

    @Override
    public ErrorType type() {
        return type;
    }

    @Override
    public String messageTemplate() {
        return messageTemplate;
    }
}
