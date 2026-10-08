package kernel.query;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PageResultTest {

    @Test
    void totalPagesRoundsUp() {
        assertEquals(0, PageResult.of(List.of(), 0, 0, 10).totalPages());
        assertEquals(1, PageResult.of(List.of(1), 10, 0, 10).totalPages());
        assertEquals(2, PageResult.of(List.of(1), 11, 0, 10).totalPages());
    }

    @Test
    void mapKeepsPaging() {
        PageResult<String> r = PageResult.of(List.of(1, 2), 25, 1, 2).map(String::valueOf);
        assertEquals(List.of("1", "2"), r.content());
        assertEquals(25, r.totalElements());
        assertEquals(1, r.page());
        assertEquals(13, r.totalPages());
    }

    @Test
    void emptyKeepsQueryPaging() {
        SearchQuery q = new SearchQuery(3, 15, List.of(), List.of());
        PageResult<Object> r = PageResult.empty(q);
        assertEquals(3, r.page());
        assertEquals(15, r.limit());
        assertTrue(r.content().isEmpty());
    }

    @Test
    void contentIsImmutable() {
        assertThrows(UnsupportedOperationException.class, () -> PageResult.of(List.of(1), 1, 0, 1).content().add(2));
    }
}
