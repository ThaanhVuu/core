package kernel.ddd;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Lớp cơ sở cho aggregate root: entity gốc đảm bảo tính nhất quán của cả aggregate.
 *
 * <p>Aggregate root thu thập các {@link DomainEvent} phát sinh trong quá trình xử lý và giữ
 * phiên bản phục vụ optimistic locking.
 *
 * @param <ID> kiểu định danh của aggregate
 */
public abstract class AggregateRoot<ID> extends Entity<ID> {
    private final List<DomainEvent> domainEvents = new ArrayList<>();

    private Long version;

    /**
     * Khởi tạo aggregate với định danh cho trước.
     *
     * @param id định danh của aggregate
     * @throws NullPointerException nếu {@code id} là {@code null}
     */
    protected AggregateRoot(ID id) {
        super(id);
    }

    /**
     * Ghi nhận một event nhưng chưa phát ngay. Tầng ngoài sẽ lấy ra bằng
     * {@link #pullDomainEvents()} và phát sau khi lưu thành công.
     *
     * @param event event phát sinh
     * @throws NullPointerException nếu {@code event} là {@code null}
     */
    protected void raise(DomainEvent event) {
        domainEvents.add(Objects.requireNonNull(event, "Event không được null"));
    }

    /**
     * Lấy toàn bộ event đang chờ và xoá chúng khỏi aggregate.
     *
     * @return danh sách event không thể sửa đổi, theo thứ tự ghi nhận; rỗng nếu không có event nào
     */
    public List<DomainEvent> pullDomainEvents() {
        List<DomainEvent> events = List.copyOf(domainEvents);
        domainEvents.clear();
        return events;
    }

    /**
     * Trả về phiên bản hiện tại của aggregate, dùng cho optimistic locking.
     *
     * @return phiên bản, hoặc {@code null} nếu aggregate mới và chưa từng được lưu
     */
    public Long getVersion() {
        return version;
    }

    /**
     * Gán phiên bản cho aggregate. Chỉ gọi khi khôi phục aggregate từ database,
     * thông qua factory {@code reconstitute} của lớp con.
     *
     * @param version phiên bản đã lưu trong database
     */
    protected void assignVersion(Long version) {
        this.version = version;
    }
}
