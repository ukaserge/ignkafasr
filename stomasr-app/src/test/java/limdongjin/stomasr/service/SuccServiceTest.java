package limdongjin.stomasr.service;

import com.google.protobuf.InvalidProtocolBufferException;
import limdongjin.stomasr.protos.InferProto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessageSendingOperations;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class SuccServiceTest {
    final static String OK_MSG = "OK; ";
    final static String FAIL_MSG = "FAIL; ";
    @Mock
    SimpMessageSendingOperations messageSendingOperations;
    SuccService succService;

    @BeforeEach
    void setUp() {
        this.succService = new SuccService(messageSendingOperations);
    }

    @Test
    void throwIfReceivedInvalidPayload() {
        byte[] payload2 = "12345".getBytes();
        byte[] payload3 = InferProto.Infer.newBuilder().setReqId(UUID.randomUUID().toString()).setInferResult("").build().toByteArray();
        byte[] payload4 = InferProto.Infer.newBuilder().setReqId("12345-12345-3432-222").setInferResult(OK_MSG).build().toByteArray();

        assertThrows(InvalidProtocolBufferException.class, () -> succService.onInferVerif(payload2));
        assertThrows(IllegalArgumentException.class, () -> succService.onInferVerif(payload3));
        assertThrows(IllegalArgumentException.class, () -> succService.onInferVerif(payload4));
    }

    @Test
    void whenOnInferThenWillSendMessageAndSaveReqId() throws InvalidProtocolBufferException {
        String reqId = UUID.randomUUID().toString();
        String userId = UUID.randomUUID().toString();
        byte[] payload = InferProto.Infer.newBuilder().setUserId(userId).setReqId(reqId).setInferResult(OK_MSG).build().toByteArray();
        // eg, "471ac3dc-99d6-4b02-930e-3cf4ef24c0cf,OK;"

        succService.onInferVerif(payload);

        Mockito.verify(messageSendingOperations, Mockito.times(1)).convertAndSendToUser(Mockito.eq(userId), Mockito.anyString(), Mockito.anyString());
    }
}