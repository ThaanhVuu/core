package kernel.query;

import kernel.common.AppException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SearchSpecTest {

    enum Status { ACTIVE, LOCKED }

    static class Base { UUID id; }
    static class User extends Base {
        String name;
        Integer age;
        Status status;
        Instant createdAt;
        boolean flag;
    }

    static final SearchSpec SPEC = SearchSpec.builder(User.class)
            .filter("name", Operator.CONTAINS)
            .filter("age", Operator.GTE, Operator.LTE, Operator.EQ)
            .filter("status", Operator.IN)
            .filter("flag", Operator.EQ)
            .sortable("name", "createdAt")
            .defaultSort("createdAt", SortDirection.DESC)
            .defaultLimit(20)
            .maxLimit(50)
            .build();

    private static SearchRequest req(Integer page, Integer limit, String sortBy, String sortDir, FilterInput... f) {
        return new SearchRequest(page, limit, List.of(f), sortBy, sortDir);
    }

    @Test
    void nullAndEmptyRequestUseDefaults() {
        for (SearchRequest r : new SearchRequest[]{null, SearchRequest.empty()}) {
            SearchQuery q = SPEC.resolve(r);
            assertEquals(0, q.page());
            assertEquals(20, q.limit());
            assertTrue(q.criteria().isEmpty());
            assertEquals(List.of(new SortOrder("createdAt", SortDirection.DESC),
                    new SortOrder("id", SortDirection.ASC)), q.sort());
        }
    }

    @Test
    void pageAndLimitAreClamped() {
        assertEquals(0, SPEC.resolve(req(-5, null, null, null)).page());
        assertEquals(3, SPEC.resolve(req(3, null, null, null)).page());
        assertEquals(20, SPEC.resolve(req(null, 0, null, null)).limit());
        assertEquals(20, SPEC.resolve(req(null, -1, null, null)).limit());
        assertEquals(50, SPEC.resolve(req(null, 9999, null, null)).limit());
        assertEquals(10, SPEC.resolve(req(null, 10, null, null)).limit());
    }

    @Test
    void filtersAreConvertedAndTrimmed() {
        SearchQuery q = SPEC.resolve(req(null, null, null, null,
                new FilterInput("name", "  bob "),
                new FilterInput("age", "18"),
                new FilterInput("flag", "true")));
        assertEquals(List.of(
                new Criterion("name", Operator.CONTAINS, "bob"),
                new Criterion("age", Operator.GTE, 18),
                new Criterion("flag", Operator.EQ, true)), q.criteria());
    }

    @Test
    void unknownBlankAndNullFiltersAreIgnored() {
        SearchQuery q = SPEC.resolve(req(null, null, null, null,
                new FilterInput("password", "x"),
                new FilterInput("name", "  "),
                new FilterInput("name", null),
                new FilterInput(null, "x"),
                new FilterInput(" ", "x")));
        assertTrue(q.criteria().isEmpty());
    }

    @Test
    void wrongTypeThrowsQueryException() {
        QueryException e = assertThrows(QueryException.class,
                () -> SPEC.resolve(req(null, null, null, null, new FilterInput("age", "abc"))));
        assertSame(QueryErrorCode.QUERY_FILTER_VALUE_INVALID, e.getErrorCode());
        assertInstanceOf(AppException.class, e);
    }

    @Test
    void inSplitsByCommaAndSkipsEmptyParts() {
        SearchQuery q = SPEC.resolve(req(null, null, null, null, new FilterInput("status", "active, locked,,")));
        assertEquals(List.of(Status.ACTIVE, Status.LOCKED), q.criteria().get(0).value());
    }

    @Test
    void inWithOnlySeparatorsIsIgnored() {
        SearchQuery q = SPEC.resolve(req(null, null, null, null, new FilterInput("status", " , ,")));
        assertTrue(q.criteria().isEmpty());
    }

    @Test
    void inRejectsTooManyValues() {
        String many = String.join(",", Collections.nCopies(SearchSpec.MAX_IN_VALUES + 1, "active"));
        QueryException e = assertThrows(QueryException.class,
                () -> SPEC.resolve(req(null, null, null, null, new FilterInput("status", many))));
        assertSame(QueryErrorCode.QUERY_TOO_MANY_VALUES, e.getErrorCode());
    }

    @Test
    void inAcceptsExactlyMaxValues() {
        String max = String.join(",", Collections.nCopies(SearchSpec.MAX_IN_VALUES, "active"));
        assertDoesNotThrow(() -> SPEC.resolve(req(null, null, null, null, new FilterInput("status", max))));
    }

    @Test
    void inWithBadElementThrows() {
        assertThrows(QueryException.class,
                () -> SPEC.resolve(req(null, null, null, null, new FilterInput("status", "active,bogus"))));
    }

    @Test
    void clientSortOverridesDefaultAndDefaultsToAsc() {
        SearchQuery q = SPEC.resolve(req(null, null, "name", null));
        assertEquals(new SortOrder("name", SortDirection.ASC), q.sort().get(0));
        assertEquals(new SortOrder("id", SortDirection.ASC), q.sort().get(1));
    }

    @Test
    void sortDirectionIsCaseInsensitive() {
        assertEquals(SortDirection.DESC, SPEC.resolve(req(null, null, "name", " desc ")).sort().get(0).direction());
    }

    @Test
    void invalidSortDirectionThrows() {
        QueryException e = assertThrows(QueryException.class, () -> SPEC.resolve(req(null, null, "name", "sideways")));
        assertSame(QueryErrorCode.QUERY_SORT_DIRECTION_INVALID, e.getErrorCode());
    }

    @Test
    void unknownSortFieldFallsBackToDefaultField() {
        SortOrder first = SPEC.resolve(req(null, null, "password", null)).sort().get(0);
        assertEquals(new SortOrder("createdAt", SortDirection.DESC), first);
    }

    @Test
    void tieBreakerNotDuplicatedWhenSortingById() {
        SearchSpec spec = SearchSpec.builder(User.class).sortable("id").build();
        assertEquals(1, spec.resolve(req(null, null, "id", "desc")).sort().size());
    }

    @Test
    void tieBreakerCanBeDisabled() {
        SearchSpec spec = SearchSpec.builder(User.class).tieBreaker(null).build();
        assertTrue(spec.resolve(null).sort().isEmpty());
    }

    @Test
    void builderRejectsBadConfig() {
        assertThrows(IllegalArgumentException.class,
                () -> SearchSpec.builder(User.class).filter("missing", Operator.EQ));
        assertThrows(IllegalArgumentException.class,
                () -> SearchSpec.builder(User.class).filter("age", Operator.CONTAINS));
        assertThrows(IllegalArgumentException.class, () -> SearchSpec.builder(User.class)
                .filter("name", Operator.EQ).filter("name", Operator.CONTAINS));
        assertThrows(IllegalArgumentException.class, () -> SearchSpec.builder(User.class).sortable("name", "name"));
        assertThrows(IllegalArgumentException.class, () -> SearchSpec.builder(User.class).tieBreaker("nope").build());
        assertThrows(IllegalStateException.class, () -> SearchSpec.builder(User.class).maxLimit(0).build());
        assertThrows(IllegalStateException.class, () -> SearchSpec.builder(User.class).defaultLimit(500).build());
        assertThrows(IllegalStateException.class, () -> SearchSpec.builder(User.class)
                .defaultSort("name", SortDirection.ASC).build());
        assertThrows(NullPointerException.class, () -> SearchSpec.builder(null));
    }

    @Test
    void toBuilderCopiesConfigurationWithoutMutatingOriginal() {
        SearchSpec extended = SPEC.toBuilder().filter("createdAt", Operator.LTE).build();
        SearchRequest r = req(null, null, null, null, new FilterInput("createdAt", "2026-01-01T00:00:00Z"));
        assertTrue(SPEC.resolve(r).criteria().isEmpty());
        assertEquals(1, extended.resolve(r).criteria().size());
        assertEquals(1, extended.resolve(req(null, null, null, null, new FilterInput("name", "a"))).criteria().size());
    }

    @Test
    void searchQueryHelpers() {
        SearchQuery q = SPEC.resolve(null);
        SearchQuery q2 = q.withCriterion("status", Operator.EQ, Status.ACTIVE);
        assertTrue(q.criteria().isEmpty());
        assertEquals(1, q2.criteria().size());
        assertEquals(40, new SearchQuery(2, 20, List.of(), List.of()).offset());
        assertThrows(IllegalArgumentException.class, () -> new SearchQuery(-1, 20, List.of(), List.of()));
        assertThrows(IllegalArgumentException.class, () -> new SearchQuery(0, 0, List.of(), List.of()));
    }

    @Test
    void clientCanPickAllowedOperatorCaseInsensitively() {
        SearchQuery q = SPEC.resolve(req(null, null, null, null,
                new FilterInput("age", "gte", "18"),
                new FilterInput("age", " LTE ", "60"),
                new FilterInput("age", "30")));
        assertEquals(List.of(
                new Criterion("age", Operator.GTE, 18),
                new Criterion("age", Operator.LTE, 60),
                new Criterion("age", Operator.GTE, 30)), q.criteria());
    }

    @Test
    void operatorOutsideWhitelistIsRejected() {
        QueryException e = assertThrows(QueryException.class,
                () -> SPEC.resolve(req(null, null, null, null, new FilterInput("age", "gt", "18"))));
        assertSame(QueryErrorCode.QUERY_OPERATOR_NOT_ALLOWED, e.getErrorCode());
        assertThrows(QueryException.class,
                () -> SPEC.resolve(req(null, null, null, null, new FilterInput("age", "drop table", "18"))));
    }

    @Test
    void rangeOperatorsAreRejectedForEnumAndBoolean() {
        for (Operator op : new Operator[]{Operator.GT, Operator.GTE, Operator.LT, Operator.LTE}) {
            assertThrows(IllegalArgumentException.class, () -> SearchSpec.builder(User.class).filter("status", op));
            assertThrows(IllegalArgumentException.class, () -> SearchSpec.builder(User.class).filter("flag", op));
            assertThrows(IllegalArgumentException.class,
                    () -> SearchSpec.builder(User.class).filter("status", Operator.EQ, op));
        }
        assertDoesNotThrow(() -> SearchSpec.builder(User.class).filter("status", Operator.EQ, Operator.NE, Operator.IN));
    }

    @Test
    void duplicateOperatorInDeclarationIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> SearchSpec.builder(User.class).filter("age", Operator.GTE, Operator.GTE));
    }

    @Test
    void textOperatorsStillNeedStringProperty() {
        assertThrows(IllegalArgumentException.class,
                () -> SearchSpec.builder(User.class).filter("age", Operator.EQ, Operator.CONTAINS));
    }
}
