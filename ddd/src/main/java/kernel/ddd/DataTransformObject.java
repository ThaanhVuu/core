package kernel.ddd;

import java.time.Instant;

/**
 * Lớp cha của các DTO cần thông tin audit.
 * Nằm trong module ddd nên KHÔNG biết JpaEntity: nơi map (tầng infrastructure) tự đưa giá trị vào.
 */
public abstract class DataTransformObject {

    private Instant createdAt;
    private Instant modifyAt;
    private String createdBy;
    private String modifiedBy;

    /** Gán audit một lần, trả về chính DTO để gọi nối tiếp. */
    public <T extends DataTransformObject> T withAudit(Instant createdAt, Instant modifyAt,
                                                       String createdBy, String modifiedBy) {
        this.createdAt = createdAt;
        this.modifyAt = modifyAt;
        this.createdBy = createdBy;
        this.modifiedBy = modifiedBy;
        @SuppressWarnings("unchecked") T self = (T) this;
        return self;
    }

    public Instant getCreatedAt()  { return createdAt; }
    public Instant getModifyAt()   { return modifyAt; }
    public String getCreatedBy()   { return createdBy; }
    public String getModifiedBy()  { return modifiedBy; }
}