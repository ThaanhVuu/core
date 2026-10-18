package kernel.ddd;

import kernel.common.CommonError;

import java.time.Instant;
import java.util.Objects;

/**
 * Khoảng hiệu lực nửa mở {@code [effectiveAt, expiresAt)}.
 * {@code expiresAt == null} nghĩa là không có hạn.
 * {@code effectiveAt == null} nghĩa là có hiệu lực từ bây giờ.
 */
public record ValidityPeriod(Instant effectiveAt, Instant expiresAt) implements ValueObject {

    public ValidityPeriod {
        if (effectiveAt != null && expiresAt != null && !expiresAt.isAfter(effectiveAt)) {
            throw new DomainException(CommonError.VALIDITY_PERIOD_INVALID);
        }
    }

    /** Khoảng nửa mở [effectiveAt, expiresAt); null ở đầu nào là không giới hạn đầu đó. */
    public boolean contains(Instant t) {
        Objects.requireNonNull(t, "t must not be null");
        return (effectiveAt == null || !t.isBefore(effectiveAt))
                && (expiresAt == null || t.isBefore(expiresAt));
    }
}