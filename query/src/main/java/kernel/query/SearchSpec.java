package kernel.query;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

/**
 * Whitelist cho một API tìm kiếm: client được lọc và sắp xếp theo thuộc tính nào, dùng operator nào,
 * giới hạn bao nhiêu phần tử.
 *
 * <p>Mỗi API tìm kiếm có một SearchSpec riêng, khai báo {@code static final} để chỉ tạo một lần.
 * SearchSpec gắn với một class (entity hoặc read model): tên thuộc tính và kiểu của chúng được
 * kiểm tra bằng reflection khi build, nên gõ sai tên sẽ báo lỗi ngay lúc ứng dụng khởi động.
 * Hai API có thể dùng chung phần giống nhau qua {@link #toBuilder()}.
 *
 * <p>Quy tắc khi {@link #resolve(SearchRequest)}:
 * <ul>
 *   <li>thuộc tính không khai báo, bộ lọc rỗng, giá trị rỗng: bỏ qua</li>
 *   <li>thuộc tính đã khai báo nhưng giá trị sai kiểu: ném {@link QueryException}</li>
 *   <li>limit thiếu hoặc không dương: dùng defaultLimit; vượt maxLimit: ép về maxLimit</li>
 *   <li>page thiếu hoặc âm: về 0</li>
 *   <li>sortBy thiếu hoặc không khai báo: dùng defaultSort; luôn thêm tieBreaker cuối cùng để phân trang ổn định</li>
 * </ul>
 *
 * <pre>
 * static final SearchSpec USER_SEARCH = SearchSpec.builder(UserEntity.class)
 *         .filter("name", Operator.CONTAINS)
 *         .filter("createdAt", Operator.GTE)
 *         .sortable("name", "createdAt")
 *         .defaultSort("createdAt", SortDirection.DESC)
 *         .build();
 * </pre>
 *
 * <p><b>Các hàm:</b>
 * <ul>
 *   <li>{@code builder(target)}: bắt đầu khai báo một SearchSpec gắn với một class</li>
 *   <li>{@code toBuilder()}: tạo builder mang sẵn cấu hình hiện tại, để dựng spec mở rộng cho API khác</li>
 *   <li>{@code resolve(request)}: cổng kiểm soát mỗi request: lọc theo whitelist, đổi kiểu giá trị, ép giới hạn, chọn sắp xếp, trả về SearchQuery</li>
 *   <li>{@code resolveLimit(requested)}: limit thiếu hoặc không dương thì dùng defaultLimit, vượt quá thì ép về maxLimit</li>
 *   <li>{@code resolveFilter(input)}: bỏ qua bộ lọc rỗng hoặc chưa khai báo; còn lại đổi giá trị và tạo Criterion</li>
 *   <li>{@code convertOne(definition, raw)}: gọi hàm chuyển đổi đã chọn sẵn; lỗi chuyển đổi được bọc thành QueryException</li>
 *   <li>{@code convertMany(definition, raw)}: cho IN: tách theo dấu phẩy, giới hạn số lượng, đổi từng giá trị</li>
 *   <li>{@code resolveSort(sortBy, sortDir)}: chọn thuộc tính sắp xếp của client hoặc mặc định, rồi thêm tieBreaker vào cuối</li>
 *   <li>{@code parseDirection(raw)}: đọc chiều sắp xếp; sai thì bọc thành QueryException</li>
 *   <li>{@code Builder.filter(property, operator, more...)}: cho phép lọc theo một thuộc tính với một hoặc nhiều operator (operator đầu là mặc định): tìm kiểu bằng reflection, chọn hàm chuyển đổi, kiểm tra từng operator hợp với kiểu; GT/GTE/LT/LTE không dùng được cho enum và Boolean</li>
 *   <li>{@code Builder.sortable(properties...)}: cho phép sắp xếp theo một hoặc nhiều thuộc tính, kiểm tra tên tồn tại</li>
 *   <li>{@code Builder.defaultSort(property, direction)}: sắp xếp khi client không gửi sortBy hợp lệ</li>
 *   <li>{@code Builder.tieBreaker(property)}: thuộc tính sắp xếp phụ để phân trang ổn định, mặc định "id", truyền null để tắt</li>
 *   <li>{@code Builder.defaultLimit(n), Builder.maxLimit(n)}: số phần tử mặc định và tối đa mỗi trang</li>
 *   <li>{@code Builder.build()}: kiểm tra toàn bộ cấu hình và tạo SearchSpec bất biến</li>
 * </ul>
 */
