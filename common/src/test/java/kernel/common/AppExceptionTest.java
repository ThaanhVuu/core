package kernel.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AppExceptionTest {

    @Test
    void formatsMessageFromErrorCode() {
        AppException e = new AppException(CommonError.NOT_FOUND, "Order", 42);
        assertSame(CommonError.NOT_FOUND, e.getErrorCode());
        assertEquals("Order not found: 42", e.getMessage());
    }

    @Test
    void mismatchedArgsFallBackToRawTemplateInsteadOfThrowing() {
        AppException e = assertDoesNotThrow(() -> new AppException(CommonError.NOT_FOUND, "only-one"));
        assertNotNull(e.getMessage());
    }

    @Test
    void keepsCause() {
        Throwable cause = new IllegalStateException("boom");
        AppException e = new AppException(CommonError.INTERNAL, cause);
        assertSame(cause, e.getCause());
    }

    @Test
    void nullErrorCodeIsRejected() {
        assertThrows(NullPointerException.class, () -> new AppException(null));
    }
}
