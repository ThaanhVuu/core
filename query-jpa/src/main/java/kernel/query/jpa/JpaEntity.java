package kernel.query.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.domain.Persistable;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

/**
 * Base class JPA dùng chung cho mọi service. Entity nào cần {@code createdAt}/{@code modifyAt}
 * tự động và optimistic locking chỉ cần {@code extends JpaEntity<ID>}, không phải khai báo lại
 * các field này ở từng service.
 *
 * <h2>Entity con phải làm gì</h2>
 *
 * <p>Khai báo field {@code @Id} và getter {@code getId()} tương ứng (thường qua Lombok). Đó là
 * method duy nhất của {@link Persistable} mà class này không implement. {@code ID} thường là
 * {@code UUID} (UUIDv7 sinh ở domain, gán tay, không dùng {@code @GeneratedValue}).
 *
 * <h2>Auditing: {@code createdAt} / {@code modifyAt}</h2>
 *
 * <p>Do {@link AuditingEntityListener} của Spring Data JPA tự set, không cần code nào gán tay.
 * Listener chỉ hoạt động khi {@code @EnableJpaAuditing} được bật; việc này do
 * {@link JpaAuditingConfig} đảm nhận qua cơ chế auto-configuration (đăng ký trong
 * {@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports}),
 * service không phải tự khai báo.
 *
 * <ul>
 *   <li>Insert ({@code @PrePersist}): set cả {@code createdAt} và {@code modifyAt}.</li>
 *   <li>Update ({@code @PreUpdate}): chỉ set {@code modifyAt}; {@code createdAt} có
 *       {@code updatable = false} nên không bao giờ bị ghi đè trong DB.</li>
 * </ul>
 *
 * <p><b>Thời điểm:</b> các callback chạy lúc Hibernate <i>flush</i> (thường là khi commit
 * transaction), không phải lúc gọi {@code save()}. Update chỉ xảy ra khi entity thực sự dirty;
 * không có field nào đổi thì không có UPDATE, {@code modifyAt} và {@code version} giữ nguyên.
 * Cần giá trị mới ngay sau khi ghi thì dùng {@code saveAndFlush()}.
 *
 * <h2>{@code version}: hai việc trong một field</h2>
 *
 * <p><b>1. Optimistic locking.</b> Không có nó, hai request đồng thời trên cùng một bản ghi sẽ
 * ghi đè nhau im lặng: cả hai đọc state cũ, cả hai ghi lại toàn bộ, và thay đổi của request đến
 * trước biến mất mà không có exception hay log nào. Với {@code @Version}, Hibernate đưa
 * {@code WHERE version = ?} vào câu UPDATE, tăng version lên 1, và ném
 * {@code OptimisticLockingFailureException} khi không khớp (web-kernel map thành HTTP 409).
 *
 * <p><b>2. Phân biệt insert với update mà không cần SELECT.</b> {@code SimpleJpaRepository.save}
 * rẽ nhánh theo {@link #isNew()}: mới thì {@code persist()} (INSERT thẳng), cũ thì
 * {@code merge()} (SELECT trước rồi mới UPDATE/INSERT). Vì id được gán tay nên luôn khác null;
 * nếu Spring Data chỉ xét id thì mọi entity đều bị coi là đã tồn tại, và mỗi lần tạo mới tốn thêm
 * một câu SELECT vô ích. {@link #isNew()} trả {@code version == null} để tránh việc đó.
 *
 * <p>Ghi chú: khi entity có {@code @Version} kiểu wrapper, cơ chế đoán mặc định của Spring Data
 * JPA cũng đã dùng đúng quy tắc {@code version == null}. Implement {@link Persistable} ở đây là
 * để logic nằm tường minh trong code, không phụ thuộc vào quy tắc đoán ngầm đó.
 *
 * <p>Vì vậy {@code version} phải là {@code Long} chứ không phải {@code long}: cần phân biệt "chưa
 * từng persist" ({@code null}) với "đã persist, đang ở version 0". Kiểu nguyên thuỷ mặc định về 0
 * và làm {@link #isNew()} luôn trả {@code false}.
 *
 * <h2>Khi map domain → entity (bắt buộc đọc)</h2>
 *
 * <ul>
 *   <li><b>Luôn mang {@code version} theo</b> khi update. Quên copy thì {@link #isNew()} trả
 *       {@code true}, Spring gọi {@code persist()} và kết quả là lỗi duplicate primary key thay vì
 *       UPDATE. Mang version cũ (đã có request khác update trước) sẽ ra
 *       {@code OptimisticLockingFailureException}, đây là hành vi mong muốn.</li>
 *   <li><b>Nên mang {@code createdAt} theo</b> nếu cần đọc lại. DB vẫn an toàn nhờ
 *       {@code updatable = false}, nhưng instance trả về sau {@code merge()} sẽ có
 *       {@code createdAt = null}. Setter của {@code createdAt}/{@code modifyAt} tồn tại chỉ để
 *       phục vụ việc map này, không dùng để gán giá trị audit bằng tay.</li>
 *   <li><b>Dùng object mà {@code save()} trả về.</b> Với entity detached, {@code merge()} trả về
 *       một instance khác; audit và version được cập nhật trên instance đó, không phải trên object
 *       truyền vào.</li>
 * </ul>
 *
 * <h2>Hệ quả về schema</h2>
 *
 * <p>{@code ddl-auto: update} thêm cột {@code version} nhưng KHÔNG backfill dữ liệu sẵn có. Bảng
 * đã có bản ghi thì phải chạy tay {@code UPDATE <bảng> SET version = 0 WHERE version IS NULL} một
 * lần, nếu không Hibernate coi các bản ghi cũ là entity mới.
 *
 * @param <ID> kiểu của khoá chính, thường là {@code UUID}
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class JpaEntity<ID> implements Persistable<ID> {

    /**
     * Thời điểm insert. Do auditing set lúc {@code @PrePersist}, không bao giờ bị UPDATE ghi đè.
     * {@code null} trước lần flush đầu tiên.
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private @Nullable Instant createdAt;

    /**
     * Thời điểm ghi gần nhất. Do auditing set lúc {@code @PrePersist} và {@code @PreUpdate}.
     * {@code null} trước lần flush đầu tiên.
     */
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private @Nullable Instant updatedAt;

    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private String createdBy;

    @LastModifiedBy
    @Column(name = "updated_by")
    private String updatedBy;
    /**
     * Version cho optimistic locking, do Hibernate quản lý: {@code null} khi chưa persist,
     * {@code 0} sau INSERT, tăng 1 sau mỗi UPDATE. Đồng thời là căn cứ của {@link #isNew()}.
     * Không tự tăng hay sửa giá trị này bằng tay; chỉ copy nguyên giá trị từ domain khi map.
     */
    @Version
    @Column(name = "version")
    private @Nullable Long version;

    /** @return thời điểm insert, hoặc {@code null} nếu entity chưa được flush lần nào */
    public @Nullable Instant getCreatedAt() {
        return createdAt;
    }

    /**
     * Chỉ dùng khi map domain → entity để giữ giá trị gốc. Không có tác dụng lên DB khi update
     * (cột {@code updatable = false}), và bị auditing ghi đè khi insert.
     */
    public void setCreatedAt(@Nullable Instant createdAt) {
        this.createdAt = createdAt;
    }

    /** @return thời điểm ghi gần nhất, hoặc {@code null} nếu entity chưa được flush lần nào */
    public @Nullable Instant getModifyAt() {
        return updatedAt;
    }

    /**
     * Chỉ dùng khi map domain → entity. Giá trị gán ở đây sẽ bị auditing ghi đè ở lần flush kế tiếp
     * nếu entity dirty.
     */
    public void setModifyAt(@Nullable Instant modifyAt) {
        this.updatedAt = modifyAt;
    }

    /** @return version hiện tại, hoặc {@code null} nếu entity chưa từng được persist */
    public @Nullable Long getVersion() {
        return version;
    }

    /**
     * Chỉ dùng khi map domain → entity: copy nguyên version mà domain đã đọc được. Gán
     * {@code null} cho một bản ghi đã tồn tại sẽ khiến {@code save()} cố INSERT và lỗi duplicate
     * key.
     */
    public void setVersion(@Nullable Long version) {
        this.version = version;
    }

    /**
     * Entity là mới khi chưa có version, tức là chưa từng được persist. Spring Data dùng kết quả
     * này để chọn {@code persist()} (INSERT thẳng) hay {@code merge()} (SELECT rồi mới ghi).
     *
     * @return {@code true} nếu {@code version == null}
     */
    @Override
    public boolean isNew() {
        return version == null;
    }
}