public final class SearchSpec {

    /** Số giá trị tối đa của một bộ lọc {@link Operator#IN}. */
    static final int MAX_IN_VALUES = 100;

    private final Class<?> target;
    private final Map<String, FilterDefinition> filters;
    private final Set<String> sortables;
    private final String defaultSortProperty;
    private final SortDirection defaultSortDirection;
    private final String tieBreaker;
    private final int defaultLimit;
    private final int maxLimit;

    private SearchSpec(Builder builder) {
        this.target = builder.target;
        this.filters = Map.copyOf(builder.filters);
        this.sortables = Set.copyOf(builder.sortables);
        this.defaultSortProperty = builder.defaultSortProperty;
        this.defaultSortDirection = builder.defaultSortDirection;
        this.tieBreaker = builder.tieBreaker;
        this.defaultLimit = builder.defaultLimit;
        this.maxLimit = builder.maxLimit;
    }

    /**
     * Bắt đầu khai báo một SearchSpec.
     *
     * @param target class chứa các thuộc tính được lọc và sắp xếp, ví dụ {@code UserEntity.class}
     * @return builder mới
     * @throws NullPointerException nếu target null
     */
    public static Builder builder(Class<?> target) {
        return new Builder(Objects.requireNonNull(target, "target must not be null"));
    }

    /**
     * Tạo builder chứa sẵn toàn bộ cấu hình hiện tại, dùng để dựng một spec mở rộng từ spec này.
     *
     * @return builder mới mang cấu hình của spec này
     */
    public Builder toBuilder() {
        Builder builder = new Builder(this.target);
        builder.filters.putAll(this.filters);
        builder.sortables.addAll(this.sortables);
        builder.defaultSortProperty = this.defaultSortProperty;
        builder.defaultSortDirection = this.defaultSortDirection;
        builder.tieBreaker = this.tieBreaker;
        builder.defaultLimit = this.defaultLimit;
        builder.maxLimit = this.maxLimit;
        return builder;
    }

    /**
     * Kiểm tra payload của client theo whitelist và sinh ra truy vấn đã được kiểm tra.
     *
     * @param request payload của client, có thể null
     * @return truy vấn đã được kiểm tra
     * @throws QueryException nếu client gửi giá trị sai kiểu cho một bộ lọc hợp lệ,
     *                        quá nhiều giá trị cho bộ lọc IN, hoặc chiều sắp xếp không hợp lệ
     */
    public SearchQuery resolve(SearchRequest request) {
        SearchRequest req = request == null ? SearchRequest.empty() : request;

        int page = req.page() == null || req.page() < 0 ? 0 : req.page();
        int limit = resolveLimit(req.limit());

        List<Criterion> criteria = new ArrayList<>();
        for (FilterInput input : req.filters()) {
            resolveFilter(input).ifPresent(criteria::add);
        }

        return new SearchQuery(page, limit, criteria, resolveSort(req.sortBy(), req.sortDir()));
    }

    /** Dùng defaultLimit khi thiếu hoặc không dương, ép về maxLimit khi vượt quá. */
    private int resolveLimit(Integer requested) {
        if (requested == null || requested <= 0) {
            return defaultLimit;
        }
        return Math.min(requested, maxLimit);
    }

    /** Trả về điều kiện lọc nếu bộ lọc hợp lệ, rỗng nếu cần bỏ qua. */
    private Optional<Criterion> resolveFilter(FilterInput input) {
        if (isBlank(input.property()) || isBlank(input.value())) {
            return Optional.empty();
        }
        FilterDefinition definition = filters.get(input.property());
        if (definition == null) {
            return Optional.empty();
        }

        Operator operator = resolveOperator(definition, input.operator());

        Object value = operator == Operator.IN
                ? convertMany(definition, input.value())
                : convertOne(definition, input.value());

        if (value instanceof List<?> values && values.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new Criterion(definition.property(), operator, value));
    }

    /** Dùng operator mặc định nếu client không gửi; ngược lại operator phải nằm trong danh sách cho phép. */
    private static Operator resolveOperator(FilterDefinition definition, String raw) {
        if (isBlank(raw)) {
            return definition.defaultOperator();
        }
        Operator requested = null;
        for (Operator candidate : definition.operators()) {
            if (candidate.name().equalsIgnoreCase(raw.trim())) {
                requested = candidate;
                break;
            }
        }
        if (requested == null) {
            throw new QueryException(QueryErrorCode.QUERY_OPERATOR_NOT_ALLOWED, raw.trim(), definition.property());
        }
        return requested;
    }

