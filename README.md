# kernel

Bộ **shared kernel** dùng chung cho các service Java: 4 thư viện nhỏ, mỗi thư viện một nhiệm vụ, publish vào Maven local để service khai báo và dùng.

| Artifact | Package | Nhiệm vụ | Phụ thuộc |
|---|---|---|---|
| `kernel:common` | `kernel.common` | Hệ thống lỗi dùng chung, sinh UUIDv7 | Không (Java thuần) |
| `kernel:ddd` | `kernel.ddd` | Khối nền DDD: Entity, AggregateRoot, Value Object, Domain Event | `common` |
| `kernel:query` | `kernel.query` | Tìm kiếm động: whitelist bộ lọc, kiểm tra và chuyển kiểu tham số, phân trang | `common` |
| `kernel:query-jpa` | `kernel.query.jpa` | Adapter: chạy truy vấn của `query` bằng Spring Data JPA | `query`, `common`, Spring Data JPA (`compileOnly`) |

Yêu cầu: Java 25, Gradle (Groovy DSL).

---

## Mục lục

1. [Sơ đồ phụ thuộc](#1-sơ-đồ-phụ-thuộc)
2. [Cài đặt](#2-cài-đặt)
3. [kernel:common](#3-kernelcommon)
4. [kernel:ddd](#4-kernelddd)
5. [kernel:query](#5-kernelquery)
6. [kernel:query-jpa](#6-kernelquery-jpa)
7. [Luồng tìm kiếm đầy đủ](#7-luồng-tìm-kiếm-đầy-đủ)
8. [Luồng lỗi](#8-luồng-lỗi)
9. [Quy ước](#9-quy-ước)
10. [Lỗi thường gặp](#10-lỗi-thường-gặp)

---

## 1. Sơ đồ phụ thuộc

```
                 kernel:common
                  ▲         ▲
                  │         │
          kernel:ddd     kernel:query
                              ▲
                              │
                       kernel:query-jpa ──► Spring Data JPA (compileOnly)
```

- `ddd` và `query` **không biết nhau**: service chỉ cần tìm kiếm thì không phải kéo phần DDD, và ngược lại.
- `common`, `ddd`, `query` là **Java thuần**, không phụ thuộc framework nào.
- Chỉ `query-jpa` biết Spring. Muốn đổi công nghệ truy vấn thì viết adapter khác, ba thư viện kia giữ nguyên.
- Phụ thuộc giữa các thư viện khai báo bằng `implementation`, **không tự lan sang service**. Service phải khai báo tường minh mọi thư viện nó dùng.

---

## 2. Cài đặt

### 2.1. Publish vào Maven local

Mỗi thư viện là một project Gradle riêng. Publish **theo thứ tự phụ thuộc**, vì thư viện sau lấy thư viện trước từ `~/.m2`:

```bash
cd common    && ./gradlew publishToMavenLocal
cd ../ddd       && ./gradlew publishToMavenLocal
cd ../query     && ./gradlew publishToMavenLocal
cd ../query-jpa && ./gradlew publishToMavenLocal
```

Mỗi thư viện publish kèm `-sources.jar` và `-javadoc.jar` để IDE bên service hiện được Javadoc.

Sửa thư viện nào thì publish lại thư viện đó (và các thư viện phụ thuộc nó nếu API thay đổi), rồi **Reload Gradle Project** ở service.

### 2.2. Khai báo ở service

```groovy
repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation 'kernel:common:1.0.0'
    implementation 'kernel:ddd:1.0.0'          // nếu dùng khối nền DDD
    implementation 'kernel:query:1.0.0'        // nếu dùng tìm kiếm động
    implementation 'kernel:query-jpa:1.0.0'    // nếu tìm kiếm bằng JPA

    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'   // bắt buộc khi dùng query-jpa
}
```

| Service cần | Khai báo |
|---|---|
| Chỉ hệ thống lỗi | `common` |
| Viết domain theo DDD | `common` + `ddd` |
| Tìm kiếm động bằng JPA | `common` + `query` + `query-jpa` + starter Data JPA |
| Tất cả | cả 4 |

---

## 3. kernel:common

**Nhiệm vụ:** cho mọi thư viện và service một cách báo lỗi thống nhất, và một cách sinh ID thống nhất.

| Class | Nhiệm vụ |
|---|---|
| `ErrorType` | Loại lỗi: `VALIDATION`, `NOT_FOUND`, `CONFLICT`, `BUSINESS_RULE`, `UNAUTHORIZED`, `FORBIDDEN`, `INTERNAL`. Tầng API dựa vào đây để chọn HTTP status, nên domain không cần biết HTTP |
| `ErrorCode` | Interface cho mã lỗi. Mỗi module có **một enum** implement nó. `code()` mặc định là tên hằng số, `formatMessage(...)` điền tham số vào mẫu thông báo |
| `AppException` | Exception gốc, luôn gắn một `ErrorCode`. Có constructor nhận `cause` để giữ lỗi gốc. Tạo thông báo an toàn: mẫu và tham số không khớp thì dùng mẫu gốc, không bao giờ làm mất mã lỗi |
| `UuidV7` | Sinh UUID phiên bản 7 (RFC 9562): 48 bit đầu là thời gian, nên ID sắp xếp được theo thời điểm tạo và hợp làm khoá chính |

### Khai báo mã lỗi

```java
public enum OrderErrorCode implements ErrorCode {

    ORDER_NOT_FOUND(ErrorType.NOT_FOUND, "Order %s not found"),
    ORDER_ALREADY_PAID(ErrorType.BUSINESS_RULE, "Order %s is already paid");

    private final ErrorType type;
    private final String messageTemplate;

    OrderErrorCode(ErrorType type, String messageTemplate) {
        this.type = type;
        this.messageTemplate = messageTemplate;
    }

    @Override public ErrorType type() { return type; }
    @Override public String messageTemplate() { return messageTemplate; }
}
```

- Constructor enum chỉ có **2 tham số**. `name()` enum có sẵn, `code()` dùng bản mặc định.
- Đặt **tiền tố** theo module (`ORDER_`) để mã lỗi không trùng giữa các module.
- **Không đổi tên hằng số** sau khi đã dùng: tên đó là `code` phía client dựa vào.

### Khi nào dùng exception nào

| Tình huống | Dùng |
|---|---|
| Người dùng gây ra, tự sửa được | Lớp con của `AppException` kèm mã lỗi |
| Bug của lập trình viên (tham số null, trạng thái không thể xảy ra) | `Objects.requireNonNull`, `IllegalArgumentException`, `IllegalStateException` |

---

## 4. kernel:ddd

**Nhiệm vụ:** các lớp nền để viết domain theo DDD, không phụ thuộc framework.

| Class | Nhiệm vụ |
|---|---|
| `Entity<ID>` | Đối tượng có định danh. Hai entity bằng nhau khi **cùng ID** (`equals`/`hashCode` theo ID, đánh dấu `final`) |
| `AggregateRoot<ID>` | Điểm vào duy nhất của một aggregate. Giữ **domain event chờ phát** (`raise` để ghi nhận, `pullDomainEvents` để lấy ra) và `version` cho optimistic locking (`null` nghĩa là chưa lưu lần nào). `assignVersion` là `protected` |
| `ValueObject` | Interface đánh dấu. Cài đặt bằng `record`: bất biến, so sánh theo giá trị |
| `DomainEvent` | Sự kiện nghiệp vụ đã xảy ra (`eventId`, `occurredOn`). Đặt tên thì quá khứ |
| `DomainException` | Lỗi vi phạm quy tắc nghiệp vụ, lớp con của `AppException` |

### Mẫu một aggregate

```java
public class Order extends AggregateRoot<UUID> {

    private final CustomerId customerId;
    private OrderStatus status;

    // private: chỉ tạo qua create hoặc reconstitute
    private Order(UUID id, CustomerId customerId, OrderStatus status) {
        super(id);
        this.customerId = Objects.requireNonNull(customerId, "customerId must not be null");
        this.status = status;
    }

    /** Tạo mới: sinh ID, phát event. */
    public static Order create(CustomerId customerId) {
        Order order = new Order(UuidV7.generate(), customerId, OrderStatus.PENDING);
        order.raise(new OrderCreated(order.getId(), customerId));
        return order;
    }

    /** Khôi phục từ database: không sinh ID mới, không phát event, gán version. */
    public static Order reconstitute(UUID id, CustomerId customerId, OrderStatus status, Long version) {
        Order order = new Order(id, customerId, status);
        order.assignVersion(version);
        return order;
    }

    /** Dữ liệu chỉ đổi qua hành vi có ý nghĩa nghiệp vụ, không có setter. */
    public void pay() {
        if (status == OrderStatus.PAID) {
            throw new DomainException(OrderErrorCode.ORDER_ALREADY_PAID, getId());
        }
        status = OrderStatus.PAID;
        raise(new OrderPaid(getId()));
    }
}
```

### Mẫu một value object

```java
public record Email(String value) implements ValueObject {

    public Email {
        if (value == null || !value.contains("@")) {
            throw new DomainException(CustomerErrorCode.CUSTOMER_EMAIL_INVALID, value);
        }
        value = value.trim().toLowerCase(Locale.ROOT);
    }
}
```

Compact constructor kiểm tra và chuẩn hoá, nên một `Email` sai **không bao giờ tồn tại**.

### Luồng ghi điển hình

```
application service                          (mở transaction)
  ├─ repository.findById(id)     → aggregate qua reconstitute (có version)
  ├─ aggregate.pay()             → kiểm tra quy tắc, đổi trạng thái, raise event
  ├─ repository.save(aggregate)  → version dùng cho optimistic locking
  └─ aggregate.pullDomainEvents() → phát event SAU KHI lưu thành công
```

Event chỉ được **ghi nhận** trong aggregate và **phát sau khi lưu**, để không bao giờ phát một sự kiện chưa thực sự xảy ra.

---

## 5. kernel:query

**Nhiệm vụ:** cho client lọc, sắp xếp, phân trang, nhưng **chỉ trong phạm vi server cho phép**, và biến tham số thô của client thành truy vấn đã kiểm tra.

### 5.1. Các class

| Class | Nhiệm vụ | Chạy khi nào |
|---|---|---|
| `SearchSpec` | **Whitelist** của một API: được lọc theo thuộc tính nào, operator nào, sort theo gì, giới hạn bao nhiêu. `resolve()` là cổng kiểm soát | Build lúc khởi động; `resolve` mỗi request |
| `PropertyResolver` | Dùng reflection tìm kiểu của thuộc tính (hỗ trợ lớp cha, thuộc tính lồng `a.b`). Tên sai → lỗi ngay lúc khởi động | Lúc khởi động |
| `ValueConverter` | Chọn hàm đổi chuỗi sang đúng kiểu (`Instant::parse`, `UUID::fromString`, enum...) | Lúc khởi động |
| `FilterDefinition` | Một dòng trong whitelist: tên, operator mặc định và các operator được phép, hàm chuyển đổi đã chọn sẵn | Lưu từ lúc khởi động |
| `SearchRequest`, `FilterInput` | Payload **thô** của client, chưa tin | Mỗi request |
| `Criterion`, `SortOrder`, `SearchQuery` | Truy vấn **đã sạch**, bất biến | Mỗi request |
| `PageResult<T>` | Kết quả kèm `total`, `totalPages`; `map()` đổi phần tử sang kiểu khác | Mỗi request |
| `Operator` | `EQ, NE, CONTAINS, STARTS_WITH, GT, GTE, LT, LTE, IN` | |
| `SortDirection` | `ASC, DESC` | |
| `QueryException`, `QueryErrorCode` | Lỗi tham số tìm kiếm → `VALIDATION` | Mỗi request |

`PropertyResolver`, `ValueConverter`, `FilterDefinition` là package-private: bên ngoài không dùng trực tiếp.

### 5.2. Khai báo một SearchSpec

```java
private static final SearchSpec ORDER_SEARCH = SearchSpec.builder(OrderEntity.class)
        .filter("status", Operator.IN)
        .filter("createdAt", Operator.GTE, Operator.LTE)
        .filter("customer.name", Operator.CONTAINS)
        .sortable("createdAt", "total")
        .defaultSort("createdAt", SortDirection.DESC)
        .maxLimit(100)
        .build();
```

- Tên thuộc tính là **tên field Java** trong class truyền vào `builder`, cũng là tên client gửi.
- **Không cần khai báo kiểu**: `PropertyResolver` tự tìm (`status` là enum, `createdAt` là `Instant`...).
- Mỗi API một `SearchSpec`, khai báo `private static final`. API mở rộng từ API khác dùng `toBuilder()`.

| Method của Builder | Tác dụng | Mặc định |
|---|---|---|
| `filter(property, operator, more...)` | Cho phép lọc theo thuộc tính; operator đầu là mặc định, các operator sau là lựa chọn thêm cho client | |
| `sortable(properties...)` | Cho phép sắp xếp | |
| `defaultSort(property, direction)` | Sắp xếp khi client không gửi `sortBy` hợp lệ | Không có |
| `tieBreaker(property)` | Thuộc tính sắp xếp phụ để phân trang ổn định; `null` để tắt | `"id"` |
| `defaultLimit(n)` | Số phần tử khi client không gửi `limit` | 20 |
| `maxLimit(n)` | Số phần tử tối đa mỗi trang | 100 |

`build()` kiểm tra toàn bộ cấu hình. Sai → ứng dụng **không khởi động**.

| Kiểu thuộc tính | Operator hợp lệ |
|---|---|
| `String` | tất cả |
| Số, `BigDecimal`, `Instant`, `LocalDate`, `LocalDateTime` | tất cả trừ `CONTAINS`, `STARTS_WITH` |
| enum | `EQ, NE, IN` |
| `UUID`, `Boolean` | `EQ, NE, IN` |
| Thuộc tính lồng (`customer.name`) | theo kiểu của thuộc tính cuối |
| Đi xuyên collection (`items.name`) | chưa hỗ trợ |

Mỗi thuộc tính khai báo **một lần**, có thể kèm nhiều operator, ví dụ lọc khoảng ngày: `.filter("createdAt", Operator.GTE, Operator.LTE)`. Client gửi thêm `"operator": "lte"` (không phân biệt hoa thường); không gửi thì dùng operator đầu tiên. Operator ngoài danh sách → `QUERY_OPERATOR_NOT_ALLOWED`.

### 5.3. Hợp đồng với client

```json
{
  "page": 0,
  "limit": 20,
  "filters": [
    { "property": "status", "value": "pending,paid" },
    { "property": "createdAt", "operator": "gte", "value": "2026-01-01T00:00:00Z" },
    { "property": "createdAt", "operator": "lte", "value": "2026-01-31T23:59:59Z" }
  ],
  "sortBy": "createdAt",
  "sortDir": "DESC"
}
```

| Trường hợp | `resolve` xử lý |
|---|---|
| Thuộc tính không có trong whitelist | Bỏ qua |
| Bộ lọc rỗng, giá trị rỗng | Bỏ qua |
| Thuộc tính hợp lệ nhưng giá trị sai kiểu | `QueryException` `QUERY_FILTER_VALUE_INVALID` |
| `IN` với hơn 100 giá trị | `QueryException` `QUERY_TOO_MANY_VALUES` |
| `operator` không nằm trong danh sách cho phép của thuộc tính | `QueryException` `QUERY_OPERATOR_NOT_ALLOWED` |
| `sortDir` không phải ASC/DESC | `QueryException` `QUERY_SORT_DIRECTION_INVALID` |
| `page` thiếu hoặc âm | 0 |
| `limit` thiếu hoặc không dương / vượt `maxLimit` | `defaultLimit` / `maxLimit` |
| `sortBy` thiếu hoặc không khai báo | `defaultSort` |
| Luôn luôn | Thêm `tieBreaker ASC` vào cuối sort |

### 5.4. Điều kiện do server ép buộc

```java
SearchQuery query = ORDER_SEARCH.resolve(request)
        .withCriterion("customerId", Operator.EQ, currentCustomerId);
```

`withCriterion` trả `SearchQuery` mới, ghép bằng AND. Client không bỏ hay ghi đè được. Nên đặt trong service, không đặt ở controller.

---

## 6. kernel:query-jpa

**Nhiệm vụ:** dịch `SearchQuery` sang Spring Data JPA và trả về `PageResult`.

| Class | Nhiệm vụ |
|---|---|
| `SearchRepository<E>` | Repository kế thừa để có sẵn `search(query)` và `search(query, scope)`. Dựng `Specification`, `Sort`, `Pageable`, gọi `findAll`, đổi `Page` thành `PageResult`. Là default method nên repository không cần khai báo hay viết implementation |
| `JpaSpecifications` | Dịch từng `Criterion` sang `Predicate`. `CONTAINS`/`STARTS_WITH` không phân biệt hoa thường và escape `%`, `_`, `\` |

### Cách dùng

Repository kế thừa `SearchRepository`:

```java
public interface OrderJpaRepository
        extends JpaRepository<OrderEntity, UUID>, SearchRepository<OrderEntity> {
}
```

Service:

```java
@Service
@Transactional(readOnly = true)
public class OrderQueryService {

    private final OrderJpaRepository repository;

    public OrderQueryService(OrderJpaRepository repository) {
        this.repository = repository;
    }

    /** Mỗi use case một method public, luôn trả DTO. */
    public PageResult<OrderDto> searchMyOrders(SearchQuery query, UUID customerId) {
        return repository.search(query.withCriterion("customerId", Operator.EQ, customerId))
                .map(OrderDto::from);
    }
}
```

- Đổi sang DTO (`.map`) **bên trong** method có `@Transactional`, để dữ liệu lazy vẫn load được.
- Controller không gọi thẳng `repository.search`: điều kiện bắt buộc của use case (`withCriterion`) và việc đổi sang DTO nằm ở service.
- Điều kiện phức tạp (OR, subquery) mà `Criterion` không diễn đạt được: viết `Specification` riêng rồi gọi `repository.search(query, scope)`.

---

## 7. Luồng tìm kiếm đầy đủ

### Lúc khởi động (một lần)

```
SearchSpec.builder(OrderEntity.class).filter("createdAt", GTE)...build()
  ├─ PropertyResolver.resolveType   "createdAt" có kiểu gì?          → Instant
  ├─ ValueConverter.parserFor       đổi chuỗi sang Instant thế nào?  → Instant::parse
  ├─ kiểm tra GTE hợp với Instant
  └─ lưu FilterDefinition vào SearchSpec (bất biến, dùng lại cho mọi request)
```

Việc tốn công (reflection, chọn hàm) làm hết ở đây. Mỗi request không còn reflection.

### Mỗi request

```
── kernel:query ─────────────────────────────────────────────
SearchRequest (JSON thô)
  │
  ▼
SearchSpec.resolve(request)                     cổng kiểm soát
  ├─ page, limit
  ├─ mỗi filter: tra whitelist → gọi hàm đã lưu sẵn → Criterion
  ├─ sort: client hoặc mặc định, thêm tieBreaker
  ▼
SearchQuery (đã sạch)
  │  (service có thể thêm withCriterion)
  ▼
── kernel:query-jpa ─────────────────────────────────────────
SearchRepository.search
  ├─ JpaSpecifications.of   → Specification (công thức, chưa chạy)
  ├─ toSort + toPageable    → Pageable
  └─ findAll                → Hibernate gọi công thức 2 lần (SELECT + COUNT)
  ▼
PageResult<Entity> → .map(Dto::from) → PageResult<Dto>
```

Cách nhớ: **`query` quyết định "được hỏi gì", `query-jpa` quyết định "hỏi database thế nào".**

---

## 8. Luồng lỗi

Mọi lỗi có mã đều là `AppException` (`DomainException`, `QueryException` là lớp con), nên service chỉ cần **một** handler:

```java
@ExceptionHandler(AppException.class)
public ResponseEntity<ErrorResponse> handleApp(AppException ex) {
    ErrorCode error = ex.getErrorCode();
    return ResponseEntity
            .status(toStatus(error.type()))
            .body(new ErrorResponse(error.code(), ex.getMessage()));
}
```

| `ErrorType` | HTTP gợi ý |
|---|---|
| `VALIDATION` | 400 |
| `UNAUTHORIZED` | 401 |
| `FORBIDDEN` | 403 |
| `NOT_FOUND` | 404 |
| `CONFLICT` | 409 |
| `BUSINESS_RULE` | 422 |
| `INTERNAL` | 500 |

Lỗi không phải `AppException` (bug, mất kết nối...) nên được bắt riêng, ghi log đầy đủ và trả 500 chung chung, không lộ chi tiết.

Client dựa vào `code` để xử lý và hiển thị. `message` (tiếng Anh) dùng cho log và debug.

---

## 9. Quy ước

- **Javadoc viết tiếng Việt**, **message của exception viết tiếng Anh**.
- Mỗi class có phần "Các hàm" trong Javadoc đầu file, liệt kê tác dụng từng hàm.
- ID sinh bằng `UuidV7.generate()` ngay khi tạo aggregate.
- Aggregate: constructor `private`, tạo qua `create(...)`, khôi phục qua `reconstitute(...)`, **không có setter**.
- Value object dùng `record`, kiểm tra trong compact constructor.
- Code domain không import Spring hay JPA.
- `SearchSpec` khai báo `private static final`.
- Controller không gọi thẳng `repository.search`, luôn đi qua method use case của service.

---

## 10. Lỗi thường gặp

| Hiện tượng | Nguyên nhân | Cách sửa |
|---|---|---|
| Không khởi động: `Property 'xxx' not found in ...` | Tên sai trong `SearchSpec`, hoặc dùng tên cột database thay vì tên field | Sửa đúng tên field Java |
| Không khởi động: `Property 'id' not found` | Class không có field `id` mà vẫn dùng tie-breaker mặc định | `.tieBreaker(null)` hoặc chỉ ra field khác |
| Không khởi động: `Operator CONTAINS requires a String property` | Operator không hợp với kiểu | Đổi operator |
| Không khởi động: `... is a collection, traversing it is not supported` | Lọc xuyên qua `List`/`Set` | Chưa hỗ trợ; viết `Specification` riêng qua `scope` |
| Bộ lọc client gửi không có tác dụng | Thuộc tính không nằm trong whitelist, hoặc sai key JSON (`filters`, `sortBy`, `sortDir`) | Khai báo thêm, hoặc sửa payload |
| 400 `QUERY_FILTER_VALUE_INVALID` | Giá trị sai định dạng, ví dụ ngày không theo ISO-8601 | Sửa giá trị ở client |
| `Expected 3 arguments but found 2` ở enum mã lỗi | Constructor enum có thừa tham số | Constructor chỉ nhận `(ErrorType, String)` |
| `LazyInitializationException` | Đổi entity sang DTO ngoài transaction | Gọi `.map(Dto::from)` trong service có `@Transactional` |
| IDE hiện "Source code recreated from a .class file" | Thiếu `-sources.jar` | Kiểm tra `withSourcesJar()`, publish lại, Reload Gradle hoặc Download Sources |
| Service không nhận thay đổi của kernel | Chưa publish lại | `./gradlew publishToMavenLocal`, rồi Reload Gradle |
| Lỗi biên dịch thiếu class `kernel.common...` ở service | Phụ thuộc giữa các thư viện là `implementation`, không lan sang service | Khai báo thêm `kernel:common` trong service |
| `ClassNotFoundException` của Spring Data khi chạy | Service thiếu `spring-boot-starter-data-jpa` | Thêm dependency |
