package limdongjin.stomasr.kafka;

import limdongjin.stomasr.service.SuccService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SuccListenerTest {
    private SuccListener listener;
    private SuccService succService;

    @BeforeEach
    public void setUp() {
        this.succService = Mockito.mock(SuccService.class);
        this.listener = new SuccListener(this.succService);
    }

    @Test
    void listenerMustHandleNullPayload(){
        assertThrows(IllegalArgumentException.class, () -> {
            listener.onInfer(0L, null);
        });
    }

    @Test
    void whenListenThenInvokeSuccService() {
        final String OK_MSG = "OK; ";
        final String FAIL_MSG = "FAIL; ";

        // Given
        final String reqId = UUID.randomUUID().toString();
        final String payload1 = String.join(",",reqId, OK_MSG);

        final String reqId2 = UUID.randomUUID().toString();
        final String payload2 = String.join(",", reqId2, FAIL_MSG);

        // When
        listener.onInfer(0L, payload1);
        listener.onInfer(1L, payload2);

        // then
        verify(succService, times(1)).onInfer(payload1);
        verify(succService, times(1)).onInfer(payload2);
    }
}