package kernel.query;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Map;

/**
 * Tìm kiểu của một thuộc tính trong class bằng reflection.
 * Hỗ trợ thuộc tính kế thừa từ lớp cha và thuộc tính lồng dạng "department.name".
 *
 * <p><b>Các hàm:</b>
 * <ul>
 *   <li>{@code resolveType(target, path)}: đi theo đường dẫn (có thể lồng "department.name") để tìm kiểu của thuộc tính; kiểu nguyên thuỷ được đổi sang kiểu bọc; không tìm thấy hoặc đi xuyên collection thì ném lỗi</li>
 *   <li>{@code findField(type, name)}: tìm field theo tên trong class và lần lượt các lớp cha; không có thì trả null</li>
 * </ul>
 */
final class PropertyResolver {

    private static final Map<Class<?>, Class<?>> WRAPPERS = Map.of(
            int.class, Integer.class,
            long.class, Long.class,
            double.class, Double.class,
            boolean.class, Boolean.class);

    private PropertyResolver() {
    }

    /**
     * Tìm kiểu của thuộc tính theo đường dẫn.
     *
     * @param target class gốc, ví dụ {@code UserEntity.class}
     * @param path   tên thuộc tính, được phép dạng lồng "department.name"
     * @return kiểu của thuộc tính cuối cùng; kiểu nguyên thuỷ được đổi sang kiểu bọc ({@code int} thành {@code Integer})
     * @throws IllegalArgumentException nếu không tìm thấy thuộc tính hoặc đường dẫn đi xuyên qua một collection
     */
    static Class<?> resolveType(Class<?> target, String path) {
        Class<?> current = target;
        String[] parts = path.split("\\.");

        for (int i = 0; i < parts.length; i++) {
            Field field = findField(current, parts[i]);
            if (field == null) {
                throw new IllegalArgumentException(
                        "Property '" + parts[i] + "' not found in " + current.getSimpleName() + " (path '" + path + "')");
            }
            current = field.getType();

            boolean isLast = i == parts.length - 1;
            if (!isLast && Collection.class.isAssignableFrom(current)) {
                throw new IllegalArgumentException(
                        "Property '" + parts[i] + "' is a collection, traversing it is not supported (path '" + path + "')");
            }
        }
        return WRAPPERS.getOrDefault(current, current);
    }

    /** Tìm field theo tên trong class và lần lượt các lớp cha, trả về null nếu không có. */
    private static Field findField(Class<?> type, String name) {
        for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass()) {
            try {
                return c.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                // Không có ở class này, thử lớp cha
            }
        }
        return null;
    }
}
