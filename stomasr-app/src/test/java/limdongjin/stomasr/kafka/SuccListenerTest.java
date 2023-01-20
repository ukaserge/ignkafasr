package limdongjin.stomasr.kafka;

import com.google.protobuf.InvalidProtocolBufferException;
import limdongjin.stomasr.protos.InferProto;
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
    void whenListenThenInvokeSuccService() throws InvalidProtocolBufferException {
        final String OK_MSG = "OK; ";
        final String FAIL_MSG = "FAIL; ";

        // Given
        final byte[] payload1 = InferProto.Infer.newBuilder().setReqId(UUID.randomUUID().toString()).setInferResult(OK_MSG).build().toByteArray();
        final byte[] payload2 = InferProto.Infer.newBuilder().setReqId(UUID.randomUUID().toString()).setInferResult(FAIL_MSG).build().toByteArray();

        // When
        listener.onInfer(0L, payload1);
        listener.onInfer(1L, payload2);

        // then
        verify(succService, times(1)).onInfer(payload1);
        verify(succService, times(1)).onInfer(payload2);
    }
}