    /** Chuyển một giá trị bằng hàm đã chọn sẵn, bọc mọi lỗi chuyển đổi thành QueryException. */
    private Object convertOne(FilterDefinition definition, String raw) {
        try {
            return definition.parser().apply(raw.trim());
        } catch (RuntimeException e) {
            throw new QueryException(QueryErrorCode.QUERY_FILTER_VALUE_INVALID, e, raw.trim(), definition.property());
        }
    }

    /** Tách chuỗi theo dấu phẩy, bỏ phần rỗng, rồi chuyển từng giá trị. */
    private List<Object> convertMany(FilterDefinition definition, String raw) {
        List<String> parts = new ArrayList<>();
        for (String part : raw.split(",")) {
            if (!part.isBlank()) {
                parts.add(part);
            }
        }
        if (parts.size() > MAX_IN_VALUES) {
            throw new QueryException(QueryErrorCode.QUERY_TOO_MANY_VALUES, definition.property(), MAX_IN_VALUES);
        }
        List<Object> values = new ArrayList<>();
        for (String part : parts) {
            values.add(convertOne(definition, part));
        }
        return values;
    }

    /** Chọn thuộc tính sắp xếp (client hoặc mặc định), rồi thêm tieBreaker vào cuối. */
    private List<SortOrder> resolveSort(String sortBy, String sortDir) {
        List<SortOrder> result = new ArrayList<>();

        String requestedField = isBlank(sortBy) || !sortables.contains(sortBy.trim()) ? null : sortBy.trim();
        String field;
        SortDirection fallbackDirection;

        if (requestedField != null) {
            field = requestedField;
            fallbackDirection = SortDirection.ASC;
        } else if (defaultSortProperty != null) {
            field = defaultSortProperty;
            fallbackDirection = defaultSortDirection;
        } else {
            field = null;
            fallbackDirection = null;
        }

        if (field != null) {
            SortDirection direction = isBlank(sortDir) ? fallbackDirection : parseDirection(sortDir);
            result.add(new SortOrder(field, direction));
        }

        if (tieBreaker != null && (field == null || !field.equals(tieBreaker))) {
            result.add(new SortOrder(tieBreaker, SortDirection.ASC));
        }
        return result;
    }

