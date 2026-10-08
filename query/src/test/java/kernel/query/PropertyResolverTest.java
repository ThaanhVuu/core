package kernel.query;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PropertyResolverTest {

    static class Base { Long id; }
    static class Dept { String name; }
    static class User extends Base {
        int age;
        boolean active;
        short level;
        float score;
        Dept department;
        List<Dept> history;
    }

    @Test
    void resolvesPlainInheritedAndNested() {
        assertEquals(Long.class, PropertyResolver.resolveType(User.class, "id"));
        assertEquals(String.class, PropertyResolver.resolveType(User.class, "department.name"));
    }

    @Test
    void wrapsPrimitives() {
        assertEquals(Integer.class, PropertyResolver.resolveType(User.class, "age"));
        assertEquals(Boolean.class, PropertyResolver.resolveType(User.class, "active"));
        assertEquals(Short.class, PropertyResolver.resolveType(User.class, "level"));
        assertEquals(Float.class, PropertyResolver.resolveType(User.class, "score"));
    }

    @Test
    void rejectsUnknownAndCollectionTraversal() {
        assertThrows(IllegalArgumentException.class, () -> PropertyResolver.resolveType(User.class, "nope"));
        assertThrows(IllegalArgumentException.class, () -> PropertyResolver.resolveType(User.class, "history.name"));
        assertEquals(List.class, PropertyResolver.resolveType(User.class, "history"));
    }
}
