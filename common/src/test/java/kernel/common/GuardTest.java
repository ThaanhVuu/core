package kernel.common;

import org.junit.jupiter.api.Test;

import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class GuardTest {

    @Test
    void notNullReturnsValueOrThrowsRequired() {
        assertEquals("a", Guard.notNull("a", "f"));
        AppException e = assertThrows(AppException.class, () -> Guard.notNull(null, "f"));
        assertSame(CommonError.REQUIRED, e.getErrorCode());
    }

    @Test
    void notBlankRejectsNullEmptyAndWhitespace() {
        for (String bad : new String[]{null, "", "   "}) {
            AppException e = assertThrows(AppException.class, () -> Guard.notBlank(bad, "f"));
            assertSame(CommonError.REQUIRED, e.getErrorCode());
        }
        assertEquals(" x ", Guard.notBlank(" x ", "f"));
    }

    @Test
    void lengthBetweenIncludesBounds() {
        assertEquals("ab", Guard.lengthBetween("ab", 2, 3, "f"));
        assertEquals("abc", Guard.lengthBetween("abc", 2, 3, "f"));
        assertSame(CommonError.INVALID_LENGTH,
                assertThrows(AppException.class, () -> Guard.lengthBetween("a", 2, 3, "f")).getErrorCode());
        assertSame(CommonError.INVALID_LENGTH,
                assertThrows(AppException.class, () -> Guard.lengthBetween("abcd", 2, 3, "f")).getErrorCode());
        assertSame(CommonError.REQUIRED,
                assertThrows(AppException.class, () -> Guard.lengthBetween(null, 2, 3, "f")).getErrorCode());
    }

    @Test
    void inRangeIncludesBounds() {
        assertEquals(18, Guard.inRange(18, 18, 60, "age"));
        assertEquals(60, Guard.inRange(60, 18, 60, "age"));
        assertSame(CommonError.OUT_OF_RANGE,
                assertThrows(AppException.class, () -> Guard.inRange(17, 18, 60, "age")).getErrorCode());
        assertSame(CommonError.OUT_OF_RANGE,
                assertThrows(AppException.class, () -> Guard.inRange(61, 18, 60, "age")).getErrorCode());
    }

    @Test
    void matchesRequiresFullMatch() {
        Pattern digits = Pattern.compile("[0-9]+");
        assertEquals("123", Guard.matches("123", digits, "f"));
        assertSame(CommonError.INVALID_FORMAT,
                assertThrows(AppException.class, () -> Guard.matches("12a", digits, "f")).getErrorCode());
    }

    @Test
    void isTrueThrowsGivenCode() {
        Guard.isTrue(true, CommonError.INTERNAL);
        assertSame(CommonError.NOT_FOUND,
                assertThrows(AppException.class, () -> Guard.isTrue(false, CommonError.NOT_FOUND, "X", 1)).getErrorCode());
    }
}
