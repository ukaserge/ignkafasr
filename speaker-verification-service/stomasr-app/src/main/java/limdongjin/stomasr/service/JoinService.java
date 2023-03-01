package limdongjin.stomasr.service;

import limdongjin.stomasr.stomp.StompConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Component;

@Component
public class JoinService {
    final static Logger logger = LoggerFactory.getLogger(JoinService.class);
    private final SimpMessageSendingOperations messageSendingOperations;
    public JoinService(final SimpMessageSendingOperations messageSendingOperations){
        this.messageSendingOperations = messageSendingOperations;
    }

    public void join(final String userName){
        logger.info("JOIN "+userName);
        messageSendingOperations.convertAndSend(StompConstants.JOINOK + userName, "HELLO; ");
    }
}
