package kernel.query;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

/**
 * Chọn hàm chuyển chuỗi client gửi sang đúng kiểu dữ liệu của thuộc tính.
 * Chỉ dùng khi build {@link SearchSpec}; hàm được chọn sẵn và lưu trong {@link FilterDefinition}.
 *
 * <p><b>Các hàm:</b>
 * <ul>
 *   <li>{@code khối static và register(type, parser)}: nạp sẵn bảng kiểu → hàm chuyển đổi một lần khi class được nạp</li>
 *   <li>{@code parserFor(type)}: chọn hàm chuyển chuỗi sang kiểu yêu cầu; enum thì tạo hàm riêng ghi nhớ kiểu enum; kiểu không hỗ trợ thì ném lỗi</li>
 *   <li>{@code parseBoolean(value)}: chỉ nhận "true" hoặc "false", chuỗi khác thì ném lỗi thay vì âm thầm thành false</li>
 *   <li>{@code parseEnum(type, value)}: tìm hằng số enum theo tên, không phân biệt hoa thường</li>
 * </ul>
 */
final class ValueConverter {

    private static final Map<Class<?>, Function<String, Object>> PARSERS = new HashMap<>();

    static {
        register(String.class, value -> value);
        register(Integer.class, Integer::valueOf);
        register(Short.class, Short::valueOf);
        register(Long.class, Long::valueOf);
        register(Float.class, Float::valueOf);
        register(Double.class, Double::valueOf);
        register(BigDecimal.class, BigDecimal::new);
        register(Boolean.class, ValueConverter::parseBoolean);
        register(UUID.class, UUID::fromString);
        register(Instant.class, Instant::parse);
        register(LocalDate.class, LocalDate::parse);
        register(LocalDateTime.class, LocalDateTime::parse);
        register(OffsetDateTime.class, OffsetDateTime::parse);
        register(ZonedDateTime.class, ZonedDateTime::parse);
    }

    private ValueConverter() {
    }

    private static void register(Class<?> type, Function<String, Object> parser) {
        PARSERS.put(type, parser);
    }

    /**
     * Chọn hàm chuyển đổi cho một kiểu.
     *
     * @param type kiểu của thuộc tính
     * @return hàm nhận chuỗi đã cắt khoảng trắng, trả về giá trị đúng kiểu;
     *         hàm ném {@link RuntimeException} nếu chuỗi không hợp lệ
     * @throws IllegalArgumentException nếu kiểu chưa được hỗ trợ
     */
    static Function<String, Object> parserFor(Class<?> type) {
        if (type.isEnum()) {
            return value -> parseEnum(type, value);
        }
        Function<String, Object> parser = PARSERS.get(type);
        if (parser == null) {
            throw new IllegalArgumentException("Unsupported type: " + type.getName());
        }
        return parser;
    }

    /** Chỉ nhận "true" hoặc "false", khác với Boolean.parseBoolean coi mọi chuỗi lạ là false. */
    private static Boolean parseBoolean(String value) {
        return switch (value.toLowerCase(Locale.ROOT)) {
            case "true" -> Boolean.TRUE;
            case "false" -> Boolean.FALSE;
            default -> throw new IllegalArgumentException("Not a boolean: " + value);
        };
    }

    /** Tìm hằng số enum theo tên, không phân biệt hoa thường. */
    private static Object parseEnum(Class<?> type, String value) {
        for (Object constant : type.getEnumConstants()) {
            if (((Enum<?>) constant).name().equalsIgnoreCase(value)) {
                return constant;
            }
        }
        throw new IllegalArgumentException("No constant " + value + " in " + type.getSimpleName());
    }
}
