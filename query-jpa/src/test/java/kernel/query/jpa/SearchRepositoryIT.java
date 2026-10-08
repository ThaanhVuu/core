package kernel.query.jpa;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import kernel.query.FilterInput;
import kernel.query.PageResult;
import kernel.query.SearchRequest;
import kernel.query.SearchSpec;
import kernel.query.Operator;
import kernel.query.SortDirection;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Chạy SearchRepository và JpaSpecifications trên H2 in-memory với Hibernate thật. */
class SearchRepositoryIT {

    public enum Status { ACTIVE, LOCKED }

    @Entity(name = "Dept")
    public static class Dept {
        @Id Long id;
        String name;
        public Dept() {}
        Dept(Long id, String name) { this.id = id; this.name = name; }
    }

    @Entity(name = "Emp")
    public static class Emp {
        @Id Long id;
        String name;
        Integer age;
        @Enumerated(EnumType.STRING) Status status;
        Instant createdAt;
        @ManyToOne Dept department;
        public Emp() {}
        Emp(Long id, String name, Integer age, Status status, Instant createdAt, Dept department) {
            this.id = id; this.name = name; this.age = age; this.status = status;
            this.createdAt = createdAt; this.department = department;
        }
    }

    interface EmpRepository extends JpaRepository<Emp, Long>, SearchRepository<Emp> {
    }

    static final SearchSpec SPEC = SearchSpec.builder(Emp.class)
            .filter("name", Operator.CONTAINS)
            .filter("age", Operator.GTE, Operator.LTE)
            .filter("status", Operator.IN)
            .filter("department.name", Operator.EQ)
            .sortable("name", "age")
            .defaultSort("name", SortDirection.ASC)
            .defaultLimit(2)
            .build();

    static SessionFactory sessionFactory;
    static EntityManager em;
    static EmpRepository repo;

    @BeforeAll
    static void setUp() {
        sessionFactory = new Configuration()
                .addAnnotatedClass(Dept.class)
                .addAnnotatedClass(Emp.class)
                .setProperty("hibernate.connection.url", "jdbc:h2:mem:kernel;DB_CLOSE_DELAY=-1")
                .setProperty("hibernate.hbm2ddl.auto", "create-drop")
                .buildSessionFactory();
        em = sessionFactory.createEntityManager();

        Dept it = new Dept(1L, "IT");
        Dept hr = new Dept(2L, "HR");
        Instant t = Instant.parse("2026-01-01T00:00:00Z");
        em.getTransaction().begin();
        em.persist(it);
        em.persist(hr);
        em.persist(new Emp(1L, "Alice", 30, Status.ACTIVE, t, it));
        em.persist(new Emp(2L, "Bob", 25, Status.LOCKED, t, it));
        em.persist(new Emp(3L, "Carol", 40, Status.ACTIVE, t, hr));
        em.persist(new Emp(4L, "100%_done", 20, Status.ACTIVE, t, null));
        em.persist(new Emp(5L, "Dave", 35, Status.ACTIVE, t, null));
        em.getTransaction().commit();

        repo = new JpaRepositoryFactory(em).getRepository(EmpRepository.class);
    }

    @AfterAll
    static void tearDown() {
        em.close();
        sessionFactory.close();
    }

    private static PageResult<Emp> search(Integer page, Integer limit, String sortBy, String sortDir, FilterInput... f) {
        return repo.search(SPEC.resolve(new SearchRequest(page, limit, List.of(f), sortBy, sortDir)));
    }

    private static List<String> names(PageResult<Emp> r) {
        return r.content().stream().map(e -> e.name).toList();
    }

    @Test
    void noFilterReturnsFirstPageWithTotals() {
        PageResult<Emp> r = search(null, null, null, null);
        assertEquals(List.of("100%_done", "Alice"), names(r));
        assertEquals(5, r.totalElements());
        assertEquals(3, r.totalPages());
    }

    @Test
    void secondPageAndDescendingSort() {
        PageResult<Emp> r = search(1, 2, "age", "desc");
        assertEquals(List.of("Alice", "Bob"), names(r));
    }

    @Test
    void containsIsCaseInsensitive() {
        assertEquals(List.of("Alice"), names(search(null, null, null, null, new FilterInput("name", "ALI"))));
    }

    @Test
    void likeWildcardsFromClientAreLiteral() {
        // "%" và "_" phải được hiểu là ký tự thường, không khớp mọi thứ
        assertEquals(List.of("100%_done"), names(search(null, null, null, null, new FilterInput("name", "0%_d"))));
        assertEquals(1, search(null, null, null, null, new FilterInput("name", "%")).totalElements());
        assertTrue(names(search(null, null, null, null, new FilterInput("name", "_lice"))).isEmpty());
    }

    @Test
    void comparisonAndInFilters() {
        assertEquals(List.of("Carol", "Dave"),
                names(search(null, 10, "name", null, new FilterInput("age", "35"))));
        assertEquals(List.of("Bob"),
                names(search(null, 10, null, null, new FilterInput("status", "locked"))));
        assertEquals(5, search(null, 10, null, null, new FilterInput("status", "ACTIVE,LOCKED")).totalElements());
    }

    @Test
    void nestedFilterWorks() {
        assertEquals(List.of("Alice", "Bob"),
                names(search(null, 10, null, null, new FilterInput("department.name", "IT"))));
    }

    @Test
    void leftJoinKeepsRowsWithoutRelationWhenFilterNotApplied() {
        // lọc theo cột lồng khác "IT" không được làm mất tổng số ở truy vấn không lọc
        assertEquals(5, search(null, 10, null, null).totalElements());
        assertEquals(1, search(null, 10, null, null, new FilterInput("department.name", "HR")).totalElements());
    }

    @Test
    void extraScopeIsAndedWithCriteria() {
        var scope = (org.springframework.data.jpa.domain.Specification<Emp>)
                (root, q, cb) -> cb.equal(root.get("status"), Status.ACTIVE);
        PageResult<Emp> r = repo.search(
                SPEC.resolve(new SearchRequest(null, 10, List.of(new FilterInput("name", "a")), null, null)), scope);
        assertEquals(List.of("Alice", "Carol", "Dave"), names(r));
    }

    @Test
    void rangeFilterOnSameProperty() {
        PageResult<Emp> r = search(null, 10, "age", null,
                new FilterInput("age", "gte", "25"), new FilterInput("age", "lte", "35"));
        assertEquals(List.of("Bob", "Alice", "Dave"), names(r));
    }
}
