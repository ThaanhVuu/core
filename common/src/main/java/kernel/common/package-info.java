/**
 * Các thành phần dùng chung cho mọi module: hợp đồng mã lỗi, exception nền và tiện ích.
 *
 * <p>Cách dùng điển hình:
 * <ol>
 *   <li>Mỗi module khai báo một enum implement {@link kernel.common.ErrorCode}.
 *       Lỗi chung đã có sẵn trong {@link kernel.common.CommonError}.</li>
 *   <li>Ném {@link kernel.common.AppException} kèm mã lỗi đó, hoặc dùng
 *       {@link kernel.common.Guard} để kiểm tra đầu vào.</li>
 *   <li>Tầng ngoài (web, messaging) đọc {@link kernel.common.ErrorType} để quyết định
 *       cách phản hồi, ví dụ HTTP status.</li>
 * </ol>
 *
 * <p>{@link kernel.common.UuidV7} dùng để sinh định danh tăng dần theo thời gian.
 */
package kernel.common;
