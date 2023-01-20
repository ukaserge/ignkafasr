package limdongjin.stomasr.service;

import com.google.protobuf.InvalidProtocolBufferException;
import limdongjin.stomasr.protos.InferProto;
import limdongjin.stomasr.repository.AuthRepository;
import limdongjin.stomasr.stomp.MessageDestinationPrefixConstants;
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

    AuthRepository authRepository;
    SuccService succService;

    @BeforeEach
    void setUp() {
        this.authRepository = Mockito.spy(new AuthRepository());
        this.succService = new SuccService(messageSendingOperations, authRepository);
    }

    @Test
    void throwIfReceivedInvalidPayload() {
        byte[] payload2 = "12345".getBytes();
        byte[] payload3 = InferProto.Infer.newBuilder().setReqId(UUID.randomUUID().toString()).setInferResult("").build().toByteArray();
        byte[] payload4 = InferProto.Infer.newBuilder().setReqId("12345-12345-3432-222").setInferResult(OK_MSG).build().toByteArray();

        assertThrows(InvalidProtocolBufferException.class, () -> succService.onInfer(payload2));
        assertThrows(IllegalArgumentException.class, () -> succService.onInfer(payload3));
        assertThrows(IllegalArgumentException.class, () -> succService.onInfer(payload4));

        assertEquals(0, authRepository.size());
    }

    @Test
    void whenOnInferThenWillSendMessageAndSaveReqId() throws InvalidProtocolBufferException {
        String reqId = UUID.randomUUID().toString();
        byte[] payload = InferProto.Infer.newBuilder().setReqId(reqId).setInferResult(OK_MSG).build().toByteArray();
        // eg, "471ac3dc-99d6-4b02-930e-3cf4ef24c0cf,OK;"

        succService.onInfer(payload);

        Mockito.verify(messageSendingOperations, Mockito.times(1)).convertAndSend(Mockito.eq( MessageDestinationPrefixConstants.SUCC + reqId), Mockito.anyString());
        Mockito.verify(authRepository, Mockito.times(1)).putIfAbsent(Mockito.eq(reqId), Mockito.anyString());
    }

    @Test
    void whenOnInferThenInsertReqIdIntoAuthRepository() throws InvalidProtocolBufferException {
        String reqId = UUID.randomUUID().toString();
        byte[] payload = InferProto.Infer.newBuilder().setReqId(reqId).setInferResult(OK_MSG).build().toByteArray();

        assertFalse(authRepository.containsKey(reqId));

        succService.onInfer(payload);

        assertTrue(authRepository.containsKey(reqId));
    }
}