    /** Đọc chiều sắp xếp, bọc lỗi thành QueryException. */
    private static SortDirection parseDirection(String raw) {
        try {
            return SortDirection.parse(raw);
        } catch (IllegalArgumentException e) {
            throw new QueryException(QueryErrorCode.QUERY_SORT_DIRECTION_INVALID, e, raw.trim());
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    /**
     * Builder để khai báo SearchSpec. Mọi tên thuộc tính đều được kiểm tra với class target.
     */
    public static final class Builder {

        private final Class<?> target;
        private final Map<String, FilterDefinition> filters = new LinkedHashMap<>();
        private final Set<String> sortables = new LinkedHashSet<>();
        private String defaultSortProperty;
        private SortDirection defaultSortDirection = SortDirection.ASC;
        private String tieBreaker = "id";
        private int defaultLimit = 20;
        private int maxLimit = 100;

        private Builder(Class<?> target) {
            this.target = target;
        }

        /**
         * Cho phép lọc theo một thuộc tính. Kiểu giá trị được đọc từ class target.
         *
         * @param property tên thuộc tính, cũng là tên client gửi; được phép dạng lồng "department.name"
         * @param operator phép so sánh mặc định, dùng khi client không chỉ định operator
         * @param more     các phép so sánh khác client được phép chọn (ví dụ GTE rồi LTE để lọc khoảng)
         * @return chính builder này
         * @throws IllegalArgumentException nếu thuộc tính không tồn tại, kiểu chưa hỗ trợ,
         *                                  operator không hợp với kiểu, hoặc khai báo trùng
         */
        public Builder filter(String property, Operator operator, Operator... more) {
            requireText(property, "property");
            Objects.requireNonNull(operator, "operator must not be null");

            Class<?> type = PropertyResolver.resolveType(target, property);

            Function<String, Object> parser;
            try {
                parser = ValueConverter.parserFor(type);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(
                        "Type " + type.getName() + " is not supported for filter '" + property + "'", e);
            }

            Set<Operator> allowed = new LinkedHashSet<>();
            allowed.add(operator);
            for (Operator extra : more) {
                if (!allowed.add(Objects.requireNonNull(extra, "operator must not be null"))) {
                    throw new IllegalArgumentException(
                            "Operator " + extra + " is listed twice, filter '" + property + "'");
                }
            }
            for (Operator each : allowed) {
                checkOperatorFitsType(each, type, property);
            }
            if (filters.containsKey(property)) {
                throw new IllegalArgumentException("Filter '" + property + "' is already declared");
            }

            filters.put(property, new FilterDefinition(property, operator, Set.copyOf(allowed), parser));
            return this;
        }

        private static void checkOperatorFitsType(Operator operator, Class<?> type, String property) {
            if (operator.isTextMatch() && type != String.class) {
                throw new IllegalArgumentException(
                        "Operator " + operator + " requires a String property, filter '" + property + "'");
            }
            if (operator.isComparison()
                    && (!Comparable.class.isAssignableFrom(type) || type.isEnum() || type == Boolean.class)) {
                throw new IllegalArgumentException(
                        "Operator " + operator + " requires a number, text or date property, filter '" + property + "'");
            }
        }

        /**
         * Cho phép sắp xếp theo một hoặc nhiều thuộc tính.
         *
         * @param properties tên các thuộc tính
         * @return chính builder này
         * @throws IllegalArgumentException nếu thuộc tính không tồn tại hoặc khai báo trùng
         */
        public Builder sortable(String... properties) {
            for (String property : properties) {
                requireText(property, "property");
                PropertyResolver.resolveType(target, property);
                if (!sortables.add(property)) {
                    throw new IllegalArgumentException("Sortable '" + property + "' is already declared");
                }
            }
            return this;
        }

        /**
         * Sắp xếp mặc định khi client không gửi sortBy hợp lệ.
         * Thuộc tính phải được khai báo bằng {@link #sortable(String...)}, kiểm tra khi {@link #build()}.
         *
         * @param property  tên thuộc tính
         * @param direction chiều sắp xếp
         * @return chính builder này
         */
        public Builder defaultSort(String property, SortDirection direction) {
            requireText(property, "property");
            this.defaultSortProperty = property;
            this.defaultSortDirection = Objects.requireNonNull(direction, "direction must not be null");
            return this;
        }

        /**
         * Thuộc tính luôn được thêm vào cuối sắp xếp để phân trang ổn định. Mặc định là "id".
         *
         * @param property tên thuộc tính; truyền null để tắt, ví dụ khi class không có thuộc tính id
         * @return chính builder này
         */
        public Builder tieBreaker(String property) {
            this.tieBreaker = property;
            return this;
        }

        /**
         * Số phần tử mỗi trang khi client không gửi limit. Mặc định là 20.
         *
         * @param defaultLimit số phần tử, trong khoảng 1 đến maxLimit
         * @return chính builder này
         */
        public Builder defaultLimit(int defaultLimit) {
            this.defaultLimit = defaultLimit;
            return this;
        }

        /**
         * Số phần tử tối đa mỗi trang. Mặc định là 100.
         *
         * @param maxLimit số phần tử tối đa, lớn hơn 0
         * @return chính builder này
         */
        public Builder maxLimit(int maxLimit) {
            this.maxLimit = maxLimit;
            return this;
        }

        /**
         * Kiểm tra cấu hình và tạo SearchSpec bất biến.
         *
         * @return SearchSpec đã cấu hình
         * @throws IllegalStateException    nếu giới hạn không hợp lệ hoặc defaultSort chưa khai báo sortable
         * @throws IllegalArgumentException nếu tieBreaker không tồn tại trong class target
         */
        public SearchSpec build() {
            if (maxLimit <= 0) {
                throw new IllegalStateException("maxLimit must be positive");
            }
            if (defaultLimit <= 0 || defaultLimit > maxLimit) {
                throw new IllegalStateException("defaultLimit must be between 1 and " + maxLimit);
            }
            if (defaultSortProperty != null && !sortables.contains(defaultSortProperty)) {
                throw new IllegalStateException("defaultSort '" + defaultSortProperty + "' is not declared as sortable");
            }
            if (tieBreaker != null) {
                PropertyResolver.resolveType(target, tieBreaker);
            }
            return new SearchSpec(this);
        }

        private static void requireText(String value, String name) {
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException(name + " must not be blank");
            }
        }
    }
}
