package kernel.ddd;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AggregateRootTest {

    static class Order extends AggregateRoot<String> {
        Order(String id) { super(id); }
        void fire(DomainEvent e) { raise(e); }
        void version(Long v) { assignVersion(v); }
    }

    record Evt(UUID eventId, Instant occurredOn) implements DomainEvent {
        Evt() {
            this(UUID.randomUUID(), Instant.now());
        }
    }

    static class Other extends AggregateRoot<String> {
        Other(String id) { super(id); }
    }

    @Test
    void equalityByClassAndId() {
        assertEquals(new Order("1"), new Order("1"));
        assertEquals(new Order("1").hashCode(), new Order("1").hashCode());
        assertNotEquals(new Order("1"), new Order("2"));
        assertNotEquals(new Order("1"), new Other("1"));
    }

    @Test
    void nullIdRejected() {
        assertThrows(NullPointerException.class, () -> new Order(null));
    }

    @Test
    void pullReturnsEventsInOrderThenClears() {
        Order o = new Order("1");
        DomainEvent a = new Evt();
        DomainEvent b = new Evt();
        o.fire(a);
        o.fire(b);
        List<DomainEvent> pulled = o.pullDomainEvents();
        assertEquals(List.of(a, b), pulled);
        assertTrue(o.pullDomainEvents().isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> pulled.add(a));
    }

    @Test
    void nullEventRejected() {
        assertThrows(NullPointerException.class, () -> new Order("1").fire(null));
    }

    @Test
    void versionStartsNullAndCanBeAssigned() {
        Order o = new Order("1");
        assertNull(o.getVersion());
        o.version(3L);
        assertEquals(3L, o.getVersion());
    }
}
