package limdongjin.stomasr.service;

import limdongjin.stomasr.repository.AuthRepository;
import limdongjin.stomasr.stomp.MessageDestinationPrefixConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.logging.Level;

@Component
public class SuccService {
    private static final Logger logger = LoggerFactory.getLogger(SuccService.class.getName());
    private SimpMessageSendingOperations messageSendingOperations;
    private AuthRepository authRepository;

//    public SuccService() {}
    public SuccService(SimpMessageSendingOperations messageSendingOperations, AuthRepository authRepository){
        this.messageSendingOperations = messageSendingOperations;
        this.authRepository = authRepository;
    }

    public void onInfer(String payload) throws IllegalArgumentException {
        String[] tmp = payload.split(",");
        if(tmp.length != 2){
            logger.error("invalid payload format");
            throw new IllegalArgumentException("invalid payload format");
        }

        String reqId = tmp[0];
        String msg = tmp[1];

        // check uuid format
        try {
            UUID uuid = UUID.fromString(reqId);
        } catch (IllegalArgumentException exception){
            logger.error("invalid uuid format");
            throw exception;
        }

        // check message format
        if(msg.isBlank()){
            throw new IllegalArgumentException("invalid message format");
        }

        messageSendingOperations.convertAndSend(MessageDestinationPrefixConstants.SUCC + reqId, msg);
        authRepository.putIfAbsent(reqId, msg);
    }

    public SimpMessageSendingOperations getMessageSendingOperations() {
        return messageSendingOperations;
    }
    public void setMessageSendingOperations(SimpMessageSendingOperations messageSendingOperations) {
        this.messageSendingOperations = messageSendingOperations;
    }
    public AuthRepository getAuthRepository() {
        return authRepository;
    }
    public void setAuthRepository(AuthRepository authRepository) {
        this.authRepository = authRepository;
    }
}
