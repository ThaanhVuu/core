package kernel.query.jpa;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JpaSupportTest {

    static class Item extends JpaEntity<UUID> {
        private final UUID id = UUID.randomUUID();

        @Override
        public UUID getId() {
            return id;
        }
    }

    @Test
    void escapeLikeLowercasesAndEscapesWildcards() {
        assertEquals("abc", JpaSpecifications.escapeLike("AbC"));
        assertEquals("100\\%", JpaSpecifications.escapeLike("100%"));
        assertEquals("a\\_b", JpaSpecifications.escapeLike("a_b"));
        assertEquals("a\\\\b", JpaSpecifications.escapeLike("a\\b"));
        assertEquals("\\\\\\%\\_", JpaSpecifications.escapeLike("\\%_"));
    }

    @Test
    void entityIsNewOnlyWhileVersionIsNull() {
        Item item = new Item();
        assertTrue(item.isNew());
        item.setVersion(0L);
        assertFalse(item.isNew());
        item.setVersion(null);
        assertTrue(item.isNew());
    }
}
