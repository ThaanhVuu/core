package kernel.common;

import java.util.regex.Pattern;

/**
 * Kiểm tra điều kiện đầu vào, ném {@link AppException} khi vi phạm.
 * Các method trả về chính giá trị đã kiểm tra để có thể gán ngay.
 *
 * <p>Ví dụ:
 * <pre>{@code
 * this.name = Guard.lengthBetween(name, 1, 100, "name");
 * this.age  = Guard.inRange(age, 18, 60, "age");
 * }</pre>
 *
 * <p>Tham số {@code field} là tên trường hiển thị trong thông báo lỗi.
 */
public final class Guard {

    private Guard() {
    }

    /**
     * Ném lỗi với mã lỗi tùy chọn nếu điều kiện sai.
     *
     * @param condition điều kiện phải đúng
     * @param errorCode mã lỗi khi điều kiện sai
     * @param args      tham số cho thông báo lỗi
     * @throws AppException với {@code errorCode} nếu {@code condition} sai
     */
    public static void isTrue(boolean condition, ErrorCode errorCode, Object... args) {
        if (!condition) {
            throw new AppException(errorCode, args);
        }
    }

    /**
     * Kiểm tra giá trị không được null.
     *
     * @param <T>   kiểu giá trị
     * @param value giá trị cần kiểm tra
     * @param field tên trường
     * @return chính {@code value}
     * @throws AppException với {@link CommonError#REQUIRED} nếu {@code value} là null
     */
    public static <T> T notNull(T value, String field) {
        if (value == null) {
            throw new AppException(CommonError.REQUIRED, field);
        }
        return value;
    }

    /**
     * Kiểm tra chuỗi không được null, rỗng hoặc chỉ có khoảng trắng.
     *
     * @param value chuỗi cần kiểm tra
     * @param field tên trường
     * @return chính {@code value}, không bị trim
     * @throws AppException với {@link CommonError#REQUIRED} nếu chuỗi null hoặc trống
     */
    public static String notBlank(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new AppException(CommonError.REQUIRED, field);
        }
        return value;
    }

    /**
     * Kiểm tra độ dài chuỗi nằm trong đoạn [min, max].
     *
     * <p>Độ dài tính theo {@link String#length()}, tức số đơn vị UTF-16, nên một số ký tự
     * như emoji được tính là 2.
     *
     * @param value chuỗi cần kiểm tra
     * @param min   độ dài nhỏ nhất, tính cả biên
     * @param max   độ dài lớn nhất, tính cả biên
     * @param field tên trường
     * @return chính {@code value}
     * @throws AppException với {@link CommonError#REQUIRED} nếu {@code value} là null,
     *                      hoặc {@link CommonError#INVALID_LENGTH} nếu độ dài ngoài đoạn
     */
    public static String lengthBetween(String value, int min, int max, String field) {
        notNull(value, field);
        int length = value.length();
        if (length < min || length > max) {
            throw new AppException(CommonError.INVALID_LENGTH, field, min, max);
        }
        return value;
    }

    /**
     * Kiểm tra giá trị nằm trong đoạn [min, max].
     *
     * @param <T>   kiểu giá trị, so sánh được
     * @param value giá trị cần kiểm tra
     * @param min   giá trị nhỏ nhất, tính cả biên, không được null
     * @param max   giá trị lớn nhất, tính cả biên, không được null
     * @param field tên trường
     * @return chính {@code value}
     * @throws AppException với {@link CommonError#REQUIRED} nếu {@code value} là null,
     *                      hoặc {@link CommonError#OUT_OF_RANGE} nếu giá trị ngoài đoạn
     */
    public static <T extends Comparable<? super T>> T inRange(T value, T min, T max, String field) {
        notNull(value, field);
        if (value.compareTo(min) < 0 || value.compareTo(max) > 0) {
            throw new AppException(CommonError.OUT_OF_RANGE, field, min, max);
        }
        return value;
    }

    /**
     * Kiểm tra chuỗi khớp toàn bộ với pattern (dùng {@link java.util.regex.Matcher#matches()},
     * không phải {@code find()}).
     *
     * @param value   chuỗi cần kiểm tra
     * @param pattern biểu thức chính quy, nên khai báo sẵn dạng hằng số để tái sử dụng
     * @param field   tên trường
     * @return chính {@code value}
     * @throws AppException với {@link CommonError#REQUIRED} nếu {@code value} là null,
     *                      hoặc {@link CommonError#INVALID_FORMAT} nếu không khớp
     */
    public static String matches(String value, Pattern pattern, String field) {
        notNull(value, field);
        if (!pattern.matcher(value).matches()) {
            throw new AppException(CommonError.INVALID_FORMAT, field);
        }
        return value;
    }
}
