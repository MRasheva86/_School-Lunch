package app.service;

import app.expetion.ClientErrorException;
import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ClientErrorExceptionTest {

    private FeignException feignException;

    @BeforeEach
    void setUp() {
        feignException = mock(FeignException.class);
        when(feignException.status()).thenReturn(400);
        when(feignException.contentUTF8()).thenReturn("Cannot delete completed order");
    }

    @Test
    void shouldCreateClientErrorExceptionWithFeignException() {

        ClientErrorException exception = new ClientErrorException(feignException);

        assertNotNull(exception);
        assertEquals(feignException, exception.getFeignException());
        assertEquals(feignException, exception.getCause());
    }

    @Test
    void shouldPreserveFeignExceptionWhenCreatingClientErrorException() {

        FeignException customFeignException = mock(FeignException.class);
        when(customFeignException.status()).thenReturn(404);
        when(customFeignException.contentUTF8()).thenReturn("Lunch not found");

        ClientErrorException exception = new ClientErrorException(customFeignException);

        assertEquals(customFeignException, exception.getFeignException());
        assertEquals(404, exception.getFeignException().status());
        assertEquals("Lunch not found", exception.getFeignException().contentUTF8());
    }

    @Test
    void shouldExtendRuntimeException() {

        ClientErrorException exception = new ClientErrorException(feignException);

        assertTrue(exception instanceof RuntimeException);
    }

    @Test
    void shouldHaveFeignExceptionAsCause() {

        ClientErrorException exception = new ClientErrorException(feignException);

        assertEquals(feignException, exception.getCause());
        assertTrue(exception.getCause() instanceof FeignException);
    }

    @Test
    void shouldAllowAccessToFeignExceptionStatusCode() {

        when(feignException.status()).thenReturn(400);

        ClientErrorException exception = new ClientErrorException(feignException);

        assertEquals(400, exception.getFeignException().status());
    }

    @Test
    void shouldAllowAccessToFeignExceptionResponseBody() {

        String errorMessage = "Cannot delete completed order";
        when(feignException.contentUTF8()).thenReturn(errorMessage);

        ClientErrorException exception = new ClientErrorException(feignException);

        assertEquals(errorMessage, exception.getFeignException().contentUTF8());
    }

    @Test
    void shouldHandleDifferent4xxStatusCodes() {

        when(feignException.status()).thenReturn(400);

        ClientErrorException exception400 = new ClientErrorException(feignException);
        assertEquals(400, exception400.getFeignException().status());

        FeignException feign404 = mock(FeignException.class);
        when(feign404.status()).thenReturn(404);

        ClientErrorException exception404 = new ClientErrorException(feign404);
        assertEquals(404, exception404.getFeignException().status());

        FeignException feign422 = mock(FeignException.class);
        when(feign422.status()).thenReturn(422);

        ClientErrorException exception422 = new ClientErrorException(feign422);
        assertEquals(422, exception422.getFeignException().status());
    }

    @Test
    void shouldPreserveExceptionMessageFromFeignException() {

        String errorMessage = "Domain Exception: Cannot delete completed order";
        when(feignException.getMessage()).thenReturn(errorMessage);

        ClientErrorException exception = new ClientErrorException(feignException);

        assertNotNull(exception.getMessage());

        assertEquals(feignException, exception.getCause());
    }

    @Test
    void shouldMaintainExceptionChain() {

        String errorMessage = "Test error message";
        when(feignException.getMessage()).thenReturn(errorMessage);

        ClientErrorException exception = new ClientErrorException(feignException);

        assertNotNull(exception.getCause());
        assertEquals(feignException, exception.getCause());
        assertSame(feignException, exception.getFeignException());
    }

    @Test
    void constructor_shouldSetFeignExceptionAsCauseAndField() {

        FeignException testFeignException = mock(FeignException.class);
        when(testFeignException.status()).thenReturn(400);
        when(testFeignException.contentUTF8()).thenReturn("Test error");

        ClientErrorException exception = new ClientErrorException(testFeignException);

        assertNotNull(exception);
        assertSame(testFeignException, exception.getFeignException());
        assertSame(testFeignException, exception.getCause());
        assertEquals(400, exception.getFeignException().status());
        assertEquals("Test error", exception.getFeignException().contentUTF8());
    }
}

