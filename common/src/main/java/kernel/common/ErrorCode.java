package kernel.common;

/**
 * Hợp đồng chung cho mã lỗi. Mỗi module/service tạo một enum implement interface này.
 *
 * <p>Mẫu thông báo viết bằng tiếng Anh; nếu cần đa ngôn ngữ, tầng ngoài dựa vào
 * {@link #code()} để tra bản dịch.
 *
 * <p>Ví dụ:
 * <pre>{@code
 * public enum OrderError implements ErrorCode {
 *     ORDER_NOT_FOUND(ErrorType.NOT_FOUND, "Order %s not found");
 *
 *     private final ErrorType type;
 *     private final String messageTemplate;
 *
 *     OrderError(ErrorType type, String messageTemplate) {
 *         this.type = type;
 *         this.messageTemplate = messageTemplate;
 *     }
 *
 *     public ErrorType type() { return type; }
 *     public String messageTemplate() { return messageTemplate; }
 * }
 *
 * throw new AppException(OrderError.ORDER_NOT_FOUND, orderId);
 * }</pre>
 */
public interface ErrorCode {

    /**
     * Tên hằng số của enum, enum tự có sẵn nên không cần implement.
     *
     * @return tên hằng số
     */
    String name();

    /**
     * Loại lỗi, dùng để tầng ngoài chọn cách phản hồi.
     *
     * @return loại lỗi
     */
    ErrorType type();

    /**
     * Mẫu thông báo, có thể chứa %s để chèn tham số.
     *
     * @return mẫu thông báo theo cú pháp {@link String#format(String, Object...)}
     */
    String messageTemplate();

    /**
     * Mã gửi cho frontend, mặc định là tên hằng số.
     * Nên giữ ổn định vì frontend có thể dựa vào mã này.
     *
     * @return mã lỗi
     */
    default String code() {
        return name();
    }

    /**
     * Tạo thông báo lỗi từ {@link #messageTemplate()} và các tham số.
     *
     * @param args tham số chèn vào mẫu; nếu rỗng thì trả nguyên mẫu
     * @return thông báo đã được định dạng
     * @throws java.util.IllegalFormatException nếu mẫu và tham số không khớp
     */
    default String formatMessage(Object... args) {
        return args.length == 0 ? messageTemplate() : String.format(messageTemplate(), args);
    }
}
