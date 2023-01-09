package limdongjin.stomasr.service;

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
        String payload1 = "";
        String payload2 = "12345";
        String payload3 = UUID.randomUUID().toString();
        String payload4 = String.join(",", "12345-12345-3432-222", OK_MSG); // invalid uuid format
        String payload5 = String.join(",", UUID.randomUUID().toString(), " "); // invalid message format

        assertThrows(IllegalArgumentException.class, () -> succService.onInfer(payload1));
        assertThrows(IllegalArgumentException.class, () -> succService.onInfer(payload2));
        assertThrows(IllegalArgumentException.class, () -> succService.onInfer(payload3));
        assertThrows(IllegalArgumentException.class, () -> succService.onInfer(payload4));
        assertThrows(IllegalArgumentException.class, () -> succService.onInfer(payload5));

        assertEquals(0, authRepository.size());
    }

    @Test
    void whenOnInferThenWillSendMessageAndSaveReqId() {
        String reqId = UUID.randomUUID().toString();
        String msg = OK_MSG;
        String payload = String.join(",", reqId, msg);
        // eg, "471ac3dc-99d6-4b02-930e-3cf4ef24c0cf,OK;"

        succService.onInfer(payload);

        Mockito.verify(messageSendingOperations, Mockito.times(1)).convertAndSend(MessageDestinationPrefixConstants.SUCC + reqId, msg);
        Mockito.verify(authRepository, Mockito.times(1)).putIfAbsent(reqId, msg);
    }

    @Test
    void whenOnInferThenInsertReqIdIntoAuthRepository() {
        String reqId = UUID.randomUUID().toString();
        String msg = OK_MSG;
        String payload = String.join(",", reqId, msg);

        assertFalse(authRepository.containsKey(reqId));

        succService.onInfer(payload);

        assertTrue(authRepository.containsKey(reqId));
    }
}