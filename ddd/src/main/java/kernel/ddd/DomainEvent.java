package kernel.ddd;

import java.time.Instant;
import java.util.UUID;

/**
 * Sự kiện nghiệp vụ, ghi nhận một điều đã xảy ra trong domain.
 *
 * @see AggregateRoot#raise(DomainEvent)
 */
public interface DomainEvent {

    /**
     * Trả về định danh duy nhất của event.
     *
     * @return định danh của event
     */
    UUID eventId();

    /**
     * Trả về thời điểm event xảy ra.
     *
     * @return thời điểm xảy ra
     */
    Instant occurredOn();
}
