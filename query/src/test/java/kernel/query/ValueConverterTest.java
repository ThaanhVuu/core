package kernel.query;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ValueConverterTest {

    enum Color { RED, GREEN }

    private static Object parse(Class<?> type, String raw) {
        return ValueConverter.parserFor(type).apply(raw);
    }

    @Test
    void parsesSupportedTypes() {
        assertEquals("x", parse(String.class, "x"));
        assertEquals(1, parse(Integer.class, "1"));
        assertEquals((short) 2, parse(Short.class, "2"));
        assertEquals(3L, parse(Long.class, "3"));
        assertEquals(1.5f, parse(Float.class, "1.5"));
        assertEquals(2.5, parse(Double.class, "2.5"));
        assertEquals(new BigDecimal("9.99"), parse(BigDecimal.class, "9.99"));
        UUID id = UUID.randomUUID();
        assertEquals(id, parse(UUID.class, id.toString()));
        assertEquals(Instant.parse("2026-01-01T00:00:00Z"), parse(Instant.class, "2026-01-01T00:00:00Z"));
        assertEquals(LocalDate.of(2026, 1, 2), parse(LocalDate.class, "2026-01-02"));
        assertEquals(LocalDateTime.of(2026, 1, 2, 3, 4), parse(LocalDateTime.class, "2026-01-02T03:04"));
        assertEquals(OffsetDateTime.parse("2026-01-02T03:04:00+07:00"),
                parse(OffsetDateTime.class, "2026-01-02T03:04:00+07:00"));
        assertInstanceOf(ZonedDateTime.class,
                parse(ZonedDateTime.class, "2026-01-02T03:04:00+07:00[Asia/Ho_Chi_Minh]"));
    }

    @Test
    void booleanIsStrict() {
        assertEquals(true, parse(Boolean.class, "TRUE"));
        assertEquals(false, parse(Boolean.class, "false"));
        assertThrows(IllegalArgumentException.class, () -> parse(Boolean.class, "yes"));
    }

    @Test
    void enumIsCaseInsensitive() {
        assertEquals(Color.GREEN, parse(Color.class, "green"));
        assertThrows(IllegalArgumentException.class, () -> parse(Color.class, "blue"));
    }

    @Test
    void unsupportedTypeRejected() {
        assertThrows(IllegalArgumentException.class, () -> ValueConverter.parserFor(Thread.class));
    }
}
